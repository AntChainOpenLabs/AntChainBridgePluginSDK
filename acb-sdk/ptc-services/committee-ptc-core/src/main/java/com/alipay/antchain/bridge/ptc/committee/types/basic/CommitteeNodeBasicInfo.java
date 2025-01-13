package com.alipay.antchain.bridge.ptc.committee.types.basic;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CommitteeNodeBasicInfo {

    private static final short TAG_NODE_ID = 0x00;
    private static final short TAG_NODE_PUBKEYS = 0x01;

    @JSONField(name = "node_id")
    @TLVField(tag = TAG_NODE_ID, type = TLVTypeEnum.STRING)
    private String nodeId;

    @JSONField(name = "node_public_keys")
    @TLVField(tag = TAG_NODE_PUBKEYS, type = TLVTypeEnum.BYTES_ARRAY, order = TAG_NODE_PUBKEYS)
    private List<NodePublicKeyEntry> nodePublicKeys = new ArrayList<>();

    public boolean hasPublicKey(String keyId) {
        for (NodePublicKeyEntry entry : nodePublicKeys) {
            if (StrUtil.equals(entry.getKeyId(), keyId)) {
                return true;
            }
        }
        return false;
    }

    public void addPublicKey(String keyId, PublicKey nodePublicKey) {
        nodePublicKeys.add(new NodePublicKeyEntry(keyId, nodePublicKey));
    }
}
