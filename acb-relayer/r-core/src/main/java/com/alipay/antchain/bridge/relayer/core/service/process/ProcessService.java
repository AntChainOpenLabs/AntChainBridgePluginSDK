package com.alipay.antchain.bridge.relayer.core.service.process;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageFactory;
import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgTrustLevelEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.UniformCrosschainPacketStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.UpperProtocolTypeBeyondAMEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.AuthMsgWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.UniformCrosschainPacketContext;
import com.alipay.antchain.bridge.relayer.core.manager.blockchain.IBlockchainManager;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerNetworkManager;
import com.alipay.antchain.bridge.relayer.core.utils.ProcessUtils;
import com.alipay.antchain.bridge.relayer.dal.repository.ICrossChainMessageRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.impl.BlockchainIdleDCache;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
@Getter
public class ProcessService {

    private static final String NOT_READY_AM_LOCK_PREFIX = "NotReadyMessageLock-";

    @Resource
    private AuthenticMessageProcess authenticMessageProcess;

    @Resource
    private ICrossChainMessageRepository crossChainMessageRepository;

    @Resource
    private IBlockchainManager blockchainManager;

    @Resource
    private IRelayerNetworkManager relayerNetworkManager;

    @Resource
    private BlockchainIdleDCache blockchainIdleDCache;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource(name = "processServiceThreadsPool")
    private ExecutorService processServiceThreadsPool;

    @Value("${relayer.service.process.ccmsg.batch_size:64}")
    private int ccmsgBatchSize;

    @Value("${relayer.service.process.ccmsg.fail_limit:10}")
    private int ccmsgFailLimit;

    @Value("${relayer.service.process.ccmsg.not_ready_port.batch_size:64}")
    private int notReadyPortBatchSize;

    @Resource
    private RedissonClient redisson;

    /**
     * 执行指定区块的分布式调度任务
     *
     * @param blockchainProduct
     * @param blockchainId
     */
    public void process(String blockchainProduct, String blockchainId) {

        log.debug("process service run with blockchain {}", blockchainId);

        List<AuthMsgWrapper> authMsgWrapperList = ListUtil.toList();

        String domainName = blockchainManager.getBlockchainDomain(blockchainProduct, blockchainId);

        if (this.blockchainIdleDCache.ifAMProcessIdle(blockchainProduct, blockchainId)) {
            log.debug("am process : blockchain is idle {}-{}.", blockchainProduct, blockchainId);
        } else if (StrUtil.isNotEmpty(domainName)) {
            authMsgWrapperList = crossChainMessageRepository.peekAuthMessages(
                    domainName,
                    ccmsgBatchSize,
                    ccmsgFailLimit
            );
        }

        if (!authMsgWrapperList.isEmpty()) {
            log.info("peek {} auth msg from pool: {}-{}", authMsgWrapperList.size(), blockchainProduct, blockchainId);
        } else {
            this.blockchainIdleDCache.setLastEmptyAMPoolTime(blockchainProduct, blockchainId);
            log.debug("{}-{} for auth msg is idle", blockchainProduct, blockchainId);
        }

        if (crossChainMessageRepository.hasNotReadyAuthMessages(domainName)) {
            log.debug("there is NOT_READY auth messages for domain {} in DB", domainName);
            processServiceThreadsPool.execute(wrapNotReadyAMPorterTask(domainName));
        }

        // 使用线程池并发执行
        ProcessUtils.waitAllFuturesDone(
                blockchainProduct,
                blockchainId,
                authMsgWrapperList.stream().map(
                        authMsgWrapper -> processServiceThreadsPool.submit(
                                wrapAMTask(authMsgWrapper.getAuthMsgId())
                        )
                ).collect(Collectors.toList()),
                log
        );
    }

    private Runnable wrapAMTask(long amId) {
        return () -> transactionTemplate.execute(
                new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {

                        AuthMsgWrapper am = crossChainMessageRepository.getAuthMessage(amId, true);
                        if (ObjectUtil.isNull(am)) {
                            log.error("none auth message found for auth id {}", amId);
                            return;
                        }

                        try {
                            if (!authenticMessageProcess.doProcess(am)) {
                                throw new RuntimeException(
                                        StrUtil.format("failed to process auth message for auth id {} for unknown reason", amId)
                                );
                            }

                        } catch (AntChainBridgeRelayerException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new AntChainBridgeRelayerException(
                                    RelayerErrorCodeEnum.SERVICE_CORE_PROCESS_PROCESS_CCMSG_FAILED,
                                    e,
                                    "failed to process auth message for auth id {}",
                                    amId
                            );
                        }
                    }
                }
        );
    }

    private Runnable wrapNotReadyAMPorterTask(String domain) {
        return () -> {
            Lock notReadyMessageLock = getNotReadyMessageLock(domain);
            if (!notReadyMessageLock.tryLock()) {
                log.debug("lock for NotReadyAMPorterTask of domain {} is not released", domain);
                return;
            }
            try {
                List<AuthMsgWrapper> authMsgWrappers = crossChainMessageRepository.peekNotReadyAuthMessages(
                        domain, notReadyPortBatchSize
                );
                if (ObjectUtil.isEmpty(authMsgWrappers)) {
                    log.debug("no not_ready am to be ported for domain {}", domain);
                    return;
                }
                int cnt = authMsgWrappers.stream().filter(
                        authMsgWrapper -> authMsgWrapper.getProtocolType() == UpperProtocolTypeBeyondAMEnum.SDP
                ).map(
                        authMsgWrapper -> {
                            try {
                                return transactionTemplate.execute(
                                        status -> {
                                            try {
                                                AuthMsgProcessStateEnum newState = getNewStateForNotReadyMsg(authMsgWrapper);
                                                if (newState == AuthMsgProcessStateEnum.NOT_READY) {
                                                    return 0;
                                                }
                                                if (
                                                        crossChainMessageRepository.updateAuthMessageState(
                                                                authMsgWrapper.getUcpId(), newState
                                                        )
                                                ) {
                                                    log.info(
                                                            "new state {} for auth message {} from domain {}",
                                                            newState.name(), authMsgWrapper.getAuthMsgId(), authMsgWrapper.getDomain()
                                                    );
                                                    return 1;
                                                } else {
                                                    log.error("failed to update auth message {} 's state to {}", authMsgWrapper.getAuthMsgId(), newState.name());
                                                    return 0;
                                                }
                                            } catch (Exception e) {
                                                throw new RuntimeException(
                                                        StrUtil.format("failed to process NOT_READY am {}: ", authMsgWrapper.getAuthMsgId()),
                                                        e
                                                );
                                            }
                                        }
                                );
                            } catch (Exception e) {
                                log.error("failed to update NOT_READY auth message {} 's state: ", authMsgWrapper.getAuthMsgId(), e);
                                return 0;
                            }
                        }
                ).filter(Objects::nonNull).reduce(Integer::sum).orElse(0);
                if (cnt > 0) {
                    log.info("successful to update {} not_ready auth messages for domain {}", cnt, domain);
                }
            } finally {
                notReadyMessageLock.unlock();
            }
        };
    }

    private AuthMsgProcessStateEnum getNewStateForNotReadyMsg(AuthMsgWrapper authMsgWrapper) {
        if (
                StrUtil.isEmpty(
                        relayerNetworkManager.findRemoteRelayer(
                                SDPMessageFactory.createSDPMessage(authMsgWrapper.getPayload())
                                        .getTargetDomain().getDomain()
                        )
                )
        ) {
            log.debug(
                    "receiver domain router for {} still not ready",
                    SDPMessageFactory.createSDPMessage(authMsgWrapper.getPayload())
                            .getTargetDomain().getDomain()
            );
            return AuthMsgProcessStateEnum.NOT_READY;
        }

        if (authMsgWrapper.getTrustLevel() == AuthMsgTrustLevelEnum.ZERO_TRUST) {
            return AuthMsgProcessStateEnum.PROVED;
        }
        if (authMsgWrapper.getTrustLevel() == AuthMsgTrustLevelEnum.POSITIVE_TRUST) {
            return AuthMsgProcessStateEnum.PENDING;
        }
        UniformCrosschainPacketContext ucpContext = crossChainMessageRepository.getUniformCrosschainPacket(
                authMsgWrapper.getUcpId(),
                true
        );
        if (ObjectUtil.isNull(ucpContext)) {
            log.error(
                    "auth message {} from domain {} has no ucp message in DB",
                    authMsgWrapper.getAuthMsgId(), authMsgWrapper.getDomain()
            );
            return AuthMsgProcessStateEnum.NOT_READY;
        }
        if (ucpContext.getProcessState() == UniformCrosschainPacketStateEnum.PROVED) {
            return AuthMsgProcessStateEnum.PROVED;
        } else {
            return AuthMsgProcessStateEnum.PENDING;
        }
    }

    private Lock getNotReadyMessageLock(String domain) {
        return redisson.getLock(NOT_READY_AM_LOCK_PREFIX + domain);
    }
}
