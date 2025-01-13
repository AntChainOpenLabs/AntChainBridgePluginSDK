package com.alipay.antchain.bridge.relayer.core.service.anchor.tasks;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.relayer.commons.exception.ProcessBlockNotifyTaskException;
import com.alipay.antchain.bridge.relayer.commons.model.TpBtaDO;
import com.alipay.antchain.bridge.relayer.core.service.anchor.context.AnchorProcessContext;
import com.alipay.antchain.bridge.relayer.core.service.anchor.workers.BlockWorker;
import com.alipay.antchain.bridge.relayer.core.service.anchor.workers.ConsensusStateWorker;
import com.alipay.antchain.bridge.relayer.core.service.anchor.workers.CrossChainMessageWorker;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.AbstractBlock;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

/**
 * notify任务，用于处理区块里面的数据，将区块里的相关请求转发给核心引擎
 * <p>
 * anchor会处理链上多种合约，每种合约的处理进度可能不一样，故每种合约会有对应一个进度。
 * <p>
 * 该任务在处理的时候，会读取不同的合约进度条去处理。
 *
 * <pre>
 * NotifyTask类的结构：
 *
 * BlockNotifyTask
 *  |-- Set(contract) // 要处理的合约集合
 *  |     |-- notify_height // 每个合约有个已处理高度
 *  |     |-- set(worker) // 每种合约有对应的workers
 *  |
 *  |-- processBlock()
 *        // NotifyTask的主逻辑：每个合约读取对应的高度，批量读取区块，使用对应的workers去处理
 *        set(contract).each().getEssentialHeader(notify_height + 1).set(worker).each.process()
 *
 * </pre>
 */
@Getter
@Slf4j
public class BlockNotifyTask extends BlockBaseTask {

    /**
     * 合约的workers
     */
    private final Map<NotifyTaskTypeEnum, List<BlockWorker>> workersByTask = new HashMap<>();

    private final List<NotifyTaskTypeEnum> workerTypes = ListUtil.toList(
            NotifyTaskTypeEnum.SYSTEM_WORKER,
            NotifyTaskTypeEnum.CROSSCHAIN_MSG_WORKER
    );

    public BlockNotifyTask(
            AnchorProcessContext processContext
    ) {
        super(
                BlockTaskTypeEnum.NOTIFY,
                processContext
        );

        workersByTask.put(
                NotifyTaskTypeEnum.SYSTEM_WORKER,
                ListUtil.toList(
                        new ConsensusStateWorker(processContext)
                )
        );
        workersByTask.put(
                NotifyTaskTypeEnum.CROSSCHAIN_MSG_WORKER,
                ListUtil.toList(
                        new CrossChainMessageWorker(processContext)
                )
        );
    }

    @Override
    public void doProcess() {
        try {
            processBlock();
        } catch (Exception e) {
            throw new RuntimeException("process block fail.", e);
        }
    }

    private void processBlock() {

        // 每个合约读取对应的高度，批量读取区块，使用对应的workers去处理
        for (NotifyTaskTypeEnum notifyTaskType : workerTypes) {

            // 如果合约还未部署，不进行该任务
            if (!ifDeployContract(notifyTaskType)) {
                log.debug("blockchain {} has not deployed {} contract yet, wait for it.",
                        getProcessContext().getBlockchainMeta().getMetaKey(), notifyTaskType.getCode());
                continue;
            }

            List<TpBtaDO> allTpBta = getProcessContext().getPtcManager()
                    .getAllValidTpBtaForDomain(new CrossChainDomain(getProcessContext().getBlockchainDomain()));
            if (ObjectUtil.isEmpty(allTpBta)) {
                try {
                    processBlockNonEndorsements(notifyTaskType);
                } catch (ProcessBlockNotifyTaskException e) {
                    log.error("notify task {} process failed: ", notifyTaskType.getCode(), e);
                }
            } else {
                processBlockWithEndorsements(notifyTaskType, allTpBta);
            }
        }
    }

    private void processBlockNonEndorsements(NotifyTaskTypeEnum notifyTaskType) throws ProcessBlockNotifyTaskException {
        // 查看已处理、未处理的区块
        long localBlockHeaderHeight = getLocalBlockHeaderHeight();
        long notifyBlockHeaderHeight = notifyTaskType == NotifyTaskTypeEnum.SYSTEM_WORKER ?
                Long.MAX_VALUE : getNotifyBlockHeaderHeight(notifyTaskType.getCode());
        log.debug(
                "blockchain {} notify task {} has localBlockHeaderHeight {} and notifyBlockHeaderHeight {} now",
                getProcessContext().getBlockchainMeta().getMetaKey(),
                notifyTaskType.getCode(),
                localBlockHeaderHeight,
                notifyBlockHeaderHeight
        );

        if (notifyBlockHeaderHeight >= localBlockHeaderHeight) {
            log.debug(
                    "height {} of notify task {} equals to local height {} for blockchain {}",
                    notifyBlockHeaderHeight,
                    notifyTaskType.getCode(),
                    localBlockHeaderHeight,
                    getProcessContext().getBlockchainMeta().getMetaKey()
            );
            return;
        }

        // 批量处理
        // each process task will process the gap of local block header and notify header now.
        long endHeight = Math.min(
                localBlockHeaderHeight,
                notifyBlockHeaderHeight + getProcessContext().getNotifyBatchSize()
        );
        long currentHeight = notifyBlockHeaderHeight + 1;

        log.info(
                "notify task {} for blockchain {} is processing from blockHeight {} to endHeight {}",
                notifyTaskType.getCode(),
                getProcessContext().getBlockchainMeta().getMetaKey(),
                currentHeight,
                endHeight
        );

        for (; currentHeight <= endHeight; ++currentHeight) {

            AbstractBlock block = getProcessContext().getBlockQueue().getBlockFromQueue(currentHeight);
            if (ObjectUtil.isNull(block)) {
                throw new ProcessBlockNotifyTaskException(
                        getProcessContext().getBlockchainDomain(),
                        BigInteger.valueOf(currentHeight),
                        "noop",
                        "none block found from queue"
                );
            }
            log.info(
                    "blockchain {} notify task {} is processing the block {}",
                    getProcessContext().getBlockchainMeta().getMetaKey(),
                    notifyTaskType.getCode(),
                    currentHeight
            );

            BigInteger currHeight = BigInteger.valueOf(currentHeight);
            getProcessContext().getTransactionTemplate().execute(
                    new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus status) {
                            // 责任链模式，一个区块交给各个worker各处理一遍，且都要处理成功
                            // TODO 一个worker处理失败，会导致该区块会全部重做一遍，这样子worker可能会收到同一个区块多次，需要能有幂等处理能力，这点可以优化
                            for (BlockWorker worker : workersByTask.get(notifyTaskType)) {
                                if (!worker.process(block, null)) {
                                    throw new ProcessBlockNotifyTaskException(
                                            block.getDomain(),
                                            currHeight,
                                            "00",
                                            StrUtil.format("worker {}:{} process failed", notifyTaskType.getCode(), worker.getClass().getSimpleName())
                                    );
                                }
                            }

                            // 处理成功，则持久化区块高度
                            saveNotifyBlockHeaderHeight(notifyTaskType.getCode(), currHeight.longValue());
                        }
                    }
            );

            log.info(
                    "successful to process block (height: {}) in notify task {} from chain (product: {}, blockchain_id: {})",
                    block.getHeight(),
                    notifyTaskType.getCode(),
                    block.getProduct(),
                    block.getBlockchainId()
            );
        }
    }

    private void processBlockWithEndorsements(NotifyTaskTypeEnum notifyTaskType, List<TpBtaDO> allTpBta) {
        //TODO: maybe in parallel
        for (TpBtaDO tpBta : allTpBta) {
            // 查看已处理、未处理的区块
            long localBlockHeaderHeight = getLocalBlockHeaderHeight();
            long notifyBlockHeaderHeight = getNotifyBlockHeaderHeight(notifyTaskType.getCode(), tpBta.getCrossChainLane());
            log.debug(
                    "blockchain {} notify task {} with tpbta {} has localBlockHeaderHeight {} and notifyBlockHeaderHeight {} now",
                    getProcessContext().getBlockchainMeta().getMetaKey(),
                    notifyTaskType.getCode(),
                    tpBta.getCrossChainLane().getLaneKey(),
                    localBlockHeaderHeight,
                    notifyBlockHeaderHeight
            );

            if (notifyBlockHeaderHeight >= localBlockHeaderHeight) {
                log.debug(
                        "height {} of notify task {} equals to local height {} for blockchain {} and tpbta lane {}",
                        notifyBlockHeaderHeight,
                        notifyTaskType.getCode(),
                        localBlockHeaderHeight,
                        getProcessContext().getBlockchainMeta().getMetaKey(),
                        tpBta.getCrossChainLane().getLaneKey()
                );
                continue;
            }

            if (notifyTaskType == NotifyTaskTypeEnum.CROSSCHAIN_MSG_WORKER) {
                long currSystemWorkHeight = getNotifyBlockHeaderHeight(NotifyTaskTypeEnum.SYSTEM_WORKER.getCode(), tpBta.getCrossChainLane());
                if (currSystemWorkHeight <= notifyBlockHeaderHeight) {
                    log.info("blockchain {} and tpbta_lane {} notify task has not processed system work block yet, skip this cross chain msg process: (ccmsg_height: {}, system_height: {})",
                            getProcessContext().getBlockchainMeta().getMetaKey(), tpBta.getCrossChainLane().getLaneKey(), notifyBlockHeaderHeight, currSystemWorkHeight);
                    continue;
                }
            }

            // 批量处理
            // each process task will process the gap of local block header and notify header now.
            long endHeight = Math.min(
                    localBlockHeaderHeight,
                    notifyBlockHeaderHeight + getProcessContext().getNotifyBatchSize()
            );
            long currentHeight = notifyBlockHeaderHeight + 1;

            log.info(
                    "notify task {} for blockchain {} and tpbta lane {} is processing from blockHeight {} to endHeight {}",
                    notifyTaskType.getCode(),
                    getProcessContext().getBlockchainMeta().getMetaKey(),
                    tpBta.getCrossChainLane().getLaneKey(),
                    currentHeight,
                    endHeight
            );

            for (; currentHeight <= endHeight; ++currentHeight) {

                AbstractBlock block = getProcessContext().getBlockQueue().getBlockFromQueue(currentHeight);
                if (ObjectUtil.isNull(block)) {
                    log.error(
                            "blockchain {} and tpbta_lane {} notify task {} can't find block {} from block queue so skip the failed task",
                            getProcessContext().getBlockchainMeta().getMetaKey(),
                            tpBta.getCrossChainLane().getLaneKey(),
                            notifyTaskType.getCode(),
                            currentHeight
                    );
                    break;
                }
                log.info(
                        "blockchain {} and tpbta_lane {} notify task {} is processing the block {}",
                        getProcessContext().getBlockchainMeta().getMetaKey(),
                        tpBta.getCrossChainLane().getLaneKey(),
                        notifyTaskType.getCode(),
                        currentHeight
                );
                try {
                    BigInteger currHeight = BigInteger.valueOf(currentHeight);
                    getProcessContext().getTransactionTemplate().execute(
                            new TransactionCallbackWithoutResult() {
                                @Override
                                protected void doInTransactionWithoutResult(TransactionStatus status) {
                                    // 责任链模式，一个区块交给各个worker各处理一遍，且都要处理成功
                                    // TODO 一个worker处理失败，会导致该区块会全部重做一遍，这样子worker可能会收到同一个区块多次，需要能有幂等处理能力，这点可以优化
                                    for (BlockWorker worker : workersByTask.get(notifyTaskType)) {
                                        if (!worker.process(block, tpBta)) {
                                            throw new ProcessBlockNotifyTaskException(
                                                    getProcessContext().getBlockchainDomain(),
                                                    currHeight,
                                                    block.getConsensusState().getHashHex(),
                                                    StrUtil.format("worker {}:{} process failed", notifyTaskType.getCode(), worker.getClass().getSimpleName())
                                            );
                                        }
                                    }

                                    saveNotifyBlockHeaderHeight(notifyTaskType.getCode(), tpBta.getCrossChainLane(), currHeight.longValue());
                                }
                            }
                    );
                } catch (Exception e) {
                    log.error(
                            "failed to process block (height: {}) in notify task {} from chain (product: {}, blockchain_id: {}, tpbta_lane: {})",
                            block.getHeight(),
                            notifyTaskType.getCode(),
                            block.getProduct(),
                            block.getBlockchainId(),
                            tpBta.getCrossChainLane().getLaneKey(),
                            e
                    );
                    break;
                }

                log.info(
                        "successful to process block (height: {}) in notify task {} from chain (product: {}, blockchain_id: {}, tpbta_lane: {})",
                        block.getHeight(),
                        notifyTaskType.getCode(),
                        block.getProduct(),
                        block.getBlockchainId(),
                        tpBta.getCrossChainLane().getLaneKey()
                );
            }
        }
    }

    private boolean ifDeployContract(NotifyTaskTypeEnum taskType) {

        // 这里面用了processContext里的内存变量（合约地址）来判断是否已部署合约，所以需要为何该内存变量是最新的
        // （如果serviceManager部署了合约，anchorProcess的这个processContext相关变量也要更新）
        if (NotifyTaskTypeEnum.CROSSCHAIN_MSG_WORKER == taskType) {
            return getProcessContext().getBlockchainClient().ifHasDeployedAMClientContract();
        }
        return NotifyTaskTypeEnum.SYSTEM_WORKER == taskType;
    }
}