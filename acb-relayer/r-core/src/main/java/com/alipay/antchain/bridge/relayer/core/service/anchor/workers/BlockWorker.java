package com.alipay.antchain.bridge.relayer.core.service.anchor.workers;

import com.alipay.antchain.bridge.relayer.commons.model.TpBtaDO;
import com.alipay.antchain.bridge.relayer.core.service.anchor.context.AnchorProcessContext;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.AbstractBlock;
import lombok.Getter;

/**
 * 区块worker，用于处理同步到的区块。
 * <p>
 * 设计worker目的是为了扩展anchor的能力，同一个区块里，存在不同的交易需要有不同的流程处理。
 */
@Getter
public abstract class BlockWorker {

    private final AnchorProcessContext processContext;

    public BlockWorker(AnchorProcessContext processContext) {
        this.processContext = processContext;
    }

    /**
     * 处理区块，返回是否处理成功，如果返回false，外层会一直重复该区块直到处理成功
     *
     * @param block
     * @return
     */
    public abstract boolean process(AbstractBlock block, TpBtaDO tpBtaDO);
}
