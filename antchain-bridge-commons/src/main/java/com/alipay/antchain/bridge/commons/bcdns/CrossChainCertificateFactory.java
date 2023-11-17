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
import java.security.Security;

import cn.hutool.crypto.PemUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTypeEnum;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class CrossChainCertificateFactory {

    public static final String DEFAULT_VERSION = CrossChainCertificateV1.MY_VERSION;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static AbstractCrossChainCertificate createCrossChainCertificate(
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
        return CrossChainCertificateFactory.createCrossChainCertificate(DEFAULT_VERSION, data);
    }

    public static AbstractCrossChainCertificate createCrossChainCertificate(String version, byte[] data) {
        switch (version) {
            case CrossChainCertificateV1.MY_VERSION:
                return TLVUtils.decode(data, CrossChainCertificateV1.class);
            default:
                throw new AntChainBridgeCommonsException(
                        CommonsErrorCodeEnum.BCDNS_WRONG_CA_VERSION,
                        "wrong version of crosschain CA: " + version
                );
        }
    }

    public static AbstractCrossChainCertificate createCrossChainCertificateFromPem(byte[] pemData) {
        return TLVUtils.decode(PemUtil.readPem(new ByteArrayInputStream(pemData)), CrossChainCertificateV1.class);
    }

    public static AbstractCrossChainCertificate createRelayerCertificateSigningRequest(
            String version,
            String name,
            ObjectIdentity applicant,
            byte[] subjectInfo
    ) {
        AbstractCrossChainCertificate crossChainCertificate = new CrossChainCertificateV1();
        crossChainCertificate.setType(CrossChainCertificateTypeEnum.RELAYER_CERTIFICATE);
        crossChainCertificate.setVersion(CrossChainCertificateV1.MY_VERSION);
        crossChainCertificate.setCredentialSubject(
                new RelayerCredentialSubject(
                        version,
                        name,
                        applicant,
                        subjectInfo
                ).encode()
        );
        return crossChainCertificate;
    }

    public static AbstractCrossChainCertificate createPTCCertificateSigningRequest(
            String version,
            String name,
            PTCTypeEnum ptcType,
            ObjectIdentity applicant,
            byte[] subjectInfo
    ) {
        AbstractCrossChainCertificate crossChainCertificate = new CrossChainCertificateV1();
        crossChainCertificate.setType(CrossChainCertificateTypeEnum.PROOF_TRANSFORMATION_COMPONENT_CERTIFICATE);
        crossChainCertificate.setVersion(CrossChainCertificateV1.MY_VERSION);
        crossChainCertificate.setCredentialSubject(
                new PTCCredentialSubject(
                        version,
                        name,
                        ptcType,
                        applicant,
                        subjectInfo
                ).encode()
        );
        return crossChainCertificate;
    }

    public static AbstractCrossChainCertificate createDomainNameCertificateSigningRequest(
            String version,
            CrossChainDomain parentDomainSpace,
            CrossChainDomain domain,
            ObjectIdentity applicant,
            byte[] subjectInfo
    ) {
        AbstractCrossChainCertificate crossChainCertificate = new CrossChainCertificateV1();
        crossChainCertificate.setType(CrossChainCertificateTypeEnum.DOMAIN_NAME_CERTIFICATE);
        crossChainCertificate.setVersion(CrossChainCertificateV1.MY_VERSION);
        crossChainCertificate.setCredentialSubject(
                new DomainNameCredentialSubject(
                        version,
                        DomainNameTypeEnum.DOMAIN_NAME,
                        parentDomainSpace,
                        domain,
                        applicant,
                        subjectInfo
                ).encode()
        );
        return crossChainCertificate;
    }

    public static AbstractCrossChainCertificate createDomainSpaceCertificateSigningRequest(
            String version,
            CrossChainDomain parentDomainSpace,
            CrossChainDomain domainSpace,
            ObjectIdentity applicant,
            byte[] subjectInfo
    ) {
        AbstractCrossChainCertificate crossChainCertificate = new CrossChainCertificateV1();
        crossChainCertificate.setType(CrossChainCertificateTypeEnum.DOMAIN_NAME_CERTIFICATE);
        crossChainCertificate.setVersion(CrossChainCertificateV1.MY_VERSION);
        crossChainCertificate.setCredentialSubject(
                new DomainNameCredentialSubject(
                        version,
                        DomainNameTypeEnum.DOMAIN_NAME_SPACE,
                        parentDomainSpace,
                        domainSpace,
                        applicant,
                        subjectInfo
                ).encode()
        );
        return crossChainCertificate;
    }
}
