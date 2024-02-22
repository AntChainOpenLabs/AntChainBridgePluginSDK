package com.alipay.antchain.bridge.commons;

import java.nio.ByteOrder;

import cn.hutool.core.util.ByteUtil;
import cn.hutool.core.util.HexUtil;
import com.alipay.antchain.bridge.commons.core.sdp.*;
import org.junit.Assert;
import org.junit.Test;

public class SDPMessageTest {

    private static final String RAW_MESSAGE_HEX_V1 = "8f5baede046f6bf200000000000000000000000000000000000000000000000017943daf4ed8cb477ef2e0048f5baede046f6bf797943daf4ed8cb477ef2e0040000000000000000000000000000000000000000000000000000000000000028ffffffff000000000000000000000000d9145cce52d386f254917e481eb44e9943f3913874657374646f6d61696e00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000a";

    private static final String TARGET_DOMAIN_V1 = HexUtil.decodeHexStr(RAW_MESSAGE_HEX_V1.substring(264, 284));

    private static final String TARGET_ID_V1 = RAW_MESSAGE_HEX_V1.substring(200, 264);

    private static final String PAYLOAD_V1 = RAW_MESSAGE_HEX_V1.substring(64, 128) + RAW_MESSAGE_HEX_V1.substring(0, 16);

    private static final String RAW_MESSAGE_HEX_V2 = "17943daf4ed8cb477ef2e0048f5baede046f6bf797943daf4ed8cb477ef2e0048f5baede046f6bf200000028ffffffff000000000000000001000000000000000000000000d9145cce52d386f254917e481eb44e9943f3913874657374646f6d61696e0000000aff000002";

    private static final String TARGET_DOMAIN_V2 = HexUtil.decodeHexStr(RAW_MESSAGE_HEX_V2.substring(178, 198));

    private static final String TARGET_ID_V2 = RAW_MESSAGE_HEX_V2.substring(114, 178);

    private static final String PAYLOAD_V2 = RAW_MESSAGE_HEX_V2.substring(0, 80);

    private static final long NONCE_V2 = ByteUtil.bytesToLong(HexUtil.decodeHex(RAW_MESSAGE_HEX_V2.substring(96, 112)), ByteOrder.BIG_ENDIAN);

    private static final String RAW_MESSAGE_HEX_ACK_ERROR_V2 = "0000002817943daf4ed8cb477ef2e0048f5baede046f6bf797943daf4ed8cb477ef2e0048f5baede046f6bf2000000056572726f7200000035ffffffff000000000000000001000000000000000000000000d9145cce52d386f254917e481eb44e9943f3913874657374646f6d61696e0000000aff000002";

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

    @Test
    public void testSDPMessageV2Encode() {
        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(
                2,
                TARGET_DOMAIN_V2,
                HexUtil.decodeHex(TARGET_ID_V2),
                AtomicFlagEnum.ATOMIC_REQUEST,
                NONCE_V2,
                AbstractSDPMessage.UNORDERED_SEQUENCE,
                HexUtil.decodeHex(PAYLOAD_V2)
        );
        Assert.assertTrue(sdpMessage instanceof SDPMessageV2);
        Assert.assertEquals(RAW_MESSAGE_HEX_V2, HexUtil.encodeHexStr(sdpMessage.encode()));

        ISDPMessage sdpMessageAckError = SDPMessageFactory.createSDPMessage(
                2,
                TARGET_DOMAIN_V2,
                HexUtil.decodeHex(TARGET_ID_V2),
                AtomicFlagEnum.ATOMIC_REQUEST,
                NONCE_V2,
                AbstractSDPMessage.UNORDERED_SEQUENCE,
                new SDPMessageV2.SDPPayloadV2(HexUtil.decodeHex(PAYLOAD_V2), "error").getPayload()
        );
        Assert.assertTrue(sdpMessageAckError instanceof SDPMessageV2);
        Assert.assertEquals(RAW_MESSAGE_HEX_ACK_ERROR_V2, HexUtil.encodeHexStr(sdpMessageAckError.encode()));
    }

    @Test
    public void testSDPMessageV2Decode() {
        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(HexUtil.decodeHex(RAW_MESSAGE_HEX_V2));
        Assert.assertTrue(sdpMessage instanceof SDPMessageV2);
        Assert.assertEquals(TARGET_DOMAIN_V2, sdpMessage.getTargetDomain().getDomain());
        Assert.assertEquals(TARGET_ID_V2, sdpMessage.getTargetIdentity().toHex());
        Assert.assertEquals(NONCE_V2, sdpMessage.getNonce());
        Assert.assertEquals(PAYLOAD_V2, HexUtil.encodeHexStr(sdpMessage.getPayload()));

        sdpMessage = SDPMessageFactory.createSDPMessage(HexUtil.decodeHex(RAW_MESSAGE_HEX_ACK_ERROR_V2));
        Assert.assertTrue(sdpMessage instanceof SDPMessageV2);
        Assert.assertEquals(TARGET_DOMAIN_V2, sdpMessage.getTargetDomain().getDomain());
        Assert.assertEquals(TARGET_ID_V2, sdpMessage.getTargetIdentity().toHex());
        Assert.assertEquals(NONCE_V2, sdpMessage.getNonce());
        Assert.assertEquals(PAYLOAD_V2, HexUtil.encodeHexStr(((SDPMessageV2) sdpMessage).getSDPPayloadV2().getOriginalMessageFromErrorAck()));
        Assert.assertEquals("error", ((SDPMessageV2) sdpMessage).getSDPPayloadV2().getErrorMsgFromErrorAck());
    }
}
