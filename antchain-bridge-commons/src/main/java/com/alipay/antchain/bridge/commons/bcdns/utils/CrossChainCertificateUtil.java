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

package com.alipay.antchain.bridge.commons.bcdns.utils;

import java.security.PublicKey;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.PemUtil;
import com.alipay.antchain.bridge.commons.bcdns.*;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;

public class CrossChainCertificateUtil {

    public static String formatCrossChainCertificateToPem(AbstractCrossChainCertificate certificate) {
        switch (certificate.getType()) {
            case RELAYER_CERTIFICATE:
                return PemUtil.toPem(
                        StrUtil.replace(CrossChainCertificateTypeEnum.RELAYER_CERTIFICATE.name(), "_", " "),
                        certificate.encode()
                );
            case PROOF_TRANSFORMATION_COMPONENT_CERTIFICATE:
                return PemUtil.toPem(
                        StrUtil.replace(CrossChainCertificateTypeEnum.PROOF_TRANSFORMATION_COMPONENT_CERTIFICATE.name(), "_", " "),
                        certificate.encode()
                );
            case DOMAIN_NAME_CERTIFICATE:
                return PemUtil.toPem(
                        StrUtil.replace(CrossChainCertificateTypeEnum.DOMAIN_NAME_CERTIFICATE.name(), "_", " "),
                        certificate.encode()
                );
            case BCDNS_TRUST_ROOT_CERTIFICATE:
                return PemUtil.toPem(
                        StrUtil.replace(CrossChainCertificateTypeEnum.BCDNS_TRUST_ROOT_CERTIFICATE.name(), "_", " "),
                        certificate.encode()
                );
            default:
                throw new AntChainBridgeCommonsException(
                        CommonsErrorCodeEnum.BCDNS_UNSUPPORTED_CA_TYPE,
                        "unsupported crosschain certificate type"
                );
        }
    }

    public static AbstractCrossChainCertificate readCrossChainCertificateFromPem(byte[] rawPem) {
        return CrossChainCertificateFactory.createCrossChainCertificateFromPem(rawPem);
    }

    public static CrossChainDomain getCrossChainDomain(AbstractCrossChainCertificate certificate) {
        Assert.equals(
                CrossChainCertificateTypeEnum.DOMAIN_NAME_CERTIFICATE,
                certificate.getType()
        );
        DomainNameCredentialSubject subject = DomainNameCredentialSubject.decode(certificate.getCredentialSubject());
        Assert.equals(
                subject.getDomainNameType(),
                DomainNameTypeEnum.DOMAIN_NAME
        );
        return subject.getDomainName();
    }

    public static CrossChainDomain getCrossChainDomainSpace(AbstractCrossChainCertificate certificate) {
        Assert.equals(
                CrossChainCertificateTypeEnum.DOMAIN_NAME_CERTIFICATE,
                certificate.getType()
        );
        DomainNameCredentialSubject subject = DomainNameCredentialSubject.decode(certificate.getCredentialSubject());
        Assert.equals(
                subject.getDomainNameType(),
                DomainNameTypeEnum.DOMAIN_NAME_SPACE
        );
        return subject.getDomainName();
    }

    public static CrossChainDomain getParentDomainSpace(AbstractCrossChainCertificate certificate) {
        Assert.equals(
                CrossChainCertificateTypeEnum.DOMAIN_NAME_CERTIFICATE,
                certificate.getType()
        );
        DomainNameCredentialSubject subject = DomainNameCredentialSubject.decode(certificate.getCredentialSubject());
        Assert.equals(
                subject.getDomainNameType(),
                DomainNameTypeEnum.DOMAIN_NAME_SPACE
        );
        return subject.getDomainName();
    }

    public static PublicKey getPublicKeyFromCrossChainCertificate(AbstractCrossChainCertificate certificate) {
        switch (certificate.getType()) {
            case BCDNS_TRUST_ROOT_CERTIFICATE:
                BCDNSTrustRootCredentialSubject trustRootCredentialSubject = BCDNSTrustRootCredentialSubject.decode(certificate.getCredentialSubject());
                return ObjectIdentityUtil.getPublicKeyFromSubject(
                        trustRootCredentialSubject.getBcdnsRootOwner(),
                        trustRootCredentialSubject.getBcdnsRootSubjectInfo()
                );
            case DOMAIN_NAME_CERTIFICATE:
                DomainNameCredentialSubject domainNameCredentialSubject = DomainNameCredentialSubject.decode(certificate.getCredentialSubject());
                return ObjectIdentityUtil.getPublicKeyFromSubject(
                        domainNameCredentialSubject.getApplicant(),
                        domainNameCredentialSubject.getSubject()
                );
            case RELAYER_CERTIFICATE:
                RelayerCredentialSubject relayerCredentialSubject = RelayerCredentialSubject.decode(certificate.getCredentialSubject());
                return ObjectIdentityUtil.getPublicKeyFromSubject(
                        relayerCredentialSubject.getApplicant(),
                        relayerCredentialSubject.getSubjectInfo()
                );
            case PROOF_TRANSFORMATION_COMPONENT_CERTIFICATE:
                PTCCredentialSubject ptcCredentialSubject = PTCCredentialSubject.decode(certificate.getCredentialSubject());
                return ObjectIdentityUtil.getPublicKeyFromSubject(
                        ptcCredentialSubject.getApplicant(),
                        ptcCredentialSubject.getSubjectInfo()
                );
            default:
                throw new AntChainBridgeCommonsException(
                        CommonsErrorCodeEnum.BCDNS_UNSUPPORTED_CA_TYPE,
                        "cert type not support" + certificate.getType().name()
                );
        }
    }
}
