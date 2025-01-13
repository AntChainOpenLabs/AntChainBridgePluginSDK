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

import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import cn.hutool.core.collection.ListUtil;
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
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.commons.core.ptc.PTCVerifyAnchor;
import com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyBlockchainTrustAnchor;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
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

    private final EmbeddedBcdnsTpBtaMapper embeddedBcdnsTpBtaMapper;

    private final EmbeddedBcdnsPtcTrustRootMapper embeddedBcdnsPtcTrustRootMapper;

    private final EmbeddedBcdnsPtcVerifyAnchorMapper embeddedBcdnsPtcVerifyAnchorMapper;

    public MyBatisPlusBcdnsState(
            EmbeddedBcdnsCertApplicationMapper embeddedBcdnsCertApplicationMapper,
            EmbeddedBcdnsDomainCertMapper embeddedBcdnsDomainCertMapper,
            EmbeddedBcdnsDomainSpaceCertMapper embeddedBcdnsDomainSpaceCertMapper,
            EmbeddedBcdnsRelayerCertMapper embeddedBcdnsRelayerCertMapper,
            EmbeddedBcdnsPtcCertMapper embeddedBcdnsPtcCertMapper,
            EmbeddedBcdnsDomainRouterMapper embeddedBcdnsDomainRouterMapper,
            EmbeddedBcdnsTpBtaMapper embeddedBcdnsTpBtaMapper,
            EmbeddedBcdnsPtcTrustRootMapper embeddedBcdnsPtcTrustRootMapper,
            EmbeddedBcdnsPtcVerifyAnchorMapper embeddedBcdnsPtcVerifyAnchorMapper
    ) {
        this.embeddedBcdnsCertApplicationMapper = embeddedBcdnsCertApplicationMapper;
        this.embeddedBcdnsDomainCertMapper = embeddedBcdnsDomainCertMapper;
        this.embeddedBcdnsDomainSpaceCertMapper = embeddedBcdnsDomainSpaceCertMapper;
        this.embeddedBcdnsRelayerCertMapper = embeddedBcdnsRelayerCertMapper;
        this.embeddedBcdnsPtcCertMapper = embeddedBcdnsPtcCertMapper;
        this.embeddedBcdnsDomainRouterMapper = embeddedBcdnsDomainRouterMapper;
        this.embeddedBcdnsTpBtaMapper = embeddedBcdnsTpBtaMapper;
        this.embeddedBcdnsPtcTrustRootMapper = embeddedBcdnsPtcTrustRootMapper;
        this.embeddedBcdnsPtcVerifyAnchorMapper = embeddedBcdnsPtcVerifyAnchorMapper;
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
        ThirdPartyBlockchainTrustAnchor tpbta = ThirdPartyBlockchainTrustAnchor.decode(rawTpBta);
        CrossChainLane lane = tpbta.getCrossChainLane();

        if (hasTpBta(lane, tpbta.getTpbtaVersion())) {
            embeddedBcdnsTpBtaMapper.update(
                    TpBtaEntity.builder().rawTpbta(rawTpBta).build(),
                    new LambdaUpdateWrapper<TpBtaEntity>()
                            .eq(TpBtaEntity::getSenderDomain, lane.getSenderDomain().getDomain())
                            .eq(ObjectUtil.isNotNull(lane.getSenderIdHex()), TpBtaEntity::getSenderId, lane.getSenderIdHex())
                            .eq(TpBtaEntity::getSenderId, ObjectUtil.isNull(lane.getSenderId()) ? "" : lane.getSenderId().toHex())
                            .eq(TpBtaEntity::getReceiverDomain, ObjectUtil.isNull(lane.getReceiverDomain()) ? "" : lane.getReceiverDomain().getDomain())
                            .eq(TpBtaEntity::getReceiverId, ObjectUtil.isNull(lane.getReceiverId()) ? "" : lane.getReceiverId().toHex())
                            .eq(TpBtaEntity::getTpbtaVersion, tpbta.getTpbtaVersion())
            );
            return;
        }
        embeddedBcdnsTpBtaMapper.insert(
                TpBtaEntity.builder()
                        .version(tpbta.getVersion())
                        .btaSubjectVersion(tpbta.getBtaSubjectVersion())
                        .tpbtaVersion(tpbta.getTpbtaVersion())
                        .ptcOidHex(tpbta.getSignerPtcCredentialSubject().getApplicant().toHex())
                        .senderDomain(lane.getSenderDomain().getDomain())
                        .senderId(ObjectUtil.isNull(lane.getSenderId()) ? "" : lane.getSenderId().toHex())
                        .receiverDomain(ObjectUtil.isNull(lane.getReceiverDomain()) ? "" : lane.getReceiverDomain().getDomain())
                        .receiverId(ObjectUtil.isNull(lane.getReceiverId()) ? "" : lane.getReceiverId().toHex())
                        .ptcVerifyAnchorVersion(tpbta.getPtcVerifyAnchorVersion().toString())
                        .rawTpbta(rawTpBta)
                        .build()
        );
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
    public boolean ifDomainExist(String domain) {
        return embeddedBcdnsDomainCertMapper.exists(
                new LambdaQueryWrapper<DomainCertEntity>()
                        .eq(DomainCertEntity::getDomain, domain)
        );
    }

    @Override
    public ThirdPartyBlockchainTrustAnchor queryExactTpBta(CrossChainLane tpbtaLane, long tpbtaVersion) {
        LambdaQueryWrapper<TpBtaEntity> wrapper = new LambdaQueryWrapper<TpBtaEntity>()
                .eq(TpBtaEntity::getSenderDomain, tpbtaLane.getSenderDomain().getDomain())
                .eq(TpBtaEntity::getSenderId, ObjectUtil.isNull(tpbtaLane.getSenderId()) ? "" : tpbtaLane.getSenderId().toHex())
                .eq(TpBtaEntity::getReceiverDomain, ObjectUtil.isNull(tpbtaLane.getReceiverDomain()) ? "" : tpbtaLane.getReceiverDomain().getDomain())
                .eq(TpBtaEntity::getReceiverId, ObjectUtil.isNull(tpbtaLane.getReceiverId()) ? "" : tpbtaLane.getReceiverId().toHex());
        List<TpBtaEntity> entityList = embeddedBcdnsTpBtaMapper.selectList(
                tpbtaVersion == -1 ? wrapper : wrapper.eq(TpBtaEntity::getTpbtaVersion, tpbtaVersion)
        );
        if (ObjectUtil.isEmpty(entityList)) {
            return null;
        }
        return ThirdPartyBlockchainTrustAnchor.decode(
                entityList.stream().max(Comparator.comparingInt(TpBtaEntity::getTpbtaVersion)).get().getRawTpbta()
        );
    }

    @Override
    public ThirdPartyBlockchainTrustAnchor queryMatchedTpBta(CrossChainLane lane) {
        List<TpBtaEntity> entityList = searchTpBta(lane, -1);
        if (ObjectUtil.isEmpty(entityList)) {
            return null;
        }
        return ThirdPartyBlockchainTrustAnchor.decode(
                entityList.stream().max(Comparator.comparingInt(TpBtaEntity::getTpbtaVersion)).get().getRawTpbta()
        );
    }

    @Override
    public PTCTrustRoot queryPTCTrustRoot(ObjectIdentity ptcOid) {
        PtcTrustRootEntity entity = embeddedBcdnsPtcTrustRootMapper.selectOne(
                new LambdaQueryWrapper<PtcTrustRootEntity>()
                        .eq(PtcTrustRootEntity::getPtcOidHex, ptcOid.toHex())
        );
        if (entity == null) {
            return null;
        }
        PTCTrustRoot ptcTrustRoot = new PTCTrustRoot();
        ptcTrustRoot.setIssuerBcdnsDomainSpace(new CrossChainDomain(entity.getIssuerBcdnsDomainSpace()));
        ptcTrustRoot.setNetworkInfo(entity.getNetworkInfo());
        ptcTrustRoot.setSigAlgo(SignAlgoEnum.getByName(entity.getSignAlgo()));
        ptcTrustRoot.setSig(entity.getSig());
        ptcTrustRoot.setPtcCrossChainCert(CrossChainCertificateFactory.createCrossChainCertificate(entity.getPtcCrosschainCert()));
        ptcTrustRoot.setVerifyAnchorMap(new HashMap<>());

        List<PtcVerifyAnchorEntity> entities = embeddedBcdnsPtcVerifyAnchorMapper.selectList(
                new LambdaQueryWrapper<PtcVerifyAnchorEntity>()
                        .eq(PtcVerifyAnchorEntity::getPtcOidHex, ptcOid.toHex())
        );
        if (ObjectUtil.isNotEmpty(entities)) {
            entities.stream()
                    .map(x -> new PTCVerifyAnchor(
                            new BigInteger(x.getVersionNum()),
                            x.getAnchor()
                    )).forEach(x -> ptcTrustRoot.getVerifyAnchorMap().put(x.getVersion(), x));
        }
        return ptcTrustRoot;
    }

    @Override
    public void addPTCTrustRoot(PTCTrustRoot ptcTrustRoot) {
        String ptcOwnerOidHex = ptcTrustRoot.getPtcCredentialSubject().getApplicant().toHex();
        PTCVerifyAnchor latestVa = ptcTrustRoot.getVerifyAnchorMap().values().stream()
                .max(Comparator.comparing(PTCVerifyAnchor::getVersion)).orElseThrow(() -> new RuntimeException("ptc trust root has no verify anchor"));
        if (embeddedBcdnsPtcTrustRootMapper.exists(
                new LambdaQueryWrapper<PtcTrustRootEntity>()
                        .eq(PtcTrustRootEntity::getPtcOidHex, ptcOwnerOidHex)
        )) {
            log.info("ptc trust root for {} already exists, update it", ptcOwnerOidHex);
            embeddedBcdnsPtcTrustRootMapper.update(
                    PtcTrustRootEntity.builder()
                            .latestVerifyAnchor(latestVa.getVersion().toString())
                            .networkInfo(ptcTrustRoot.getNetworkInfo())
                            .build(),
                    new LambdaUpdateWrapper<PtcTrustRootEntity>()
                            .eq(PtcTrustRootEntity::getPtcOidHex, ptcOwnerOidHex)
            );
        } else {
            log.info("ptc trust root for {} not exists, insert it", ptcOwnerOidHex);
            embeddedBcdnsPtcTrustRootMapper.insert(
                    PtcTrustRootEntity.builder()
                            .latestVerifyAnchor(latestVa.getVersion().toString())
                            .issuerBcdnsDomainSpace(ptcTrustRoot.getIssuerBcdnsDomainSpace().getDomain())
                            .ptcCrosschainCert(ptcTrustRoot.getPtcCrossChainCert().encode())
                            .signAlgo(ptcTrustRoot.getSigAlgo().getName())
                            .sig(ptcTrustRoot.getSig())
                            .networkInfo(ptcTrustRoot.getNetworkInfo())
                            .ptcOidHex(ptcOwnerOidHex)
                            .build()
            );
        }

        ptcTrustRoot.getVerifyAnchorMap().values()
                .forEach(verifyAnchor -> addPtcVerifyAnchor(ptcOwnerOidHex, verifyAnchor));
    }

    private void addPtcVerifyAnchor(String ptcOwnerOidHex, PTCVerifyAnchor ptcVerifyAnchor) {
        if (embeddedBcdnsPtcVerifyAnchorMapper.exists(
                new LambdaQueryWrapper<PtcVerifyAnchorEntity>()
                        .eq(PtcVerifyAnchorEntity::getPtcOidHex, ptcOwnerOidHex)
                        .eq(PtcVerifyAnchorEntity::getVersionNum, ptcVerifyAnchor.getVersion().toString())
        )) {
            log.info("ptc verify anchor with version {} for {} already exists, skip it",
                    ptcVerifyAnchor.getVersion().toString(), ptcOwnerOidHex);
            return;
        }
        log.info("add ptc verify anchor for {} with version {}", ptcOwnerOidHex, ptcVerifyAnchor.getVersion());

        embeddedBcdnsPtcVerifyAnchorMapper.insert(
                PtcVerifyAnchorEntity.builder()
                        .ptcOidHex(ptcOwnerOidHex)
                        .versionNum(ptcVerifyAnchor.getVersion().toString())
                        .anchor(ptcVerifyAnchor.getAnchor())
                        .build()
        );
    }

    public boolean hasTpBta(CrossChainLane lane, int tpbtaVersion) {
        return embeddedBcdnsTpBtaMapper.exists(
                new LambdaQueryWrapper<TpBtaEntity>()
                        .eq(TpBtaEntity::getSenderDomain, lane.getSenderDomain().getDomain())
                        .eq(ObjectUtil.isNotNull(lane.getSenderIdHex()), TpBtaEntity::getSenderId, lane.getSenderIdHex())
                        .eq(TpBtaEntity::getSenderId, ObjectUtil.isNull(lane.getSenderId()) ? "" : lane.getSenderId().toHex())
                        .eq(TpBtaEntity::getReceiverDomain, ObjectUtil.isNull(lane.getReceiverDomain()) ? "" : lane.getReceiverDomain().getDomain())
                        .eq(TpBtaEntity::getReceiverId, ObjectUtil.isNull(lane.getReceiverId()) ? "" : lane.getReceiverId().toHex())
                        .eq(TpBtaEntity::getTpbtaVersion, tpbtaVersion)
        );
    }

    private List<TpBtaEntity> searchTpBta(CrossChainLane lane, int tpbtaVersion) {
        // search the blockchain level first
        LambdaQueryWrapper<TpBtaEntity> wrapper = new LambdaQueryWrapper<TpBtaEntity>()
                .eq(TpBtaEntity::getSenderDomain, lane.getSenderDomain().getDomain())
                .eq(TpBtaEntity::getReceiverDomain, "")
                .eq(TpBtaEntity::getSenderId, "")
                .eq(TpBtaEntity::getReceiverId, "");
        List<TpBtaEntity> entityList = embeddedBcdnsTpBtaMapper.selectList(
                tpbtaVersion == -1 ? wrapper : wrapper.eq(TpBtaEntity::getTpbtaVersion, tpbtaVersion)
        );
        if (ObjectUtil.isNotEmpty(entityList)) {
            return entityList;
        }

        if (ObjectUtil.isNull(lane.getReceiverDomain()) || ObjectUtil.isEmpty(lane.getReceiverDomain().getDomain())) {
            return ListUtil.empty();
        }
        // search the channel level
        wrapper = new LambdaQueryWrapper<TpBtaEntity>()
                .eq(TpBtaEntity::getSenderDomain, lane.getSenderDomain().getDomain())
                .eq(TpBtaEntity::getReceiverDomain, lane.getReceiverDomain().getDomain())
                .eq(TpBtaEntity::getSenderId, "")
                .eq(TpBtaEntity::getReceiverId, "");
        entityList = embeddedBcdnsTpBtaMapper.selectList(
                tpbtaVersion == -1 ? wrapper : wrapper.eq(TpBtaEntity::getTpbtaVersion, tpbtaVersion)
        );
        if (ObjectUtil.isNotEmpty(entityList)) {
            return entityList;
        }

        if (ObjectUtil.isNull(lane.getSenderId()) || ObjectUtil.isNull(lane.getReceiverId())
                || ObjectUtil.isEmpty(lane.getSenderId().getRawID()) || ObjectUtil.isEmpty(lane.getReceiverId().getRawID())) {
            return ListUtil.empty();
        }
        // search the lane level
        wrapper = new LambdaQueryWrapper<TpBtaEntity>()
                .eq(TpBtaEntity::getSenderDomain, lane.getSenderDomain().getDomain())
                .eq(TpBtaEntity::getSenderId, lane.getSenderId().toHex())
                .eq(TpBtaEntity::getReceiverDomain, lane.getReceiverDomain().getDomain())
                .eq(TpBtaEntity::getReceiverId, lane.getReceiverId().toHex());
        entityList = embeddedBcdnsTpBtaMapper.selectList(
                tpbtaVersion == -1 ? wrapper : wrapper.eq(TpBtaEntity::getTpbtaVersion, tpbtaVersion)
        );
        if (ObjectUtil.isNotEmpty(entityList)) {
            return entityList;
        }

        return ListUtil.empty();
    }
}
