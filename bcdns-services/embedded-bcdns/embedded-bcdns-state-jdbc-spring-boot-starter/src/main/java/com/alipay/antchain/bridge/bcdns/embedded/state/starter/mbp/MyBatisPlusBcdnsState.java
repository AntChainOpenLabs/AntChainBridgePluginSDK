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

package com.alipay.antchain.bridge.bcdns.embedded.state.starter.mbp;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.bcdns.embedded.server.IBcdnsState;
import com.alipay.antchain.bridge.bcdns.embedded.state.starter.mbp.entities.*;
import com.alipay.antchain.bridge.bcdns.embedded.state.starter.mbp.mapper.*;
import com.alipay.antchain.bridge.bcdns.embedded.types.enums.ApplicationStateEnum;
import com.alipay.antchain.bridge.bcdns.embedded.types.models.CertApplicationResult;
import com.alipay.antchain.bridge.bcdns.types.base.DomainRouter;
import com.alipay.antchain.bridge.bcdns.types.base.Relayer;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateTypeEnum;
import com.alipay.antchain.bridge.commons.bcdns.DomainNameCredentialSubject;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public class MyBatisPlusBcdnsState implements IBcdnsState {

    private final EmbeddedBcdnsCertApplicationMapper embeddedBcdnsCertApplicationMapper;

    private final EmbeddedBcdnsDomainCertMapper embeddedBcdnsDomainCertMapper;

    private final EmbeddedBcdnsDomainSpaceCertMapper embeddedBcdnsDomainSpaceCertMapper;

    private final EmbeddedBcdnsRelayerCertMapper embeddedBcdnsRelayerCertMapper;

    private final EmbeddedBcdnsPtcCertMapper embeddedBcdnsPtcCertMapper;

    private final EmbeddedBcdnsDomainRouterMapper embeddedBcdnsDomainRouterMapper;

    public MyBatisPlusBcdnsState(
            EmbeddedBcdnsCertApplicationMapper embeddedBcdnsCertApplicationMapper,
            EmbeddedBcdnsDomainCertMapper embeddedBcdnsDomainCertMapper,
            EmbeddedBcdnsDomainSpaceCertMapper embeddedBcdnsDomainSpaceCertMapper,
            EmbeddedBcdnsRelayerCertMapper embeddedBcdnsRelayerCertMapper,
            EmbeddedBcdnsPtcCertMapper embeddedBcdnsPtcCertMapper,
            EmbeddedBcdnsDomainRouterMapper embeddedBcdnsDomainRouterMapper
    ) {
        this.embeddedBcdnsCertApplicationMapper = embeddedBcdnsCertApplicationMapper;
        this.embeddedBcdnsDomainCertMapper = embeddedBcdnsDomainCertMapper;
        this.embeddedBcdnsDomainSpaceCertMapper = embeddedBcdnsDomainSpaceCertMapper;
        this.embeddedBcdnsRelayerCertMapper = embeddedBcdnsRelayerCertMapper;
        this.embeddedBcdnsPtcCertMapper = embeddedBcdnsPtcCertMapper;
        this.embeddedBcdnsDomainRouterMapper = embeddedBcdnsDomainRouterMapper;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void saveApplication(AbstractCrossChainCertificate csr, String receipt, ApplicationStateEnum state) {
        if (embeddedBcdnsCertApplicationMapper.exists(
                new LambdaQueryWrapper<CertApplicationEntity>()
                        .eq(CertApplicationEntity::getReceipt, receipt)
        )) {
            throw new RuntimeException("receipt already exists");
        }
        embeddedBcdnsCertApplicationMapper.insert(
                CertApplicationEntity.builder()
                        .state(state.name())
                        .receipt(receipt)
                        .certType(csr.getType().name())
                        .rawCsr(csr.encode())
                        .build()
        );
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void updateApplication(String receipt, ApplicationStateEnum state) {
        if (!embeddedBcdnsCertApplicationMapper.exists(
                new LambdaQueryWrapper<CertApplicationEntity>()
                        .eq(CertApplicationEntity::getReceipt, receipt)
        )) {
            throw new RuntimeException("receipt not exists");
        }
        embeddedBcdnsCertApplicationMapper.update(
                CertApplicationEntity.builder()
                        .state(state.name())
                        .build(),
                new LambdaQueryWrapper<CertApplicationEntity>()
                        .eq(CertApplicationEntity::getReceipt, receipt)
        );
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void saveCrossChainCert(AbstractCrossChainCertificate crossChainCertificate) {
        switch (crossChainCertificate.getType()) {
            case RELAYER_CERTIFICATE:
                if (embeddedBcdnsRelayerCertMapper.exists(
                        new LambdaQueryWrapper<RelayerCertEntity>()
                                .eq(RelayerCertEntity::getCertId, crossChainCertificate.getId())
                )) {
                    throw new RuntimeException(StrUtil.format("relayer cert {} already exists", crossChainCertificate.getId()));
                }
                embeddedBcdnsRelayerCertMapper.insert(
                        RelayerCertEntity.builder()
                                .certId(crossChainCertificate.getId())
                                .issuerOid(crossChainCertificate.getIssuer().encode())
                                .subjectOid(crossChainCertificate.getCredentialSubjectInstance().getApplicant().encode())
                                .rawCert(crossChainCertificate.encode())
                                .build()
                );
                break;
            case DOMAIN_NAME_CERTIFICATE:
                CrossChainDomain crossChainDomain = CrossChainCertificateUtil.getCrossChainDomain(crossChainCertificate);
                if (embeddedBcdnsDomainCertMapper.exists(
                        new LambdaQueryWrapper<DomainCertEntity>()
                                .eq(DomainCertEntity::getDomain, crossChainDomain.getDomain())
                )) {
                    throw new RuntimeException(StrUtil.format("domain cert {} already exists", crossChainDomain.getDomain()));
                }
                DomainNameCredentialSubject domainNameCredentialSubject = (DomainNameCredentialSubject) crossChainCertificate.getCredentialSubjectInstance();
                embeddedBcdnsDomainCertMapper.insert(
                        DomainCertEntity.builder()
                                .domain(crossChainDomain.getDomain())
                                .domainSpace(domainNameCredentialSubject.getParentDomainSpace().getDomain())
                                .issuerOid(crossChainCertificate.getIssuer().encode())
                                .subjectOid(domainNameCredentialSubject.getApplicant().encode())
                                .domainCert(crossChainCertificate.encode())
                                .build()
                );
                break;
            case PROOF_TRANSFORMATION_COMPONENT_CERTIFICATE:
                if (embeddedBcdnsPtcCertMapper.exists(
                        new LambdaQueryWrapper<PtcCertEntity>()
                                .eq(PtcCertEntity::getCertId, crossChainCertificate.getId())
                )) {
                    throw new RuntimeException(StrUtil.format("ptc cert {} already exists", crossChainCertificate.getId()));
                }
                embeddedBcdnsPtcCertMapper.insert(
                        PtcCertEntity.builder()
                                .certId(crossChainCertificate.getId())
                                .issuerOid(crossChainCertificate.getIssuer().encode())
                                .subjectOid(crossChainCertificate.getCredentialSubjectInstance().getApplicant().encode())
                                .rawCert(crossChainCertificate.encode())
                                .build()
                );
                break;
            default:
                throw new RuntimeException("unknown cert type: " + crossChainCertificate.getType());
        }
    }

    @Override
    public CertApplicationResult queryApplication(String receipt) {
        CertApplicationEntity certApplicationEntity = embeddedBcdnsCertApplicationMapper.selectOne(
                new LambdaQueryWrapper<CertApplicationEntity>()
                        .eq(CertApplicationEntity::getReceipt, receipt)
        );
        if (ObjectUtil.isNull(certApplicationEntity)) {
            log.debug("no application found for receipt {}", receipt);
            return null;
        }
        if (certApplicationEntity.getStateEnum() == ApplicationStateEnum.INIT
                || certApplicationEntity.getStateEnum() == ApplicationStateEnum.REFUSED) {
            return CertApplicationResult.builder()
                    .receipt(receipt)
                    .state(certApplicationEntity.getStateEnum())
                    .build();
        }

        if (certApplicationEntity.getCertTypeEnum() == CrossChainCertificateTypeEnum.DOMAIN_NAME_CERTIFICATE) {
            CrossChainDomain domain = CrossChainCertificateUtil.getCrossChainDomain(
                    CrossChainCertificateFactory.createCrossChainCertificate(certApplicationEntity.getRawCsr())
            );
            DomainCertEntity domainCertEntity = embeddedBcdnsDomainCertMapper.selectOne(
                    new LambdaQueryWrapper<DomainCertEntity>()
                            .eq(DomainCertEntity::getDomain, domain.getDomain())
            );
            if (ObjectUtil.isNull(domainCertEntity)) {
                log.error("no domain cert found for domain {}", domain.getDomain());
                return null;
            }
            return CertApplicationResult.builder()
                    .receipt(receipt)
                    .certificate(CrossChainCertificateFactory.createCrossChainCertificate(domainCertEntity.getDomainCert()))
                    .state(certApplicationEntity.getStateEnum())
                    .build();
        } else if (certApplicationEntity.getCertTypeEnum() == CrossChainCertificateTypeEnum.PROOF_TRANSFORMATION_COMPONENT_CERTIFICATE) {
            PtcCertEntity ptcCertEntity = embeddedBcdnsPtcCertMapper.selectOne(
                    new LambdaQueryWrapper<PtcCertEntity>()
                            .eq(PtcCertEntity::getCertId, certApplicationEntity.getReceipt())
            );
            if (ObjectUtil.isNull(ptcCertEntity)) {
                log.error("no ptc cert found for cert id {}", certApplicationEntity.getReceipt());
                return null;
            }
            return CertApplicationResult.builder()
                    .receipt(receipt)
                    .certificate(CrossChainCertificateFactory.createCrossChainCertificate(ptcCertEntity.getRawCert()))
                    .state(certApplicationEntity.getStateEnum())
                    .build();
        } else if (certApplicationEntity.getCertTypeEnum() == CrossChainCertificateTypeEnum.RELAYER_CERTIFICATE) {
            RelayerCertEntity relayerCertEntity = embeddedBcdnsRelayerCertMapper.selectOne(
                    new LambdaQueryWrapper<RelayerCertEntity>()
                            .eq(RelayerCertEntity::getCertId, certApplicationEntity.getReceipt())
            );
            if (ObjectUtil.isNull(relayerCertEntity)) {
                log.error("no relayer cert found for cert id {}", certApplicationEntity.getReceipt());
                return null;
            }
            return CertApplicationResult.builder()
                    .receipt(receipt)
                    .certificate(CrossChainCertificateFactory.createCrossChainCertificate(relayerCertEntity.getRawCert()))
                    .state(certApplicationEntity.getStateEnum())
                    .build();
        }
        throw new RuntimeException("cert type not support: " + certApplicationEntity.getCertType());
    }

    @Override
    public AbstractCrossChainCertificate queryCrossChainCert(String certId, CrossChainCertificateTypeEnum certificateType) {
        switch (certificateType) {
            case RELAYER_CERTIFICATE:
                RelayerCertEntity relayerCertEntity = embeddedBcdnsRelayerCertMapper.selectOne(
                        new LambdaQueryWrapper<RelayerCertEntity>()
                                .eq(RelayerCertEntity::getCertId, certId)
                );
                if (ObjectUtil.isNull(relayerCertEntity)) {
                    log.debug("no relayer cert found for cert id {}", certId);
                    return null;
                }
                return CrossChainCertificateFactory.createCrossChainCertificate(relayerCertEntity.getRawCert());
            case PROOF_TRANSFORMATION_COMPONENT_CERTIFICATE:
                PtcCertEntity ptcCertEntity = embeddedBcdnsPtcCertMapper.selectOne(
                        new LambdaQueryWrapper<PtcCertEntity>()
                                .eq(PtcCertEntity::getCertId, certId)
                );
                if (ObjectUtil.isNull(ptcCertEntity)) {
                    log.debug("no ptc cert found for cert id {}", certId);
                    return null;
                }
                return CrossChainCertificateFactory.createCrossChainCertificate(ptcCertEntity.getRawCert());
            default:
                throw new RuntimeException("cert type not support: " + certificateType);
        }
    }

    @Override
    public AbstractCrossChainCertificate queryDomainCert(String domain) {
        DomainCertEntity entity = embeddedBcdnsDomainCertMapper.selectOne(
                new LambdaQueryWrapper<DomainCertEntity>()
                        .eq(DomainCertEntity::getDomain, domain)
        );
        if (ObjectUtil.isNull(entity)) {
            log.debug("no domain cert found for domain {}", domain);
            return null;
        }
        return CrossChainCertificateFactory.createCrossChainCertificate(entity.getDomainCert());
    }

    @Override
    public void registerDomainRouter(DomainRouter domainRouter) {
        if (embeddedBcdnsDomainRouterMapper.exists(
                new LambdaQueryWrapper<DomainRouterEntity>()
                        .eq(DomainRouterEntity::getDestDomain, domainRouter.getDestDomain().getDomain())
        )) {
            log.info("router for {} already exist, update it", domainRouter.getDestDomain().getDomain());
            embeddedBcdnsDomainRouterMapper.update(
                    DomainRouterEntity.builder()
                            .relayerCertId(domainRouter.getDestRelayer().getRelayerCertId())
                            .relayerCert(domainRouter.getDestRelayer().getRelayerCert().encode())
                            .netAddressList(StrUtil.join(",", domainRouter.getDestRelayer().getNetAddressList()))
                            .build(),
                    new LambdaUpdateWrapper<DomainRouterEntity>()
                            .eq(DomainRouterEntity::getDestDomain, domainRouter.getDestDomain().getDomain())
            );
            return;
        }

        embeddedBcdnsDomainRouterMapper.insert(
                DomainRouterEntity.builder()
                        .destDomain(domainRouter.getDestDomain().getDomain())
                        .relayerCertId(domainRouter.getDestRelayer().getRelayerCertId())
                        .relayerCert(domainRouter.getDestRelayer().getRelayerCert().encode())
                        .netAddressList(StrUtil.join(",", domainRouter.getDestRelayer().getNetAddressList()))
                        .build()
        );
    }

    @Override
    public void registerTPBTA(byte[] rawTpBta) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public DomainRouter queryDomainRouter(String destDomain) {
        DomainRouterEntity entity = embeddedBcdnsDomainRouterMapper.selectOne(
                new LambdaQueryWrapper<DomainRouterEntity>()
                        .eq(DomainRouterEntity::getDestDomain, destDomain)
        );
        if (ObjectUtil.isNull(entity)) {
            log.debug("no router found for domain {}", destDomain);
            return null;
        }

        return new DomainRouter(
                new CrossChainDomain(destDomain),
                new Relayer(
                        entity.getRelayerCertId(),
                        CrossChainCertificateFactory.createCrossChainCertificate(entity.getRelayerCert()),
                        StrUtil.split(entity.getNetAddressList(), ",")
                )
        );
    }

    @Override
    public byte[] queryTpBta(String request) {
        return new byte[0];
    }

    @Override
    public boolean ifDomainExist(String domain) {
        return embeddedBcdnsDomainCertMapper.exists(
                new LambdaQueryWrapper<DomainCertEntity>()
                        .eq(DomainCertEntity::getDomain, domain)
        );
    }
}
