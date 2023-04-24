package com.alipay.antchain.bridge.commons;

import cn.hutool.core.util.HexUtil;
import com.alipay.antchain.bridge.commons.core.sdp.AbstractSDPMessage;
import com.alipay.antchain.bridge.commons.core.sdp.ISDPMessage;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageFactory;
import org.junit.Assert;
import org.junit.Test;

public class SDPMessageTest {

    private static final String RAW_MESSAGE_HEX_V1 = "8f5baede046f6bf200000000000000000000000000000000000000000000000017943daf4ed8cb477ef2e0048f5baede046f6bf797943daf4ed8cb477ef2e0040000000000000000000000000000000000000000000000000000000000000028ffffffff000000000000000000000000d9145cce52d386f254917e481eb44e9943f3913874657374646f6d61696e00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000a";

    private static final String TARGET_DOMAIN_V1 = HexUtil.decodeHexStr(RAW_MESSAGE_HEX_V1.substring(264, 284));

    private static final String TARGET_ID_V1 = RAW_MESSAGE_HEX_V1.substring(200, 264);

    private static final String PAYLOAD_V1 = RAW_MESSAGE_HEX_V1.substring(64, 128) + RAW_MESSAGE_HEX_V1.substring(0, 16);

    @Test
    public void testSDPMessageV1Decode() {
        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(HexUtil.decodeHex(RAW_MESSAGE_HEX_V1));
        Assert.assertEquals(1, sdpMessage.getVersion());
        Assert.assertEquals(TARGET_DOMAIN_V1, sdpMessage.getTargetDomain().getDomain());
        Assert.assertEquals(AbstractSDPMessage.UNORDERED_SEQUENCE, sdpMessage.getSequence());
        Assert.assertEquals(TARGET_ID_V1, sdpMessage.getTargetIdentity().toHex());
        Assert.assertEquals(PAYLOAD_V1, HexUtil.encodeHexStr(sdpMessage.getPayload()));
    }

    @Test
    public void testSDPMessageV1Encode() {
        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(1, TARGET_DOMAIN_V1,
                HexUtil.decodeHex(TARGET_ID_V1), AbstractSDPMessage.UNORDERED_SEQUENCE, HexUtil.decodeHex(PAYLOAD_V1));
        Assert.assertEquals(RAW_MESSAGE_HEX_V1, HexUtil.encodeHexStr(sdpMessage.encode()));
    }
}
