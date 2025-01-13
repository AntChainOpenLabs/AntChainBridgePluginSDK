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

package com.alipay.antchain.bridge.commons.core.ptc;

import com.alipay.antchain.bridge.commons.core.base.ConsensusState;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVPacket;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class ValidatedConsensusState extends ConsensusState {

    private static final short VCS_VERSION = 0x0100;

    private static final short VCS_PTC_TYPE = 0x0101;

    private static final short VCS_PTC_OID = 0x0102;

    private static final short VCS_TPBTA_VERSION = 0x0103;

    private static final short VCS_PTC_PROOF = 0x01ff;

    public static ValidatedConsensusState decode(byte[] raw) {
        short version = TLVPacket.decode(raw).getItemForTag(VCS_VERSION).getUint16Value();
        if (version == ValidatedConsensusStateV1.MY_VERSION) {
            return ValidatedConsensusStateV1.decode(raw);
        }
        throw new RuntimeException("Unsupported version of ValidatedConsensusState: " + version);
    }

    @TLVField(tag = VCS_VERSION, type = TLVTypeEnum.UINT16, order = VCS_VERSION)
    private short vcsVersion;

    @TLVField(tag = VCS_PTC_TYPE, type = TLVTypeEnum.UINT8, order = VCS_PTC_TYPE)
    private PTCTypeEnum ptcType;

    @TLVField(tag = VCS_PTC_OID, type = TLVTypeEnum.BYTES, order = VCS_PTC_OID)
    private ObjectIdentity ptcOid;

    @TLVField(tag = VCS_TPBTA_VERSION, type = TLVTypeEnum.UINT32, order = VCS_TPBTA_VERSION)
    private int tpbtaVersion;

    @TLVField(tag = VCS_PTC_PROOF, type = TLVTypeEnum.BYTES, order = VCS_PTC_PROOF)
    private byte[] ptcProof;

    public byte[] getEncodedToSign() {
        return TLVUtils.encode(this, VCS_TPBTA_VERSION);
    }
}
