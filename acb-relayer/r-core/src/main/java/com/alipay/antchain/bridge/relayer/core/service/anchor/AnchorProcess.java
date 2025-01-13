package com.alipay.antchain.bridge.relayer.core.service.anchor;

import java.util.concurrent.ExecutorService;

import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import com.alipay.antchain.bridge.relayer.core.manager.ptc.IPtcManager;
import com.alipay.antchain.bridge.relayer.core.service.anchor.context.AnchorProcessContext;
import com.alipay.antchain.bridge.relayer.core.service.anchor.tasks.BlockNotifyTask;
import com.alipay.antchain.bridge.relayer.core.service.anchor.tasks.BlockPollingTask;
import com.alipay.antchain.bridge.relayer.core.service.anchor.tasks.BlockSyncTask;
import com.alipay.antchain.bridge.relayer.core.service.receiver.ReceiverService;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.BlockchainClientPool;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * anchorProcess对象是一条区块链的锚定器，包括同步远端最新高度、同步账本、处理账本等锚定任务，以及向区块链提交交易的操作接口。
 *
 * <pre>
 * 该对象有以下结构
 *  1. 区块链配置上下文 ProcessContext
 *  2. 三组任务
 *   - 最新高度同步任务
 *   - 账本同步任务
 *   - 账本处理任务
 *  3. 一个提交器，该提交器封装了像该区块链提交tx的逻辑
 * </pre>
 */
@Getter
@Setter
@Slf4j
public class AnchorProcess {

    /**
     * process上下文
     */
    private AnchorProcessContext processContext;

    // 三组锚定任务
    private BlockPollingTask blockPollingTask;

    private BlockSyncTask blockSyncTask;

    private BlockNotifyTask notifyTask;

    public AnchorProcess(
            BlockchainMeta chainMeta,
            TransactionTemplate transactionTemplate,
            BlockchainClientPool blockchainClientPool,
            RedissonClient redisson,
            ExecutorService blockSyncTaskThreadsPool,
            ReceiverService receiverService,
            IPtcManager ptcManager,
            int blockCacheCapacity,
            int blockCacheTTL,
            int syncBatchSize,
            int syncAsyncQuerySize,
            int maxDiffBetweenSyncAndNotify,
            int notifyBatchSize,
            long heightDelayAlarmThreshold
    ) {
        // init context
        this.processContext = new AnchorProcessContext(
                chainMeta,
                transactionTemplate,
                blockchainClientPool,
                redisson,
                blockSyncTaskThreadsPool,
                receiverService,
                ptcManager,
                blockCacheCapacity,
                blockCacheTTL,
                syncBatchSize,
                syncAsyncQuerySize,
                maxDiffBetweenSyncAndNotify,
                notifyBatchSize,
                heightDelayAlarmThreshold
        );

        // init tasks
        this.blockPollingTask = new BlockPollingTask(this.processContext);
        this.blockSyncTask = new BlockSyncTask(this.processContext);
        this.notifyTask = new BlockNotifyTask(this.processContext);
    }

    public void run() {
        log.debug("start anchor process for {} ", processContext.getBlockchainMeta().getMetaKey());

        try {
            // 同步最新高度
            this.blockPollingTask.doProcess();
            // 同步远程区块
            this.blockSyncTask.doProcess();
            // 区块处理任务
            this.notifyTask.doProcess();
        } catch (Exception e) {
            log.error("anchor process failed for {} : ", processContext.getBlockchainMeta().getMetaKey(), e);
            return;
        }

        log.debug("success to run anchor process for {} : ", processContext.getBlockchainMeta().getMetaKey());
    }

    public void updateBlockchainMetaIntoClient(BlockchainMeta blockchainMeta) {
        processContext.getBlockchainClient().setBlockchainMeta(blockchainMeta);
    }

    public String getDomain() {
        return processContext.getBlockchainClient().getDomain();
    }

    public void setDomain(String domain) {
        processContext.getBlockchainClient().setDomain(domain);
    }
}
