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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.bif.api.BIFSDK;
import cn.bif.model.request.BIFAccountGetNonceRequest;
import cn.bif.model.request.BIFContractCallRequest;
import cn.bif.model.request.BIFContractInvokeRequest;
import cn.bif.model.response.BIFAccountGetNonceResponse;
import cn.bif.model.response.BIFContractCallResponse;
import cn.bif.model.response.BIFContractInvokeResponse;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.bcdns.impl.bif.conf.BifBCNDSConfig;
import com.alipay.antchain.bridge.bcdns.impl.bif.conf.BifCertificationServiceConfig;
import com.alipay.antchain.bridge.bcdns.impl.bif.conf.BifChainConfig;
import com.alipay.antchain.bridge.bcdns.impl.bif.resp.QueryStatusRespDto;
import com.alipay.antchain.bridge.bcdns.impl.bif.resp.VcInfoRespDto;
import com.alipay.antchain.bridge.bcdns.service.IBlockChainDomainNameService;
import com.alipay.antchain.bridge.bcdns.types.base.DomainRouter;
import com.alipay.antchain.bridge.bcdns.types.base.Relayer;
import com.alipay.antchain.bridge.bcdns.types.exception.AntChainBridgeBCDNSException;
import com.alipay.antchain.bridge.bcdns.types.exception.BCDNSErrorCodeEnum;
import com.alipay.antchain.bridge.bcdns.types.req.*;
import com.alipay.antchain.bridge.bcdns.types.resp.*;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import lombok.Getter;

@Getter
public class BifBCDNSClient implements IBlockChainDomainNameService {

    private static final String DOMAIN_CALL_GET_CERT_BY_NAME_TEMPLATE
            = "{\"function\":\"getCertByName(string)\",\"args\":\"'{}'\",\"return\":\"returns(string)\"}";

    private static final String PTC_CALL_GET_CERT_BY_ID_TEMPLATE
            = "{\"function\":\"getCertById(string)\",\"args\":\"'{}'\",\"return\":\"returns(string)\"}";

    private static final String RELAY_CALL_BINDING_DOMAIN_NAME_WITH_RELAY_TEMPLATE
            = "{\"function\":\"bindingDomainNameWithRelay(string,string,bytes)\",\"args\":\"'{}','{}','{}'\"}";

    private static final String RELAY_CALL_BINDING_DOMAIN_NAME_WITH_TPBTA_TEMPLATE
            = "{\"function\":\"bindingDomainNameWithTPBTA(string,bytes)\",\"args\":\"'{}','{}'\"}";

    private static final String RELAY_CALL_GET_TPBTA_BY_DOMAIN_NAME_TEMPLATE
            = "{\"function\":\"getTPBTAByDomainName(string)\",\"args\":\"'{}'\",\"return\":\"returns(string)\"}";

    private static final String RELAY_CALL_GET_CERT_BY_ID_TEMPLATE
            = "{\"function\":\"getCertById(string)\",\"args\":\"'{}'\",\"return\":\"returns(string)\"}";

    private static final String RELAY_CALL_GET_RELAY_BY_DOMAIN_NAME_TEMPLATE
            = "{\"function\":\"getRelayByDomainName(string)\",\"args\":\"'{}'\",\"return\":\"returns(string,string)\"}";

    public static BifBCDNSClient generateFrom(byte[] rawConf) {
        BifBCNDSConfig config = JSON.parseObject(rawConf, BifBCNDSConfig.class);
        return new BifBCDNSClient(
                config.getCertificationServiceConfig(),
                config.getChainConfig()
        );
    }

    private final BifCertificationServiceClient certificationServiceClient;

    private final BIFSDK bifsdk;

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
        bifsdk = ObjectUtil.isNull(bifChainConfig.getBifChainRpcPort()) ?
                BIFSDK.getInstance(bifChainConfig.getBifChainRpcUrl()) :
                BIFSDK.getInstance(bifChainConfig.getBifChainRpcUrl(), bifChainConfig.getBifChainRpcPort());
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
            bifContractCallRequest.setInput(
                    StrUtil.format(
                            RELAY_CALL_GET_CERT_BY_ID_TEMPLATE,
                            request.getRelayerCertId()
                    )
            );
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

            String res = decodeResultFromResponse(response);
            boolean exist = StrUtil.isNotEmpty(res);
            return new QueryRelayerCertificateResponse(
                    exist,
                    exist ? CrossChainCertificateFactory.createCrossChainCertificate(Base64.decode(res)) : null
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
        try {
            BIFContractCallRequest bifContractCallRequest = new BIFContractCallRequest();
            bifContractCallRequest.setContractAddress(bifChainConfig.getPtcGovernContract());
            bifContractCallRequest.setInput(
                    StrUtil.format(
                            PTC_CALL_GET_CERT_BY_ID_TEMPLATE,
                            request.getPtcCertId()
                    )
            );
            BIFContractCallResponse response = bifsdk.getBIFContractService().contractQuery(bifContractCallRequest);
            if (0 != response.getErrorCode()) {
                throw new AntChainBridgeBCDNSException(
                        BCDNSErrorCodeEnum.BCDNS_QUERY_PTC_CERT_FAILED,
                        StrUtil.format(
                                "failed to call getCertByName to BIF chain ( err_code: {}, err_msg: {} )",
                                response.getErrorCode(), response.getErrorDesc()
                        )
                );
            }

            String res = decodeResultFromResponse(response);
            boolean exist = StrUtil.isNotEmpty(res);
            return new QueryPTCCertificateResponse(
                    exist,
                    exist ? CrossChainCertificateFactory.createCrossChainCertificate(Base64.decode(res)) : null
            );
        } catch (AntChainBridgeBCDNSException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_QUERY_PTC_CERT_FAILED,
                    StrUtil.format(
                            "failed to query PTC certificate for (cert_id: {}, name: {}) from BIF chain",
                            request.getPtcCertId(), request.getName()
                    ),
                    e
            );
        }
    }

    @Override
    public QueryDomainNameCertificateResponse queryDomainNameCertificate(QueryDomainNameCertificateRequest request) {
        try {
            BIFContractCallRequest bifContractCallRequest = new BIFContractCallRequest();
            bifContractCallRequest.setContractAddress(bifChainConfig.getDomainGovernContract());
            bifContractCallRequest.setInput(
                    StrUtil.format(
                            DOMAIN_CALL_GET_CERT_BY_NAME_TEMPLATE,
                            request.getDomain().getDomain()
                    )
            );
            BIFContractCallResponse response = bifsdk.getBIFContractService().contractQuery(bifContractCallRequest);
            if (0 != response.getErrorCode()) {
                throw new AntChainBridgeBCDNSException(
                        BCDNSErrorCodeEnum.BCDNS_QUERY_DOMAIN_CERT_FAILED,
                        StrUtil.format(
                                "failed to call getCertByName to BIF chain ( err_code: {}, err_msg: {} )",
                                response.getErrorCode(), response.getErrorDesc()
                        )
                );
            }

            String res = decodeResultFromResponse(response);
            boolean exist = StrUtil.isNotEmpty(res);
            return new QueryDomainNameCertificateResponse(
                    exist,
                    exist ? CrossChainCertificateFactory.createCrossChainCertificate(Base64.decode(res)) : null
            );
        } catch (AntChainBridgeBCDNSException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_QUERY_DOMAIN_CERT_FAILED,
                    StrUtil.format(
                            "failed to query domain certificate for domain {} from BIF chain",
                            request.getDomain()
                    ),
                    e
            );
        }
    }

    @Override
    public void registerDomainRouter(RegisterDomainRouterRequest request) throws AntChainBridgeBCDNSException {
        try {
            BIFContractInvokeRequest bifContractInvokeRequest = new BIFContractInvokeRequest();
            bifContractInvokeRequest.setSenderAddress(bifChainConfig.getBifAddress());
            bifContractInvokeRequest.setPrivateKey(bifChainConfig.getBifPrivateKey());
            bifContractInvokeRequest.setContractAddress(bifChainConfig.getRelayerGovernContract());
            bifContractInvokeRequest.setBIFAmount(0L);
            bifContractInvokeRequest.setGasPrice(1L);
            bifContractInvokeRequest.setInput(
                    StrUtil.format(
                            RELAY_CALL_BINDING_DOMAIN_NAME_WITH_RELAY_TEMPLATE,
                            request.getRouter().getDestDomain().getDomain(),
                            request.getRouter().getDestRelayer().getRelayerCertId(),
                            "0x" + HexUtil.encodeHexStr(StrUtil.join("^", request.getRouter().getDestRelayer().getNetAddressList()).getBytes())
                    )
            );

            BIFContractInvokeResponse response = bifsdk.getBIFContractService().contractInvoke(bifContractInvokeRequest);
            if (0 != response.getErrorCode()) {
                throw new AntChainBridgeBCDNSException(
                        BCDNSErrorCodeEnum.BCDNS_REGISTER_DOMAIN_ROUTER_FAILED,
                        StrUtil.format(
                                "failed to call bindingDomainNameWithRelay to BIF chain ( err_code: {}, err_msg: {} )",
                                response.getErrorCode(), response.getErrorDesc()
                        )
                );
            }
        } catch (AntChainBridgeBCDNSException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_REGISTER_DOMAIN_ROUTER_FAILED,
                    StrUtil.format(
                            "failed to registerDomainRouter (domain: {}, relayer_cert_id: {}, net_addresses: [{}]) to BIF chain",
                            request.getRouter().getDestDomain().getDomain(),
                            request.getRouter().getDestRelayer().getRelayerCert(),
                            StrUtil.join(",", request.getRouter().getDestRelayer().getNetAddressList())
                    ),
                    e
            );
        }
    }

    @Override
    public void registerThirdPartyBlockchainTrustAnchor(RegisterThirdPartyBlockchainTrustAnchorRequest request) throws AntChainBridgeBCDNSException {
        try {
            BIFContractInvokeRequest bifContractInvokeRequest = new BIFContractInvokeRequest();
            bifContractInvokeRequest.setSenderAddress(bifChainConfig.getBifAddress());
            bifContractInvokeRequest.setPrivateKey(bifChainConfig.getBifPrivateKey());
            bifContractInvokeRequest.setContractAddress(bifChainConfig.getRelayerGovernContract());
            bifContractInvokeRequest.setBIFAmount(0L);
            bifContractInvokeRequest.setGasPrice(1L);
            bifContractInvokeRequest.setInput(
                    StrUtil.format(
                            RELAY_CALL_BINDING_DOMAIN_NAME_WITH_TPBTA_TEMPLATE,
                            request.getDomain().getDomain(),
                            "0x" + HexUtil.encodeHexStr(request.getTpbta())
                    )
            );

            BIFContractInvokeResponse response = bifsdk.getBIFContractService().contractInvoke(bifContractInvokeRequest);
            if (0 != response.getErrorCode()) {
                throw new AntChainBridgeBCDNSException(
                        BCDNSErrorCodeEnum.BCDNS_REGISTER_TPBTA_FAILED,
                        StrUtil.format(
                                "failed to call bindingDomainNameWithTPBTA to BIF chain ( err_code: {}, err_msg: {} )",
                                response.getErrorCode(), response.getErrorDesc()
                        )
                );
            }
        } catch (AntChainBridgeBCDNSException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_REGISTER_TPBTA_FAILED,
                    StrUtil.format(
                            "failed to register tpbta {} for domain {} to BIF chain",
                            Base64.encode(request.getTpbta()),
                            request.getDomain().getDomain()
                    ),
                    e
            );
        }
    }

    @Override
    public DomainRouter queryDomainRouter(QueryDomainRouterRequest request) {
        try {
            BIFContractCallRequest bifContractCallRequest = new BIFContractCallRequest();
            bifContractCallRequest.setContractAddress(bifChainConfig.getRelayerGovernContract());
            bifContractCallRequest.setInput(
                    StrUtil.format(
                            RELAY_CALL_GET_RELAY_BY_DOMAIN_NAME_TEMPLATE,
                            request.getDestDomain().getDomain()
                    )
            );
            BIFContractCallResponse response = bifsdk.getBIFContractService().contractQuery(bifContractCallRequest);
            if (0 != response.getErrorCode() || ObjectUtil.isNull(response.getResult())) {
                throw new RuntimeException(StrUtil.format("call BIF chain failed: ( err_code: {}, err_msg: {} )",
                        response.getErrorCode(), response.getErrorDesc()));
            }
            List<String> res = decodeResultsFromResponse(response);
            if (ObjectUtil.isNull(res) || res.size() < 2 || StrUtil.isEmpty(res.get(0))) {
                return null;
            }
            AbstractCrossChainCertificate relayerCert = CrossChainCertificateFactory.createCrossChainCertificate(
                    Base64.decode(res.get(0))
            );
            List<String> netAddresses = StrUtil.split(res.get(1), "^")
                    .stream().map(HexUtil::decodeHexStr).collect(Collectors.toList());
            return new DomainRouter(
                    request.getDestDomain(),
                    new Relayer(
                            relayerCert.getId(),
                            relayerCert,
                            netAddresses
                    )
            );
        } catch (AntChainBridgeBCDNSException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_QUERY_DOMAIN_ROUTER_FAILED,
                    StrUtil.format(
                            "failed to query domain router for domain {} from BIF chain",
                            request.getDestDomain().getDomain()
                    ),
                    e
            );
        }
    }

    @Override
    public byte[] queryThirdPartyBlockchainTrustAnchor(QueryThirdPartyBlockchainTrustAnchorRequest request) {
        try {
            BIFContractCallRequest bifContractCallRequest = new BIFContractCallRequest();
            bifContractCallRequest.setContractAddress(bifChainConfig.getRelayerGovernContract());
            bifContractCallRequest.setInput(
                    StrUtil.format(
                            RELAY_CALL_GET_TPBTA_BY_DOMAIN_NAME_TEMPLATE,
                            request.getDomain().getDomain()
                    )
            );
            BIFContractCallResponse response = bifsdk.getBIFContractService().contractQuery(bifContractCallRequest);
            if (0 != response.getErrorCode()) {
                throw new AntChainBridgeBCDNSException(
                        BCDNSErrorCodeEnum.BCDNS_QUERY_TPBTA_FAILED,
                        StrUtil.format(
                                "failed to call getTPBTAByDomainName to BIF chain ( err_code: {}, err_msg: {} )",
                                response.getErrorCode(), response.getErrorDesc()
                        )
                );
            }
            String res = decodeResultFromResponse(response);
            return StrUtil.isEmpty(res) ? null : HexUtil.decodeHex(res);
        } catch (AntChainBridgeBCDNSException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_QUERY_TPBTA_FAILED,
                    StrUtil.format(
                            "failed to query TPBTA for domain {} from BIF chain",
                            request.getDomain()
                    ),
                    e
            );
        }
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

    private String decodeResultFromResponse(BIFContractCallResponse response) {
        Map<String, Map<String, String>> resMap = (Map<String, Map<String, String>>) (response.getResult().getQueryRets().get(0));
        String res = resMap.get("result").get("data").trim();
        return StrUtil.replaceFirst(
                StrUtil.removeSuffix(
                        StrUtil.removePrefix(res, "["),
                        "]"
                ),
                "0x", ""
        );
    }

    private List<String> decodeResultsFromResponse(BIFContractCallResponse response) {
        Map<String, Map<String, String>> resMap = (Map<String, Map<String, String>>) (response.getResult().getQueryRets().get(0));
        String res = resMap.get("result").get("data").trim();

        res = StrUtil.replace(
                StrUtil.removeSuffix(
                        StrUtil.removePrefix(res, "["),
                        "]"
                ),
                "0x", ""
        );
        return StrUtil.split(res, ",").stream().map(StrUtil::trim).collect(Collectors.toList());
    }
}
