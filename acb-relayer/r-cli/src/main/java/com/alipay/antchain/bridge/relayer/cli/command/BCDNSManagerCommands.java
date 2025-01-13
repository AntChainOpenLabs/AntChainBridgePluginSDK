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

package com.alipay.antchain.bridge.relayer.cli.command;

import javax.annotation.Resource;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.relayer.cli.glclient.GrpcClient;
import lombok.Getter;
import org.springframework.shell.standard.*;

@Getter
@ShellCommandGroup(value = "Commands about BCDNS")
@ShellComponent
public class BCDNSManagerCommands extends BaseCommands {

    @Resource
    private GrpcClient grpcClient;

    @Override
    public String name() {
        return "bcdns";
    }

    @ShellMethod(value = "Register a new BCDNS bound with specified domain space into Relayer")
    Object registerBCDNSService(
            @ShellOption(help = "The domain space owned by the BCDNS, default the root space \"\"", defaultValue = "") String domainSpace,
            @ShellOption(help = "The type of the BCDNS, e.g. embedded, bif") String bcdnsType,
            @ShellOption(valueProvider = FileValueProvider.class, help = "The properties file path needed to initialize the service stub, e.g. /path/to/bif_bcdns_conf.json") String propFile,
            @ShellOption(valueProvider = FileValueProvider.class, help = "The path to BCDNS trust root certificate file if you have it", defaultValue = "") String bcdnsCertPath
    ) {
        if (StrUtil.isEmpty(bcdnsCertPath)) {
            return queryAPI("registerBCDNSService", domainSpace, bcdnsType, propFile);
        }
        return queryAPI("registerBCDNSService", domainSpace, bcdnsType, propFile, bcdnsCertPath);
    }

    @ShellMethod(value = "Get the BCDNS data bound with specified domain space")
    Object getBCDNSService(@ShellOption(help = "The domain space bound with BCDNS, default the root space", defaultValue = "") String domainSpace) {
        return queryAPI("getBCDNSService", domainSpace);
    }

    @ShellMethod(value = "Delete the BCDNS bound with specified domain space")
    Object deleteBCDNSService(@ShellOption(help = "The domain space bound with BCDNS, default the root space", defaultValue = "") String domainSpace) {
        return queryAPI("deleteBCDNSService", domainSpace);
    }

    @ShellMethod(value = "Get the BCDNS trust root certificate bound with specified domain space")
    Object getBCDNSCertificate(@ShellOption(help = "The domain space bound with BCDNS, default the root space", defaultValue = "") String domainSpace) {
        return queryAPI("getBCDNSCertificate", domainSpace);
    }

    @ShellMethod(value = "Stop the local BCDNS service stub")
    Object stopBCDNSService(@ShellOption(help = "The domain space bound with BCDNS, default the root space", defaultValue = "") String domainSpace) {
        return queryAPI("stopBCDNSService", domainSpace);
    }

    @ShellMethod(value = "Restart the local BCDNS service stub from stop")
    Object restartBCDNSService(@ShellOption(help = "domain space, default the root space", defaultValue = "") String domainSpace) {
        return queryAPI("restartBCDNSService", domainSpace);
    }

    @ShellMethod(value = "Apply a domain certificate for a blockchain from the BCDNS with specified domain space")
    Object applyDomainNameCert(
            @ShellOption(help = "The domain space bound with BCDNS, default the root space", defaultValue = "") String domainSpace,
            @ShellOption(help = "The domain applying") String domain,
            @ShellOption(help = "The type for applicant subject, e.g. `X509_PUBLIC_KEY_INFO` or `BID`", defaultValue = "BID") String applicantOidType,
            @ShellOption(valueProvider = FileValueProvider.class, help = "The subject file like public key file in PEM or BID document file") String oidFilePath
    ) {
        if (StrUtil.equalsIgnoreCase(applicantOidType, "bid")) {
            applicantOidType = "1";
        } else {
            applicantOidType = "0";
        }
        return queryAPI("applyDomainNameCert", domainSpace, domain, applicantOidType, oidFilePath);
    }

    @ShellMethod(value = "Query the state of application for a specified blockchain domain")
    Object queryDomainCertApplicationState(@ShellOption(help = "The specified domain") String domain) {
        return queryAPI("queryDomainCertApplicationState", domain);
    }

    @ShellMethod(value = "Fetch the certificate for a specified blockchain domain from the BCDNS with the domain space")
    Object fetchDomainNameCertFromBCDNS(
            @ShellOption(help = "The specified domain") String domain,
            @ShellOption(help = "The BCDNS domain space, default the root space", defaultValue = "") String domainSpace
    ) {

        return queryAPI("fetchDomainNameCertFromBCDNS", domain, domainSpace);
    }

    @ShellMethod(value = "Query the domain name certificate from the BCDNS with the domain space")
    Object queryDomainNameCertFromBCDNS(
            @ShellOption(help = "The specified domain") String domain,
            @ShellOption(help = "The BCDNS domain space, default the root space", defaultValue = "") String domainSpace
    ) {
        return queryAPI("queryDomainNameCertFromBCDNS", domain, domainSpace);
    }

    @ShellMethod(value = "Register the domain router including the specified domain name and " +
            "local relayer information to the BCDNS with the parent domain space for the domain")
    Object registerDomainRouter(@ShellOption(help = "The specified domain") String domain) {
        return queryAPI("registerDomainRouter", domain);
    }

    @ShellMethod(value = "Query the domain router for the domain from the BCDNS with the domain space")
    Object queryDomainRouter(
            @ShellOption(help = "The specified domain") String domain,
            @ShellOption(help = "The BCDNS domain space, default the root space", defaultValue = "") String domainSpace
    ) {
        return queryAPI("queryDomainRouter", domainSpace, domain);
    }

    @ShellMethod(value = "Upload the TpBta with specified crosschain lane and version to the BCDNS")
    Object uploadTpBta(
            @ShellOption(help = "TpBta crosschain lane's sender domain") String senderDomain,
            @ShellOption(help = "TpBta crosschain lane's sender id", defaultValue = "") String senderId,
            @ShellOption(help = "TpBta crosschain lane's receiver domain", defaultValue = "") String receiverDomain,
            @ShellOption(help = "TpBta crosschain lane's receiver id", defaultValue = "") String receiverId,
            @ShellOption(help = "TpBta version to upload", defaultValue = "") String tpbtaVersion
    ) {
        CrossChainLane lane;
        if (StrUtil.isEmpty(receiverDomain)) {
            lane = new CrossChainLane(
                    new CrossChainDomain(senderDomain)
            );
        } else if (StrUtil.isEmpty(senderId) && StrUtil.isEmpty(receiverId)) {
            lane = new CrossChainLane(
                    new CrossChainDomain(senderDomain),
                    new CrossChainDomain(receiverDomain)
            );
        } else {
            lane = new CrossChainLane(
                    new CrossChainDomain(senderDomain),
                    new CrossChainDomain(receiverDomain),
                    getIdentity(senderId),
                    getIdentity(receiverId)
            );
        }
        return queryAPI("uploadTpBta", lane.getLaneKey(), tpbtaVersion);
    }

    Object addBlockchainTrustAnchor() {
        return queryAPI("addBlockchainTrustAnchor");
    }
}
