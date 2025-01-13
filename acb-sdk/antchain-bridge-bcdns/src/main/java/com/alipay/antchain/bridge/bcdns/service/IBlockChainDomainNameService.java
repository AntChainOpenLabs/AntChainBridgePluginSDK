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

package com.alipay.antchain.bridge.bcdns.service;

import com.alipay.antchain.bridge.bcdns.types.base.DomainRouter;
import com.alipay.antchain.bridge.bcdns.types.exception.AntChainBridgeBCDNSException;
import com.alipay.antchain.bridge.bcdns.types.req.*;
import com.alipay.antchain.bridge.bcdns.types.resp.*;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyBlockchainTrustAnchor;

public interface IBlockChainDomainNameService {

    QueryBCDNSTrustRootCertificateResponse queryBCDNSTrustRootCertificate();

    ApplyRelayerCertificateResponse applyRelayerCertificate(AbstractCrossChainCertificate certSigningRequest);

    ApplicationResult queryRelayerCertificateApplicationResult(String applyReceipt);

    ApplyPTCCertificateResponse applyPTCCertificate(AbstractCrossChainCertificate certSigningRequest);

    ApplicationResult queryPTCCertificateApplicationResult(String applyReceipt);

    ApplyDomainNameCertificateResponse applyDomainNameCertificate(AbstractCrossChainCertificate certSigningRequest);

    ApplicationResult queryDomainNameCertificateApplicationResult(String applyReceipt);

    QueryRelayerCertificateResponse queryRelayerCertificate(QueryRelayerCertificateRequest request);

    QueryPTCCertificateResponse queryPTCCertificate(QueryPTCCertificateRequest request);

    QueryDomainNameCertificateResponse queryDomainNameCertificate(QueryDomainNameCertificateRequest request);

    void registerDomainRouter(RegisterDomainRouterRequest request) throws AntChainBridgeBCDNSException;

    void registerThirdPartyBlockchainTrustAnchor(RegisterThirdPartyBlockchainTrustAnchorRequest request) throws AntChainBridgeBCDNSException;

    DomainRouter queryDomainRouter(QueryDomainRouterRequest request);

    ThirdPartyBlockchainTrustAnchor queryThirdPartyBlockchainTrustAnchor(QueryThirdPartyBlockchainTrustAnchorRequest request);

    PTCTrustRoot queryPTCTrustRoot(ObjectIdentity ptcOid);

    void addPTCTrustRoot(PTCTrustRoot ptcTrustRoot);
}
