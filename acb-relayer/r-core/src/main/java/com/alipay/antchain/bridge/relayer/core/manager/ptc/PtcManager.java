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

package com.alipay.antchain.bridge.relayer.core.manager.ptc;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.PTCCredentialSubject;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTypeEnum;
import com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyBlockchainTrustAnchor;
import com.alipay.antchain.bridge.ptc.service.IPTCService;
import com.alipay.antchain.bridge.relayer.commons.constant.PtcServiceStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.PtcServiceException;
import com.alipay.antchain.bridge.relayer.commons.model.PtcServiceDO;
import com.alipay.antchain.bridge.relayer.commons.model.PtcTrustRootDO;
import com.alipay.antchain.bridge.relayer.commons.model.TpBtaDO;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.dal.repository.IPtcServiceRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class PtcManager implements IPtcManager {

    @Resource
    private IBCDNSManager bcdnsManager;

    @Resource
    private IPtcServiceRepository ptcServiceRepository;

    private final Map<String, IPTCService> ptcServiceMap = new ConcurrentHashMap<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerPtcService(@NonNull String serviceId, @NonNull CrossChainDomain issuerDomainSpace, @NonNull AbstractCrossChainCertificate ptcCert, byte[] clientConfig) {
        if (!issuerDomainSpace.isDomainSpace()) {
            throw new PtcServiceException("Invalid issuer domain space {}", issuerDomainSpace.getDomain());
        }
        if (ptcServiceRepository.hasPtcServiceData(serviceId)) {
            throw new PtcServiceException("Ptc service {} already registered", serviceId);
        }

        PTCTrustRoot trustRoot = null;
        if (bcdnsManager.hasBCDNSServiceData(issuerDomainSpace.getDomain())) {
            trustRoot = bcdnsManager.getBCDNSService(issuerDomainSpace.getDomain()).queryPTCTrustRoot(ptcCert.getCredentialSubjectInstance().getApplicant());
        }
        if (ObjectUtil.isNotNull(trustRoot)) {
            log.info("Use the ptc cert inside trust root");
            ptcCert = trustRoot.getPtcCrossChainCert();
        }

        PTCTypeEnum ptcType = ((PTCCredentialSubject) ptcCert.getCredentialSubjectInstance()).getType();
        clientConfig = PtcServiceFactory.buildPtcConfig(ptcCert, trustRoot, clientConfig);
        if (ObjectUtil.isEmpty(clientConfig)) {
            throw new PtcServiceException("Build ptc client config but get empty result");
        }
        // create and start
        IPTCService ptcService = PtcServiceFactory.createPtcServiceStub(ptcCert, clientConfig);
        if (ObjectUtil.isNull(ptcService)) {
            throw new PtcServiceException("Create ptc service stub returned null");
        }

        log.info("save the data for ptc service {}", serviceId);
        ptcServiceRepository.savePtcServiceData(
                PtcServiceDO.builder()
                        .type(ptcType)
                        .state(PtcServiceStateEnum.WORKING)
                        .serviceId(serviceId)
                        .issuerBcdnsDomainSpace(issuerDomainSpace.getDomain())
                        .ptcCert(ptcCert)
                        .clientConfig(clientConfig)
                        .build()
        );
        if (ObjectUtil.isNotNull(trustRoot)) {
            log.info("save the trust root of ptc service {}", serviceId);
            ptcServiceRepository.savePtcTrustRoot(PtcTrustRootDO.builder().ptcServiceId(serviceId).ptcTrustRoot(trustRoot).build());
        }
        ptcServiceMap.put(serviceId, ptcService);
    }

    @Override
    public void stopPtcService(String serviceId) {
        if (!ptcServiceRepository.hasPtcServiceData(serviceId)) {
            throw new PtcServiceException("Ptc service {} not found", serviceId);
        }
        ptcServiceMap.remove(serviceId);
        ptcServiceRepository.updatePtcServiceState(serviceId, PtcServiceStateEnum.FROZEN);
    }

    @Override
    public void startPtcService(String serviceId) {
        if (!ptcServiceRepository.hasPtcServiceData(serviceId)) {
            throw new PtcServiceException("Ptc service {} not found", serviceId);
        }
        PtcServiceDO ptcServiceDO = ptcServiceRepository.getPtcServiceData(serviceId);
        IPTCService ptcService = PtcServiceFactory.createPtcServiceStub(ptcServiceDO.getPtcCert(), ptcServiceDO.getClientConfig());
        if (ObjectUtil.isNull(ptcService)) {
            throw new PtcServiceException("Create ptc service stub returned null");
        }
        if (ptcServiceDO.getState() != PtcServiceStateEnum.WORKING) {
            ptcServiceRepository.updatePtcServiceState(serviceId, PtcServiceStateEnum.WORKING);
        }
        ptcServiceMap.put(serviceId, ptcService);
    }

    @Override
    public IPTCService getPtcService(String serviceId) {
        if (!ptcServiceRepository.hasPtcServiceData(serviceId)) {
            throw new PtcServiceException("Ptc service {} not found", serviceId);
        }
        PtcServiceDO ptcServiceDO = ptcServiceRepository.getPtcServiceData(serviceId);
        if (ptcServiceDO.getState() != PtcServiceStateEnum.WORKING) {
            throw new PtcServiceException("Ptc service {} is not working", serviceId);
        }
        if (ptcServiceMap.containsKey(serviceId)) {
            return ptcServiceMap.get(serviceId);
        }
        startPtcService(serviceId);
        return ptcServiceMap.get(serviceId);
    }

    @Override
    public boolean isPtcServiceWork(String serviceId) {
        if (!ptcServiceRepository.hasPtcServiceData(serviceId)) {
            return false;
        }
        PtcServiceDO ptcServiceDO = ptcServiceRepository.getPtcServiceData(serviceId);
        return ptcServiceDO.getState() == PtcServiceStateEnum.WORKING;
    }

    @Override
    public void removePtcService(String serviceId) {
        if (!ptcServiceRepository.hasPtcServiceData(serviceId)) {
            throw new PtcServiceException("Ptc service {} not found", serviceId);
        }
        PtcServiceDO ptcServiceDO = ptcServiceRepository.getPtcServiceData(serviceId);
        if (ptcServiceDO.getState() == PtcServiceStateEnum.WORKING) {
            throw new PtcServiceException("Ptc service {} is working, stop it and then remove it", serviceId);
        }
        ptcServiceMap.remove(serviceId);
        ptcServiceRepository.removePtcServiceData(serviceId);
    }

    @Override
    public PtcServiceDO getPtcServiceDO(String serviceId) {
        return ptcServiceRepository.getPtcServiceData(serviceId);
    }

    @Override
    public PtcServiceDO getPtcServiceDO(ObjectIdentity ptcOwnerOid) {
        return ptcServiceRepository.getPtcServiceData(ptcOwnerOid);
    }

    @Override
    public boolean isPtcServiceWorking(String serviceId) {
        return ptcServiceRepository.queryPtcServiceState(serviceId) == PtcServiceStateEnum.WORKING;
    }

    @Override
    public List<String> getAllPtcServiceIdForDomain(CrossChainDomain domain) {
        return ptcServiceRepository.getAllPtcServiceIdForDomain(domain.getDomain());
    }

    @Override
    public List<PtcServiceDO> getAllWorkingPtcServices() {
        return ptcServiceRepository.getAllWorkingPtcServices();
    }

    @Override
    public TpBtaDO getExactTpBta(CrossChainLane lane, int tpbtaVersion) {
        return ptcServiceRepository.getExactTpBta(lane, tpbtaVersion);
    }

    @Override
    public TpBtaDO getLatestExactTpBta(CrossChainLane lane) {
        return ptcServiceRepository.getExactTpBta(lane);
    }

    @Override
    public boolean hasExactTpBta(CrossChainLane lane, int tpbtaVersion) {
        return ptcServiceRepository.hasTpBta(lane, tpbtaVersion);
    }

    @Override
    public TpBtaDO getMatchedTpBta(CrossChainLane lane, int tpbtaVersion) {
        return ptcServiceRepository.getMatchedTpBta(lane, tpbtaVersion);
    }

    @Override
    public TpBtaDO getMatchedTpBta(CrossChainLane lane) {
        return ptcServiceRepository.getMatchedTpBta(lane);
    }

    @Override
    public List<TpBtaDO> getAllTpBtaForDomain(String ptcServiceId, CrossChainDomain domain) {
        return ptcServiceRepository.getAllTpBtaByDomain(ptcServiceId, domain.getDomain());
    }

    @Override
    public List<TpBtaDO> getAllValidTpBtaForDomain(CrossChainDomain domain) {
        return ptcServiceRepository.getAllValidTpBtaForDomain(domain);
    }

    @Override
    public void saveTpBta(@NonNull String senderProduct, @NonNull String senderBlockchainId, String ptcServiceId, @NonNull ThirdPartyBlockchainTrustAnchor tpBta) {
        if (StrUtil.isEmpty(ptcServiceId)) {
            PtcServiceDO ptcServiceDO = ptcServiceRepository.getPtcServiceData(tpBta.getSignerPtcCredentialSubject().getApplicant());
            if (ObjectUtil.isNotNull(ptcServiceDO)) {
                ptcServiceId = ptcServiceDO.getServiceId();
            }
        }
        ptcServiceRepository.setTpBta(
                TpBtaDO.builder()
                        .tpbta(tpBta)
                        .blockchainProduct(senderProduct)
                        .blockchainId(senderBlockchainId)
                        .ptcServiceId(ptcServiceId)
                        .build()
        );
    }

    @Override
    public PtcTrustRootDO savePtcTrustRoot(String ptcServiceId, PTCTrustRoot ptcTrustRoot) {
        if (StrUtil.isEmpty(ptcServiceId)) {
            PtcServiceDO ptcServiceDO = ptcServiceRepository.getPtcServiceData(ptcTrustRoot.getPtcCredentialSubject().getApplicant());
            if (ObjectUtil.isNotNull(ptcServiceDO)) {
                ptcServiceId = ptcServiceDO.getServiceId();
            }
        }
        PtcTrustRootDO ptcTrustRootDO = PtcTrustRootDO.builder()
                .ptcServiceId(ptcServiceId)
                .ptcTrustRoot(ptcTrustRoot)
                .build();
        ptcServiceRepository.savePtcTrustRoot(ptcTrustRootDO);

        return ptcTrustRootDO;
    }

    @Override
    public BigInteger getMaxPtcVerifyAnchorVersion(ObjectIdentity ptcOwnerOid) {
        return ptcServiceRepository.getMaxPtcVerifyAnchorVersion(ptcOwnerOid);
    }

    @Override
    public PtcTrustRootDO getPtcTrustRoot(ObjectIdentity ptcOwnerOid) {
        return ptcServiceRepository.getPtcTrustRoot(ptcOwnerOid);
    }

    @Override
    public PtcTrustRootDO getPtcTrustRoot(String ptcServiceId) {
        return ptcServiceRepository.getPtcTrustRoot(ptcServiceId);
    }
}
