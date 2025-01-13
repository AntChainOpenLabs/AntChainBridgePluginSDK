package com.alipay.antchain.bridge.ptc.committee.types.basic;

import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.*;

/**
 * Committee proof in {@link com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyBlockchainTrustAnchor ThirdPartyBlockchainTrustAnchor}#endorseProof
 * and {@link com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyProof ThirdPartyProof}#rawProof
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommitteeEndorseProof {

    private static final short TAG_COMMITTEE_ID = 0x00;
    private static final short TAG_SIGS = 0x01;

    public static CommitteeEndorseProof decodeJson(String json) {
        return JSON.parseObject(json, CommitteeEndorseProof.class);
    }

    public static CommitteeEndorseProof decode(byte[] raw) {
        return TLVUtils.decode(raw, CommitteeEndorseProof.class);
    }

    @TLVField(tag = TAG_COMMITTEE_ID, type = TLVTypeEnum.STRING)
    @JSONField(name = "committee_id")
    private String committeeId;

    @TLVField(tag = TAG_SIGS, type = TLVTypeEnum.BYTES_ARRAY, order = TAG_SIGS)
    @JSONField(name = "sigs")
    private List<CommitteeNodeProof> sigs;

    public byte[] encodeJson() {
        return JSON.toJSONBytes(this);
    }

    public byte[] encode() {
        return TLVUtils.encode(this);
    }
}
