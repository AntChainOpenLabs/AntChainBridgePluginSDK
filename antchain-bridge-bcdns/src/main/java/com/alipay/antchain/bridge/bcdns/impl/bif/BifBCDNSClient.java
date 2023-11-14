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

package com.alipay.antchain.bridge.bcdns.impl.bif;

import com.alipay.antchain.bridge.bcdns.service.IBlockChainDomainNameService;
import com.alipay.antchain.bridge.bcdns.types.base.DomainRouter;
import com.alipay.antchain.bridge.bcdns.types.exception.AntChainBridgeBCDNSException;
import com.alipay.antchain.bridge.bcdns.types.req.*;
import com.alipay.antchain.bridge.bcdns.types.resp.*;
import lombok.Getter;

@Getter
public class BifBCDNSClient implements IBlockChainDomainNameService {

    private final BifCertificationServiceClient certificationServiceClient;

    public BifBCDNSClient(
            String url,
            String clientCrossChainCertPem,
            String clientPrivateKeyPem,
            String sigAlgo
    ) {
        certificationServiceClient = new BifCertificationServiceClient(
                url,
                new BifBCDNSClientCredential(clientCrossChainCertPem, clientPrivateKeyPem, sigAlgo)
        );
    }

    @Override
    public QueryBCDNSTrustRootCertificateResponse queryBCDNSTrustRootCertificate() {
        return null;
    }

    @Override
    public ApplyRelayerCertificateResponse applyRelayerCertificate(ApplyRelayerCertificateRequest request) {
        return new ApplyRelayerCertificateResponse(
                certificationServiceClient.applyCrossChainCertificate(
                        request.getCertificate()
                ).getApplyNo()
        );
    }

    @Override
    public RelayerCertificateApplicationReceipt queryRelayerCertificateApplicationReceipt(QueryRelayerCertificateApplicationReceiptRequest request) {
        return null;
    }

    @Override
    public ApplyPTCCertificateResponse applyPTCCertificate(ApplyPTCCertificateRequest request) {
        return null;
    }

    @Override
    public PTCCertificateApplicationReceipt queryPTCCertificateApplicationReceipt(QueryPTCCertificateApplicationReceiptRequest request) {
        return null;
    }

    @Override
    public ApplyPTCCertificateResponse applyDomainNameCertificate(ApplyPTCCertificateRequest request) {
        return null;
    }

    @Override
    public DomainNameCertificateApplicationReceipt queryPTCCertificateApplicationReceipt(QueryDomainNameCertificateApplicationReceiptRequest request) {
        return null;
    }

    @Override
    public QueryRelayerCertificateResponse queryRelayerCertificate(QueryRelayerCertificateRequest request) {
        return null;
    }

    @Override
    public QueryPTCCertificateResponse queryPTCCertificate(QueryPTCCertificateRequest request) {
        return null;
    }

    @Override
    public QueryDomainNameCertificateResponse queryDomainNameCertificate(QueryDomainNameCertificateRequest request) {
        return null;
    }

    @Override
    public void registerDomainRouter(RegisterDomainRouterRequest request) throws AntChainBridgeBCDNSException {

    }

    @Override
    public void registerThirdPartyBlockchainTrustAnchor(RegisterThirdPartyBlockchainTrustAnchorRequest request) throws AntChainBridgeBCDNSException {

    }

    @Override
    public DomainRouter queryDomainRouter(QueryDomainRouterRequest request) {
        return null;
    }

    @Override
    public byte[] queryThirdPartyBlockchainTrustAnchor() {
        return new byte[0];
    }
}
