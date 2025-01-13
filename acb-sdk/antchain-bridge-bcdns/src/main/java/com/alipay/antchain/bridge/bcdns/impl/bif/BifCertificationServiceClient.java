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

import cn.hutool.core.codec.Base64;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alipay.antchain.bridge.bcdns.impl.bif.req.*;
import com.alipay.antchain.bridge.bcdns.impl.bif.resp.*;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateTypeEnum;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyBlockchainTrustAnchor;
import lombok.Getter;
import okhttp3.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.util.Objects;

@Getter
public class BifCertificationServiceClient {

    public static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final String VC_APPLY_URL = "/vc/apply";

    private static final String VC_STATUS_URL = "/vc/apply/status";

    private static final String VC_DOWNLOAD_URL = "/vc/download";

    private static final String VC_ROOT = "/vc/root";

    private static final String VC_ADD_PTCTRUSTROOT_URL = "/vc/add/ptctrustroot";

    private static final String VC_ADD_TPBTA_URL = "/vc/add/tpbta";

    private final OkHttpClient httpClient;

    private final String serviceUrl;

    private final BifBCDNSClientCredential clientCredential;

    public BifCertificationServiceClient(String serviceUrl, BifBCDNSClientCredential bifBCDNSClientCredential) {
        if (serviceUrl.startsWith("https://")) {
            try {
                TrustManager[] trustAllCerts = buildTrustManagers();
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

                httpClient = new OkHttpClient.Builder()
                        .sslSocketFactory(
                                sslContext.getSocketFactory(),
                                (X509TrustManager) trustAllCerts[0]
                        ).hostnameVerifier((hostname, session) -> true)
                        .build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            httpClient = new OkHttpClient.Builder().build();
            if (!serviceUrl.startsWith("http://")) {
                serviceUrl = StrUtil.format("http://{}", serviceUrl);
            }
        }

        this.serviceUrl = serviceUrl;
        this.clientCredential = bifBCDNSClientCredential;
    }

    public VcApplyRespDto applyCrossChainCertificate(AbstractCrossChainCertificate certSigningRequest) {
        VcApplyReqDto vcApplyReqDto = new VcApplyReqDto();
        vcApplyReqDto.setContent(certSigningRequest.getEncodedToSign());
        vcApplyReqDto.setCredentialType(certSigningRequest.getType().ordinal());
        vcApplyReqDto.setPublicKey(
                certSigningRequest.getType() == CrossChainCertificateTypeEnum.RELAYER_CERTIFICATE
                        || certSigningRequest.getType() == CrossChainCertificateTypeEnum.PROOF_TRANSFORMATION_COMPONENT_CERTIFICATE ?
                        clientCredential.getBifFormatAuthorizedPublicKey() :
                        Base64.encode(CrossChainCertificateUtil.getPublicKeyFromCrossChainCertificate(clientCredential.getClientCert()).getEncoded())
        );
        vcApplyReqDto.setSign(
                certSigningRequest.getType() == CrossChainCertificateTypeEnum.RELAYER_CERTIFICATE
                        || certSigningRequest.getType() == CrossChainCertificateTypeEnum.PROOF_TRANSFORMATION_COMPONENT_CERTIFICATE ?
                        clientCredential.signAuthorizedRequest(certSigningRequest.getEncodedToSign()) :
                        clientCredential.signRequest(certSigningRequest.getEncodedToSign())
        );
        vcApplyReqDto.setSignAlgo(clientCredential.getAuthorizedSigAlgo().getName());

        try (
                Response response = httpClient.newCall(
                        new Request.Builder()
                                .url(getRequestUrl(VC_APPLY_URL))
                                .post(RequestBody.create(JSON.toJSONString(vcApplyReqDto), JSON_MEDIA_TYPE))
                                .build()
                ).execute();
        ) {
            if (!response.isSuccessful()) {
                throw new RuntimeException(
                        StrUtil.format(
                                "http request failed: {} - {}",
                                response.code(), response.message()
                        )
                );
            }
            DataResp<VcApplyRespDto> resp = JSON.parseObject(
                    Objects.requireNonNull(response.body(), "empty resp body").string(),
                    new TypeReference<DataResp<VcApplyRespDto>>() {
                    }
            );
            if (resp.getErrorCode() != 0) {
                throw new RuntimeException(
                        StrUtil.format(
                                "resp with error ( code: {}, msg: {} )",
                                resp.getErrorCode(),
                                resp.getMessage()
                        )
                );
            }
            return Assert.notNull(resp.getData());
        } catch (Exception e) {
            throw new RuntimeException(
                    StrUtil.format(
                            "failed to call BIF BCDNS for {} : ",
                            VC_APPLY_URL
                    ),
                    e
            );
        }
    }

    public QueryStatusRespDto queryApplicationStatus(String applyReceipt) {
        QueryStatusReqDto queryStatusRespDto = new QueryStatusReqDto();
        queryStatusRespDto.setApplyNo(applyReceipt);

        try (
                Response response = httpClient.newCall(
                        new Request.Builder()
                                .url(getRequestUrl(VC_STATUS_URL))
                                .post(RequestBody.create(JSON.toJSONString(queryStatusRespDto), JSON_MEDIA_TYPE))
                                .build()
                ).execute();
        ) {
            if (!response.isSuccessful()) {
                throw new RuntimeException(
                        StrUtil.format(
                                "http request failed: {} - {}",
                                response.code(), response.message()
                        )
                );
            }
            DataResp<QueryStatusRespDto> resp = JSON.parseObject(
                    Objects.requireNonNull(response.body(), "empty resp body").string(),
                    new TypeReference<DataResp<QueryStatusRespDto>>() {
                    }
            );
            if (resp.getErrorCode() != 0) {
                throw new RuntimeException(
                        StrUtil.format(
                                "resp with error ( code: {}, msg: {} )",
                                resp.getErrorCode(),
                                resp.getMessage()
                        )
                );
            }
            return Assert.notNull(resp.getData());
        } catch (Exception e) {
            throw new RuntimeException(
                    StrUtil.format(
                            "failed to call BIF BCDNS for {} : ",
                            VC_STATUS_URL
                    ),
                    e
            );
        }
    }

    public VcInfoRespDto downloadCrossChainCert(String credentialId) {
        VcInfoReqDto vcInfoReqDto = new VcInfoReqDto();
        vcInfoReqDto.setCredentialId(credentialId);

        try (
                Response response = httpClient.newCall(
                        new Request.Builder()
                                .url(getRequestUrl(VC_DOWNLOAD_URL))
                                .post(RequestBody.create(JSON.toJSONString(vcInfoReqDto), JSON_MEDIA_TYPE))
                                .build()
                ).execute();
        ) {
            if (!response.isSuccessful()) {
                throw new RuntimeException(
                        StrUtil.format(
                                "http request failed: {} - {}",
                                response.code(), response.message()
                        )
                );
            }
            DataResp<VcInfoRespDto> resp = JSON.parseObject(
                    Objects.requireNonNull(response.body(), "empty resp body").string(),
                    new TypeReference<DataResp<VcInfoRespDto>>() {
                    }
            );
            if (resp.getErrorCode() != 0) {
                throw new RuntimeException(
                        StrUtil.format(
                                "resp with error ( code: {}, msg: {} )",
                                resp.getErrorCode(),
                                resp.getMessage()
                        )
                );
            }
            return Assert.notNull(resp.getData());
        } catch (Exception e) {
            throw new RuntimeException(
                    StrUtil.format(
                            "failed to call BIF BCDNS for {} : ",
                            VC_DOWNLOAD_URL
                    ),
                    e
            );
        }
    }

    public VcRootRespDto queryRootCert() {
        try (
                Response response = httpClient.newCall(
                        new Request.Builder()
                                .url(getRequestUrl(VC_ROOT))
                                .post(RequestBody.create(StrUtil.EMPTY_JSON, JSON_MEDIA_TYPE))
                                .build()
                ).execute();
        ) {
            if (!response.isSuccessful()) {
                throw new RuntimeException(
                        StrUtil.format(
                                "http request failed: {} - {}",
                                response.code(), response.message()
                        )
                );
            }
            DataResp<VcRootRespDto> resp = JSON.parseObject(
                    Objects.requireNonNull(response.body(), "empty resp body").string(),
                    new TypeReference<DataResp<VcRootRespDto>>() {
                    }
            );
            if (resp.getErrorCode() != 0) {
                throw new RuntimeException(
                        StrUtil.format(
                                "resp with error ( code: {}, msg: {} )",
                                resp.getErrorCode(),
                                resp.getMessage()
                        )
                );
            }
            return Assert.notNull(resp.getData());
        } catch (Exception e) {
            throw new RuntimeException(
                    StrUtil.format(
                            "failed to call BIF BCDNS for {} : ",
                            VC_ROOT
                    ),
                    e
            );
        }
    }

    public VcPTCTrustRootRespDto addPTCTrustRoot(PTCTrustRoot ptcTrustRoot) {
        VcPTCTrustRootReqDto vcPTCTrustRootReqDto = new VcPTCTrustRootReqDto();
        vcPTCTrustRootReqDto.setPtcTrustRoot(ptcTrustRoot.encode());
        try (
                Response response = httpClient.newCall(
                        new Request.Builder()
                                .url(getRequestUrl(VC_ADD_PTCTRUSTROOT_URL))
                                .post(RequestBody.create(JSON.toJSONString(vcPTCTrustRootReqDto), JSON_MEDIA_TYPE))
                                .build()
                ).execute();
        ) {
            if (!response.isSuccessful()) {
                throw new RuntimeException(
                        StrUtil.format(
                                "http request failed: {} - {}",
                                response.code(), response.message()
                        )
                );
            }
            DataResp<VcPTCTrustRootRespDto> resp = JSON.parseObject(
                    Objects.requireNonNull(response.body(), "empty resp body").string(),
                    new TypeReference<DataResp<VcPTCTrustRootRespDto>>() {
                    }
            );
            if (resp.getErrorCode() != 0) {
                throw new RuntimeException(
                        StrUtil.format(
                                "resp with error ( code: {}, msg: {} )",
                                resp.getErrorCode(),
                                resp.getMessage()
                        )
                );
            }
            return Assert.notNull(resp.getData());
        } catch (Exception e) {
            throw new RuntimeException(
                    StrUtil.format(
                            "failed to add PTCTrustRoot"
                    ),
                    e
            );
        }
    }

    public VcTpBtaRespDto addThirdPartyBlockchainTrustAnchor(ThirdPartyBlockchainTrustAnchor tpbta) {
        VcTpBtaReqDto vcTpBtaReqDto = new VcTpBtaReqDto();
        vcTpBtaReqDto.setVcId(clientCredential.getClientCert().getId()); // RELAYER's vcId
        vcTpBtaReqDto.setTpbta(tpbta.encode());
        vcTpBtaReqDto.setCredentialType(CrossChainCertificateTypeEnum.RELAYER_CERTIFICATE.ordinal());
        vcTpBtaReqDto.setSignAlgo(clientCredential.getSigAlgo().getName()); // ED25519
        vcTpBtaReqDto.setPublicKey(Base64.encode(
                CrossChainCertificateUtil.getPublicKeyFromCrossChainCertificate(clientCredential.getClientCert()).getEncoded())
        );
//        PublicKey publicKey = new X509PubkeyInfoObjectIdentity(Base64.decode(vcTpBtaReqDto.getPublicKey())).getPublicKey();
        vcTpBtaReqDto.setSign(clientCredential.signRequest(vcTpBtaReqDto.getTpbta()));
        try (
                Response response = httpClient.newCall(
                        new Request.Builder()
                                .url(getRequestUrl(VC_ADD_TPBTA_URL))
                                .post(RequestBody.create(JSONObject.toJSONString(vcTpBtaReqDto), JSON_MEDIA_TYPE))
                                .build()
                ).execute();
        ) {
            if (!response.isSuccessful()) {
                throw new RuntimeException(
                        StrUtil.format(
                                "http request failed: {} - {}",
                                response.code(), response.message()
                        )
                );
            }
            DataResp<VcTpBtaRespDto> resp = JSON.parseObject(
                    Objects.requireNonNull(response.body(), "empty resp body").string(),
                    new TypeReference<DataResp<VcTpBtaRespDto>>() {
                    }
            );
            if(resp.getErrorCode() != 0) {
                throw new RuntimeException(
                        StrUtil.format(
                                "resp with error ( code: {}, msg: {} )",
                                resp.getErrorCode(),
                                resp.getMessage()
                        )
                );
            }
            return Assert.notNull(resp.getData());
        } catch (Exception e) {
            throw new RuntimeException(
                    StrUtil.format(
                            "failed to add TpBta"
                    ),
                    e
            );
        }
    }

    private static TrustManager[] buildTrustManagers() {
        return new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };
    }

    private String getRequestUrl(String req) {
        return StrUtil.concat(
                true,
                StrUtil.endWith(serviceUrl, "/") ? StrUtil.replaceLast(serviceUrl, "/", "") : serviceUrl,
                "/external",
                req
        );
    }
}
