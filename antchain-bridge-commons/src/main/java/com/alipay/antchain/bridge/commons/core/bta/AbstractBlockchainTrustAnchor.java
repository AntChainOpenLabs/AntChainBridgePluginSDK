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

package com.alipay.antchain.bridge.commons.core.bta;

import cn.hutool.core.util.ByteUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractBlockchainTrustAnchor implements IBlockchainTrustAnchor {

    public static final short TLV_TYPE_VERSION = 0x0000;
    public static final short TLV_TYPE_DOMAIN_NAME = 0x0001;
    public static final short TLV_TYPE_SUBJECT_PROD_ID = 0x0002;
    public static final short TLV_TYPE_SUBJECT_PROD_SVN = 0x0003;
    public static final short TLV_TYPE_SUBJECT_IDENTITY = 0x0004;
    public static final short TLV_TYPE_EXTENSION = 0x0005;
    public static final short TLV_TYPE_DOMAIN_CERT_PUB_KEY = 0x0006;
    public static final short TLV_TYPE_UDNS_HASH = 0x0007;
    public static final short TLV_TYPE_DOMAIN_CERT_SIGN_ALGO = 0x0008;
    public static final short TLV_TYPE_DOMAIN_CERT_SIGNATURE = 0x0009;

    public static final short TLV_TYPE_AM_ID = 0x000a;

    @Getter
    public enum SignType {
        SIGN_ALGO_SHA256_WITH_RSA(ByteUtil.intToByte(0)),

        SIGN_ALGO_SHA256_WITH_ECC(ByteUtil.intToByte(1));

        SignType(byte value) {
            this.value = value;
        }

        final byte value;

        public static SignType valueOf(Byte value) {
            switch (value) {
                case 0:
                    return SIGN_ALGO_SHA256_WITH_RSA;
                case 1:
                    return SIGN_ALGO_SHA256_WITH_ECC;
                default:
                    return null;
            }
        }
    }

    @TLVField(tag = TLV_TYPE_DOMAIN_NAME, type = TLVTypeEnum.STRING, order = 1)
    private CrossChainDomain domain;

    @TLVField(tag = TLV_TYPE_SUBJECT_PROD_ID, type = TLVTypeEnum.STRING, order = 2)
    private String subjectProductID;

    @TLVField(tag = TLV_TYPE_SUBJECT_PROD_SVN, type = TLVTypeEnum.UINT32, order = 3)
    private int subjectProductSVN;

    @TLVField(tag = TLV_TYPE_AM_ID, type = TLVTypeEnum.BYTES, order = 4)
    private byte[] authMessageID;

    @TLVField(tag = TLV_TYPE_SUBJECT_IDENTITY, type = TLVTypeEnum.BYTES, order = 5)
    private byte[] subjectIdentity;

    @TLVField(tag = TLV_TYPE_EXTENSION, type = TLVTypeEnum.BYTES, order = 6)
    private byte[] extension;

    @TLVField(tag = TLV_TYPE_DOMAIN_CERT_PUB_KEY, type = TLVTypeEnum.BYTES, order = 7)
    private byte[] bcOwnerPublicKey;

    @TLVField(tag = TLV_TYPE_UDNS_HASH, type = TLVTypeEnum.BYTES, order = 8)
    private byte[] hashToSign;

    @TLVField(tag = TLV_TYPE_DOMAIN_CERT_SIGN_ALGO, type = TLVTypeEnum.UINT8, order = 9)
    private SignType bcOwnerSigAlgo;

    @TLVField(tag = TLV_TYPE_DOMAIN_CERT_SIGNATURE, type = TLVTypeEnum.BYTES, order = 10)
    private byte[] bcOwnerSig;

    @Override
    public byte getBcOwnerSigAlgoValue() {
        return bcOwnerSigAlgo.value;
    }
}
