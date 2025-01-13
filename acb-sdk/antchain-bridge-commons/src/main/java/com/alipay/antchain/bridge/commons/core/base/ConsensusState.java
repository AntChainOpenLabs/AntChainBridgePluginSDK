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

import java.math.BigInteger;

import cn.hutool.core.util.HexUtil;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ConsensusState {

    private static final short CS_VERSION = 0x00;

    private static final short CS_DOMAIN = 0x01;

    private static final short CS_HEIGHT = 0x02;

    private static final short CS_HASH = 0x03;

    private static final short CS_PARENT_HASH = 0x04;

    private static final short CS_STATE_TIMESTAMP = 0x05;

    private static final short CS_STATE_DATA = 0x06;

    private static final short CS_CONSENSUS_NODE_INFO = 0x07;

    private static final short CS_ENDORSEMENTS = 0x08;

    public static ConsensusState decode(byte[] raw) {
        return TLVUtils.decode(raw, ConsensusState.class);
    }

    @TLVField(tag = CS_VERSION, type = TLVTypeEnum.UINT16)
    private short csVersion;

    @TLVField(tag = CS_DOMAIN, type = TLVTypeEnum.STRING, order = CS_DOMAIN)
    private CrossChainDomain domain;

    @TLVField(tag = CS_HEIGHT, type = TLVTypeEnum.VAR_INT, order = CS_HEIGHT)
    private BigInteger height;

    @TLVField(tag = CS_HASH, type = TLVTypeEnum.BYTES, order = CS_HASH)
    private byte[] hash;

    @TLVField(tag = CS_PARENT_HASH, type = TLVTypeEnum.BYTES, order = CS_PARENT_HASH)
    private byte[] parentHash;

    @TLVField(tag = CS_STATE_TIMESTAMP, type = TLVTypeEnum.UINT64, order = CS_STATE_TIMESTAMP)
    private long stateTimestamp;

    @TLVField(tag = CS_STATE_DATA, type = TLVTypeEnum.BYTES, order = CS_STATE_DATA)
    private byte[] stateData;

    @TLVField(tag = CS_CONSENSUS_NODE_INFO, type = TLVTypeEnum.BYTES, order = CS_CONSENSUS_NODE_INFO)
    private byte[] consensusNodeInfo;

    @TLVField(tag = CS_ENDORSEMENTS, type = TLVTypeEnum.BYTES, order = CS_ENDORSEMENTS)
    private byte[] endorsements;

    public ConsensusState(
            CrossChainDomain domain,
            BigInteger height,
            byte[] hash,
            byte[] parentHash,
            long stateTimestamp,
            byte[] stateData,
            byte[] consensusNodeInfo,
            byte[] endorsements
    ) {
        this.csVersion = 1;
        this.domain = domain;
        this.height = height;
        this.hash = hash;
        this.parentHash = parentHash;
        this.stateTimestamp = stateTimestamp;
        this.stateData = stateData;
        this.consensusNodeInfo = consensusNodeInfo;
        this.endorsements = endorsements;
    }

    public ConsensusState(
            BigInteger height,
            byte[] hash,
            byte[] parentHash,
            long stateTimestamp,
            byte[] stateData,
            byte[] consensusNodeInfo,
            byte[] endorsements
    ) {
        this.csVersion = 1;
        this.height = height;
        this.hash = hash;
        this.parentHash = parentHash;
        this.stateTimestamp = stateTimestamp;
        this.stateData = stateData;
        this.consensusNodeInfo = consensusNodeInfo;
        this.endorsements = endorsements;
    }

    public byte[] encode() {
        return TLVUtils.encode(this);
    }

    public String getParentHashHex() {
        return HexUtil.encodeHexStr(parentHash);
    }

    public String getHashHex() {
        return HexUtil.encodeHexStr(this.hash);
    }
}
