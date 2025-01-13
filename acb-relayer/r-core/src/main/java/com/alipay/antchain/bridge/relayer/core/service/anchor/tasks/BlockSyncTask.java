package com.alipay.antchain.bridge.relayer.core.service.anchor.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.core.service.anchor.context.AnchorProcessContext;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.AbstractBlock;
import lombok.extern.slf4j.Slf4j;

/**
 * sync remote block, and update local block header.
 */
@Slf4j
public class BlockSyncTask extends BlockBaseTask {

    public BlockSyncTask(
            AnchorProcessContext processContext
    ) {
        super(BlockTaskTypeEnum.SYNC, processContext);
        Assert.isTrue(processContext.getSyncBatchSize() > 0);
    }

    @Override
    public void doProcess() {
        try {
            poll();
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.SERVICE_MULTI_ANCHOR_PROCESS_SYNC_TASK_FAILED,
                    e,
                    "failed to sync block for {}",
                    getProcessContext().getBlockchainMeta().getMetaKey()
            );
        }
    }

    private void poll() {
        if (!getProcessContext().getBlockchainClient().ifHasDeployedAMClientContract()) {
            log.debug("skip sync task because of BBC contracts not ready now for blockchain {}-{}",
                    getProcessContext().getAnchorProduct(), getProcessContext().getAnchorBlockchainId());
            return;
        }
        long localBlockHeaderHeight = getLocalBlockHeaderHeight();
        long remoteBlockHeaderHeight = getRemoteBlockHeaderHeight();

        if (localBlockHeaderHeight >= remoteBlockHeaderHeight) {
            log.debug(
                    "local block synced {} had equal to remote header {} for blockchain {}",
                    localBlockHeaderHeight,
                    remoteBlockHeaderHeight,
                    getProcessContext().getBlockchainMeta().getMetaKey()
            );
            return;
        }

        long ccmsgHeight = getMaxNotifyBlockHeaderHeightForAllLanes(NotifyTaskTypeEnum.CROSSCHAIN_MSG_WORKER.getCode());
        if (getProcessContext().getHeightDelayAlarmThreshold() != -1 && ccmsgHeight + getProcessContext().getHeightDelayAlarmThreshold() < remoteBlockHeaderHeight) {
            log.error("🚨[ALARM] Block processing of blockchain {} has height delayed over threshold {} : (ccmsg-height: {}, remote-height: {})",
                    getProcessContext().getBlockchainMeta().getMetaKey(), getProcessContext().getHeightDelayAlarmThreshold(), ccmsgHeight, remoteBlockHeaderHeight);
        }

        if (localBlockHeaderHeight >= ccmsgHeight + getProcessContext().getMaxDiffBetweenSyncAndNotify()) {
            log.info(
                    "local block synced {} had greater than notify ccmsg worker height {} plus with maxHeightDiff {} for blockchain {}",
                    localBlockHeaderHeight,
                    ccmsgHeight,
                    getProcessContext().getMaxDiffBetweenSyncAndNotify(),
                    getProcessContext().getBlockchainMeta().getMetaKey()
            );
            return;
        }

        // each process task will process the gap of local block header and remote header now.
        long endHeight = Math.min(remoteBlockHeaderHeight, localBlockHeaderHeight + getProcessContext().getSyncBatchSize());
        long currentHeight = localBlockHeaderHeight + 1;

        log.info(
                "block sync task for blockchain {} is processing from blockHeight {} to endHeight {}",
                getProcessContext().getBlockchainMeta().getMetaKey(),
                currentHeight,
                endHeight
        );

        while (currentHeight <= endHeight) {

            long syncBatch = (endHeight - currentHeight + 1) >= getProcessContext().getSyncAsyncQuerySize() ?
                    getProcessContext().getSyncAsyncQuerySize() : (endHeight - currentHeight + 1);

            List<AbstractBlock> blocks = queryRemoteBlock(currentHeight, syncBatch);

            if (blocks.isEmpty()) {
                log.error(
                        "query remote block from {} to {} failed for {}",
                        currentHeight,
                        currentHeight + syncBatch,
                        getProcessContext().getBlockchainMeta().getMetaKey()
                );
                break;
            }
            blocks.forEach(
                    block -> getProcessContext().getBlockQueue().putBlockIntoQueue(block)
            );
            saveLocalBlockHeaderHeight(blocks.get(blocks.size() - 1).getHeight());
            currentHeight = blocks.get(blocks.size() - 1).getHeight() + 1;
        }
    }

    public List<AbstractBlock> queryRemoteBlock(long height, long size) {
        List<Future<AbstractBlock>> blockFutures = new ArrayList<>((int) size);
        for (long queryHeight = height; queryHeight < height + size; ++queryHeight) {
            long finalQueryHeight = queryHeight;
            blockFutures.add(
                    getProcessContext().getBlockSyncTaskThreadsPool().submit(
                            () -> getProcessContext().getBlockchainClient().getEssentialBlockByHeight(finalQueryHeight)
                    )
            );
        }
        return blockFutures.stream().map(
                future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        throw new RuntimeException("failed to get block from future object: ", e);
                    }
                }
        ).collect(Collectors.toList());
    }
}