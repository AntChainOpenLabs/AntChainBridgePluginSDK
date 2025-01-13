package com.alipay.antchain.bridge.commons.core.rcc;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bouncycastle.util.encoders.Hex;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IdempotentInfo {
    private static final short SENDER_DOMAIN = 0x00;
    private static final short SENDER_IDENTITY = 0x01;
    private static final short RECEIVER_DOMAIN = 0x02;
    private static final short RECEIVER_IDENTITY = 0x03;
    private static final short NONCE = 0x04;

    @TLVField(tag = SENDER_DOMAIN, type = TLVTypeEnum.STRING)
    private String senderDomain;

    @TLVField(tag = SENDER_IDENTITY, type = TLVTypeEnum.BYTES, order = SENDER_IDENTITY)
    private byte[] senderIdentity;

    @TLVField(tag = RECEIVER_DOMAIN, type = TLVTypeEnum.STRING, order = RECEIVER_DOMAIN)
    private String receiverDomain;

    @TLVField(tag = RECEIVER_IDENTITY, type = TLVTypeEnum.BYTES, order = RECEIVER_IDENTITY)
    private byte[] receiverIdentity;

    @TLVField(tag = NONCE, type = TLVTypeEnum.UINT64, order = NONCE)
    private long nonce;

    public byte[] encode() {
        return TLVUtils.encode(this);
    }

    public static IdempotentInfo decode(byte[] raw) {
        return TLVUtils.decode(raw, IdempotentInfo.class);
    }

    public String getInfo(){
        return StrUtil.format("{}-{}-{}-{}-{}",
                senderDomain,
                Hex.toHexString(senderIdentity),
                receiverDomain,
                Hex.toHexString(receiverIdentity),
                nonce);
    }
}
