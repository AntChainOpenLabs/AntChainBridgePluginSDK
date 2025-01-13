package com.alipay.antchain.bridge.commons;

import com.alipay.antchain.bridge.commons.core.rcc.IdempotentInfo;
import com.alipay.antchain.bridge.commons.core.rcc.ReliableCrossChainMessage;
import com.alipay.antchain.bridge.commons.core.rcc.ReliableCrossChainMsgProcessStateEnum;
import org.junit.Assert;
import org.junit.Test;

public class ReliableCrossChainMessageTest {
    @Test
    public void test() {
        ReliableCrossChainMessage rccMsg = new ReliableCrossChainMessage(
                new IdempotentInfo(
                        "senderDomain",
                        "senderIdentity".getBytes(),
                        "receiverDomain",
                        "receiverIdentity".getBytes(),
                        10
                ),
                ReliableCrossChainMsgProcessStateEnum.PENDING,
                "originalHash",
                "currentHash",
                10,
                System.currentTimeMillis(),
                "errorMsg",
                "rawTx".getBytes()
        );

        byte[] data = rccMsg.encode();
        ReliableCrossChainMessage rccMsg1 = ReliableCrossChainMessage.decode(data);
        Assert.assertEquals(rccMsg.getOriginalHash(), rccMsg1.getOriginalHash());
    }
}
