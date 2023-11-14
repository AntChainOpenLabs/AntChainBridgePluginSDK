package com.alipay.antchain.bridge.commons.bcdns;

import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PTCContentEntity {
    public static final short TLV_TYPE_PTC_CONTENT_NAME = 0x0000;

    public static final short TLV_TYPE_PTC_CONTENT_TYPE = 0x0001;

    public static final short TLV_TYPE_PTC_CONTENT_ID = 0x0002;

    public static final short TLV_TYPE_PTC_CONTENT_PUBLIC_KEY = 0x0003;

    public static PTCContentEntity decode(byte[] rawData) {
        return TLVUtils.decode(rawData, PTCContentEntity.class);
    }

    @TLVField(tag = TLV_TYPE_PTC_CONTENT_NAME, type = TLVTypeEnum.STRING)
    private String name;

    @TLVField(tag = TLV_TYPE_PTC_CONTENT_TYPE, type = TLVTypeEnum.UINT8, order = 1)
    private PTCTypeEnum type;

    @TLVField(tag = TLV_TYPE_PTC_CONTENT_ID, type = TLVTypeEnum.BYTES, order = 2)
    private ObjectIdentity applicant;

    @TLVField(tag = TLV_TYPE_PTC_CONTENT_PUBLIC_KEY, type = TLVTypeEnum.STRING, order = 3)
    private String publicKey;

    public byte[] encode() {
        return TLVUtils.encode(this);
    }
}
