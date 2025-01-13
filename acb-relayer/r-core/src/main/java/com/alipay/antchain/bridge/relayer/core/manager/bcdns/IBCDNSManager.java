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

import java.util.List;
import java.util.Map;

import com.alipay.antchain.bridge.bcdns.service.BCDNSTypeEnum;
import com.alipay.antchain.bridge.bcdns.service.IBlockChainDomainNameService;
import com.alipay.antchain.bridge.bcdns.types.base.DomainRouter;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.relayer.commons.model.BCDNSServiceDO;
import com.alipay.antchain.bridge.relayer.commons.model.DomainCertApplicationDO;
import com.alipay.antchain.bridge.relayer.commons.model.DomainSpaceCertWrapper;

public interface IBCDNSManager {

    IBlockChainDomainNameService getBCDNSService(String domainSpace);

    void registerBCDNSService(String domainSpace, BCDNSTypeEnum bcdnsType, String propFilePath, String bcdnsCertPath);

    IBlockChainDomainNameService startBCDNSService(BCDNSServiceDO bcdnsServiceDO);

    void restartBCDNSService(String domainSpace);

    void stopBCDNSService(String domainSpace);

    void saveBCDNSServiceData(BCDNSServiceDO bcdnsServiceDO);

    BCDNSServiceDO getBCDNSServiceData(String domainSpace);

    void deleteBCDNSServiceDate(String domainSpace);

    List<String> getAllBCDNSDomainSpace();

    boolean hasBCDNSServiceData(String domainSpace);

    Map<String, AbstractCrossChainCertificate> getTrustRootCertChain(String domainSpace);

    List<String> getDomainSpaceChain(String domainSpace);

    AbstractCrossChainCertificate getTrustRootCertForRootDomain();

    boolean validateCrossChainCertificate(AbstractCrossChainCertificate certificate);

    boolean validateCrossChainCertificate(
            AbstractCrossChainCertificate certificate,
            Map<String, AbstractCrossChainCertificate> domainSpaceCertPath
    );

    DomainSpaceCertWrapper getDomainSpaceCert(String domainSpace);

    void saveDomainSpaceCerts(Map<String, AbstractCrossChainCertificate> domainSpaceCerts);

    String applyDomainCertificate(String domainSpace, String domain, ObjectIdentity applicantOid, byte[] rawSubject);

    AbstractCrossChainCertificate queryDomainCertificateFromBCDNS(String domain, String domainSpace, boolean saveOrNot);

    List<DomainCertApplicationDO> getAllApplyingDomainCertApplications();

    DomainCertApplicationDO getDomainCertApplication(String domain);

    void saveDomainCertApplicationResult(String domain, AbstractCrossChainCertificate domainCert);

    void bindDomainCertWithBlockchain(String domain, String product, String blockchainId);

    void registerDomainRouter(String domain);

    DomainRouter getDomainRouter(String destDomain);

    void uploadTpBta(CrossChainLane tpbtaLane, int tpbtaVersion);
}
