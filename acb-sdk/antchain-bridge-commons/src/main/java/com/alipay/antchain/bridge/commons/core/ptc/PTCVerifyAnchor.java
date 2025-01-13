package com.alipay.antchain.bridge.commons.core.ptc;

import java.math.BigInteger;

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
public class PTCVerifyAnchor {

    private static final short PTC_VA_VERSION = 0;

    private static final short PTC_VA_ANCHOR = 1;

    public static PTCVerifyAnchor decode(byte[] data) {
        return TLVUtils.decode(data, PTCVerifyAnchor.class);
    }

    @TLVField(tag = PTC_VA_VERSION, type = TLVTypeEnum.VAR_INT, order = PTC_VA_VERSION)
    private BigInteger version;

    @TLVField(tag = PTC_VA_ANCHOR, type = TLVTypeEnum.BYTES, order = PTC_VA_ANCHOR)
    private byte[] anchor;

    public byte[] encode() {
        return TLVUtils.encode(this);
    }
}
