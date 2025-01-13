/*
 * Copyright 2023 Ant Group
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

package com.alipay.antchain.bridge.commons.core.base;

import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageFactory;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyProof;
import com.alipay.antchain.bridge.commons.core.sdp.ISDPMessage;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageFactory;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import com.alipay.antchain.bridge.commons.utils.crypto.HashAlgoEnum;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class UniformCrosschainPacket {

    private static final short UCP_VERSION = 0;

    private static final short UCP_SRC_DOMAIN = 1;

    private static final short UCP_SRC_MSG = 2;

    private static final short UCP_PTC_ID = 3;

    private static final short UCP_TP_PROOF = 0xff;

    public static UniformCrosschainPacket decode(byte[] data) {
        return TLVUtils.decode(data, UniformCrosschainPacket.class);
    }

    @TLVField(tag = UCP_VERSION, type = TLVTypeEnum.UINT16)
    private short version = 1;

    @TLVField(tag = UCP_SRC_DOMAIN, type = TLVTypeEnum.STRING, order = UCP_SRC_DOMAIN)
    private CrossChainDomain srcDomain;

    @TLVField(tag = UCP_SRC_MSG, type = TLVTypeEnum.BYTES, order = UCP_SRC_MSG)
    private CrossChainMessage srcMessage;

    @TLVField(tag = UCP_PTC_ID, type = TLVTypeEnum.BYTES, order = UCP_PTC_ID)
    private ObjectIdentity ptcId;

    @TLVField(tag = UCP_TP_PROOF, type = TLVTypeEnum.BYTES, order = UCP_TP_PROOF)
    private ThirdPartyProof tpProof;

    public UniformCrosschainPacket(CrossChainDomain srcDomain, CrossChainMessage srcMessage, ObjectIdentity ptcId) {
        this.srcDomain = srcDomain;
        this.srcMessage = srcMessage;
        this.ptcId = ptcId;
    }

    public byte[] encode() {
        return TLVUtils.encode(this);
    }

    @JSONField(deserialize = false, serialize = false)
    public CrossChainLane getCrossChainLane() {
        if (srcMessage.getType() != CrossChainMessage.CrossChainMessageType.AUTH_MSG) {
            return null;
        }
        IAuthMessage authMessage = AuthMessageFactory.createAuthMessage(srcMessage.getMessage());
        if (authMessage.getUpperProtocol() != 0) {
            return new CrossChainLane(srcDomain);
        }
        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(authMessage.getPayload());

        return new CrossChainLane(srcDomain, sdpMessage.getTargetDomain(), authMessage.getIdentity(), sdpMessage.getTargetIdentity());
    }

    public void cleanProvableData() {
        this.srcMessage.setProvableData(null);
    }

    @JSONField(deserialize = false, serialize = false)
    public byte[] getSigningBody() {
        CrossChainMessage.ProvableLedgerData provableLedgerData = getSrcMessage().getProvableData();
        cleanProvableData();
        byte[] result = TLVUtils.encode(this, ListUtil.toList((int) UCP_SRC_DOMAIN, (int) UCP_SRC_MSG));
        getSrcMessage().setProvableData(provableLedgerData);
        return result;
    }

    @JSONField(deserialize = false, serialize = false)
    public byte[] getMessageHash(HashAlgoEnum hashAlgo) {
        return hashAlgo.hash(getSigningBody());
    }
}
