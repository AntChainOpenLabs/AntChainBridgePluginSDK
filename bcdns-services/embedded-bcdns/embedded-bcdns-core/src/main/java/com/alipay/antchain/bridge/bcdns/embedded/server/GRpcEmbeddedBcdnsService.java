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

package com.alipay.antchain.bridge.bcdns.embedded.server;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Date;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.bcdns.embedded.grpc.*;
import com.alipay.antchain.bridge.bcdns.embedded.server.config.ServerConfig;
import com.alipay.antchain.bridge.bcdns.embedded.types.enums.ApplicationStateEnum;
import com.alipay.antchain.bridge.bcdns.embedded.types.models.CertApplicationResult;
import com.alipay.antchain.bridge.bcdns.types.base.DomainRouter;
import com.alipay.antchain.bridge.bcdns.types.exception.AntChainBridgeBCDNSException;
import com.alipay.antchain.bridge.bcdns.types.exception.BCDNSErrorCodeEnum;
import com.alipay.antchain.bridge.commons.bcdns.*;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.utils.crypto.HashAlgoEnum;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GRpcEmbeddedBcdnsService extends EmbeddedBcdnsServiceGrpc.EmbeddedBcdnsServiceImplBase {

    private final IBcdnsState bcdnsState;

    private final PrivateKey bcdnsRootKey;

    private final AbstractCrossChainCertificate bcdnsRootCert;

    private final SignAlgoEnum signAlgo;

    private final HashAlgoEnum signCertHashAlgo;

    @SneakyThrows
    public GRpcEmbeddedBcdnsService(IBcdnsState bcdnsState, byte[] config) {
        ServerConfig serverConfig = ServerConfig.decode(config);
        this.bcdnsState = bcdnsState;
        this.signCertHashAlgo = serverConfig.getSignCertHashAlgo();
        this.signAlgo = serverConfig.getSignAlgo();
        this.bcdnsRootKey = serverConfig.getSignAlgo().getSigner().readPemPrivateKey(
                Files.readAllBytes(Paths.get(serverConfig.getPrivateKeyFile()))
        );
        this.bcdnsRootCert = CrossChainCertificateUtil.readCrossChainCertificateFromPem(
                Files.readAllBytes(Paths.get(serverConfig.getBcdnsRootCertFile()))
        );
    }

    public GRpcEmbeddedBcdnsService(
            IBcdnsState bcdnsState,
            HashAlgoEnum signCertHashAlgo,
            SignAlgoEnum bcdnsSignAlgo,
            PrivateKey bcdnsRootKey,
            AbstractCrossChainCertificate bcdnsRootCert
    ) {
        this.bcdnsState = bcdnsState;
        this.signCertHashAlgo = signCertHashAlgo;
        this.signAlgo = bcdnsSignAlgo;
        this.bcdnsRootKey = bcdnsRootKey;
        this.bcdnsRootCert = bcdnsRootCert;
    }

    @Override
    public void heartbeat(HeartbeatRequest request, StreamObserver<Response> responseObserver) {
        super.heartbeat(request, responseObserver);
    }

    @Override
    public void queryBCDNSTrustRootCertificate(Empty request, StreamObserver<Response> responseObserver) {
        log.info("query BCDNS root certificate");
        try {
            responseObserver.onNext(
                    Response.newBuilder().setQueryBCDNSTrustRootCertificateResp(
                            QueryBCDNSTrustRootCertificateResp.newBuilder()
                                    .setBcdnsTrustRootCertificate(ByteString.copyFrom(bcdnsRootCert.encode()))
                    ).build()
            );
            responseObserver.onCompleted();
        } catch (Throwable t) {
            log.error("query BCDNS root certificate failed: ", t);
            responseObserver.onNext(Response.newBuilder().setErrorMsg(t.getMessage()).setCode(-1).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    @SneakyThrows
    public void applyRelayerCertificate(ApplyRelayerCertificateReq request, StreamObserver<Response> responseObserver) {
        log.info("apply relayer certificate");
        try {
            AbstractCrossChainCertificate csr = CrossChainCertificateFactory.createCrossChainCertificate(
                    request.getCertSigningRequest().toByteArray()
            );

            String receipt = HexUtil.encodeHexStr(RandomUtil.randomBytes(32));
            AbstractCrossChainCertificate certificate = signCrossChainCert(
                    csr,
                    RelayerCredentialSubject.decode(csr.getCredentialSubject()),
                    receipt
            );

            bcdnsState.saveCrossChainCert(certificate);
            bcdnsState.saveApplication(csr, receipt, ApplicationStateEnum.ACCEPTED);
            responseObserver.onNext(
                    Response.newBuilder().setApplyRelayerCertificateResp(
                            ApplyRelayerCertificateResp.newBuilder().setApplyReceipt(receipt).build()
                    ).build()
            );
            responseObserver.onCompleted();
            log.info("apply relayer certificate success with receipt {}", receipt);
        } catch (Throwable t) {
            log.error("apply relayer certificate failed: ", t);
            responseObserver.onNext(Response.newBuilder().setErrorMsg(t.getMessage()).setCode(-1).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void queryRelayerCertificateApplicationResult(QueryRelayerCertApplicationResultReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("query relayer application result with receipt {}", request.getApplyReceipt());
            queryApplicationResult(request.getApplyReceipt(), responseObserver);
        } catch (Throwable t) {
            log.error("query relayer application result with receipt {} failed: ", request.getApplyReceipt(), t);
            responseObserver.onNext(Response.newBuilder().setErrorMsg(t.getMessage()).setCode(-1).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void applyPTCCertificate(ApplyPTCCertificateReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("apply ptc certificate");
            AbstractCrossChainCertificate csr = CrossChainCertificateFactory.createCrossChainCertificate(
                    request.getCertSigningRequest().toByteArray()
            );

            String receipt = HexUtil.encodeHexStr(RandomUtil.randomBytes(32));
            AbstractCrossChainCertificate certificate = signCrossChainCert(
                    csr,
                    PTCCredentialSubject.decode(csr.getCredentialSubject()),
                    receipt
            );

            bcdnsState.saveCrossChainCert(certificate);
            bcdnsState.saveApplication(csr, receipt, ApplicationStateEnum.ACCEPTED);
            responseObserver.onNext(
                    Response.newBuilder().setApplyPTCCertificateResp(
                            ApplyPTCCertificateResp.newBuilder().setApplyReceipt(receipt).build()
                    ).build()
            );
            responseObserver.onCompleted();
            log.info("apply ptc certificate success with receipt {}", receipt);
        } catch (Throwable t) {
            log.error("apply ptc certificate failed: ", t);
            responseObserver.onNext(Response.newBuilder().setErrorMsg(t.getMessage()).setCode(-1).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void queryPTCCertificateApplicationResult(QueryPTCCertApplicationResultReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("query ptc application result with receipt {}", request.getApplyReceipt());
            queryApplicationResult(request.getApplyReceipt(), responseObserver);
        } catch (Throwable t) {
            log.error("query ptc application result with receipt {} failed: ", request.getApplyReceipt(), t);
            responseObserver.onNext(Response.newBuilder().setErrorMsg(t.getMessage()).setCode(-1).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void applyDomainNameCertificate(ApplyDomainNameCertificateReq request, StreamObserver<Response> responseObserver) {
        CrossChainDomain domain = new CrossChainDomain("unknown");
        try {
            log.info("apply domain name certificate");
            AbstractCrossChainCertificate csr = CrossChainCertificateFactory.createCrossChainCertificate(
                    request.getCertSigningRequest().toByteArray()
            );
            domain = CrossChainCertificateUtil.getCrossChainDomain(csr);

            if (!bcdnsState.ifDomainExist(domain.getDomain())) {
                String receipt = HexUtil.encodeHexStr(RandomUtil.randomBytes(32));
                AbstractCrossChainCertificate certificate = signCrossChainCert(
                        csr,
                        DomainNameCredentialSubject.decode(csr.getCredentialSubject()),
                        receipt
                );

                bcdnsState.saveCrossChainCert(certificate);
                bcdnsState.saveApplication(csr, receipt, ApplicationStateEnum.ACCEPTED);
                responseObserver.onNext(
                        Response.newBuilder().setApplyDomainNameCertificateResp(
                                ApplyDomainNameCertificateResp.newBuilder().setApplyReceipt(receipt).build()
                        ).build()
                );
                responseObserver.onCompleted();

                log.info("apply domain cert {} success with receipt {}", domain.getDomain(), receipt);
            } else {
                throw new AntChainBridgeBCDNSException(
                        BCDNSErrorCodeEnum.BCDNS_APPLY_DOMAIN_CERT_FAILED,
                        StrUtil.format("domain {} already exist", domain.getDomain())
                );
            }

        } catch (Throwable t) {
            log.error("apply domain cert {} failed: ", domain.getDomain(), t);
            responseObserver.onNext(Response.newBuilder().setErrorMsg(t.getMessage()).setCode(-1).build());
            responseObserver.onCompleted();
        }
    }

    @SneakyThrows
    private AbstractCrossChainCertificate signCrossChainCert(AbstractCrossChainCertificate csr, ICredentialSubject credentialSubject, String receipt) {
        AbstractCrossChainCertificate certificate = CrossChainCertificateFactory.createCrossChainCertificate(
                csr.getVersion(),
                receipt,
                bcdnsRootCert.getIssuer(),
                DateUtil.currentSeconds(),
                DateUtil.offsetDay(new Date(), 365).getTime() / 1000,
                credentialSubject
        );

        AbstractCrossChainCertificate.IssueProof proof = new AbstractCrossChainCertificate.IssueProof();

        byte[] csrEncoded = csr.getEncodedToSign();
        proof.setHashAlgo(signCertHashAlgo.getName());
        proof.setCertHash(signCertHashAlgo.hash(csrEncoded));
        proof.setSigAlgo(signAlgo.getName());
        proof.setRawProof(signAlgo.getSigner().sign(bcdnsRootKey, csrEncoded));

        certificate.setProof(proof);

        return certificate;
    }

    @Override
    public void queryDomainNameCertificateApplicationResult(QueryDomainNameCertApplicationResultReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("query domain application result for receipt {}", request.getApplyReceipt());
            queryApplicationResult(request.getApplyReceipt(), responseObserver);
        } catch (Throwable t) {
            log.error("query domain application result for receipt {} failed: ", request.getApplyReceipt(), t);
            responseObserver.onNext(Response.newBuilder().setErrorMsg(t.getMessage()).setCode(-1).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void queryRelayerCertificate(QueryRelayerCertificateReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("query relayer cert for cert id {}", request.getRelayerCertId());
            AbstractCrossChainCertificate certificate = bcdnsState.queryCrossChainCert(
                    request.getRelayerCertId(), CrossChainCertificateTypeEnum.RELAYER_CERTIFICATE
            );
            boolean exist = ObjectUtil.isNotNull(certificate);
            responseObserver.onNext(
                    Response.newBuilder().setQueryRelayerCertificateResp(
                            QueryRelayerCertificateResp.newBuilder()
                                    .setCertificate(exist ? ByteString.copyFrom(certificate.encode()) : ByteString.empty())
                                    .setExist(exist)
                    ).build()
            );
            responseObserver.onCompleted();
        } catch (Throwable t) {
            log.error("query relayer cert for cert id {}: ", request.getRelayerCertId(), t);
            responseObserver.onNext(Response.newBuilder().setErrorMsg(t.getMessage()).setCode(-1).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void queryPTCCertificate(QueryPTCCertificateReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("query ptc cert for cert id {}", request.getPtcCertId());
            AbstractCrossChainCertificate certificate = bcdnsState.queryCrossChainCert(
                    request.getPtcCertId(), CrossChainCertificateTypeEnum.PROOF_TRANSFORMATION_COMPONENT_CERTIFICATE
            );
            boolean exist = ObjectUtil.isNotNull(certificate);
            responseObserver.onNext(
                    Response.newBuilder().setQueryPTCCertificateResp(
                            QueryPTCCertificateResp.newBuilder()
                                    .setCertificate(exist ? ByteString.copyFrom(certificate.encode()) : ByteString.empty())
                                    .setExist(exist)
                    ).build()
            );
            responseObserver.onCompleted();
        } catch (Throwable t) {
            log.error("query ptc cert for cert id {} failed: ", request.getPtcCertId(), t);
            responseObserver.onNext(Response.newBuilder().setErrorMsg(t.getMessage()).setCode(-1).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void queryDomainNameCertificate(QueryDomainNameCertificateReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("query domain cert for domain {}", request.getDomain());
            AbstractCrossChainCertificate certificate = bcdnsState.queryDomainCert(request.getDomain());
            boolean exist = ObjectUtil.isNotNull(certificate);
            responseObserver.onNext(
                    Response.newBuilder().setQueryDomainNameCertificateResp(
                            QueryDomainNameCertificateResp.newBuilder()
                                    .setCertificate(exist ? ByteString.copyFrom(certificate.encode()) : ByteString.empty())
                                    .setExist(exist)
                    ).build()
            );
            responseObserver.onCompleted();
        } catch (Throwable t) {
            log.error("query domain cert for domain {} failed: ", request.getDomain(), t);
            responseObserver.onNext(Response.newBuilder().setErrorMsg(t.getMessage()).setCode(-1).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void registerDomainRouter(RegisterDomainRouterReq request, StreamObserver<Response> responseObserver) {
        CrossChainDomain domain = new CrossChainDomain("unknown");
        try {
            log.info("register domain router...");
            DomainRouter router = DomainRouter.decode(request.getDomainRouter().toByteArray());
            domain = router.getDestDomain();
            bcdnsState.registerDomainRouter(router);
            log.info("register domain router success for dest domain {}", domain.getDomain());
        } catch (Throwable t) {
            log.error("register domain router for dest domain {} failed: ", domain.getDomain(), t);
            responseObserver.onNext(Response.newBuilder().setErrorMsg(t.getMessage()).setCode(-1).build());
            responseObserver.onCompleted();
            return;
        }
        responseObserver.onNext(Response.newBuilder().setCode(0).build());
        responseObserver.onCompleted();
    }

    @Override
    public void registerThirdPartyBlockchainTrustAnchor(RegisterThirdPartyBlockchainTrustAnchorReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("register TpBTA...");
            bcdnsState.registerTPBTA(request.getTpbta().toByteArray());
        } catch (Throwable t) {
            log.error("register TpBTA failed: ", t);
            responseObserver.onNext(Response.newBuilder().setErrorMsg(t.getMessage()).setCode(-1).build());
            responseObserver.onCompleted();
            return;
        }
        responseObserver.onNext(Response.newBuilder().setCode(0).build());
        responseObserver.onCompleted();
    }

    @Override
    public void queryDomainRouter(QueryDomainRouterReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("query domain router for dest domain {}", request.getDestDomain());
            DomainRouter dr = bcdnsState.queryDomainRouter(request.getDestDomain());
            if (ObjectUtil.isNull(dr)) {
                responseObserver.onNext(
                        Response.newBuilder().setQueryDomainRouterResp(
                                QueryDomainRouterResp.newBuilder().setDomainRouter(ByteString.empty())
                        ).build()
                );
                responseObserver.onCompleted();
                return;
            }
            responseObserver.onNext(
                    Response.newBuilder().setQueryDomainRouterResp(
                            QueryDomainRouterResp.newBuilder().setDomainRouter(ByteString.copyFrom(dr.encode()))
                    ).build()
            );
            responseObserver.onCompleted();
        } catch (Throwable t) {
            log.error("query domain router for domain {} failed: ", request.getDestDomain(), t);
            responseObserver.onNext(Response.newBuilder().setErrorMsg(t.getMessage()).setCode(-1).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void queryThirdPartyBlockchainTrustAnchor(QueryThirdPartyBlockchainTrustAnchorReq request, StreamObserver<Response> responseObserver) {
        try {
            log.info("query TpBta for domain {}", request.getDomain());
            byte[] raw = bcdnsState.queryTpBta(request.getDomain());
            responseObserver.onNext(
                    Response.newBuilder().setQueryThirdPartyBlockchainTrustAnchorResp(
                            QueryThirdPartyBlockchainTrustAnchorResp.newBuilder().setTpbta(
                                    ObjectUtil.isEmpty(raw) ? ByteString.empty() : ByteString.copyFrom(raw)
                            )
                    ).build()
            );
            responseObserver.onCompleted();
        } catch (Throwable t) {
            log.error("query TpBta failed: ", t);
            responseObserver.onNext(Response.newBuilder().setErrorMsg(t.getMessage()).setCode(-1).build());
            responseObserver.onCompleted();
        }
    }

    private void queryApplicationResult(String receipt, StreamObserver<Response> responseObserver) {
        CertApplicationResult applicationResult = bcdnsState.queryApplication(receipt);
        if (ObjectUtil.isNull(applicationResult)) {
            throw new AntChainBridgeBCDNSException(BCDNSErrorCodeEnum.BCDNS_QUERY_APPLICATION_RESULT_FAILED, "application not exist");
        }

        responseObserver.onNext(
                Response.newBuilder().setApplicationResult(
                        ApplicationResult.newBuilder()
                                .setCertificate(
                                        applicationResult.getState() == ApplicationStateEnum.ACCEPTED ?
                                                ByteString.copyFrom(applicationResult.getCertificate().encode()) : ByteString.empty()
                                ).setIsFinalResult(applicationResult.isFinal())
                ).build()
        );
        responseObserver.onCompleted();
    }
}