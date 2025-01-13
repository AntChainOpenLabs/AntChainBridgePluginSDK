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

import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVCreator;

public enum CrossChainCertificateTypeEnum {

    BCDNS_TRUST_ROOT_CERTIFICATE,

    DOMAIN_NAME_CERTIFICATE,

    PROOF_TRANSFORMATION_COMPONENT_CERTIFICATE,

    RELAYER_CERTIFICATE;

    @TLVCreator
    public static CrossChainCertificateTypeEnum valueOf(Byte value) {
        switch (value) {
            case 0:
                return BCDNS_TRUST_ROOT_CERTIFICATE;
            case 1:
                return DOMAIN_NAME_CERTIFICATE;
            case 2:
                return PROOF_TRANSFORMATION_COMPONENT_CERTIFICATE;
            case 3:
                return RELAYER_CERTIFICATE;
            default:
                throw new AntChainBridgeCommonsException(
                        CommonsErrorCodeEnum.BCDNS_UNSUPPORTED_CA_TYPE,
                        "failed to parse type from value: " + value.intValue()
                );
        }
    }

    public static CrossChainCertificateTypeEnum getTypeByCredentialSubject(ICredentialSubject credentialSubject) {
        if (credentialSubject instanceof BCDNSTrustRootCredentialSubject) {
            return BCDNS_TRUST_ROOT_CERTIFICATE;
        } else if (credentialSubject instanceof DomainNameCredentialSubject) {
            return DOMAIN_NAME_CERTIFICATE;
        } else if (credentialSubject instanceof PTCCredentialSubject) {
            return PROOF_TRANSFORMATION_COMPONENT_CERTIFICATE;
        } else if (credentialSubject instanceof RelayerCredentialSubject) {
            return RELAYER_CERTIFICATE;
        }

        throw new AntChainBridgeCommonsException(
                CommonsErrorCodeEnum.BCDNS_UNSUPPORTED_CA_TYPE,
                "failed to parse type from subject class " + credentialSubject.getClass().getName()
        );
    }
}
