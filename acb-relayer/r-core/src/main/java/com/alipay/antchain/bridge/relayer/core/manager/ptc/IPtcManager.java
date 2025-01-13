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

import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyBlockchainTrustAnchor;
import com.alipay.antchain.bridge.ptc.service.IPTCService;
import com.alipay.antchain.bridge.relayer.commons.model.PtcServiceDO;
import com.alipay.antchain.bridge.relayer.commons.model.PtcTrustRootDO;
import com.alipay.antchain.bridge.relayer.commons.model.TpBtaDO;

public interface IPtcManager {

    void registerPtcService(String serviceId, CrossChainDomain issuerDomainSpace, AbstractCrossChainCertificate ptcCert, byte[] clientConfig);

    void stopPtcService(String serviceId);

    void startPtcService(String serviceId);

    IPTCService getPtcService(String serviceId);

    boolean isPtcServiceWork(String serviceId);

    void removePtcService(String serviceId);

    PtcServiceDO getPtcServiceDO(String serviceId);

    PtcServiceDO getPtcServiceDO(ObjectIdentity ptcOwnerOid);

    boolean isPtcServiceWorking(String serviceId);

    List<String> getAllPtcServiceIdForDomain(CrossChainDomain domain);

    List<PtcServiceDO> getAllWorkingPtcServices();

    TpBtaDO getExactTpBta(CrossChainLane lane, int tpbtaVersion);

    TpBtaDO getLatestExactTpBta(CrossChainLane lane);

    boolean hasExactTpBta(CrossChainLane lane, int tpbtaVersion);

    TpBtaDO getMatchedTpBta(CrossChainLane lane, int tpbtaVersion);

    TpBtaDO getMatchedTpBta(CrossChainLane lane);

    List<TpBtaDO> getAllTpBtaForDomain(String ptcServiceId, CrossChainDomain domain);

    List<TpBtaDO> getAllValidTpBtaForDomain(CrossChainDomain domain);

    void saveTpBta(String senderProduct, String senderBlockchainId, String ptcServiceId, ThirdPartyBlockchainTrustAnchor tpBta);

    PtcTrustRootDO savePtcTrustRoot(String ptcServiceId, PTCTrustRoot ptcTrustRoot);

    BigInteger getMaxPtcVerifyAnchorVersion(ObjectIdentity ptcOwnerOid);

    PtcTrustRootDO getPtcTrustRoot(ObjectIdentity ptcOwnerOid);

    PtcTrustRootDO getPtcTrustRoot(String ptcServiceId);
}
