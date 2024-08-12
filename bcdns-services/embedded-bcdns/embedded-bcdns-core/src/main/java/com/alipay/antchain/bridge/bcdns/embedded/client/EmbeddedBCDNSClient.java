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

package com.alipay.antchain.bridge.bcdns.embedded.client;

import java.io.ByteArrayInputStream;

import cn.hutool.core.net.DefaultTrustManager;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.bcdns.embedded.client.conf.EmbeddedBCDNSConfig;
import com.alipay.antchain.bridge.bcdns.embedded.grpc.*;
import com.alipay.antchain.bridge.bcdns.embedded.types.endpoint.EndpointAddress;
import com.alipay.antchain.bridge.bcdns.embedded.types.endpoint.ProtocolHeaderEnum;
import com.alipay.antchain.bridge.bcdns.service.IBlockChainDomainNameService;
import com.alipay.antchain.bridge.bcdns.types.base.DomainRouter;
import com.alipay.antchain.bridge.bcdns.types.exception.AntChainBridgeBCDNSException;
import com.alipay.antchain.bridge.bcdns.types.exception.BCDNSErrorCodeEnum;
import com.alipay.antchain.bridge.bcdns.types.req.*;
import com.alipay.antchain.bridge.bcdns.types.resp.ApplicationResult;
import com.alipay.antchain.bridge.bcdns.types.resp.*;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.google.protobuf.ByteString;
import io.grpc.TlsChannelCredentials;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EmbeddedBCDNSClient implements IBlockChainDomainNameService {

    public static EmbeddedBCDNSClient generateFrom(byte[] confJson) {
        return new EmbeddedBCDNSClient(confJson);
    }

    private EmbeddedBcdnsServiceGrpc.EmbeddedBcdnsServiceBlockingStub stub;

    public EmbeddedBCDNSClient(byte[] confJson) {
        EmbeddedBCDNSConfig config = JSON.parseObject(confJson, EmbeddedBCDNSConfig.class);
        createStub(config.getServerAddress(), config.getTlsClientKey(), config.getTlsClientCert(), config.getTlsTrustCertChain());
    }

    @SneakyThrows
    private void createStub(
            EndpointAddress serverAddress,
            String tlsClientPemPkcs8Key,
            String tlsClientPemCert,
            String trustCertChain
    ) {
        NettyChannelBuilder channelBuilder;
        if (serverAddress.getProtocolHeader() == ProtocolHeaderEnum.GRPCS) {
            TlsChannelCredentials.Builder tlsBuilder = TlsChannelCredentials.newBuilder();
            if (StrUtil.isAllNotEmpty(tlsClientPemPkcs8Key, tlsClientPemCert)) {
                tlsBuilder.keyManager(
                        new ByteArrayInputStream(tlsClientPemCert.getBytes()),
                        new ByteArrayInputStream(tlsClientPemPkcs8Key.getBytes())
                );
            }
            if (StrUtil.isNotEmpty(trustCertChain)) {
                tlsBuilder.trustManager(new ByteArrayInputStream(trustCertChain.getBytes()));
            } else {
                tlsBuilder.trustManager(DefaultTrustManager.INSTANCE);
            }
            channelBuilder = NettyChannelBuilder.forAddress(
                    serverAddress.getHost(),
                    serverAddress.getPort(),
                    tlsBuilder.build()
            );
        } else if (serverAddress.getProtocolHeader() == ProtocolHeaderEnum.GRPC) {
            channelBuilder = NettyChannelBuilder.forAddress(
                    serverAddress.getHost(),
                    serverAddress.getPort()
            ).usePlaintext();
        } else {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_CLIENT_INIT_FAILED,
                    "unsupported protocol: " + serverAddress.getProtocolHeader().getHeader()
            );
        }

        stub = EmbeddedBcdnsServiceGrpc.newBlockingStub(channelBuilder.build());
    }

    @Override
    public QueryBCDNSTrustRootCertificateResponse queryBCDNSTrustRootCertificate() {
        Response response = stub.queryBCDNSTrustRootCertificate(Empty.getDefaultInstance())
                .toBuilder()
                .build();
        if (response.getCode() != 0) {
            throw new AntChainBridgeBCDNSException(BCDNSErrorCodeEnum.BCDNS_QUERY_ROOT_CERT_FAILED, response.getErrorMsg());
        }
        if (!response.hasQueryBCDNSTrustRootCertificateResp()) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_QUERY_ROOT_CERT_FAILED,
                    "response has no queryBCDNSTrustRootCertificateResp"
            );
        }
        return new QueryBCDNSTrustRootCertificateResponse(
                CrossChainCertificateFactory.createCrossChainCertificate(
                        response.getQueryBCDNSTrustRootCertificateResp().getBcdnsTrustRootCertificate().toByteArray()
                )
        );
    }

    @Override
    public ApplyRelayerCertificateResponse applyRelayerCertificate(AbstractCrossChainCertificate certSigningRequest) {
        Response response = stub.applyRelayerCertificate(
                ApplyRelayerCertificateReq.newBuilder()
                        .setCertSigningRequest(ByteString.copyFrom(certSigningRequest.encode()))
                        .build()
        );
        if (response.getCode() != 0) {
            throw new AntChainBridgeBCDNSException(BCDNSErrorCodeEnum.BCDNS_APPLY_RELAYER_CERT_FAILED, response.getErrorMsg());
        }
        if (!response.hasApplyRelayerCertificateResp()) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_APPLY_RELAYER_CERT_FAILED,
                    "response has no applyRelayerCertificateResp"
            );
        }
        return new ApplyRelayerCertificateResponse(
                response.getApplyRelayerCertificateResp().getApplyReceipt()
        );
    }

    @Override
    public ApplicationResult queryRelayerCertificateApplicationResult(String applyReceipt) {
        Response response = stub.queryRelayerCertificateApplicationResult(
                QueryRelayerCertApplicationResultReq.newBuilder()
                        .setApplyReceipt(applyReceipt)
                        .build()
        );
        return assembleApplicationResult(response);
    }

    @Override
    public ApplyPTCCertificateResponse applyPTCCertificate(AbstractCrossChainCertificate certSigningRequest) {
        Response response = stub.applyPTCCertificate(
                ApplyPTCCertificateReq.newBuilder()
                        .setCertSigningRequest(ByteString.copyFrom(certSigningRequest.encode()))
                        .build()
        );
        if (response.getCode() != 0) {
            throw new AntChainBridgeBCDNSException(BCDNSErrorCodeEnum.BCDNS_APPLY_PTC_CERT_FAILED, response.getErrorMsg());
        }
        if (!response.hasApplyPTCCertificateResp()) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_APPLY_PTC_CERT_FAILED,
                    "response has no ApplyPTCCertificateResp"
            );
        }
        return new ApplyPTCCertificateResponse(
                response.getApplyPTCCertificateResp().getApplyReceipt()
        );
    }

    @Override
    public ApplicationResult queryPTCCertificateApplicationResult(String applyReceipt) {
        Response response = stub.queryPTCCertificateApplicationResult(
                QueryPTCCertApplicationResultReq.newBuilder()
                        .setApplyReceipt(applyReceipt)
                        .build()
        );
        return assembleApplicationResult(response);
    }

    @Override
    public ApplyDomainNameCertificateResponse applyDomainNameCertificate(AbstractCrossChainCertificate certSigningRequest) {
        Response response = stub.applyDomainNameCertificate(
                ApplyDomainNameCertificateReq.newBuilder()
                        .setCertSigningRequest(ByteString.copyFrom(certSigningRequest.encode()))
                        .build()
        );
        if (response.getCode() != 0) {
            throw new AntChainBridgeBCDNSException(BCDNSErrorCodeEnum.BCDNS_APPLY_DOMAIN_CERT_FAILED, response.getErrorMsg());
        }
        if (!response.hasApplyDomainNameCertificateResp()) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_APPLY_DOMAIN_CERT_FAILED,
                    "response has no ApplyDomainNameCertificateResp"
            );
        }
        return new ApplyDomainNameCertificateResponse(
                response.getApplyDomainNameCertificateResp().getApplyReceipt()
        );
    }

    @Override
    public ApplicationResult queryDomainNameCertificateApplicationResult(String applyReceipt) {
        Response response = stub.queryDomainNameCertificateApplicationResult(
                QueryDomainNameCertApplicationResultReq.newBuilder()
                        .setApplyReceipt(applyReceipt)
                        .build()
        );
        return assembleApplicationResult(response);
    }

    @Override
    public QueryRelayerCertificateResponse queryRelayerCertificate(QueryRelayerCertificateRequest request) {
        Response response = stub.queryRelayerCertificate(
                QueryRelayerCertificateReq.newBuilder()
                        .setApplicant(ByteString.copyFrom(request.getApplicant().encode()))
                        .setName(request.getName())
                        .setRelayerCertId(request.getRelayerCertId())
                        .build()
        );
        if (response.getCode() != 0) {
            throw new AntChainBridgeBCDNSException(BCDNSErrorCodeEnum.BCDNS_QUERY_RELAYER_CERT_FAILED, response.getErrorMsg());
        }
        if (!response.hasQueryRelayerCertificateResp()) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_QUERY_RELAYER_CERT_FAILED,
                    "response has no QueryRelayerCertificateResp"
            );
        }
        return new QueryRelayerCertificateResponse(
                response.getQueryRelayerCertificateResp().getExist(),
                response.getQueryRelayerCertificateResp().getExist() ? CrossChainCertificateFactory.createCrossChainCertificate(
                        response.getQueryRelayerCertificateResp().getCertificate().toByteArray()
                ) : null
        );
    }

    @Override
    public QueryPTCCertificateResponse queryPTCCertificate(QueryPTCCertificateRequest request) {
        Response response = stub.queryPTCCertificate(
                QueryPTCCertificateReq.newBuilder()
                        .setApplicant(ByteString.copyFrom(request.getApplicant().encode()))
                        .setName(request.getName())
                        .setPtcCertId(request.getPtcCertId())
                        .build()
        );
        if (response.getCode() != 0) {
            throw new AntChainBridgeBCDNSException(BCDNSErrorCodeEnum.BCDNS_QUERY_PTC_CERT_FAILED, response.getErrorMsg());
        }
        if (!response.hasQueryPTCCertificateResp()) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_QUERY_PTC_CERT_FAILED,
                    "response has no QueryPTCCertificateResp"
            );
        }
        return new QueryPTCCertificateResponse(
                response.getQueryPTCCertificateResp().getExist(),
                response.getQueryPTCCertificateResp().getExist() ? CrossChainCertificateFactory.createCrossChainCertificate(
                        response.getQueryPTCCertificateResp().getCertificate().toByteArray()
                ) : null
        );
    }

    @Override
    public QueryDomainNameCertificateResponse queryDomainNameCertificate(QueryDomainNameCertificateRequest request) {
        Response response = stub.queryDomainNameCertificate(
                QueryDomainNameCertificateReq.newBuilder()
                        .setDomain(request.getDomain().getDomain())
                        .build()
        );
        if (response.getCode() != 0) {
            throw new AntChainBridgeBCDNSException(BCDNSErrorCodeEnum.BCDNS_QUERY_DOMAIN_CERT_FAILED, response.getErrorMsg());
        }
        if (!response.hasQueryDomainNameCertificateResp()) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_QUERY_DOMAIN_CERT_FAILED,
                    "response has no QueryDomainNameCertificateResp"
            );
        }
        return new QueryDomainNameCertificateResponse(
                response.getQueryDomainNameCertificateResp().getExist(),
                response.getQueryDomainNameCertificateResp().getExist() ? CrossChainCertificateFactory.createCrossChainCertificate(
                        response.getQueryDomainNameCertificateResp().getCertificate().toByteArray()
                ) : null
        );
    }

    @Override
    public void registerDomainRouter(RegisterDomainRouterRequest request) throws AntChainBridgeBCDNSException {
        Response response = stub.registerDomainRouter(
                RegisterDomainRouterReq.newBuilder()
                        .setDomainCert(ByteString.copyFrom(request.getDomainCert().encode()))
                        .setDomainRouter(ByteString.copyFrom(request.getRouter().encode()))
                        .build()
        );
        if (response.getCode() != 0) {
            throw new AntChainBridgeBCDNSException(BCDNSErrorCodeEnum.BCDNS_REGISTER_DOMAIN_ROUTER_FAILED, response.getErrorMsg());
        }
    }

    @Override
    public void registerThirdPartyBlockchainTrustAnchor(RegisterThirdPartyBlockchainTrustAnchorRequest request) throws AntChainBridgeBCDNSException {
        Response response = stub.registerThirdPartyBlockchainTrustAnchor(
                RegisterThirdPartyBlockchainTrustAnchorReq.newBuilder()
                        .setDomain(request.getDomain().getDomain())
                        .setPtcId(ByteString.copyFrom(request.getPtcId().encode()))
                        .setTpbta(ByteString.copyFrom(request.getTpbta()))
                        .build()
        );
        if (response.getCode() != 0) {
            throw new AntChainBridgeBCDNSException(BCDNSErrorCodeEnum.BCDNS_REGISTER_TPBTA_FAILED, response.getErrorMsg());
        }
    }

    @Override
    public DomainRouter queryDomainRouter(QueryDomainRouterRequest request) {
        Response response = stub.queryDomainRouter(
                QueryDomainRouterReq.newBuilder()
                        .setDestDomain(request.getDestDomain().getDomain())
                        .build()
        );
        if (response.getCode() != 0) {
            throw new AntChainBridgeBCDNSException(BCDNSErrorCodeEnum.BCDNS_QUERY_DOMAIN_ROUTER_FAILED, response.getErrorMsg());
        }
        if (!response.hasQueryDomainRouterResp()) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_QUERY_DOMAIN_ROUTER_FAILED,
                    "response has no QueryDomainRouterResp"
            );
        }
        if (response.getQueryDomainRouterResp().getDomainRouter().isEmpty()) {
            return null;
        }
        return DomainRouter.decode(response.getQueryDomainRouterResp().getDomainRouter().toByteArray());
    }

    @Override
    public byte[] queryThirdPartyBlockchainTrustAnchor(QueryThirdPartyBlockchainTrustAnchorRequest request) {
        Response response = stub.queryThirdPartyBlockchainTrustAnchor(
                QueryThirdPartyBlockchainTrustAnchorReq.newBuilder()
                        .setDomain(request.getDomain().getDomain())
                        .build()
        );
        if (response.getCode() != 0) {
            throw new AntChainBridgeBCDNSException(BCDNSErrorCodeEnum.BCDNS_QUERY_TPBTA_FAILED, response.getErrorMsg());
        }
        if (!response.hasQueryThirdPartyBlockchainTrustAnchorResp()) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_QUERY_TPBTA_FAILED,
                    "response has no QueryThirdPartyBlockchainTrustAnchorResp"
            );
        }
        return response.getQueryThirdPartyBlockchainTrustAnchorResp().getTpbta().toByteArray();
    }

    private ApplicationResult assembleApplicationResult(Response response) {
        if (response.getCode() != 0) {
            throw new AntChainBridgeBCDNSException(BCDNSErrorCodeEnum.BCDNS_QUERY_APPLICATION_RESULT_FAILED, response.getErrorMsg());
        }
        if (!response.hasApplicationResult()) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_QUERY_APPLICATION_RESULT_FAILED,
                    "response has no ApplicationResult"
            );
        }
        return new ApplicationResult(
                response.getApplicationResult().getIsFinalResult(),
                response.getApplicationResult().getIsFinalResult() ?
                        CrossChainCertificateFactory.createCrossChainCertificate(response.getApplicationResult().getCertificate().toByteArray()) :
                        null
        );
    }
}
