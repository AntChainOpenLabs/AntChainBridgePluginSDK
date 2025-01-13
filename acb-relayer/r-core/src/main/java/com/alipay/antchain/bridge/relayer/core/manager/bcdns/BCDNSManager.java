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

package com.alipay.antchain.bridge.relayer.core.manager.bcdns;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.thread.lock.LockUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.bcdns.service.BCDNSTypeEnum;
import com.alipay.antchain.bridge.bcdns.service.IBlockChainDomainNameService;
import com.alipay.antchain.bridge.bcdns.types.base.DomainRouter;
import com.alipay.antchain.bridge.bcdns.types.base.Relayer;
import com.alipay.antchain.bridge.bcdns.types.req.QueryDomainNameCertificateRequest;
import com.alipay.antchain.bridge.bcdns.types.req.QueryDomainRouterRequest;
import com.alipay.antchain.bridge.bcdns.types.req.RegisterDomainRouterRequest;
import com.alipay.antchain.bridge.bcdns.types.req.RegisterThirdPartyBlockchainTrustAnchorRequest;
import com.alipay.antchain.bridge.bcdns.types.resp.ApplyDomainNameCertificateResponse;
import com.alipay.antchain.bridge.bcdns.types.resp.QueryBCDNSTrustRootCertificateResponse;
import com.alipay.antchain.bridge.bcdns.types.resp.QueryDomainNameCertificateResponse;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.relayer.commons.constant.BCDNSStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.DomainCertApplicationStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.*;
import com.alipay.antchain.bridge.relayer.dal.repository.IBCDNSRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.IBlockchainRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.IPtcServiceRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.ISystemConfigRepository;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class BCDNSManager implements IBCDNSManager {

    private static final String BCDNS_CALLING_LOCK_PREFIX = "BCDNS_CALLING_LOCK_";

    @Resource
    private IBCDNSRepository bcdnsRepository;

    @Resource
    private IBlockchainRepository blockchainRepository;

    @Resource
    private ISystemConfigRepository systemConfigRepository;

    @Resource
    private RedissonClient redisson;

    @Resource
    private IPtcServiceRepository ptcServiceRepository;

    @Value("#{relayerCoreConfig.localRelayerCrossChainCertificate}")
    private AbstractCrossChainCertificate localRelayerCertificate;

    private final Map<String, IBlockChainDomainNameService> bcdnsClientMap = new ConcurrentHashMap<>();

    @Override
    public IBlockChainDomainNameService getBCDNSService(String domainSpace) {
        if (bcdnsClientMap.containsKey(domainSpace)) {
            return bcdnsClientMap.get(domainSpace);
        }

        BCDNSServiceDO bcdnsServiceDO = getBCDNSServiceData(domainSpace);
        if (ObjectUtil.isNull(bcdnsServiceDO)) {
            log.warn("none bcdns data found for domain space {}", domainSpace);
            return null;
        }
        if (bcdnsServiceDO.getState() != BCDNSStateEnum.WORKING) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BCDNS_MANAGER_ERROR,
                    "BCDNS with domain space {} is not working now",
                    domainSpace
            );
        }

        return startBCDNSService(bcdnsServiceDO);
    }

    @Override
    @Synchronized
    @Transactional(rollbackFor = AntChainBridgeRelayerException.class)
    public void registerBCDNSService(String domainSpace, BCDNSTypeEnum bcdnsType, String propFilePath, String bcdnsCertPath) {
        try {
            if (hasBCDNSServiceData(domainSpace)) {
                throw new RuntimeException("bcdns already registered");
            }

            BCDNSServiceDO bcdnsServiceDO = new BCDNSServiceDO();
            bcdnsServiceDO.setType(bcdnsType);
            bcdnsServiceDO.setDomainSpace(domainSpace);

            try {
                bcdnsServiceDO.setProperties(Files.readAllBytes(Paths.get(propFilePath)));
            } catch (Exception e) {
                throw new RuntimeException("failed to read properties from file " + propFilePath, e);
            }

            if (StrUtil.isNotEmpty(bcdnsCertPath)) {
                try {
                    AbstractCrossChainCertificate certificate = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
                            Files.readAllBytes(Paths.get(bcdnsCertPath))
                    );
                    if (CrossChainCertificateUtil.isBCDNSTrustRoot(certificate)
                            && !StrUtil.equals(domainSpace, CrossChainDomain.ROOT_DOMAIN_SPACE)) {
                        throw new RuntimeException("the space name of bcdns trust root certificate supposed to have the root space name bug got : " + domainSpace);
                    } else if (!CrossChainCertificateUtil.isBCDNSTrustRoot(certificate)
                            && !CrossChainCertificateUtil.isDomainSpaceCert(certificate)) {
                        throw new RuntimeException("expected bcdns trust root or domain space type certificate bug got : " + certificate.getType().name());
                    }
                    bcdnsServiceDO.setDomainSpaceCertWrapper(
                            new DomainSpaceCertWrapper(certificate)
                    );
                    bcdnsServiceDO.setOwnerOid(certificate.getCredentialSubjectInstance().getApplicant());
                } catch (Exception e) {
                    throw new RuntimeException("failed to read bcdns cert from file " + bcdnsCertPath, e);
                }
            }

            startBCDNSService(bcdnsServiceDO);
            saveBCDNSServiceData(bcdnsServiceDO);
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BCDNS_MANAGER_ERROR,
                    e,
                    "failed to register bcdns service client for [{}]",
                    domainSpace
            );
        }
    }

    @Override
    @Synchronized
    public IBlockChainDomainNameService startBCDNSService(BCDNSServiceDO bcdnsServiceDO) {
        log.info("starting the bcdns service ( type: {}, domain_space: {} )",
                bcdnsServiceDO.getType().getCode(), bcdnsServiceDO.getDomainSpace());
        try {
            IBlockChainDomainNameService service = new BCDNSWrapper(
                    bcdnsServiceDO,
                    getBCDNSCallingLock(bcdnsServiceDO.getDomainSpace(), bcdnsServiceDO.getType())
            );
            if (ObjectUtil.isNull(service)) {
                throw new AntChainBridgeRelayerException(
                        RelayerErrorCodeEnum.CORE_BCDNS_MANAGER_ERROR,
                        "bcdns {} start failed",
                        bcdnsServiceDO.getDomainSpace()
                );
            }
            if (
                    ObjectUtil.isNull(bcdnsServiceDO.getDomainSpaceCertWrapper())
                            || ObjectUtil.isNull(bcdnsServiceDO.getDomainSpaceCertWrapper().getDomainSpaceCert())
            ) {
                QueryBCDNSTrustRootCertificateResponse response = service.queryBCDNSTrustRootCertificate();
                if (ObjectUtil.isNull(response) || ObjectUtil.isNull(response.getBcdnsTrustRootCertificate())) {
                    throw new AntChainBridgeRelayerException(
                            RelayerErrorCodeEnum.CORE_BCDNS_MANAGER_ERROR,
                            "query empty root cert from bcdns {}",
                            bcdnsServiceDO.getDomainSpace()
                    );
                }
                bcdnsServiceDO.setOwnerOid(response.getBcdnsTrustRootCertificate().getCredentialSubjectInstance().getApplicant());
                bcdnsServiceDO.setDomainSpaceCertWrapper(
                        new DomainSpaceCertWrapper(response.getBcdnsTrustRootCertificate())
                );
            }
            bcdnsServiceDO.setState(BCDNSStateEnum.WORKING);
            bcdnsClientMap.put(bcdnsServiceDO.getDomainSpace(), service);

            return service;
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BCDNS_MANAGER_ERROR,
                    e,
                    "failed to start bcdns service client for [{}]",
                    bcdnsServiceDO.getDomainSpace()
            );
        }
    }

    @Override
    @Transactional(rollbackFor = AntChainBridgeRelayerException.class)
    @Synchronized
    public void restartBCDNSService(String domainSpace) {
        log.info("restarting the bcdns service ( domain_space: {} )", domainSpace);
        try {
            BCDNSServiceDO bcdnsServiceDO = getBCDNSServiceData(domainSpace);
            if (ObjectUtil.isNull(bcdnsServiceDO)) {
                throw new RuntimeException(StrUtil.format("bcdns {} not exist", domainSpace));
            }
            if (bcdnsServiceDO.getState() != BCDNSStateEnum.FROZEN) {
                throw new RuntimeException(StrUtil.format("bcdns {} already in state {}", domainSpace, bcdnsServiceDO.getState().getCode()));
            }
            IBlockChainDomainNameService service = new BCDNSWrapper(
                    bcdnsServiceDO,
                    getBCDNSCallingLock(bcdnsServiceDO.getDomainSpace(), bcdnsServiceDO.getType())
            );
            if (ObjectUtil.isNull(service)) {
                throw new AntChainBridgeRelayerException(
                        RelayerErrorCodeEnum.CORE_BCDNS_MANAGER_ERROR,
                        "bcdns {} start failed",
                        bcdnsServiceDO.getDomainSpace()
                );
            }
            bcdnsRepository.updateBCDNSServiceState(domainSpace, BCDNSStateEnum.WORKING);
            bcdnsClientMap.put(bcdnsServiceDO.getDomainSpace(), service);

        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BCDNS_MANAGER_ERROR,
                    e,
                    "failed to restart bcdns service client for {}",
                    domainSpace
            );
        }
    }

    @Override
    @Transactional(rollbackFor = AntChainBridgeRelayerException.class)
    @Synchronized
    public void stopBCDNSService(String domainSpace) {
        log.info("stopping the bcdns service ( domain_space: {} )", domainSpace);
        try {
            if (!hasBCDNSServiceData(domainSpace)) {
                throw new RuntimeException("bcdns not exist for " + domainSpace);
            }
            bcdnsRepository.updateBCDNSServiceState(domainSpace, BCDNSStateEnum.FROZEN);
            bcdnsClientMap.remove(domainSpace);
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BCDNS_MANAGER_ERROR,
                    e,
                    "failed to stop bcdns service client for {}",
                    domainSpace
            );
        }
    }

    @Override
    public void saveBCDNSServiceData(BCDNSServiceDO bcdnsServiceDO) {
        if (bcdnsRepository.hasBCDNSService(bcdnsServiceDO.getDomainSpace())) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BCDNS_MANAGER_ERROR,
                    "bcdns {} not exist or data incomplete",
                    bcdnsServiceDO.getDomainSpace()
            );
        }
        bcdnsRepository.saveBCDNSServiceDO(bcdnsServiceDO);
    }

    @Override
    public BCDNSServiceDO getBCDNSServiceData(String domainSpace) {
        return bcdnsRepository.getBCDNSServiceDO(domainSpace);
    }

    @Override
    @Synchronized
    public void deleteBCDNSServiceDate(String domainSpace) {
        bcdnsRepository.deleteBCDNSServiceDO(domainSpace);
        bcdnsClientMap.remove(domainSpace);
    }

    @Override
    public List<String> getAllBCDNSDomainSpace() {
        return bcdnsRepository.getAllBCDNSDomainSpace();
    }

    @Override
    public boolean hasBCDNSServiceData(String domainSpace) {
        return bcdnsRepository.hasBCDNSService(domainSpace);
    }

    @Override
    public Map<String, AbstractCrossChainCertificate> getTrustRootCertChain(String domainSpace) {
        return bcdnsRepository.getDomainSpaceCertChain(domainSpace).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getDomainSpaceCert()
                ));
    }

    @Override
    public List<String> getDomainSpaceChain(String domainSpace) {
        return bcdnsRepository.getDomainSpaceChain(domainSpace);
    }

    @Override
    public AbstractCrossChainCertificate getTrustRootCertForRootDomain() {
        DomainSpaceCertWrapper wrapper = bcdnsRepository.getDomainSpaceCert(CrossChainDomain.ROOT_DOMAIN_SPACE);
        if (ObjectUtil.isNull(wrapper)) {
            return null;
        }
        return wrapper.getDomainSpaceCert();
    }

    @Override
    public boolean validateCrossChainCertificate(AbstractCrossChainCertificate certificate) {
        DomainSpaceCertWrapper trustRootCert = bcdnsRepository.getDomainSpaceCert(certificate.getIssuer());
        if (ObjectUtil.isNull(trustRootCert)) {
            log.warn(
                    "none trust root found for {} to verify for relayer cert: {}",
                    HexUtil.encodeHexStr(certificate.getIssuer().encode()),
                    CrossChainCertificateUtil.formatCrossChainCertificateToPem(certificate)
            );
            return false;
        }
        return trustRootCert.getDomainSpaceCert().getCredentialSubjectInstance().verifyIssueProof(
                certificate.getEncodedToSign(),
                certificate.getProof()
        );
    }

    @Override
    public DomainSpaceCertWrapper getDomainSpaceCert(String domainSpace) {
        return bcdnsRepository.getDomainSpaceCert(domainSpace);
    }

    @Override
    public boolean validateCrossChainCertificate(AbstractCrossChainCertificate certificate, Map<String, AbstractCrossChainCertificate> domainSpaceCertPath) {
        try {
            List<AbstractCrossChainCertificate> sequentialCerts = domainSpaceCertPath.entrySet().stream().sorted(
                            Comparator.comparing(o -> StrUtil.reverse(o.getKey()))
                    ).map(Map.Entry::getValue)
                    .collect(Collectors.toList());

            DomainSpaceCertWrapper bcdnsRootCert = bcdnsRepository.getDomainSpaceCert(CrossChainDomain.ROOT_DOMAIN_SPACE);
            if (ObjectUtil.isNull(bcdnsRootCert)) {
                throw new RuntimeException("none root domain space cert set in DB");
            }
            sequentialCerts.set(0, bcdnsRootCert.getDomainSpaceCert());

            if (sequentialCerts.size() > 1) {
                verifyCertPath(sequentialCerts, 0);
                saveDomainSpaceCerts(domainSpaceCertPath);
            }

            return sequentialCerts.get(sequentialCerts.size() - 1)
                    .getCredentialSubjectInstance().verifyIssueProof(
                            certificate.getEncodedToSign(),
                            certificate.getProof()
                    );
        } catch (Exception e) {
            log.error("failed to verify crosschain cert (type: {}, cert_id: {})", certificate.getType().name(), certificate.getId(), e);
            return false;
        }
    }

    @Override
    public void saveDomainSpaceCerts(Map<String, AbstractCrossChainCertificate> domainSpaceCerts) {
        for (Map.Entry<String, AbstractCrossChainCertificate> entry : domainSpaceCerts.entrySet()) {
            try {
                if (bcdnsRepository.hasDomainSpaceCert(entry.getKey())) {
                    log.info("DomainSpace {} already exists", entry.getKey());
                    continue;
                }
                bcdnsRepository.saveDomainSpaceCert(new DomainSpaceCertWrapper(entry.getValue()));
                log.info("successful to save domain space cert for {}", entry.getKey());
            } catch (Exception e) {
                log.error("failed to save domain space certs for space {} : ", entry.getKey(), e);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = AntChainBridgeRelayerException.class)
    public String applyDomainCertificate(String domainSpace, String domain, ObjectIdentity applicantOid, byte[] rawSubject) {
        log.info("try to apply the domain cert for {} from bcdns with space {}", domain, domainSpace);
        try {
            if (!CrossChainDomain.isDerivedFrom(domain, domainSpace)) {
                throw new RuntimeException(StrUtil.format("domain {} is not derived from space {}", domain, domainSpace));
            }
            if (blockchainRepository.hasDomainCert(domain)) {
                throw new RuntimeException(StrUtil.format("domain {} already exist locally", domain));
            }
            if (bcdnsRepository.hasDomainCertApplicationEntry(domain)) {
                throw new RuntimeException(StrUtil.format("domain {} already in applying locally", domain));
            }
            if (!hasBCDNSServiceData(domainSpace)) {
                throw new RuntimeException("bcdns not exist for " + domainSpace);
            }

            IBlockChainDomainNameService bcdnsService = getBCDNSService(domainSpace);
            if (ObjectUtil.isNull(bcdnsService)) {
                throw new RuntimeException("null bcdns client after start " + domainSpace);
            }

            QueryDomainNameCertificateResponse domainNameCertificateResponse = bcdnsService.queryDomainNameCertificate(
                    QueryDomainNameCertificateRequest.builder()
                            .domain(new CrossChainDomain(domain))
                            .build()
            );
            if (domainNameCertificateResponse.isExist()) {
                throw new RuntimeException(
                        StrUtil.format("domain {} already registered on BCDNS with space {}", domain, domainSpace)
                );
            }

            ApplyDomainNameCertificateResponse response = bcdnsService.applyDomainNameCertificate(
                    CrossChainCertificateFactory.createDomainNameCertificateSigningRequest(
                            CrossChainCertificateFactory.DEFAULT_VERSION,
                            new CrossChainDomain(domainSpace),
                            new CrossChainDomain(domain),
                            applicantOid,
                            rawSubject
                    )
            );

            bcdnsRepository.saveDomainCertApplicationEntry(
                    new DomainCertApplicationDO(
                            domain,
                            domainSpace,
                            response.getApplyReceipt(),
                            DomainCertApplicationStateEnum.APPLYING
                    )
            );

            return response.getApplyReceipt();
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BCDNS_MANAGER_ERROR,
                    e,
                    "failed to apply domain cert for {} to {}",
                    domain, domainSpace
            );
        }
    }

    @Override
    public AbstractCrossChainCertificate queryDomainCertificateFromBCDNS(String domain, String domainSpace, boolean saveOrNot) {
        log.info("try to get domain cert of {} from BCDNS {}", domain, domainSpace);
        try {
            IBlockChainDomainNameService bcdnsService = getBCDNSService(domainSpace);
            if (ObjectUtil.isNull(bcdnsService)) {
                throw new RuntimeException("none bcdns service created");
            }

            QueryDomainNameCertificateResponse response = bcdnsService.queryDomainNameCertificate(
                    QueryDomainNameCertificateRequest.builder()
                            .domain(new CrossChainDomain(domain))
                            .build()
            );
            if (ObjectUtil.isNull(response)) {
                throw new RuntimeException("none response from BCDNS");
            }
            if (response.isExist()) {
                if (!CrossChainCertificateUtil.isDomainCert(response.getCertificate())) {
                    throw new RuntimeException(
                            "the type of cert from bcdns is not domain cert: \n"
                                    + CrossChainCertificateUtil.formatCrossChainCertificateToPem(response.getCertificate())
                    );
                }
                if (!StrUtil.equals(domain, CrossChainCertificateUtil.getCrossChainDomain(response.getCertificate()).getDomain())) {
                    throw new RuntimeException(
                            StrUtil.format(
                                    "unexpected domain {} in cert from BCDNS not equal to {}",
                                    CrossChainCertificateUtil.getCrossChainDomain(response.getCertificate()).getDomain(),
                                    domain
                            )
                    );
                }
                if (saveOrNot) {
                    blockchainRepository.saveDomainCert(new DomainCertWrapper(response.getCertificate()));
                }
                return response.getCertificate();
            }
            return null;
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BCDNS_MANAGER_ERROR,
                    e,
                    "failed to query and save domain cert for {} from {}",
                    domain, domainSpace
            );
        }
    }

    @Override
    public List<DomainCertApplicationDO> getAllApplyingDomainCertApplications() {
        log.debug("try to get all applying domain cert applications");
        try {
            return bcdnsRepository.getDomainCertApplicationsByState(DomainCertApplicationStateEnum.APPLYING);
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BCDNS_MANAGER_ERROR,
                    e,
                    "failed to get all applying domain cert applications"
            );
        }
    }

    @Override
    public DomainCertApplicationDO getDomainCertApplication(String domain) {
        try {
            return bcdnsRepository.getDomainCertApplicationEntry(domain);
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BCDNS_MANAGER_ERROR,
                    e,
                    "failed to get domain cert application for {}",
                    domain
            );
        }
    }

    @Override
    public void saveDomainCertApplicationResult(String domain, AbstractCrossChainCertificate domainCert) {
        try {
            if (!bcdnsRepository.hasDomainCertApplicationEntry(domain)) {
                throw new RuntimeException("application not exist");
            }
            if (blockchainRepository.hasDomainCert(domain)) {
                throw new RuntimeException("domain already exist");
            }
            if (!CrossChainCertificateUtil.isDomainCert(domainCert)) {
                throw new RuntimeException("cert is not domain cert");
            }

            bcdnsRepository.updateDomainCertApplicationState(
                    domain,
                    ObjectUtil.isNull(domainCert) ?
                            DomainCertApplicationStateEnum.APPLY_FAILED : DomainCertApplicationStateEnum.APPLY_SUCCESS
            );

            if (ObjectUtil.isNotNull(domainCert)) {
                blockchainRepository.saveDomainCert(new DomainCertWrapper(domainCert));
            }
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BCDNS_MANAGER_ERROR,
                    e,
                    "failed to save domain cert application result for {}",
                    domain
            );
        }
    }

    @Override
    @Transactional(rollbackFor = AntChainBridgeRelayerException.class)
    public void bindDomainCertWithBlockchain(String domain, String product, String blockchainId) {
        try {
            if (!blockchainRepository.hasDomainCert(domain)) {
                throw new RuntimeException("domain not exist");
            }
            blockchainRepository.updateBlockchainInfoOfDomainCert(domain, product, blockchainId);
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BCDNS_MANAGER_ERROR,
                    e,
                    "failed to bind domain cert of {} with blockchain (product: {}, blockchain_id: {})",
                    domain, product, blockchainId
            );
        }
    }

    @Override
    @Transactional(rollbackFor = AntChainBridgeRelayerException.class)
    public void registerDomainRouter(String domain) {
        try {
            if (!blockchainRepository.hasDomainCert(domain)) {
                throw new RuntimeException("dest domain not exist in local");
            }

            DomainCertWrapper domainCertWrapper = blockchainRepository.getDomainCert(domain);
            getBCDNSService(domainCertWrapper.getDomainSpace())
                    .registerDomainRouter(
                            RegisterDomainRouterRequest.builder()
                                    .domainCert(domainCertWrapper.getCrossChainCertificate())
                                    .router(
                                            new DomainRouter(
                                                    new CrossChainDomain(domain),
                                                    new Relayer(
                                                            localRelayerCertificate.getId(),
                                                            localRelayerCertificate,
                                                            systemConfigRepository.getLocalEndpoints()
                                                    )
                                            )
                                    ).build()
                    );

            log.info("successful to register domain router for {} to BCDNS with domain space [{}]",
                    domainCertWrapper.getDomain(), domainCertWrapper.getDomainSpace());
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BCDNS_MANAGER_ERROR,
                    e,
                    "failed to register domain router for domain {}",
                    domain
            );
        }
    }

    @Override
    public DomainRouter getDomainRouter(String destDomain) {
        for (String domainSpace : ListUtil.sort(getAllBCDNSDomainSpace(), String::compareTo)) {
            if (!hasBCDNSServiceData(domainSpace)) {
                continue;
            }
            if (!CrossChainDomain.isDerivedFrom(destDomain, domainSpace)) {
                continue;
            }
            log.info("look up the domain router for {} from BCDNS with space [{}]", destDomain, domainSpace);
            DomainRouter currDomainRouter = getBCDNSService(domainSpace).queryDomainRouter(
                    QueryDomainRouterRequest.builder()
                            .destDomain(new CrossChainDomain(destDomain))
                            .build()
            );
            if (ObjectUtil.isNull(currDomainRouter)) {
                log.info("the domain router for {} not found on BCDNS with space [{}]", destDomain, domainSpace);
                continue;
            }
            if (ObjectUtil.isNull(currDomainRouter.getDestRelayer())) {
                log.info("the domain router for {} found on BCDNS with space [{}] has no relayer info", destDomain, domainSpace);
                continue;
            }

            log.info("get domain router for {} from the BCDNS {}", destDomain, domainSpace);
            return currDomainRouter;
        }
        return null;
    }

    @Override
    public void uploadTpBta(CrossChainLane tpbtaLane, int tpbtaVersion) {
        log.info("try to register tpbta {} with version {}", tpbtaLane, tpbtaVersion);
        try {
            TpBtaDO tpBtaDO;
            if (tpbtaVersion == -1) {
                tpBtaDO = ptcServiceRepository.getExactTpBta(tpbtaLane);
            } else {
                tpBtaDO = ptcServiceRepository.getExactTpBta(tpbtaLane, tpbtaVersion);
            }
            if (ObjectUtil.isNull(tpBtaDO)) {
                throw new RuntimeException("tpbta not exist");
            }

            DomainCertWrapper domainCertWrapper = blockchainRepository.getDomainCert(tpBtaDO.getCrossChainLane().getSenderDomain().toString());
            if (ObjectUtil.isNull(domainCertWrapper)) {
                throw new RuntimeException(StrUtil.format("domain {} isn't registered in local relayer! ", tpBtaDO.getCrossChainLane().getSenderDomain().toString()));
            }

            getBCDNSService(domainCertWrapper.getDomainSpace()).registerThirdPartyBlockchainTrustAnchor(
                    RegisterThirdPartyBlockchainTrustAnchorRequest.builder()
                            .domain(tpBtaDO.getCrossChainLane().getSenderDomain())
                            .ptcId(tpBtaDO.getTpbta().getSignerPtcCredentialSubject().getApplicant())
                            .tpbta(tpBtaDO.getTpbta())
                            .build()
            );
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BCDNS_MANAGER_ERROR,
                    e,
                    "failed to register tpbta {} with version {} to BCDNS",
                    tpbtaLane, tpbtaVersion
            );
        }
    }

    private void verifyCertPath(List<AbstractCrossChainCertificate> certPath, int currIndex) {
        if (currIndex == certPath.size() - 2) {
            if (
                    !certPath.get(currIndex).getCredentialSubjectInstance().verifyIssueProof(
                            certPath.get(currIndex + 1).getEncodedToSign(),
                            certPath.get(currIndex + 1).getProof()
                    )
            ) {
                throw new RuntimeException(
                        StrUtil.format(
                                "failed to verify {} cert with its parent {}",
                                CrossChainCertificateUtil.getCrossChainDomain(certPath.get(currIndex)),
                                CrossChainCertificateUtil.getCrossChainDomain(certPath.get(currIndex + 1))
                        )
                );
            }
            return;
        }

        verifyCertPath(certPath, currIndex + 1);
    }

    private Lock getBCDNSCallingLock(String domainSpace, BCDNSTypeEnum bcdnsType) {
        if (BCDNSTypeEnum.BIF == bcdnsType) {
            return redisson.getLock(BCDNS_CALLING_LOCK_PREFIX + domainSpace);
        }
        return LockUtil.getNoLock();
    }
}
