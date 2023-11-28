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

import java.util.concurrent.atomic.AtomicLong;

import cn.bif.api.BIFSDK;
import cn.bif.model.request.BIFAccountGetNonceRequest;
import cn.bif.model.request.BIFContractCallRequest;
import cn.bif.model.response.BIFAccountGetNonceResponse;
import cn.bif.model.response.BIFContractCallResponse;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.bcdns.impl.bif.conf.BifBCNDSConfig;
import com.alipay.antchain.bridge.bcdns.impl.bif.conf.BifCertificationServiceConfig;
import com.alipay.antchain.bridge.bcdns.impl.bif.conf.BifChainConfig;
import com.alipay.antchain.bridge.bcdns.impl.bif.resp.QueryStatusRespDto;
import com.alipay.antchain.bridge.bcdns.impl.bif.resp.VcInfoRespDto;
import com.alipay.antchain.bridge.bcdns.service.IBlockChainDomainNameService;
import com.alipay.antchain.bridge.bcdns.types.base.DomainRouter;
import com.alipay.antchain.bridge.bcdns.types.exception.AntChainBridgeBCDNSException;
import com.alipay.antchain.bridge.bcdns.types.exception.BCDNSErrorCodeEnum;
import com.alipay.antchain.bridge.bcdns.types.req.*;
import com.alipay.antchain.bridge.bcdns.types.resp.*;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import lombok.Getter;

@Getter
public class BifBCDNSClient implements IBlockChainDomainNameService {

    public static BifBCDNSClient generateFrom(byte[] rawConf) {
        BifBCNDSConfig config = JSON.parseObject(rawConf, BifBCNDSConfig.class);
        return new BifBCDNSClient(
                config.getCertificationServiceConfig(),
                config.getChainConfig()
        );
    }

    private final BifCertificationServiceClient certificationServiceClient;

    private BIFSDK bifsdk;

    private AtomicLong bifNonce;

    private final BifChainConfig bifChainConfig;

    public BifBCDNSClient(
            BifCertificationServiceConfig bifCertificationServiceConfig,
            BifChainConfig bifChainConfig
    ) {
        certificationServiceClient = new BifCertificationServiceClient(
                bifCertificationServiceConfig.getUrl(),
                new BifBCDNSClientCredential(
                        bifCertificationServiceConfig.getClientCrossChainCertPem(),
                        bifCertificationServiceConfig.getClientPrivateKeyPem(),
                        bifCertificationServiceConfig.getSigAlgo(),
                        bifCertificationServiceConfig.getAuthorizedKeyPem(),
                        bifCertificationServiceConfig.getAuthorizedPublicKeyPem(),
                        bifCertificationServiceConfig.getAuthorizedSigAlgo()
                )
        );
        this.bifChainConfig = bifChainConfig;
//        bifsdk = ObjectUtil.isNull(bifChainConfig.getBifChainRpcPort()) ?
//                BIFSDK.getInstance(bifChainConfig.getBifChainRpcUrl()) :
//                BIFSDK.getInstance(bifChainConfig.getBifChainRpcUrl(), bifChainConfig.getBifChainRpcPort());
//        bifNonce = new AtomicLong(queryBifAccNonce());
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
        try {
            BIFContractCallRequest bifContractCallRequest = new BIFContractCallRequest();
            bifContractCallRequest.setContractAddress(bifChainConfig.getRelayerGovernContract());
            // TODO what's the input
            bifContractCallRequest.setInput("");
            BIFContractCallResponse response = bifsdk.getBIFContractService().contractQuery(bifContractCallRequest);
            if (0 != response.getErrorCode()) {
                throw new AntChainBridgeBCDNSException(
                        BCDNSErrorCodeEnum.BCDNS_QUERY_RELAYER_INFO_FAILED,
                        StrUtil.format(
                                "failed to query relayer info from BIF chain ( err_code: {}, err_msg: {} )",
                                response.getErrorCode(), response.getErrorDesc()
                        )
                );
            }
            return new QueryRelayerCertificateResponse(
                    true,
                    CrossChainCertificateFactory.createCrossChainCertificate(
                            (byte[]) response.getResult().getQueryRets().get(0)
                    )
            );
        } catch (AntChainBridgeBCDNSException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_QUERY_RELAYER_INFO_FAILED,
                    StrUtil.format(
                            "failed to query relayer {} : {} info from BIF chain",
                            request.getName(), HexUtil.encodeHexStr(request.getApplicant().encode())
                    ),
                    e
            );
        }
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

    private long queryBifAccNonce() {
        BIFAccountGetNonceRequest request = new BIFAccountGetNonceRequest();
        request.setAddress(bifChainConfig.getBifAddress());
        BIFAccountGetNonceResponse response = bifsdk.getBIFAccountService().getNonce(request);
        if (0 != response.getErrorCode()) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_CLIENT_INIT_FAILED,
                    StrUtil.format(
                            "failed to query nonce for bif account ( err_code: {}, err_msg: {}, acc: {} )",
                            response.getErrorCode(), response.getErrorDesc(), bifChainConfig.getBifAddress()
                    )
            );
        }
        return response.getResult().getNonce();
    }
}
