package com.alipay.antchain.bridge.commons.core.base;

import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CrossChainChannel {

    private static final int TAG_SENDER_DOMAIN = 0;

    private static final int TAG_RECEIVER_DOMAIN = 1;

    public static CrossChainChannel decode(byte[] data) {
        return TLVUtils.decode(data, CrossChainChannel.class);
    }

    @TLVField(tag = TAG_SENDER_DOMAIN, type = TLVTypeEnum.STRING)
    private CrossChainDomain senderDomain;

    @TLVField(tag = TAG_RECEIVER_DOMAIN, type = TLVTypeEnum.STRING, order = TAG_RECEIVER_DOMAIN)
    private CrossChainDomain receiverDomain;

    private byte[] encode() {
        return TLVUtils.encode(this);
    }
}
