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

package com.alipay.antchain.bridge.commons.core.ptc;

import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ThirdPartyProof {

    private static final short TP_PROOF_TPBTA_VERSION = 0x0100;

    private static final short TLV_PROOF_TPBTA_CROSSCHAIN_LANE = 0x0101;

    // tags from ODATS
    public static final short TLV_ORACLE_PUBKEY_HASH = 0;
    public static final short TLV_ORACLE_REQUEST_ID = 1;
    public static final short TLV_ORACLE_REQUEST_BODY = 2;
    public static final short TLV_ORACLE_SIGNATURE_TYPE = 3;
    public static final short TLV_ORACLE_REQUEST = 4;
    public static final short TLV_ORACLE_RESPONSE_BODY = 5;  // 这里填充RESPONSE 内容
    public static final short TLV_ORACLE_RESPONSE_SIGNATURE = 6;
    public static final short TLV_ORACLE_ERROR_CODE = 7;
    public static final short TLV_ORACLE_ERROR_MSG = 8;
    public static short TLV_PROOF_SENDER_DOMAIN = 9;

    private static final short TP_PROOF_RAW_PROOF = 0x01ff;

    public static ThirdPartyProof decode(byte[] data) {
        return TLVUtils.decode(data, ThirdPartyProof.class);
    }

    public static ThirdPartyProof create(
            int tpbtaVersion,
            byte[] rawMessage,
            CrossChainLane tpbtaCrossChainLane
    ) {
        ThirdPartyProof thirdPartyProof = new ThirdPartyProof();
        thirdPartyProof.setResp(new ThirdPartyResp(rawMessage));
        thirdPartyProof.setTpbtaVersion(tpbtaVersion);
        thirdPartyProof.setTpbtaCrossChainLane(tpbtaCrossChainLane);
        return thirdPartyProof;
    }

    @TLVField(tag = TLV_ORACLE_RESPONSE_BODY, type = TLVTypeEnum.BYTES, order = TLV_ORACLE_RESPONSE_BODY)
    private ThirdPartyResp resp;

    @TLVField(tag = TLV_PROOF_TPBTA_CROSSCHAIN_LANE, type = TLVTypeEnum.BYTES, order = TLV_PROOF_TPBTA_CROSSCHAIN_LANE)
    private CrossChainLane tpbtaCrossChainLane;

    @TLVField(tag = TP_PROOF_TPBTA_VERSION, type = TLVTypeEnum.UINT32)
    private int tpbtaVersion;

    @TLVField(tag = TP_PROOF_RAW_PROOF, type = TLVTypeEnum.BYTES, order = TP_PROOF_RAW_PROOF)
    private byte[] rawProof;

    public byte[] encode() {
        return TLVUtils.encode(this);
    }

    public byte[] getEncodedToSign() {
        return TLVUtils.encode(this, TP_PROOF_RAW_PROOF - 1);
    }
}
