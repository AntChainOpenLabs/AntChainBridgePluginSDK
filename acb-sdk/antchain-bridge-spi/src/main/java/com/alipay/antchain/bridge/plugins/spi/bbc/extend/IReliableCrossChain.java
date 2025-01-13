package com.alipay.antchain.bridge.plugins.spi.bbc.extend;

import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.commons.core.rcc.ReliableCrossChainMessage;

/**
 * Supports reliable cross-chain features.
 */
public interface IReliableCrossChain {
    /**
     * 可靠上链消息重试
     *  该方法在可靠上链重试分布式任务中进行被调用以进行消息的重试发送
     *  该方法不会对消息内容进行修改，只做消息的提交发送
     * @param msg
     */
    CrossChainMessageReceipt reliableRetry(ReliableCrossChainMessage msg);
}
