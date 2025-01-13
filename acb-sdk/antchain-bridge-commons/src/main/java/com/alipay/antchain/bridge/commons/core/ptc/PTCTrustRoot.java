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

import java.math.BigInteger;
import java.security.PrivateKey;
import java.util.Map;

import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.PTCCredentialSubject;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PTCTrustRoot {

    private static final int PTC_TRUST_ROOT_ISSUER_BCDNS_DOMAIN_SPACE = 0;

    private static final int PTC_TRUST_ROOT_PTC_CROSSCHAIN_CERT = 1;

    private static final int PTC_TRUST_ROOT_NETWORK_INFO = 2;

    private static final int PTC_TRUST_ROOT_VA_MAP = 3;

    private static final int PTC_TRUST_ROOT_SIG_ALGO = 4;

    private static final int PTC_TRUST_ROOT_SIG = 5;

    public static PTCTrustRoot decode(byte[] data) {
        return TLVUtils.decode(data, PTCTrustRoot.class);
    }

    @TLVField(tag = PTC_TRUST_ROOT_ISSUER_BCDNS_DOMAIN_SPACE, type = TLVTypeEnum.STRING)
    private CrossChainDomain issuerBcdnsDomainSpace;

    @TLVField(tag = PTC_TRUST_ROOT_PTC_CROSSCHAIN_CERT, type = TLVTypeEnum.BYTES, order = PTC_TRUST_ROOT_PTC_CROSSCHAIN_CERT)
    private AbstractCrossChainCertificate ptcCrossChainCert;

    @TLVField(tag = PTC_TRUST_ROOT_NETWORK_INFO, type = TLVTypeEnum.BYTES, order = PTC_TRUST_ROOT_NETWORK_INFO)
    private byte[] networkInfo;

    /**
     * key of the map is the verify anchor's version number
     */
    @TLVField(tag = PTC_TRUST_ROOT_VA_MAP, type = TLVTypeEnum.MAP, order = PTC_TRUST_ROOT_VA_MAP)
    private Map<BigInteger, PTCVerifyAnchor> verifyAnchorMap;

    @TLVField(tag = PTC_TRUST_ROOT_SIG_ALGO, type = TLVTypeEnum.STRING, order = PTC_TRUST_ROOT_SIG_ALGO)
    private SignAlgoEnum sigAlgo;

    @TLVField(tag = PTC_TRUST_ROOT_SIG, type = TLVTypeEnum.BYTES, order = PTC_TRUST_ROOT_SIG)
    private byte[] sig;

    public PTCCredentialSubject getPtcCredentialSubject() {
        return (PTCCredentialSubject) ptcCrossChainCert.getCredentialSubjectInstance();
    }

    public void sign(PrivateKey privateKey) {
        setSig(sigAlgo.getSigner().sign(privateKey, getEncodedToSign()));
    }

    public byte[] getEncodedToSign() {
        return TLVUtils.encode(this, PTC_TRUST_ROOT_SIG_ALGO);
    }

    public byte[] encode() {
        return TLVUtils.encode(this);
    }
}
