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

package com.alipay.antchain.bridge.ptc.committee.types.tpbta;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import com.alipay.antchain.bridge.ptc.committee.exception.InvalidatedSignatureException;
import com.alipay.antchain.bridge.ptc.committee.types.basic.CommitteeEndorseProof;
import com.alipay.antchain.bridge.ptc.committee.types.basic.CommitteeNodeProof;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class CommitteeEndorseRoot {

    private static final short TAG_COMMITTEE_ID = 0x00;
    private static final short TAG_POLICY = 0x01;
    private static final short TAG_ENDORSERS = 0x02;

    public static CommitteeEndorseRoot decodeJson(String json) {
        return JSON.parseObject(json, CommitteeEndorseRoot.class);
    }

    public static CommitteeEndorseRoot decode(byte[] raw) {
        return TLVUtils.decode(raw, CommitteeEndorseRoot.class);
    }

    @JSONField(name = "committee_id")
    @TLVField(tag = TAG_COMMITTEE_ID, type = TLVTypeEnum.STRING)
    private String committeeId;

    @JSONField(name = "policy")
    @TLVField(tag = TAG_POLICY, type = TLVTypeEnum.BYTES, order = TAG_POLICY)
    private OptionalEndorsePolicy policy;

    @JSONField(name = "endorsers")
    @TLVField(tag = TAG_ENDORSERS, type = TLVTypeEnum.BYTES_ARRAY, order = TAG_ENDORSERS)
    private List<NodeEndorseInfo> endorsers;

    public byte[] encodeJson() {
        return JSON.toJSONBytes(this);
    }

    public byte[] encode() {
        return TLVUtils.encode(this);
    }

    public boolean check(CommitteeEndorseProof proof, byte[] dataSigned) throws InvalidatedSignatureException {
        if (!StrUtil.equals(proof.getCommitteeId(), committeeId)) {
            return false;
        }
        Map<String, NodeEndorseInfo> endorsersRequired = endorsers.stream().filter(NodeEndorseInfo::isRequired)
                .collect(Collectors.toMap(
                        NodeEndorseInfo::getNodeId,
                        endorser -> endorser
                ));
        Map<String, NodeEndorseInfo> endorsersOptional = endorsers.stream().filter(NodeEndorseInfo::isOptional)
                .collect(Collectors.toMap(
                        NodeEndorseInfo::getNodeId,
                        endorser -> endorser
                ));
        int optionalCnt = 0;
        for (CommitteeNodeProof sig : proof.getSigs()) {
            if (endorsersRequired.containsKey(sig.getNodeId())) {
                if (!sig.getSignAlgo().getSigner().verify(
                        endorsersRequired.get(sig.getNodeId()).getPublicKey().getPublicKey(),
                        dataSigned,
                        Base64.decode(sig.getSignature())
                )) {
                    log.error(StrUtil.format("Invalidated signature for required node, committeeId: {}, nodeId: {}, sigBase64: {}, publicKey: {}",
                            committeeId, sig.getNodeId(), sig.getSignature(),
                            Base64.encode(endorsersRequired.get(sig.getNodeId()).getPublicKey().getPublicKey().getEncoded())));
                    return false;
                }
                endorsersRequired.remove(sig.getNodeId());
            } else if (endorsersOptional.containsKey(sig.getNodeId())) {
                if(!sig.getSignAlgo().getSigner().verify(
                        endorsersOptional.get(sig.getNodeId()).getPublicKey().getPublicKey(),
                        dataSigned,
                        Base64.decode(sig.getSignature())
                )) {
                    log.error(StrUtil.format("Invalidated signature for optional node, committeeId: {}, nodeId: {}, sigBase64: {}, publicKey: {}",
                            committeeId, sig.getNodeId(), sig.getSignature(),
                            Base64.encode(endorsersRequired.get(sig.getNodeId()).getPublicKey().getPublicKey().getEncoded())));
                    return false;
                }
                ++optionalCnt;
                endorsersOptional.remove(sig.getNodeId());
            } else {
                log.warn("extra sig for node: {}", sig.getNodeId());
            }
        }

        return this.policy.getThreshold().check(optionalCnt) && endorsersRequired.isEmpty();
    }
}
