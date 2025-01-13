/*
 * Copyright 2024 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.bridge.ptc.committee.types.trustroot;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import com.alipay.antchain.bridge.ptc.committee.exception.InvalidatedSignatureException;
import com.alipay.antchain.bridge.ptc.committee.types.basic.CommitteeEndorseProof;
import com.alipay.antchain.bridge.ptc.committee.types.basic.CommitteeNodeProof;
import com.alipay.antchain.bridge.ptc.committee.types.basic.NodePublicKeyEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class CommitteeVerifyAnchor {

    private static final short TAG_COMMITTEE_ID = 0x00;
    private static final short TAG_ANCHORS = 0x01;

    public static CommitteeVerifyAnchor decodeJson(byte[] raw) {
        return JSON.parseObject(raw, CommitteeVerifyAnchor.class);
    }

    public static CommitteeVerifyAnchor decode(byte[] raw) {
        return TLVUtils.decode(raw, CommitteeVerifyAnchor.class);
    }

    public CommitteeVerifyAnchor(String committeeId) {
        this.committeeId = committeeId;
        anchors = new ArrayList<>();
    }

    @JSONField(name = "committee_id")
    @TLVField(tag = TAG_COMMITTEE_ID, type = TLVTypeEnum.STRING)
    private String committeeId;

    @TLVField(tag = TAG_ANCHORS, type = TLVTypeEnum.BYTES_ARRAY, order = TAG_ANCHORS)
    private List<NodeAnchorInfo> anchors;

    public CommitteeVerifyAnchor addNode(String nodeId, String keyId, PublicKey nodePublicKey) {
        List<NodeAnchorInfo> nodeAnchorInfos = anchors.stream().filter(anchor -> StrUtil.equals(anchor.getNodeId(), nodeId)).collect(Collectors.toList());
        if (!nodeAnchorInfos.isEmpty()) {
            if (nodeAnchorInfos.get(0).hasPublicKey(keyId)) {
                return this;
            }
            nodeAnchorInfos.get(0).addPublicKey(keyId, nodePublicKey);
        } else {
            NodeAnchorInfo nodeAnchorInfo = new NodeAnchorInfo();
            nodeAnchorInfo.setNodeId(nodeId);
            nodeAnchorInfo.addPublicKey(keyId, nodePublicKey);
            anchors.add(nodeAnchorInfo);
        }
        return this;
    }

    public byte[] encodeJson() {
        return JSON.toJSONBytes(this);
    }

    public byte[] encode() {
        return TLVUtils.encode(this);
    }

    public boolean check(CommitteeEndorseProof proof, byte[] dataSigned) throws InvalidatedSignatureException {
        if (!StrUtil.equals(proof.getCommitteeId(), committeeId)) {
            log.error("committee id not match, expect: {}, actual: {}", committeeId, proof.getCommitteeId());
            return false;
        }
        Map<String, NodeAnchorInfo> nodeAnchorInfoMap = anchors.stream().collect(Collectors.toMap(NodeAnchorInfo::getNodeId, x -> x));
        int cnt = 0;
        for (CommitteeNodeProof sig : proof.getSigs()) {
            if (nodeAnchorInfoMap.containsKey(sig.getNodeId())) {
                for (NodePublicKeyEntry nodePublicKey : nodeAnchorInfoMap.get(sig.getNodeId()).getNodePublicKeys()) {
                    if (sig.getSignAlgo().getSigner().verify(nodePublicKey.getPublicKey(), dataSigned, sig.getSig())) {
                        cnt++;
                        break;
                    }
                }
                nodeAnchorInfoMap.remove(sig.getNodeId());
            } else {
                log.warn("extra sig for node: {}", sig.getNodeId());
            }
        }

        return cnt * 3 > anchors.size() * 2;
    }
}
