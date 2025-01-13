package com.alipay.antchain.bridge.ptc.committee.types.basic;

import cn.hutool.core.codec.Base64;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommitteeNodeProof {

    private static final short TAG_NODE_ID = 0x00;
    private static final short TAG_SIGN_ALGO = 0x01;
    private static final short TAG_SIG_HEX = 0x02;

    public static CommitteeNodeProof decode(byte[] raw) {
        return TLVUtils.decode(raw, CommitteeNodeProof.class);
    }

    public static CommitteeNodeProof decodeJson(String raw) {
        return JSON.parseObject(raw, CommitteeNodeProof.class);
    }

    @TLVField(tag = TAG_NODE_ID, type = TLVTypeEnum.STRING)
    @JSONField(name = "node_id")
    private String nodeId;

    @TLVField(tag = TAG_SIGN_ALGO, type = TLVTypeEnum.STRING, order = TAG_SIGN_ALGO)
    @JSONField(name = "sign_algo")
    private SignAlgoEnum signAlgo;

    @TLVField(tag = TAG_SIG_HEX, type = TLVTypeEnum.BYTES, order = TAG_SIG_HEX)
    @JSONField(name = "signature")
    private byte[] signature;

    public String getSignature() {
        return Base64.encode(signature);
    }

    public byte[] getSig() {
        return signature;
    }

    public byte[] encodeJson() {
        return JSON.toJSONString(this).getBytes();
    }

    public byte[] encode() {
        return TLVUtils.encode(this);
    }
}
