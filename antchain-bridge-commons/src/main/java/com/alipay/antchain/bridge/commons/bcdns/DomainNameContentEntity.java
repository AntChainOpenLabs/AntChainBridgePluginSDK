package com.alipay.antchain.bridge.commons.bcdns;

import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
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
public class DomainNameContentEntity {

    public static final short TLV_TYPE_DOMAIN_NAME_CONTENT_DOMAIN_NAME_TYPE = 0x0000;

    public static final short TLV_TYPE_DOMAIN_NAME_CONTENT_DOMAIN_NAME = 0x0001;

    public static final short TLV_TYPE_DOMAIN_NAME_CONTENT_ID = 0x0002;

    public static final short TLV_TYPE_DOMAIN_NAME_CONTENT_PUBLIC_KEY = 0x0003;

    public static DomainNameContentEntity decode(byte[] rawData) {
        return TLVUtils.decode(rawData, DomainNameContentEntity.class);
    }

    @TLVField(tag = TLV_TYPE_DOMAIN_NAME_CONTENT_DOMAIN_NAME_TYPE, type = TLVTypeEnum.UINT8)
    private DomainNameTypeEnum type;

    @TLVField(tag = TLV_TYPE_DOMAIN_NAME_CONTENT_DOMAIN_NAME, type = TLVTypeEnum.STRING, order = 1)
    private CrossChainDomain domainName;

    @TLVField(tag = TLV_TYPE_DOMAIN_NAME_CONTENT_ID, type = TLVTypeEnum.BYTES, order = 2)
    private ObjectIdentity applicant;

    @TLVField(tag = TLV_TYPE_DOMAIN_NAME_CONTENT_PUBLIC_KEY, type = TLVTypeEnum.STRING, order = 3)
    private String publicKey;

    public byte[] encode() {
        return TLVUtils.encode(this);
    }
}
