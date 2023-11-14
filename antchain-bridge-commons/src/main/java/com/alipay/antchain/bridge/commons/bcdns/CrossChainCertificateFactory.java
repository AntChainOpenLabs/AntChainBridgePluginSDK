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

import java.io.ByteArrayInputStream;
import java.util.List;

import cn.hutool.crypto.PemUtil;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;

public class CrossChainCertificateFactory {

    public static AbstractCrossChainCertificate createCrossChainCertificate(
            String context,
            String version,
            String id,
            ObjectIdentity issuer,
            long issuanceDate,
            long expirationDate,
            ICredentialSubject credentialSubject
    ) {
        switch (version) {
            case CrossChainCertificateV1.MY_VERSION:
                return new CrossChainCertificateV1(
                        context,
                        id,
                        CrossChainCertificateTypeEnum.getTypeByCredentialSubject(credentialSubject),
                        issuer,
                        issuanceDate,
                        expirationDate,
                        credentialSubject
                );
            default:
                throw new AntChainBridgeCommonsException(
                        CommonsErrorCodeEnum.BCDNS_WRONG_CA_VERSION,
                        "wrong version of crosschain CA: " + version
                );
        }
    }

    public static AbstractCrossChainCertificate createCrossChainCertificate(byte[] data) {
        return TLVUtils.decode(data, CrossChainCertificateV1.class);
    }

    public static AbstractCrossChainCertificate createCrossChainCertificateFromPem(byte[] pemData) {
        return TLVUtils.decode(PemUtil.readPem(new ByteArrayInputStream(pemData)), CrossChainCertificateV1.class);
    }
}
