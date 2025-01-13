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

package com.alipay.antchain.bridge.relayer.server.admin.impl;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PublicKey;
import javax.annotation.Resource;

import cn.ac.caict.bid.model.BIDDocumentOperation;
import cn.hutool.core.io.file.PathUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.PemUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.bcdns.service.BCDNSTypeEnum;
import com.alipay.antchain.bridge.bcdns.types.base.DomainRouter;
import com.alipay.antchain.bridge.bcdns.types.req.QueryDomainRouterRequest;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.utils.BIDHelper;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.base.*;
import com.alipay.antchain.bridge.relayer.commons.constant.DomainCertApplicationStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.DomainCertApplicationDO;
import com.alipay.antchain.bridge.relayer.commons.model.DomainSpaceCertWrapper;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.core.manager.ptc.IPtcManager;
import com.alipay.antchain.bridge.relayer.server.admin.AbstractNamespace;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.springframework.stereotype.Component;
import sun.security.x509.AlgorithmId;

@Component
@Slf4j
public class BCDNSNamespace extends AbstractNamespace {

    @Resource
    private IBCDNSManager bcdnsManager;

    @Resource
    private IPtcManager ptcManager;

    public BCDNSNamespace() {
        addCommand("registerBCDNSService", this::registerBCDNSService);
        addCommand("stopBCDNSService", this::stopBCDNSService);
        addCommand("restartBCDNSService", this::restartBCDNSService);
        addCommand("getBCDNSService", this::getBCDNSService);
        addCommand("deleteBCDNSService", this::deleteBCDNSService);
        addCommand("getBCDNSCertificate", this::getBCDNSCertificate);
        addCommand("applyDomainNameCert", this::applyDomainNameCert);
        addCommand("queryDomainCertApplicationState", this::queryDomainCertApplicationState);
        addCommand("fetchDomainNameCertFromBCDNS", this::fetchDomainNameCertFromBCDNS);
        addCommand("queryDomainNameCertFromBCDNS", this::queryDomainNameCertFromBCDNS);
        addCommand("registerDomainRouter", this::registerDomainRouter);
        addCommand("queryDomainRouter", this::queryDomainRouter);
        addCommand("uploadTpBta", this::uploadTpBta);
        addCommand("addBlockchainTrustAnchor", this::addBlockchainTrustAnchor);
    }

    Object registerBCDNSService(String... args) {
        if (args.length != 3 && args.length != 4) {
            return "wrong number of arguments";
        }
        try {
            String domainSpace = args[0];
            String bcdnsType = args[1];
            String propFile = args[2];

            if (bcdnsManager.hasBCDNSServiceData(domainSpace)) {
                return "already exist";
            }
            bcdnsManager.registerBCDNSService(
                    domainSpace,
                    BCDNSTypeEnum.parseFromValue(bcdnsType),
                    propFile,
                    args.length == 4 ? args[3] : ""
            );

            return "success";
        } catch (Throwable e) {
            log.error("failed to register BCDNS for domain space {}", args[0], e);
            return "failed to register BCDNS: " + ObjectUtil.defaultIfNull(e.getCause(), e).getMessage();
        }
    }

    Object stopBCDNSService(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }
        try {
            bcdnsManager.stopBCDNSService(args[0]);
            return "success";
        } catch (Throwable e) {
            log.error("failed to stop BCDNS for domain space {}", args[0], e);
            return "failed to stop BCDNS: " + e.getMessage();
        }
    }

    Object restartBCDNSService(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }
        try {
            bcdnsManager.restartBCDNSService(args[0]);
            return "success";
        } catch (Throwable e) {
            log.error("failed to restart BCDNS for domain space {}", args[0], e);
            return "failed to restart BCDNS: " + e.getMessage();
        }
    }

    Object getBCDNSService(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }

        String domainSpace = args[0];
        try {
            if (!bcdnsManager.hasBCDNSServiceData(domainSpace)) {
                return "not found";
            }
            return JSON.toJSONString(bcdnsManager.getBCDNSServiceData(domainSpace));
        } catch (Throwable e) {
            log.error("failed to get BCDNS for domain space {}", domainSpace, e);
            return "failed to get BCDNS: " + e.getMessage();
        }
    }

    Object deleteBCDNSService(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }

        String domainSpace = args[0];
        try {
            if (!bcdnsManager.hasBCDNSServiceData(domainSpace)) {
                return "not found";
            }
            bcdnsManager.deleteBCDNSServiceDate(domainSpace);
            return "success";
        } catch (Throwable e) {
            log.error("failed to del BCDNS for domain space {}", domainSpace, e);
            return "failed to del BCDNS: " + e.getMessage();
        }
    }

    Object getBCDNSCertificate(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }

        String domainSpace = args[0];
        try {
            DomainSpaceCertWrapper domainSpaceCertWrapper = bcdnsManager.getDomainSpaceCert(domainSpace);
            if (ObjectUtil.isNull(domainSpaceCertWrapper) || ObjectUtil.isNull(domainSpaceCertWrapper.getDomainSpaceCert())) {
                return "not found";
            }
            return CrossChainCertificateUtil.formatCrossChainCertificateToPem(domainSpaceCertWrapper.getDomainSpaceCert());
        } catch (Throwable e) {
            log.error("failed to get BCDNS certificate for {}", domainSpace, e);
            return "failed to get BCDNS certificate: " + e.getMessage();
        }
    }

    Object applyDomainNameCert(String... args) {
        if (args.length != 4) {
            return "wrong number of arguments";
        }

        String domainSpace = args[0];
        String domain = args[1];
        int applicantOidType = Integer.parseInt(args[2]);

        if (StrUtil.length(domain) >= CrossChainDomain.MAX_DOMAIN_LENGTH) {
            return "your domain should less than max length " + CrossChainDomain.MAX_DOMAIN_LENGTH;
        }
        try {
            byte[] rawContent;
            if (PathUtil.isFile(Paths.get(args[3]), false)) {
                rawContent = Files.readAllBytes(Paths.get(args[3]));
            } else {
                rawContent = args[3].getBytes();
            }

            byte[] rawSubject = null;
            ObjectIdentity oid = null;
            if (ObjectIdentityType.parseFromValue(applicantOidType) == ObjectIdentityType.BID) {
                rawSubject = rawContent;
                BIDDocumentOperation bidDocumentOperation = BIDHelper.getBIDDocumentFromRawSubject(rawContent);
                oid = new BIDInfoObjectIdentity(
                        BIDHelper.encAddress(
                                bidDocumentOperation.getPublicKey()[0].getType(),
                                BIDHelper.getRawPublicKeyFromBIDDocument(bidDocumentOperation)
                        )
                );
            } else if (ObjectIdentityType.parseFromValue(applicantOidType) == ObjectIdentityType.X509_PUBLIC_KEY_INFO) {
                PublicKey publicKey = readPublicKeyFromPem(rawContent);
                oid = new X509PubkeyInfoObjectIdentity(publicKey.getEncoded());
                rawSubject = new byte[]{};
            }

            String receipt = bcdnsManager.applyDomainCertificate(
                    domainSpace,
                    domain,
                    oid,
                    rawSubject
            );
            return "your receipt is " + receipt;
        } catch (Throwable e) {
            log.error("failed to apply domain cert from domain space [{}]", args[0], e);
            return "failed to apply domain cert : " + e.getMessage();
        }
    }

    Object queryDomainCertApplicationState(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }

        String domain = args[0];
        try {
            DomainCertApplicationDO domainCertApplicationDO = this.bcdnsManager.getDomainCertApplication(domain);
            if (ObjectUtil.isNull(domainCertApplicationDO)) {
                return "no application record found";
            }
            if (domainCertApplicationDO.getState() != DomainCertApplicationStateEnum.APPLYING) {
                return "your application finished: " + domainCertApplicationDO.getState().getCode();
            }
            return "your application not finished: " + domainCertApplicationDO.getState().getCode();
        } catch (Throwable e) {
            log.error("failed to query domain cert from BCDNS with domain space [{}]", args[0], e);
            return "failed to query domain cert: " + e.getMessage();
        }
    }

    Object queryDomainNameCertFromBCDNS(String... args) {
        if (args.length != 2) {
            return "wrong number of arguments";
        }

        String domain = args[0];
        String domainSpace = args[1];

        try {
            AbstractCrossChainCertificate certificate = bcdnsManager.queryDomainCertificateFromBCDNS(domain, domainSpace, false);
            if (ObjectUtil.isNull(certificate)) {
                return StrUtil.format("none cert found for domain {} on BCDNS {}", domain, domainSpace);
            }
            return "the cert is : \n" + CrossChainCertificateUtil.formatCrossChainCertificateToPem(certificate);
        } catch (Throwable e) {
            log.error("failed to query from BCDNS:", e);
            return "failed to query from BCDNS: " + e.getMessage();
        }
    }

    Object fetchDomainNameCertFromBCDNS(String... args) {
        if (args.length != 2) {
            return "wrong number of arguments";
        }

        String domain = args[0];
        String domainSpace = args[1];

        try {
            AbstractCrossChainCertificate certificate = bcdnsManager.queryDomainCertificateFromBCDNS(domain, domainSpace, true);
            if (ObjectUtil.isNull(certificate)) {
                return StrUtil.format("none cert found for domain {} on BCDNS {}", domain, domainSpace);
            }
            return "the cert is : \n" + CrossChainCertificateUtil.formatCrossChainCertificateToPem(certificate);
        } catch (Throwable e) {
            log.error("failed to query from BCDNS:", e);
            return "failed to query from BCDNS: " + ObjectUtil.defaultIfNull(e.getCause(), e).getMessage();
        }
    }

    Object registerDomainRouter(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }

        String domain = args[0];
        try {
            bcdnsManager.registerDomainRouter(domain);
            return "success";
        } catch (Throwable e) {
            log.error("failed to register router for domain {} to BCDNS:", domain, e);
            return "failed to register router: " + ObjectUtil.defaultIfNull(e.getCause(), e).getMessage();
        }
    }

    Object uploadTpBta(String... args) {
        if (args.length != 2) {
            return "wrong number of arguments";
        }

        try {
            String laneKey = args[0];
            int tpbtaVersion = -1;
            if (StrUtil.isNotEmpty(args[1])) {
                tpbtaVersion = Integer.parseInt(args[1]);
            }
            bcdnsManager.uploadTpBta(CrossChainLane.fromLaneKey(laneKey), tpbtaVersion);
            return "success";
        } catch (Throwable e) {
            log.error("failed to register tpbta {}:{} to BCDNS:", args[0], args[1], e);
            return "failed to register tpbta: " + ObjectUtil.defaultIfNull(e.getCause(), e).getMessage();
        }
    }

    Object queryDomainRouter(String... args) {
        if (args.length != 2) {
            return "wrong number of arguments";
        }

        String domainSpace = args[0];
        String domain = args[1];

        try {
            DomainRouter domainRouter = bcdnsManager.getBCDNSService(domainSpace).queryDomainRouter(
                    QueryDomainRouterRequest.builder().destDomain(new CrossChainDomain(domain)).build()
            );
            if (ObjectUtil.isNull(domainRouter)) {
                return "not found";
            }
            return JSON.toJSONString(domainRouter);
        } catch (Throwable e) {
            log.error("failed to register router for domain {} to BCDNS:", domain, e);
            return "failed to register router: " + e.getMessage();
        }
    }

    Object addBlockchainTrustAnchor(String... args) {
        return "success";
    }

    @SneakyThrows
    private PublicKey readPublicKeyFromPem(byte[] publicKeyPem) {
        SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(PemUtil.readPem(new ByteArrayInputStream(publicKeyPem)));
        return KeyUtil.generatePublicKey(
                AlgorithmId.get(keyInfo.getAlgorithm().getAlgorithm().getId()).getName(),
                keyInfo.getEncoded()
        );
    }
}
