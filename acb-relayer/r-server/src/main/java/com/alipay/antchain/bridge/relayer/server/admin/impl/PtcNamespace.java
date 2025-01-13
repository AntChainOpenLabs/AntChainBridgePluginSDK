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

package com.alipay.antchain.bridge.relayer.server.admin.impl;

import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alipay.antchain.bridge.bcdns.service.IBlockChainDomainNameService;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.relayer.commons.model.PtcServiceDO;
import com.alipay.antchain.bridge.relayer.commons.model.PtcTrustRootDO;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.core.manager.ptc.IPtcManager;
import com.alipay.antchain.bridge.relayer.server.admin.AbstractNamespace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PtcNamespace extends AbstractNamespace {

    @Resource
    private IPtcManager ptcManager;

    @Resource
    private IBCDNSManager bcdnsManager;

    public PtcNamespace() {
        addCommand("registerPtcService", this::registerPtcService);
        addCommand("getPtcServiceInfo", this::getPtcServiceInfo);
        addCommand("startPtcService", this::startPtcService);
        addCommand("stopPtcService", this::stopPtcService);
        addCommand("removePtcService", this::removePtcService);
        addCommand("getPtcCert", this::getPtcCert);
        addCommand("getPtcTrustRoot", this::getPtcTrustRoot);
        addCommand("listPtcServices", this::listPtcServices);
    }

    public Object registerPtcService(String... args) {
        if (args.length != 4) {
            return "wrong number of arguments";
        }

        String serviceId = args[0];
        String issuerBcdnsDomainSpace = args[1];
        String ptcCertPem = args[2];
        String configBase64 = args[3];

        try {
            ptcManager.registerPtcService(
                    serviceId,
                    new CrossChainDomain(issuerBcdnsDomainSpace),
                    CrossChainCertificateUtil.readCrossChainCertificateFromPem(ptcCertPem.getBytes()),
                    Base64.decode(configBase64)
            );
        } catch (Exception e) {
            log.error("failed to register plugin server: ", e);
            return "get some exception: " + e.getMessage();
        }

        return "success";
    }

    public Object getPtcServiceInfo(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }

        String serviceId = args[0];
        try {
            return JSON.toJSONString(ptcManager.getPtcServiceDO(serviceId), SerializerFeature.PrettyFormat);
        } catch (Exception e) {
            log.error("failed to get plugin server: ", e);
            return "get some exception: " + e.getMessage();
        }
    }

    public Object startPtcService(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }

        String serviceId = args[0];
        try {
            ptcManager.startPtcService(serviceId);
        } catch (Exception e) {
            log.error("failed to start plugin server: ", e);
            return "get some exception: " + e.getMessage();
        }

        return "success";
    }

    public Object stopPtcService(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }

        String serviceId = args[0];
        try {
            ptcManager.stopPtcService(serviceId);
        } catch (Exception e) {
            log.error("failed to stop plugin server: ", e);
            return "get some exception: " + e.getMessage();
        }

        return "success";
    }

    public Object removePtcService(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }

        String serviceId = args[0];
        try {
            ptcManager.removePtcService(serviceId);
        } catch (Exception e) {
            log.error("failed to remove plugin server: ", e);
            return "get some exception: " + e.getMessage();
        }

        return "success";
    }

    public Object getPtcCert(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }

        String ptcServiceId = args[0];
        try {
            PtcServiceDO ptcServiceDO = ptcManager.getPtcServiceDO(ptcServiceId);
            if (ObjectUtil.isNull(ptcServiceDO)) {
                return "ptc service not found";
            }
            return CrossChainCertificateUtil.formatCrossChainCertificateToPem(ptcServiceDO.getPtcCert());
        } catch (Exception e) {
            log.error("failed to get ptc cert: ", e);
            return "get some exception: " + e.getMessage();
        }
    }

    public Object getPtcTrustRoot(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }
        String ptcServiceId = args[0];
        try {
            PtcServiceDO ptcServiceDO = ptcManager.getPtcServiceDO(ptcServiceId);
            if (ObjectUtil.isNull(ptcServiceDO)) {
                return "ptc service not found";
            }
            PtcTrustRootDO ptcTrustRootDO = ptcManager.getPtcTrustRoot(ptcServiceId);
            if (ObjectUtil.isNotNull(ptcTrustRootDO)) {
                return Base64.encode(ptcTrustRootDO.getPtcTrustRoot().encode());
            }
            if (!bcdnsManager.hasBCDNSServiceData(ptcServiceDO.getIssuerBcdnsDomainSpace())) {
                return "bcdns service not found or not ready";
            }

            IBlockChainDomainNameService bcdnsService = bcdnsManager.getBCDNSService(ptcServiceDO.getIssuerBcdnsDomainSpace());
            PTCTrustRoot ptcTrustRoot = bcdnsService.queryPTCTrustRoot(ptcServiceDO.getOwnerId());

            ptcManager.savePtcTrustRoot(ptcServiceId, ptcTrustRoot);

            return Base64.encode(ptcTrustRoot.encode());
        } catch (Exception e) {
            log.error("failed to get ptc cert: ", e);
            return "get some exception: " + e.getMessage();
        }
    }

    Object listPtcServices(String... args) {
        try {
            return StrUtil.join("@@@", ptcManager.getAllWorkingPtcServices().stream().map(JSON::toJSONString).collect(Collectors.toList()));
        } catch (Throwable t) {
            log.error("failed to list ptc services: ", t);
            return "internal error";
        }
    }
}
