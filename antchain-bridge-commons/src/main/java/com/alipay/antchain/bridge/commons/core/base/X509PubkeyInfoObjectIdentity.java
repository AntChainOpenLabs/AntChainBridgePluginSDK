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

import java.security.PublicKey;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.KeyUtil;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import sun.security.x509.AlgorithmId;

public class X509PubkeyInfoObjectIdentity extends ObjectIdentity {

    private SubjectPublicKeyInfo subjectPublicKeyInfo;

    public X509PubkeyInfoObjectIdentity(byte[] rawSubjectPubkeyInfo) {
        super(ObjectIdentityType.X509_PUBLIC_KEY_INFO, rawSubjectPubkeyInfo);
    }

    public SubjectPublicKeyInfo getSubjectPublicKeyInfo() {
        if (ObjectUtil.isNull(subjectPublicKeyInfo)) {
            this.subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(this.getRawId());
        }
        return subjectPublicKeyInfo;
    }

    public PublicKey getPublicKey() {
        try {
            return KeyUtil.generatePublicKey(
                    AlgorithmId.get(getSubjectPublicKeyInfo().getAlgorithm().getAlgorithm().getId()).getName(),
                    getSubjectPublicKeyInfo().getEncoded()
            );
        } catch (Exception e) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.BCDNS_OID_X509_PUBLIC_KEY_INFO_ERROR,
                    "failed to get public key from subject pubkey info",
                    e
            );
        }
    }
}
