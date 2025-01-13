package com.alipay.antchain.bridge.ptc.committee.types.tpbta;

import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import com.alipay.antchain.bridge.ptc.committee.types.basic.NodePublicKeyEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NodeEndorseInfo {

    private static final short TAG_NODE_ID = 0x00;
    private static final short TAG_REQUIRED = 0x01;
    private static final short TAG_PUBLIC_KEY = 0x02;

    public static NodeEndorseInfo decode(byte[] raw) {
        return TLVUtils.decode(raw, NodeEndorseInfo.class);
    }

    @JSONField(name = "node_id")
    @TLVField(tag = TAG_NODE_ID, type = TLVTypeEnum.STRING, order = TAG_NODE_ID)
    private String nodeId;

    @JSONField(name = "required")
    @TLVField(tag = TAG_REQUIRED, type = TLVTypeEnum.UINT8, order = TAG_REQUIRED)
    private boolean required;

    @JSONField(name = "node_public_key")
    @TLVField(tag = TAG_PUBLIC_KEY, type = TLVTypeEnum.BYTES, order = TAG_PUBLIC_KEY)
    private NodePublicKeyEntry publicKey;

    public boolean isOptional() {
        return !required;
    }

    public byte[] encode() {
        return TLVUtils.encode(this);
    }
}
