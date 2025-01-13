package com.alipay.antchain.bridge.relayer.core.service.anchor;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import javax.annotation.Resource;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import com.alipay.antchain.bridge.relayer.core.manager.blockchain.IBlockchainManager;
import com.alipay.antchain.bridge.relayer.core.manager.ptc.IPtcManager;
import com.alipay.antchain.bridge.relayer.core.service.receiver.ReceiverService;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.BlockchainClientPool;
import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * AnchorProcess管理器，负责AnchorProcess的声明周期管理、执行。
 */
@Service
@Slf4j
@Getter
public class MultiAnchorProcessService {

    /**
     * anchorProcess
     */
    private final ConcurrentMap<String, AnchorProcess> anchorProcessMap = MapUtil.newConcurrentHashMap();

    @Resource
    private IBlockchainManager blockchainManager;

    @Resource
    private BlockchainClientPool blockchainClientPool;

    @Resource
    private RedissonClient redisson;

    @Resource
    private ExecutorService blockSyncTaskThreadsPool;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private ReceiverService receiverService;

    @Resource
    private IPtcManager ptcManager;

    @Value("${relayer.service.anchor.sync_task.batch_size:32}")
    private int syncTaskBatchSize;

    @Value("${relayer.service.anchor.sync_task.async_size:10}")
    private int syncTaskAsyncQuerySize;

    @Value("${relayer.service.anchor.sync_task.max_diff_with_notify:32}")
    private int maxDiffBetweenSyncAndNotify;

    @Value("${relayer.service.anchor.notify_task.batch_size:32}")
    private int notifyTaskBatchSize;

    @Value("${relayer.service.anchor.block_cache_capacity:100}")
    private int blockCacheCapacity;

    @Value("${relayer.service.anchor.block_cache_ttl:300000}")
    private int blockCacheTTL;

    @Value("${relayer.service.anchor.height_delay_alarm_threshold:-1}")
    private long heightDelayAlarmThreshold;

    /**
     * 启动指定anchorProcess
     *
     * @param blockchainProduct
     * @param blockchainId
     */
    public void runAnchorProcess(String blockchainProduct, String blockchainId) {
        AnchorProcess anchorProcess = getAnchorProcess(blockchainProduct, blockchainId);
        if (ObjectUtil.isNull(anchorProcess)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.SERVICE_MULTI_ANCHOR_PROCESS_START_ANCHOR_FAILED,
                    "null anchor process for {}-{}", blockchainProduct, blockchainId
            );
        }
        // 触发执行
        anchorProcess.run();
    }

    /**
     * 获取anchorProcess对象
     *
     * @param blockchainProduct
     * @param blockchainId
     * @return
     */
    @Synchronized
    public AnchorProcess getAnchorProcess(String blockchainProduct, String blockchainId) {
        if (!anchorProcessMap.containsKey(getAnchorProcessKey(blockchainProduct, blockchainId))) {
            log.info("build new anchor process object for {}-{}", blockchainProduct, blockchainId);
            newAnchorProcess(blockchainProduct, blockchainId);
        }

        AnchorProcess anchorProcess = anchorProcessMap.getOrDefault(
                getAnchorProcessKey(blockchainProduct, blockchainId),
                null
        );
        if (ObjectUtil.isNotNull(anchorProcess)) {
            updateAnchorProcess(anchorProcess);
        }

        return anchorProcess;
    }

    private String getAnchorProcessKey(String blockchainProduct, String blockchainId) {
        return blockchainProduct + "_" + blockchainId;
    }

    /**
     * 添加anchorProcess对象
     */
    private void newAnchorProcess(String blockchainProduct, String blockchainId) {

        BlockchainMeta blockchainMeta = blockchainManager.getBlockchainMeta(blockchainProduct, blockchainId);
        if (ObjectUtil.isNull(blockchainMeta)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.SERVICE_MULTI_ANCHOR_PROCESS_START_ANCHOR_FAILED,
                    "none blockchain meta found for {}-{}",
                    blockchainProduct, blockchainId
            );
        }

        AnchorProcess anchorProcess = new AnchorProcess(
                blockchainMeta,
                transactionTemplate,
                blockchainClientPool,
                redisson,
                blockSyncTaskThreadsPool,
                receiverService,
                ptcManager,
                blockCacheCapacity,
                blockCacheTTL,
                syncTaskBatchSize,
                syncTaskAsyncQuerySize,
                maxDiffBetweenSyncAndNotify,
                notifyTaskBatchSize,
                heightDelayAlarmThreshold
        );
        if (ObjectUtil.isNull(anchorProcess)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.SERVICE_MULTI_ANCHOR_PROCESS_START_ANCHOR_FAILED,
                    "null anchor process returned for {}-{}",
                    blockchainProduct, blockchainId
            );
        }
        this.anchorProcessMap.putIfAbsent(
                anchorProcess.getProcessContext().getBlockchainMeta().getMetaKey(),
                anchorProcess
        );
    }

    /**
     * 更新anchorProcess对象
     *
     * @param anchorProcess
     */
    private void updateAnchorProcess(AnchorProcess anchorProcess) {

        // 这里可以使用缓存，隔几分钟才能更新到最新的配置，没必要每次都更新
        BlockchainMeta blockchainMeta = blockchainManager.getBlockchainMeta(
                anchorProcess.getProcessContext().getBlockchainMeta().getProduct(),
                anchorProcess.getProcessContext().getBlockchainMeta().getBlockchainId()
        );
        if (ObjectUtil.isNull(blockchainMeta)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.SERVICE_MULTI_ANCHOR_PROCESS_START_ANCHOR_FAILED,
                    "none blockchain meta found when update anchor process for {}",
                    anchorProcess.getProcessContext().getBlockchainMeta().getMetaKey()
            );
        }
        anchorProcess.updateBlockchainMetaIntoClient(blockchainMeta);

        log.debug("update anchor blockchain meta for : {}_{}", blockchainMeta.getProduct(), blockchainMeta.getBlockchainId());

        // 如果部署了跨链服务，就同步domain name信息
        if (
                anchorProcess.getProcessContext().getBlockchainClient().ifHasDeployedAMClientContract()
                        && StrUtil.isEmpty(anchorProcess.getDomain())
        ) {
            String domain = blockchainManager.getBlockchainDomain(
                    blockchainMeta.getProduct(),
                    blockchainMeta.getBlockchainId()
            );
            if (StrUtil.isEmpty(domain)) {
                return;
            }
            log.info(
                    "update domain name info anchor for {} - {}",
                    anchorProcess.getProcessContext().getBlockchainMeta().getMetaKey(),
                    domain
            );
            anchorProcess.setDomain(domain);
        }
    }
}
