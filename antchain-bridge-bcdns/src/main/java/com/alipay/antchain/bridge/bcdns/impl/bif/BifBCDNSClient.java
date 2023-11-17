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

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.bcdns.impl.bif.resp.QueryStatusRespDto;
import com.alipay.antchain.bridge.bcdns.impl.bif.resp.VcInfoRespDto;
import com.alipay.antchain.bridge.bcdns.service.IBlockChainDomainNameService;
import com.alipay.antchain.bridge.bcdns.types.base.DomainRouter;
import com.alipay.antchain.bridge.bcdns.types.exception.AntChainBridgeBCDNSException;
import com.alipay.antchain.bridge.bcdns.types.req.*;
import com.alipay.antchain.bridge.bcdns.types.resp.*;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import lombok.Getter;

@Getter
public class BifBCDNSClient implements IBlockChainDomainNameService {

    private final BifCertificationServiceClient certificationServiceClient;

    public BifBCDNSClient(
            String url,
            String clientCrossChainCertPem,
            String clientPrivateKeyPem,
            String sigAlgo,
            String authorizedKeyPem,
            String authorizedSigAlgo
    ) {
        certificationServiceClient = new BifCertificationServiceClient(
                url,
                new BifBCDNSClientCredential(
                        clientCrossChainCertPem,
                        clientPrivateKeyPem,
                        sigAlgo,
                        authorizedKeyPem,
                        authorizedSigAlgo
                )
        );
    }

    @Override
    public QueryBCDNSTrustRootCertificateResponse queryBCDNSTrustRootCertificate() {
        return new QueryBCDNSTrustRootCertificateResponse(
                CrossChainCertificateFactory.createCrossChainCertificate(
                        certificationServiceClient.queryRootCert().getBcdnsRootCredential()
                )
        );
    }

    @Override
    public ApplyRelayerCertificateResponse applyRelayerCertificate(AbstractCrossChainCertificate certSigningRequest) {
        return new ApplyRelayerCertificateResponse(
                certificationServiceClient.applyCrossChainCertificate(
                        certSigningRequest
                ).getApplyNo()
        );
    }

    @Override
    public ApplicationResult queryRelayerCertificateApplicationResult(String applyReceipt) {
        return queryApplicationResult(applyReceipt);
    }

    @Override
    public ApplyPTCCertificateResponse applyPTCCertificate(AbstractCrossChainCertificate certSigningRequest) {
        return new ApplyPTCCertificateResponse(
                certificationServiceClient.applyCrossChainCertificate(
                        certSigningRequest
                ).getApplyNo()
        );
    }

    @Override
    public ApplicationResult queryPTCCertificateApplicationResult(String applyReceipt) {
        return queryApplicationResult(applyReceipt);
    }

    @Override
    public ApplyDomainNameCertificateResponse applyDomainNameCertificate(AbstractCrossChainCertificate certSigningRequest) {
        return new ApplyDomainNameCertificateResponse(
                certificationServiceClient.applyCrossChainCertificate(
                        certSigningRequest
                ).getApplyNo()
        );
    }

    @Override
    public ApplicationResult queryDomainNameCertificateApplicationResult(String applyReceipt) {
        return queryApplicationResult(applyReceipt);
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

    private ApplicationResult queryApplicationResult(String applyReceipt) {
        QueryStatusRespDto queryStatusRespDto = certificationServiceClient.queryApplicationStatus(applyReceipt);
        switch (queryStatusRespDto.getStatus()) {
            case 1:
                return new ApplicationResult(false, null);
            case 2:
                VcInfoRespDto vcInfoRespDto = certificationServiceClient.downloadCrossChainCert(queryStatusRespDto.getCredentialId());
                return new ApplicationResult(
                        true,
                        CrossChainCertificateFactory.createCrossChainCertificate(vcInfoRespDto.getCredential())
                );
            case 3:
                return new ApplicationResult(true, null);
            default:
                throw new RuntimeException(
                        StrUtil.format(
                                "unexpected status {} for application receipt {}",
                                queryStatusRespDto.getStatus(), applyReceipt
                        )
                );
        }
    }
}
