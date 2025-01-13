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

import java.util.concurrent.locks.Lock;

import com.alipay.antchain.bridge.bcdns.factory.BlockChainDomainNameServiceFactory;
import com.alipay.antchain.bridge.bcdns.service.IBlockChainDomainNameService;
import com.alipay.antchain.bridge.bcdns.types.base.DomainRouter;
import com.alipay.antchain.bridge.bcdns.types.exception.AntChainBridgeBCDNSException;
import com.alipay.antchain.bridge.bcdns.types.req.*;
import com.alipay.antchain.bridge.bcdns.types.resp.*;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyBlockchainTrustAnchor;
import com.alipay.antchain.bridge.relayer.commons.model.BCDNSServiceDO;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BCDNSWrapper implements IBlockChainDomainNameService {

    private IBlockChainDomainNameService bcdns;

    private Lock callingLock;

    public BCDNSWrapper(BCDNSServiceDO bcdnsServiceDO, Lock callingLock) {
        this.bcdns = BlockChainDomainNameServiceFactory.create(
                bcdnsServiceDO.getType(),
                bcdnsServiceDO.getProperties()
        );
        this.callingLock = callingLock;
    }

    @Override
    public QueryBCDNSTrustRootCertificateResponse queryBCDNSTrustRootCertificate() {
        callingLock.lock();
        try {
            return bcdns.queryBCDNSTrustRootCertificate();
        } finally {
            callingLock.unlock();
        }
    }

    @Override
    public ApplyRelayerCertificateResponse applyRelayerCertificate(AbstractCrossChainCertificate abstractCrossChainCertificate) {
        callingLock.lock();
        try {
            return bcdns.applyRelayerCertificate(abstractCrossChainCertificate);
        } finally {
            callingLock.unlock();
        }
    }

    @Override
    public ApplicationResult queryRelayerCertificateApplicationResult(String relayerCertId) {
        callingLock.lock();
        try {
            return bcdns.queryRelayerCertificateApplicationResult(relayerCertId);
        } finally {
            callingLock.unlock();
        }
    }

    @Override
    public ApplyPTCCertificateResponse applyPTCCertificate(AbstractCrossChainCertificate abstractCrossChainCertificate) {
        callingLock.lock();
        try {
            return bcdns.applyPTCCertificate(abstractCrossChainCertificate);
        } finally {
            callingLock.unlock();
        }
    }

    @Override
    public ApplicationResult queryPTCCertificateApplicationResult(String s) {
        callingLock.lock();
        try {
            return bcdns.queryPTCCertificateApplicationResult(s);
        } finally {
            callingLock.unlock();
        }
    }

    @Override
    public ApplyDomainNameCertificateResponse applyDomainNameCertificate(AbstractCrossChainCertificate abstractCrossChainCertificate) {
        callingLock.lock();
        try {
            return bcdns.applyDomainNameCertificate(abstractCrossChainCertificate);
        } finally {
            callingLock.unlock();
        }
    }

    @Override
    public ApplicationResult queryDomainNameCertificateApplicationResult(String s) {
        callingLock.lock();
        try {
            return bcdns.queryDomainNameCertificateApplicationResult(s);
        } finally {
            callingLock.unlock();
        }
    }

    @Override
    public QueryRelayerCertificateResponse queryRelayerCertificate(QueryRelayerCertificateRequest queryRelayerCertificateRequest) {
        callingLock.lock();
        try {
            return bcdns.queryRelayerCertificate(queryRelayerCertificateRequest);
        } finally {
            callingLock.unlock();
        }
    }

    @Override
    public QueryPTCCertificateResponse queryPTCCertificate(QueryPTCCertificateRequest queryPTCCertificateRequest) {
        callingLock.lock();
        try {
            return bcdns.queryPTCCertificate(queryPTCCertificateRequest);
        } finally {
            callingLock.unlock();
        }
    }

    @Override
    public QueryDomainNameCertificateResponse queryDomainNameCertificate(QueryDomainNameCertificateRequest queryDomainNameCertificateRequest) {
        callingLock.lock();
        try {
            return bcdns.queryDomainNameCertificate(queryDomainNameCertificateRequest);
        } finally {
            callingLock.unlock();
        }
    }

    @Override
    public void registerDomainRouter(RegisterDomainRouterRequest registerDomainRouterRequest) throws AntChainBridgeBCDNSException {
        callingLock.lock();
        try {
            bcdns.registerDomainRouter(registerDomainRouterRequest);
        } finally {
            callingLock.unlock();
        }
    }

    @Override
    public void registerThirdPartyBlockchainTrustAnchor(RegisterThirdPartyBlockchainTrustAnchorRequest registerThirdPartyBlockchainTrustAnchorRequest) throws AntChainBridgeBCDNSException {
        callingLock.lock();
        try {
            bcdns.registerThirdPartyBlockchainTrustAnchor(registerThirdPartyBlockchainTrustAnchorRequest);
        } finally {
            callingLock.unlock();
        }
    }

    @Override
    public DomainRouter queryDomainRouter(QueryDomainRouterRequest queryDomainRouterRequest) {
        callingLock.lock();
        try {
            return bcdns.queryDomainRouter(queryDomainRouterRequest);
        } finally {
            callingLock.unlock();
        }
    }

    @Override
    public ThirdPartyBlockchainTrustAnchor queryThirdPartyBlockchainTrustAnchor(QueryThirdPartyBlockchainTrustAnchorRequest queryThirdPartyBlockchainTrustAnchorRequest) {
        callingLock.lock();
        try {
            return bcdns.queryThirdPartyBlockchainTrustAnchor(queryThirdPartyBlockchainTrustAnchorRequest);
        } finally {
            callingLock.unlock();
        }
    }

    @Override
    public PTCTrustRoot queryPTCTrustRoot(ObjectIdentity ptcOwnerOid) {
        callingLock.lock();
        try {
            return bcdns.queryPTCTrustRoot(ptcOwnerOid);
        } finally {
            callingLock.unlock();
        }
    }

    @Override
    public void addPTCTrustRoot(PTCTrustRoot ptcTrustRoot) {
        callingLock.lock();
        try {
            bcdns.addPTCTrustRoot(ptcTrustRoot);
        } finally {
            callingLock.unlock();
        }
    }
}
