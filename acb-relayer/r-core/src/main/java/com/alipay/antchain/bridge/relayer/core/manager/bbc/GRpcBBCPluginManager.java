package com.alipay.antchain.bridge.relayer.core.manager.bbc;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.pluginserver.service.CrossChainServiceGrpc;
import com.alipay.antchain.bridge.pluginserver.service.Empty;
import com.alipay.antchain.bridge.pluginserver.service.Response;
import com.alipay.antchain.bridge.relayer.commons.constant.PluginServerStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.PluginServerDO;
import com.alipay.antchain.bridge.relayer.commons.model.PluginServerInfo;
import com.alipay.antchain.bridge.relayer.core.types.pluginserver.GRpcBBCServiceClient;
import com.alipay.antchain.bridge.relayer.core.types.pluginserver.GRpcPluginServerClient;
import com.alipay.antchain.bridge.relayer.core.types.pluginserver.IBBCServiceClient;
import com.alipay.antchain.bridge.relayer.core.types.pluginserver.IPluginServerClient;
import com.alipay.antchain.bridge.relayer.core.types.pluginserver.exception.PluginServerConnectionFailException;
import com.alipay.antchain.bridge.relayer.core.types.pluginserver.exception.PluginServerRegistrationFailException;
import com.alipay.antchain.bridge.relayer.core.utils.PluginServerUtils;
import com.alipay.antchain.bridge.relayer.dal.repository.IPluginServerRepository;
import com.google.common.collect.Sets;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.OpenSsl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public class GRpcBBCPluginManager implements IBBCPluginManager {

    private final ExecutorService clientExecutorService;

    private final ScheduledExecutorService heartbeatExecutorService;

    private final Resource tlsClientKeyFile;

    private final Resource tlsClientCaFile;

    private IPluginServerRepository pluginServerRepository;

    private TransactionTemplate transactionTemplate;

    private final Map<String, IPluginServerClient> pluginServerClientMap = new ConcurrentHashMap<>();

    private final Map<String, ScheduledFuture> heartbeatFutureMap = new HashMap<>();

    private final Map<String, CrossChainServiceGrpc.CrossChainServiceBlockingStub> blockingStubMap = new HashMap<>();

    private final long heartbeatDelayedTime;

    private final int errorLimitForHeartbeat;

    public GRpcBBCPluginManager(
            Resource clientKeyPath,
            Resource clientCaPath,
            IPluginServerRepository pluginServerRepository,
            TransactionTemplate transactionTemplate,
            ExecutorService clientExecutorService,
            ScheduledExecutorService heartbeatExecutorService,
            long heartbeatDelayedTime,
            int errorLimitForHeartbeat
    ) {
        this.tlsClientKeyFile = clientKeyPath;
        this.tlsClientCaFile = clientCaPath;
        this.pluginServerRepository = pluginServerRepository;
        this.transactionTemplate = transactionTemplate;
        this.clientExecutorService = clientExecutorService;
        this.heartbeatExecutorService = heartbeatExecutorService;
        this.heartbeatDelayedTime = heartbeatDelayedTime;
        this.errorLimitForHeartbeat = errorLimitForHeartbeat;
    }

    /**
     * 创建插件服务的client
     * <pre>
     *     处理逻辑：
     *      1. 获取指定 id 的插件服务 pluginServerDO （应当已存在且为ready状态）
     *      2. 获取插件服务 pluginServerDO 的 client
     *      3. 如果 client 不存在则执行 startPluginServerClient 创建并连接插件（同时添加心跳、更新数据库）
     *     使用场景：
     *      区块链获取插件时需要先判断插件 client 是否存在，不存在时调用当前方法创建 client
     * </pre>
     *
     * @param psId
     * @param product
     * @param domain
     * @return
     */
    @Override
    public IBBCServiceClient createBBCClient(String psId, String product, String domain) {
        PluginServerDO pluginServerDO = this.pluginServerRepository.getPluginServer(psId);
        if (ObjectUtil.isEmpty(pluginServerDO)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_PLUGIN_SERVER_ERROR,
                    String.format("data of plugin server %s not found", psId)
            );
        }
        if (PluginServerStateEnum.READY != pluginServerDO.getState()) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_PLUGIN_SERVER_ERROR,
                    String.format("Plugin server %s not started", psId)
            );
        }

        IPluginServerClient psClient = this.getPluginServerClient(psId);
        if (ObjectUtil.isNull(psClient)) {
            log.info("client of plugin server {} not started and start it now", psId);
            this.startPluginServerClient(pluginServerDO);
        }
        if (!this.checkIfProductSupport(psId, product)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_PLUGIN_SERVER_ERROR,
                    String.format("Plugin server %s supports no product %s", psId, product)
            );
        }

        return new GRpcBBCServiceClient(psId, product, domain, this.blockingStubMap.get(psId));
    }

    private boolean checkIfProductSupport(String psId, String product) {
        IPluginServerClient psClient = this.getPluginServerClient(psId);
        if (ObjectUtil.isNull(psClient)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_PLUGIN_SERVER_ERROR,
                    String.format("client of plugin server %s not started and start it now", psId)
            );
        }

        Map<String, Boolean> res = psClient.ifProductSupport(ListUtil.toList(product));
        if (ObjectUtil.isEmpty(res)) {
            return false;
        }
        return res.getOrDefault(product, false);
    }

    /**
     * 注册指定id的插件服务
     * <pre>
     *     处理逻辑：
     *      1.获取指定 id 的插件服务 pluginServerDO （应当不存在）
     *      2.创建 pluginServerDO 并设置为 init 状态
     *      3.创建 pluginServerDO 的 client （调用 startPluginServerClient 创建并连接）
     *     使用场景：
     *      首次注册插件服务，注册后不需要 startPluginServer
     * </pre>
     *
     * @param psId
     * @param address
     * @param properties
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerPluginServer(String psId, String address, String properties) {
        PluginServerDO pluginServerDO = pluginServerRepository.getPluginServer(psId);
        if (ObjectUtil.isNotEmpty(pluginServerDO)) {
            throw new PluginServerRegistrationFailException(String.format("plugin server %s already registered", psId));
        }

        pluginServerDO = new PluginServerDO();
        pluginServerDO.setPsId(psId);
        pluginServerDO.setAddress(address);
        pluginServerDO.setState(PluginServerStateEnum.INIT);
        pluginServerDO.setProperties(PluginServerDO.PluginServerProperties.decode(properties.getBytes()));

        pluginServerRepository.insertNewPluginServer(pluginServerDO);
        log.info("Plugin server {} has been registered as INIT", psId);

        startPluginServerClient(pluginServerDO);
        log.info("Plugin server {} works READY as required", psId);
    }

    private CrossChainServiceGrpc.CrossChainServiceBlockingStub tryConnectionWithPluginServer(PluginServerDO pluginServerDO) {
        Response response;
        CrossChainServiceGrpc.CrossChainServiceBlockingStub stub;
        try {
            stub = getPluginServerGRpcStub(pluginServerDO);
            response = stub.heartbeat(Empty.getDefaultInstance());
        } catch (Exception e) {
            throw new PluginServerConnectionFailException(
                    String.format("test connection with plugin server %s failed, %b, %s",
                            pluginServerDO.getPsId(),
                            OpenSsl.isAlpnSupported(),
                            System.getProperty("java.version")),
                    e
            );
        }
        if (!ObjectUtil.isNotNull(response) || response.getCode() != 0) {
            throw new PluginServerConnectionFailException(
                    String.format("test connection with plugin server %s end with empty response or error code shows something wrong",
                            pluginServerDO.getPsId())
            );
        }
        return stub;
    }

    private CrossChainServiceGrpc.CrossChainServiceBlockingStub createPluginServerGRpcStub(PluginServerDO pluginServerDO) {
        String commonName = PluginServerUtils.getPluginServerCertX509CommonName(pluginServerDO.getProperties().getPluginServerCert());
        if (StrUtil.isEmpty(commonName)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_PLUGIN_SERVER_ERROR,
                    String.format("failed to get common name from x509 subject for plugin server %s", pluginServerDO.getPsId())
            );
        }

        Pair<String, Integer> addressPair = this.decodeAddress(pluginServerDO.getAddress());

        ManagedChannel channel;
        try {
            TlsChannelCredentials.Builder tlsBuilder = TlsChannelCredentials.newBuilder();
            tlsBuilder.keyManager(this.tlsClientCaFile.getInputStream(), this.tlsClientKeyFile.getInputStream());
            tlsBuilder.trustManager(
                    new ByteArrayInputStream(pluginServerDO.getProperties().getPluginServerCert().getBytes())
            );
            channel = NettyChannelBuilder.forAddress(addressPair.getKey(), addressPair.getValue(), tlsBuilder.build())
                    .executor(this.clientExecutorService)
                    .overrideAuthority(commonName)
                    .build();
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_PLUGIN_SERVER_ERROR,
                    String.format("failed to create client for psId %s with %s", pluginServerDO.getPsId(), pluginServerDO.getAddress()),
                    e
            );
        }

        return CrossChainServiceGrpc.newBlockingStub(channel);
    }

    private CrossChainServiceGrpc.CrossChainServiceBlockingStub getPluginServerGRpcStub(PluginServerDO pluginServerDO) {
        if (this.blockingStubMap.containsKey(pluginServerDO.getPsId())) {
            return this.blockingStubMap.get(pluginServerDO.getPsId());
        }
        this.blockingStubMap.put(pluginServerDO.getPsId(), this.createPluginServerGRpcStub(pluginServerDO));
        return this.blockingStubMap.get(pluginServerDO.getPsId());
    }

    /**
     * 删除指定id的插件服务
     * <pre>
     *     调用前提：
     *      检查插件服务是否存在已绑定的区块链，当不存在绑定区块链时才可以调用
     *     处理逻辑：
     *      1.如果插件服务不存在，直接返回
     *      2.如果插件服务存在，先停止心跳（如果有）并移除client，然后从数据库删除条目
     * </pre>
     *
     * @param psId
     */
    @Override
    public void deletePluginServer(String psId) {
        PluginServerDO pluginServerDO = this.pluginServerRepository.getPluginServer(psId);
        if (ObjectUtil.isEmpty(pluginServerDO)) {
            return;
        }

        if (this.heartbeatFutureMap.containsKey(psId)) {
            if (!this.heartbeatFutureMap.get(psId).isDone()
                    && !this.heartbeatFutureMap.get(psId).cancel(false)) {
                throw new AntChainBridgeRelayerException(
                        RelayerErrorCodeEnum.CORE_PLUGIN_SERVER_ERROR,
                        String.format("Plugin server %s heartbeat task stop failed", psId)
                );
            }
            this.heartbeatFutureMap.remove(psId);
        }

        this.pluginServerClientMap.remove(psId);
        this.blockingStubMap.remove(psId);

        this.pluginServerRepository.deletePluginServer(pluginServerDO);
    }

    /**
     * 启动指定id的插件服务
     * <pre>
     *     处理逻辑：
     *      1.获取指定id的插件服务 pluginServerDO （应当已存在且状态不为init或ready）
     *      2.创建 pluginServerDO 的 client （调用 startPluginServerClient 创建并连接）
     *     使用场景：
     *      启动被主动停止（手动调用了 stopPluginServer ）或被动异常（心跳丢失）的插件服务
     * </pre>
     *
     * @param psId
     */
    @Override
    public void startPluginServer(String psId) {
        PluginServerDO pluginServerDO = this.pluginServerRepository.getPluginServer(psId);
        if (ObjectUtil.isEmpty(pluginServerDO)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_PLUGIN_SERVER_ERROR,
                    String.format("data of plugin server %s not found", psId)
            );
        }

        if (PluginServerStateEnum.INIT == pluginServerDO.getState()) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_PLUGIN_SERVER_ERROR,
                    String.format("plugin server %s is initiating", psId)
            );
        }
        if (PluginServerStateEnum.READY == pluginServerDO.getState()) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_PLUGIN_SERVER_ERROR,
                    String.format("plugin server %s already started", psId)
            );
        }

        this.startPluginServerClient(pluginServerDO);

        log.info("client of plugin server {} started", psId);
    }

    @Override
    public void forceStartPluginServer(String psId) {
        PluginServerDO pluginServerDO = this.pluginServerRepository.getPluginServer(psId);
        if (ObjectUtil.isEmpty(pluginServerDO)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_PLUGIN_SERVER_ERROR,
                    String.format("data of plugin server %s not found", psId)
            );
        }

        this.startPluginServerClient(pluginServerDO);

        log.info("force to start client of plugin server {}", psId);
    }

    private void startPluginServerClient(PluginServerDO pluginServerDO) {
        this.pluginServerClientMap.put(
                pluginServerDO.getPsId(),
                new GRpcPluginServerClient(
                        pluginServerDO.getPsId(),
                        tryConnectionWithPluginServer(pluginServerDO),
                        errorLimitForHeartbeat
                )
        );
        this.addHeartbeatTask(pluginServerDO.getPsId());
        if (pluginServerDO.getState() != PluginServerStateEnum.READY) {
            this.pluginServerRepository.updatePluginServerState(pluginServerDO.getPsId(), PluginServerStateEnum.READY);
        }
    }

    private void addHeartbeatTask(String psId) {
        if (this.heartbeatFutureMap.containsKey(psId)) {
            log.info("heartbeat task already exists for plugin server {}", psId);
            return;
        }

        IPluginServerClient pluginServerClient = this.pluginServerClientMap.get(psId);
        ScheduledFuture future = this.heartbeatExecutorService.scheduleWithFixedDelay(
                () -> {
                    Lock lock;
                    try {
                        lock = pluginServerRepository.getHeartbeatLock(psId);
                    } catch (Exception e) {
                        log.error("Failed to get heartbeat lock for plugin server {}: ", psId, e);
                        return;
                    }

                    transactionTemplate.execute(
                            new TransactionCallbackWithoutResult() {
                                @Override
                                protected void doInTransactionWithoutResult(TransactionStatus status) {
                                    PluginServerStateEnum stateEnum = null;
                                    try {
                                        if (lock.tryLock()) {
                                            stateEnum = pluginServerRepository.getPluginServerStateEnum(psId);
                                            if (ObjectUtil.isNull(stateEnum) || stateEnum == PluginServerStateEnum.NOT_FOUND) {
                                                log.info("Plugin server {} is not found, so stop the heartbeat", psId);
                                                throw new AntChainBridgeRelayerException(
                                                        RelayerErrorCodeEnum.CORE_PLUGIN_SERVER_ERROR,
                                                        String.format("Plugin server %s state not found", psId)
                                                );
                                            }
                                            if (stateEnum != PluginServerStateEnum.READY
                                                    && stateEnum != PluginServerStateEnum.HEARTBEAT_LOST) {
                                                log.error("Plugin server {} is not READY or HEARTBEAT_LOST but has a heartbeat task running", psId);
                                                return;
                                            }

                                            PluginServerInfo info = pluginServerClient.heartbeat();
                                            updatePluginServer(psId, info);
                                            if (PluginServerStateEnum.HEARTBEAT_LOST == stateEnum) {
                                                pluginServerRepository.updatePluginServerState(psId, PluginServerStateEnum.READY);
                                            }

                                            log.info(
                                                    "Heartbeat task success for plugin server {} : ( products: {} , domains: {} )",
                                                    psId,
                                                    String.join(",", ObjectUtil.defaultIfNull(info.getProducts(), ListUtil.of())),
                                                    String.join(",", ObjectUtil.defaultIfNull(info.getDomains(), ListUtil.of()))
                                            );
                                        }
                                    } catch (Exception e) {
                                        if (PluginServerStateEnum.HEARTBEAT_LOST != stateEnum && PluginServerStateEnum.STOP != stateEnum) {
                                            log.error("heartbeat failed for plugin server {} and freeze this plugin server", psId, e);
                                            freezePluginServer(psId);
                                        } else if (PluginServerStateEnum.STOP == stateEnum) {
                                            log.warn("heartbeat failed for plugin server {} which is STOP", psId);
                                            // throw exception to stop the scheduled task
                                            throw new AntChainBridgeRelayerException(
                                                    RelayerErrorCodeEnum.CORE_PLUGIN_SERVER_ERROR,
                                                    String.format("heartbeat is supposed to stop cause that plugin server %s's state is %s", psId, stateEnum),
                                                    e
                                            );
                                        } else {
                                            log.warn("heartbeat failed for plugin server {} and keeping HEARTBEAT_LOST", psId, e);
                                        }
                                    } finally {
                                        lock.unlock();
                                    }
                                }
                            }
                    );
                },
                1000,
                heartbeatDelayedTime,
                TimeUnit.MILLISECONDS
        );
        this.heartbeatFutureMap.put(psId, future);
    }

    private void freezePluginServer(String psId) {
        try {
            this.pluginServerRepository.updatePluginServerState(psId, PluginServerStateEnum.HEARTBEAT_LOST);
        } catch (Exception e) {
            log.error("failed to freeze plugin server {}", psId, e);
        }
    }

    private boolean ifNeedUpdatePluginServer(PluginServerDO pluginServerDO, PluginServerInfo pluginServerInfo) {
        Set<String> products = Sets.newHashSet(
                ObjectUtil.defaultIfNull(pluginServerDO.getProductsSupported(), new HashSet<>())
        );
        Set<String> domains = Sets.newHashSet(
                ObjectUtil.defaultIfNull(pluginServerDO.getDomainsServing(), new HashSet<>())
        );
        Set<String> productsNew = Sets.newHashSet(
                ObjectUtil.defaultIfNull(pluginServerInfo.getProducts(), new HashSet<>())
        );
        Set<String> domainsNew = Sets.newHashSet(
                ObjectUtil.defaultIfNull(pluginServerInfo.getDomains(), new HashSet<>())
        );

        if (products.size() != productsNew.size() || domains.size() != domainsNew.size()) {
            return true;
        }

        int productsSize = products.size();
        products.addAll(productsNew);
        if (products.size() > productsSize) {
            return true;
        }

        int domainsSize = domains.size();
        domains.addAll(domainsNew);
        return domains.size() > domainsSize;
    }

    private Pair<String, Integer> decodeAddress(String address) {
        String[] arr = address.split(":");
        if (arr.length != 2) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_PLUGIN_SERVER_ERROR,
                    "Invalid address: " + address
            );
        }
        return Pair.of(arr[0], Integer.valueOf(arr[1]));
    }

    /**
     * 停止指定id的插件服务
     * <pre>
     *     处理逻辑：
     *      1.判断该插件服务是否在心跳任务集合中（应当在），如果不在说明该服务已经停止，会抛出异常
     *      2.如果心跳任务还在执行则停止心跳任务
     *      3.移除插件服务的 client
     *      4.移除链的 stub
     *      5.移除心跳任务
     *      6.更新数据库状态为`stop`，schedule 的 Cleaner 会根据`stop`状态清除链的 client
     *     使用场景：
     *      cli中可以手动停止插件服务
     * </pre>
     *
     * @param psId
     */
    @Override
    public void stopPluginServer(String psId) {
        if (
                this.heartbeatFutureMap.containsKey(psId)
                        && !this.heartbeatFutureMap.get(psId).isDone()
                        && !this.heartbeatFutureMap.get(psId).cancel(false)
        ) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_PLUGIN_SERVER_ERROR,
                    String.format("Plugin server %s heartbeat task stop failed", psId)
            );
        }

        this.pluginServerClientMap.remove(psId);
        this.blockingStubMap.remove(psId);
        this.heartbeatFutureMap.remove(psId);

        this.pluginServerRepository.updatePluginServerState(psId, PluginServerStateEnum.STOP);
    }

    @Override
    public IPluginServerClient getPluginServerClient(String psId) {
        return this.pluginServerClientMap.get(psId);
    }

    @Override
    public List<String> getAllPluginServerId() {
        return ListUtil.toList(this.pluginServerClientMap.keySet());
    }

    @Override
    public PluginServerStateEnum getPluginServerState(String psId) {
        return this.pluginServerRepository.getPluginServerStateEnum(psId);
    }

    @Override
    public List<String> getProductsSupportedByPsId(String psId) {
        return this.pluginServerRepository.getProductsSupportedOfPluginServer(psId);
    }

    @Override
    public List<String> getDomainsSupportedByPsId(String psId) {
        return this.pluginServerRepository.getDomainsServingOfPluginServer(psId);
    }

    @Override
    public PluginServerInfo getPluginServerInfo(String psId) {
        return this.pluginServerRepository.getPluginServerInfo(psId);
    }

    @Override
    public void updatePluginServerInfo(String psId) {
        if (!this.pluginServerClientMap.containsKey(psId)) {
            forceStartPluginServer(psId);
        }
        updatePluginServer(psId, this.pluginServerClientMap.get(psId).heartbeat());
    }

    private void updatePluginServer(String psId, PluginServerInfo info) {
        if (ObjectUtil.isEmpty(info)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_PLUGIN_SERVER_ERROR,
                    "null response from heartbeat for plugin server " + psId
            );
        }

        if (ifNeedUpdatePluginServer(pluginServerRepository.getPluginServer(psId), info)) {
            pluginServerRepository.updatePluginServerInfo(psId, info);
        }
    }
}
