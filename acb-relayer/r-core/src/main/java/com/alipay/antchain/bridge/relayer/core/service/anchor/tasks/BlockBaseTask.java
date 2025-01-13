package com.alipay.antchain.bridge.relayer.core.service.anchor.tasks;

import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.relayer.commons.model.BtaDO;
import com.alipay.antchain.bridge.relayer.commons.model.TpBtaDO;
import com.alipay.antchain.bridge.relayer.core.service.anchor.context.AnchorProcessContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public abstract class BlockBaseTask {

    private final BlockTaskTypeEnum taskType;

    private final AnchorProcessContext processContext;

    public BlockBaseTask(
            BlockTaskTypeEnum taskName,
            AnchorProcessContext processContext
    ) {
        this.taskType = taskName;
        this.processContext = processContext;
    }

    public abstract void doProcess();

    public void saveRemoteBlockHeaderHeight(long height) {
        processContext.getBlockchainRepository().setAnchorProcessHeight(
                processContext.getBlockchainMeta().getProduct(),
                processContext.getBlockchainMeta().getBlockchainId(),
                BlockTaskTypeEnum.POLLING.getCode(),
                height
        );
    }

    protected long getRemoteBlockHeaderHeight() {
        return processContext.getBlockchainRepository().getAnchorProcessHeight(
                processContext.getBlockchainMeta().getProduct(),
                processContext.getBlockchainMeta().getBlockchainId(),
                BlockTaskTypeEnum.POLLING.getCode()
        );
    }

    protected long getLocalBlockHeaderHeight() {
        long curr =  processContext.getBlockchainRepository().getAnchorProcessHeight(
                processContext.getBlockchainMeta().getProduct(),
                processContext.getBlockchainMeta().getBlockchainId(),
                BlockTaskTypeEnum.SYNC.getCode()
        );
        if (curr > 0) {
            return curr;
        }
        BtaDO btaDO = processContext.getBlockchainRepository()
                .getBta(new CrossChainDomain(processContext.getBlockchainDomain()));
        long initHeightInMeta = processContext.getBlockchainMeta().getProperties().getInitBlockHeight();
        if (ObjectUtil.isNull(btaDO)) {
            return initHeightInMeta;
        }
        return Math.min(initHeightInMeta, btaDO.getBta().getInitHeight().longValue());
    }

    protected void saveLocalBlockHeaderHeight(long height) {
        processContext.getBlockchainRepository().setAnchorProcessHeight(
                processContext.getBlockchainMeta().getProduct(),
                processContext.getBlockchainMeta().getBlockchainId(),
                BlockTaskTypeEnum.SYNC.getCode(),
                height
        );
    }

    public long getNotifyBlockHeaderHeight(String workerType) {
        long curr = processContext.getBlockchainRepository().getAnchorProcessHeight(
                processContext.getBlockchainMeta().getProduct(),
                processContext.getBlockchainMeta().getBlockchainId(),
                BlockTaskTypeEnum.NOTIFY.toNotifyWorkerHeightType(workerType)
        );
        if (curr > 0) {
            return curr;
        }
        BtaDO btaDO = processContext.getBlockchainRepository()
                .getBta(new CrossChainDomain(processContext.getBlockchainDomain()));
        long initHeightInMeta = processContext.getBlockchainMeta().getProperties().getInitBlockHeight();
        if (ObjectUtil.isNull(btaDO)) {
            return initHeightInMeta;
        }
        return Math.min(initHeightInMeta, btaDO.getBta().getInitHeight().longValue());
    }

    public long getMaxNotifyBlockHeaderHeightForAllLanes(String workerType) {
        long curr;
        if (processContext.isPtcSupport()) {
            List<TpBtaDO> tpBtaDOList = processContext.getPtcManager().getAllValidTpBtaForDomain(new CrossChainDomain(processContext.getBlockchainDomain()));
            log.debug("get all valid tpbta list with size {}", tpBtaDOList.size());
            curr = tpBtaDOList.stream().map(tpBtaDO -> processContext.getBlockchainRepository().getAnchorProcessHeight(
                    processContext.getBlockchainMeta().getProduct(),
                    processContext.getBlockchainMeta().getBlockchainId(),
                    BlockTaskTypeEnum.NOTIFY.toNotifyWorkerHeightType(workerType),
                    tpBtaDO.getCrossChainLane()
            )).max(Long::compareTo).orElse(0L);
            log.debug("get max curr height {} of all tpbta from domain {}", curr, processContext.getBlockchainDomain());
        } else {
            curr = processContext.getBlockchainRepository().getAnchorProcessHeight(
                    processContext.getBlockchainMeta().getProduct(),
                    processContext.getBlockchainMeta().getBlockchainId(),
                    BlockTaskTypeEnum.NOTIFY.toNotifyWorkerHeightType(workerType)
            );
        }
        if (curr > 0) {
            return curr;
        }
        BtaDO btaDO = processContext.getBlockchainRepository()
                .getBta(new CrossChainDomain(processContext.getBlockchainDomain()));
        long initHeightInMeta = processContext.getBlockchainMeta().getProperties().getInitBlockHeight();
        if (ObjectUtil.isNull(btaDO)) {
            return initHeightInMeta;
        }
        return Math.min(initHeightInMeta, btaDO.getBta().getInitHeight().longValue());
    }

    public long getNotifyBlockHeaderHeight(String workerType, CrossChainLane tpbtaLane) {
        long curr = processContext.getBlockchainRepository().getAnchorProcessHeight(
                processContext.getBlockchainMeta().getProduct(),
                processContext.getBlockchainMeta().getBlockchainId(),
                BlockTaskTypeEnum.NOTIFY.toNotifyWorkerHeightType(workerType),
                tpbtaLane
        );
        if (curr > 0) {
            return curr;
        }
        if (StrUtil.equals(workerType, NotifyTaskTypeEnum.SYSTEM_WORKER.getCode())) {
            return processContext.getBlockchainRepository()
                    .getBta(new CrossChainDomain(processContext.getBlockchainDomain()))
                    .getBta().getInitHeight().longValue();
        }
        return Math.min(
                processContext.getBlockchainMeta().getProperties().getInitBlockHeight(),
                processContext.getBlockchainRepository()
                        .getBta(new CrossChainDomain(processContext.getBlockchainDomain()))
                        .getBta().getInitHeight().longValue()
        );
    }

    public void saveNotifyBlockHeaderHeight(String workerType, long height) {
        processContext.getBlockchainRepository().setAnchorProcessHeight(
                processContext.getBlockchainMeta().getProduct(),
                processContext.getBlockchainMeta().getBlockchainId(),
                BlockTaskTypeEnum.NOTIFY.toNotifyWorkerHeightType(workerType),
                height
        );
    }

    public void saveNotifyBlockHeaderHeight(String workerType, CrossChainLane tpbtaLane, long height) {
        processContext.getBlockchainRepository().setAnchorProcessHeight(
                processContext.getBlockchainMeta().getProduct(),
                processContext.getBlockchainMeta().getBlockchainId(),
                BlockTaskTypeEnum.NOTIFY.toNotifyWorkerHeightType(workerType),
                tpbtaLane,
                height
        );
    }
}