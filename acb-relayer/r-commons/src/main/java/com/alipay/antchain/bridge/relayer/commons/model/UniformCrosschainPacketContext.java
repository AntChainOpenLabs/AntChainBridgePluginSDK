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

package com.alipay.antchain.bridge.relayer.commons.model;

import java.util.Date;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.commons.core.base.UniformCrosschainPacket;
import com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyProof;
import com.alipay.antchain.bridge.relayer.commons.constant.UniformCrosschainPacketStateEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UniformCrosschainPacketContext {

    @JSONField(serialize = false, deserialize = false)
    private long id;

    private String ucpId;

    private String product;

    private String blockchainId;

    private String tpbtaLaneKey;

    private Integer tpbtaVersion;

    private UniformCrosschainPacket ucp;

    private String udagPath;

    private UniformCrosschainPacketStateEnum processState;

    private boolean fromNetwork;

    private String relayerId;

    @JSONField(serialize = false, deserialize = false)
    private RelayerNodeInfo remoteRelayerNodeInfo;

    public UniformCrosschainPacketContext() {
        generateUcpId();
    }

    private void generateUcpId() {
        this.ucpId = StrUtil.isEmpty(this.udagPath) ?
                HexUtil.encodeHexStr(RandomUtil.randomBytes(32))
                : DigestUtil.sha256Hex(this.udagPath);
    }

    @JSONField(serialize = false, deserialize = false)
    public int getVersion() {
        return this.ucp.getVersion();
    }

    @JSONField(serialize = false, deserialize = false)
    public String getSrcDomain() {
        return this.ucp.getSrcDomain().getDomain();
    }

    @JSONField(serialize = false, deserialize = false)
    public String getBlockHash() {
        return HexUtil.encodeHexStr(this.ucp.getSrcMessage().getProvableData().getBlockHash());
    }

    @JSONField(serialize = false, deserialize = false)
    public String getTxHash() {
        return HexUtil.encodeHexStr(this.ucp.getSrcMessage().getProvableData().getTxHash());
    }

    @JSONField(serialize = false, deserialize = false)
    public int getProtocolType() {
        return this.ucp.getSrcMessage().getType().ordinal();
    }

    @JSONField(serialize = false, deserialize = false)
    public byte[] getSrcMessage() {
        return this.ucp.getSrcMessage().encode();
    }

    @JSONField(serialize = false, deserialize = false)
    public String getPtcOid() {
        return ObjectUtil.isNull(this.ucp.getPtcId()) ? null : HexUtil.encodeHexStr(this.ucp.getPtcId().encode());
    }

    @JSONField(serialize = false, deserialize = false)
    public ThirdPartyProof getTpProof() {
        return this.ucp.getTpProof();
    }

    @JSONField(serialize = false, deserialize = false)
    public Date getLedgerTime() {
        return new Date(this.ucp.getSrcMessage().getProvableData().getTimestamp());
    }

    @JSONField(serialize = false, deserialize = false)
    public CrossChainLane getCrossChainLane() {
        return ucp.getCrossChainLane();
    }

    @JSONField(serialize = false, deserialize = false)
    public CrossChainLane getTpBtaLane() {
        return CrossChainLane.fromLaneKey(this.tpbtaLaneKey);
    }

    public boolean isEndorsedByTpBta() {
        return StrUtil.isNotEmpty(this.tpbtaLaneKey);
    }

    @JSONField(serialize = false, deserialize = false)
    public byte[] getRawTpProof() {
        return ObjectUtil.isNotNull(this.getTpProof()) ? this.getTpProof().encode() : null;
    }
}
