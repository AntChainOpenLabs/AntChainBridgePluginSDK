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

package com.alipay.antchain.bridge.commons.bcdns;

import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AbstractCrossChainCertificate {
    @Getter
    @Setter
    @AllArgsConstructor
    public static class IssueProof {

        public static final IssueProof EMPTY_PROOF = new IssueProof("", "".getBytes(), "".getBytes());

        public static final short TLV_TYPE_ISSUE_PROOF_HASH_ALGO = 0x0000;

        public static final short TLV_TYPE_ISSUE_PROOF_CERT_HASH = 0x0001;

        public static final short TLV_TYPE_ISSUE_PROOF_RAW_PROOF = 0x0002;

        @TLVField(tag = TLV_TYPE_ISSUE_PROOF_HASH_ALGO, type = TLVTypeEnum.STRING)
        private String hashAlgo;

        @TLVField(tag = TLV_TYPE_ISSUE_PROOF_CERT_HASH, type = TLVTypeEnum.BYTES, order = 1)
        private byte[] certHash;

        @TLVField(tag = TLV_TYPE_ISSUE_PROOF_RAW_PROOF, type = TLVTypeEnum.BYTES, order = 2)
        private byte[] rawProof;
    }

    public static final short TLV_TYPE_CERT_VERSION = 0x0000;

    public static final short TLV_TYPE_CERT_ID = 0x0001;

    public static final short TLV_TYPE_CERT_TYPE = 0x0002;

    public static final short TLV_TYPE_CERT_ISSUER = 0x0003;

    public static final short TLV_TYPE_CERT_ISSUANCE_DATE = 0x0004;

    public static final short TLV_TYPE_CERT_EXPIRATION_DATE = 0x0005;

    public static final short TLV_TYPE_CERT_CREDENTIAL_SUBJECT = 0x0006;

    public static final short TLV_TYPE_CERT_PROOF = 0x0007;

    public static AbstractCrossChainCertificate decode(byte[] rawData) {
        return TLVUtils.decode(rawData, AbstractCrossChainCertificate.class);
    }

    @TLVField(tag = TLV_TYPE_CERT_VERSION, type = TLVTypeEnum.STRING)
    private String version;

    @TLVField(tag = TLV_TYPE_CERT_ID, type = TLVTypeEnum.STRING, order = 1)
    private String id;

    @TLVField(tag = TLV_TYPE_CERT_TYPE, type = TLVTypeEnum.UINT8, order = 2)
    private CrossChainCertificateTypeEnum type;

    @TLVField(tag = TLV_TYPE_CERT_ISSUER, type = TLVTypeEnum.BYTES, order = 3)
    private ObjectIdentity issuer;

    @TLVField(tag = TLV_TYPE_CERT_ISSUANCE_DATE, type = TLVTypeEnum.UINT64, order = 4)
    private long issuanceDate;

    @TLVField(tag = TLV_TYPE_CERT_EXPIRATION_DATE, type = TLVTypeEnum.UINT64, order = 5)
    private long expirationDate;

    @TLVField(tag = TLV_TYPE_CERT_CREDENTIAL_SUBJECT, type = TLVTypeEnum.BYTES, order = 6)
    private byte[] credentialSubject;

    @TLVField(tag = TLV_TYPE_CERT_PROOF, type = TLVTypeEnum.BYTES, order = 7)
    private AbstractCrossChainCertificate.IssueProof proof;

    public byte[] getEncodedToSign() {
        return TLVUtils.encode(this, 6);
    }

    public byte[] encode() {
        return TLVUtils.encode(this);
    }
}
