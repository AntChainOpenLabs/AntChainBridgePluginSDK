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

import java.security.Security;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.exception.IllegalCrossChainCertException;
import com.alipay.antchain.bridge.commons.utils.crypto.HashAlgoEnum;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AbstractCrossChainCertificate {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IssueProof {

        public static final IssueProof EMPTY_PROOF = new IssueProof(HashAlgoEnum.SHA2_256, "".getBytes(), SignAlgoEnum.SHA256_WITH_RSA, "".getBytes());

        public static final short TLV_TYPE_ISSUE_PROOF_HASH_ALGO = 0x0000;

        public static final short TLV_TYPE_ISSUE_PROOF_CERT_HASH = 0x0001;

        public static final short TLV_TYPE_ISSUE_PROOF_SIG_ALGO = 0x0002;

        public static final short TLV_TYPE_ISSUE_PROOF_RAW_PROOF = 0x0003;

        @TLVField(tag = TLV_TYPE_ISSUE_PROOF_HASH_ALGO, type = TLVTypeEnum.STRING)
        private HashAlgoEnum hashAlgo;

        @TLVField(tag = TLV_TYPE_ISSUE_PROOF_CERT_HASH, type = TLVTypeEnum.BYTES, order = 1)
        private byte[] certHash;

        @TLVField(tag = TLV_TYPE_ISSUE_PROOF_SIG_ALGO, type = TLVTypeEnum.STRING, order = 2)
        private SignAlgoEnum sigAlgo;

        @TLVField(tag = TLV_TYPE_ISSUE_PROOF_RAW_PROOF, type = TLVTypeEnum.BYTES, order = 3)
        private byte[] rawProof;

        public boolean validateHash(byte[] certEncoded) {
            return ArrayUtil.equals(hashAlgo.hash(certEncoded), certHash);
        }

        public boolean validateProof(byte[] certEncoded, ICredentialSubject issuerCredentialSubject) {
            if (issuerCredentialSubject instanceof ICredentialSubjectWithSingleKey) {
                return sigAlgo.getSigner().verify(
                        ((ICredentialSubjectWithSingleKey) issuerCredentialSubject).getSubjectPublicKey(),
                        certEncoded,
                        rawProof
                );
            }
            throw new IllegalArgumentException("Unsupported credential subject type: " + issuerCredentialSubject.getApplicant().getType().name());
        }
    }

    public static final short TLV_TYPE_CERT_VERSION = 0x0000;

    public static final short TLV_TYPE_CERT_ID = 0x0001;

    public static final short TLV_TYPE_CERT_TYPE = 0x0002;

    public static final short TLV_TYPE_CERT_ISSUER = 0x0003;

    public static final short TLV_TYPE_CERT_ISSUANCE_DATE = 0x0004;

    public static final short TLV_TYPE_CERT_EXPIRATION_DATE = 0x0005;

    public static final short TLV_TYPE_CERT_CREDENTIAL_SUBJECT = 0x0006;

    public static final short TLV_TYPE_CERT_PROOF = 0x0007;

    @TLVField(tag = TLV_TYPE_CERT_VERSION, type = TLVTypeEnum.STRING)
    private String version;

    @TLVField(tag = TLV_TYPE_CERT_ID, type = TLVTypeEnum.STRING, order = 1)
    private String id;

    @TLVField(tag = TLV_TYPE_CERT_TYPE, type = TLVTypeEnum.UINT8, order = 2)
    private CrossChainCertificateTypeEnum type;

    @TLVField(tag = TLV_TYPE_CERT_ISSUER, type = TLVTypeEnum.BYTES, order = 3)
    private ObjectIdentity issuer;

    /**
     * It's seconds for timestamp
     */
    @TLVField(tag = TLV_TYPE_CERT_ISSUANCE_DATE, type = TLVTypeEnum.UINT64, order = 4)
    private long issuanceDate;

    /**
     * It's seconds for timestamp
     */
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

    public String encodeToBase64() {
        return Base64.encode(encode());
    }

    public ICredentialSubject getCredentialSubjectInstance() {
        switch (type) {
            case BCDNS_TRUST_ROOT_CERTIFICATE:
                return BCDNSTrustRootCredentialSubject.decode(this.credentialSubject);
            case DOMAIN_NAME_CERTIFICATE:
                return DomainNameCredentialSubject.decode(this.credentialSubject);
            case PROOF_TRANSFORMATION_COMPONENT_CERTIFICATE:
                return PTCCredentialSubject.decode(this.credentialSubject);
            case RELAYER_CERTIFICATE:
                return RelayerCredentialSubject.decode(this.credentialSubject);
            default:
                throw new AntChainBridgeCommonsException(
                        CommonsErrorCodeEnum.BCDNS_UNSUPPORTED_CA_TYPE,
                        "unrecognised cert type: " + type
                );
        }
    }

    public void validate(ICredentialSubject issuerCredentialSubject) throws IllegalCrossChainCertException {
        if (StrUtil.isBlank(version)) {
            throw new IllegalCrossChainCertException("cert version is empty");
        }
        if (StrUtil.isBlank(id)) {
            throw new IllegalCrossChainCertException("cert id is empty");
        }
        if (type == null) {
            throw new IllegalCrossChainCertException("cert type is empty");
        }
        if (issuer == null) {
            throw new IllegalCrossChainCertException("cert issuer is empty");
        }
        if (issuanceDate <= 0) {
            throw new IllegalCrossChainCertException("cert issuance date is empty");
        }
        if (expirationDate <= 0) {
            throw new IllegalCrossChainCertException("cert expiration date is empty");
        }
        if (credentialSubject == null) {
            throw new IllegalCrossChainCertException("cert credential subject is empty");
        }
        if (proof == null) {
            throw new IllegalCrossChainCertException("cert proof is empty");
        }
        if (!proof.validateHash(this.getEncodedToSign())) {
            throw new IllegalCrossChainCertException("cert proof hash is invalid");
        }
        if (!proof.validateProof(this.getEncodedToSign(), issuerCredentialSubject)) {
            throw new IllegalCrossChainCertException("cert proof is invalid");
        }
    }
}
