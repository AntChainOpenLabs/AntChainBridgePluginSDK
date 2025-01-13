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

package com.alipay.antchain.bridge.relayer.cli.command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTypeEnum;
import com.alipay.antchain.bridge.ptc.committee.types.network.CommitteeNetworkInfo;
import com.alipay.antchain.bridge.ptc.committee.types.tpbta.CommitteeEndorseRoot;
import com.alipay.antchain.bridge.ptc.committee.types.tpbta.VerifyBtaExtension;
import com.alipay.antchain.bridge.ptc.committee.types.trustroot.CommitteeVerifyAnchor;
import com.alipay.antchain.bridge.relayer.cli.glclient.GrpcClient;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.shell.standard.*;

@Getter
@ShellCommandGroup(value = "Commands about PTC management")
@ShellComponent
public class PtcCommands extends BaseCommands {

    @Resource
    private GrpcClient grpcClient;

    @Override
    public String name() {
        return "ptc";
    }

    @Override
    public GrpcClient getGrpcClient() {
        return grpcClient;
    }

    @ShellMethod(value = "Register a PTC service stub into Relayer")
    @SneakyThrows
    Object registerPtcService(
            @ShellOption(help = "Unique ID inside Relayer for your ptc service") String ptcServiceId,
            @ShellOption(
                    help = "Domain space for BCDNS who issues the PTC crosschain certificate",
                    defaultValue = ""
            ) String issuerBcdnsDomainSpace,
            @ShellOption(
                    help = "The file path for PTC crosschain certificate in PEM format",
                    valueProvider = FileValueProvider.class
            ) String ptcCertFile,
            @ShellOption(
                    valueProvider = FileValueProvider.class,
                    help = "The client configuration file for PTC service"
            ) String configFile
    ) {
        Path ptcCertPath = Paths.get(ptcCertFile);
        if (!Files.exists(ptcCertPath)) {
            return "ptc cert file not exists";
        }
        Path configPath = Paths.get(configFile);
        if (!Files.exists(configPath)) {
            return "config file not exists";
        }

        byte[] rawPtcPemCert = Files.readAllBytes(ptcCertPath);
        byte[] rawConfig = Files.readAllBytes(configPath);

        return queryAPI("registerPtcService", ptcServiceId, issuerBcdnsDomainSpace, new String(rawPtcPemCert), Base64.encode(rawConfig));
    }

    @ShellMethod(value = "Get the PTC service information registered in Relayer")
    Object getPtcService(
            @ShellOption(help = "Unique ID inside Relayer for your ptc service") String ptcServiceId
    ) {
        return queryAPI("getPtcServiceInfo", ptcServiceId);
    }

    @ShellMethod(value = "Start PTC service stub in Relayer")
    Object startPtcService(
            @ShellOption(help = "Unique ID inside Relayer for your ptc service") String ptcServiceId
    ) {
        return queryAPI("startPtcService", ptcServiceId);
    }

    @ShellMethod(value = "Stop PTC service stub in Relayer")
    Object stopPtcService(
            @ShellOption(help = "Unique ID inside Relayer for your ptc service") String ptcServiceId
    ) {
        return queryAPI("stopPtcService", ptcServiceId);
    }

    @ShellMethod(value = "Remove PTC service stub in Relayer")
    Object removePtcService(
            @ShellOption(help = "Unique ID inside Relayer for your ptc service") String ptcServiceId
    ) {
        return queryAPI("removePtcService", ptcServiceId);
    }

    @ShellMethod(value = "Get the PTC crosschain certificate in PEM format")
    Object getPtcCert(
            @ShellOption(help = "Unique ID inside Relayer for your ptc service") String ptcServiceId
    ) {
        return queryAPI("getPtcCert", ptcServiceId);
    }

    @ShellMethod(value = "Query the ptc trust root")
    Object getPtcTrustRoot(
            @ShellOption(help = "The ptc service id") String ptcServiceId,
            @ShellOption(help = "The ptc type", valueProvider = EnumValueProvider.class, defaultValue = "COMMITTEE") PTCTypeEnum ptcType,
            @ShellOption(help = "Decode and show the network info", defaultValue = "true") String showNetworkInfo,
            @ShellOption(help = "Decode and show the ptc root info", defaultValue = "true") String showVerifyAnchors
    ) {
        String rawRootBase64 = queryAPI("getPtcTrustRoot", ptcServiceId);
        if (!Base64.isBase64(rawRootBase64)) {
            return rawRootBase64;
        }

        String result = StrUtil.format( "raw ptc trust root: {}", rawRootBase64);
        PTCTrustRoot ptcTrustRoot = PTCTrustRoot.decode(Base64.decode(rawRootBase64));
        if (Boolean.parseBoolean(showNetworkInfo)) {
            if (ptcType == PTCTypeEnum.COMMITTEE) {
                result += StrUtil.format("\nnetwork info: {}",
                        JSON.toJSONString(JSON.parseObject(ptcTrustRoot.getNetworkInfo(), CommitteeNetworkInfo.class), SerializerFeature.PrettyFormat));
            } else {
                result += StrUtil.format("\nnetwork info: {}", Base64.encode(ptcTrustRoot.getNetworkInfo()));
            }
        }
        if (Boolean.parseBoolean(showVerifyAnchors)) {
            if (ptcType == PTCTypeEnum.COMMITTEE) {
                Map<String, CommitteeVerifyAnchor> anchorMap = ptcTrustRoot.getVerifyAnchorMap().entrySet().stream()
                        .collect(Collectors.toMap(
                                entry -> entry.getKey().toString(),
                                entry -> CommitteeVerifyAnchor.decode(entry.getValue().getAnchor())
                        ));
                result += StrUtil.format("\nverify-anchors info: {}", JSON.toJSONString(anchorMap, SerializerFeature.PrettyFormat));
            } else {
                result += StrUtil.format("\nverify-anchors info: {}", JSON.toJSONString(ptcTrustRoot.getVerifyAnchorMap()));
            }
        }

        return result;
    }

    @ShellMethod(value = "Construct the committee ptc extension going to involved in BTA construction")
    Object constructExtensionInBtaForCommitteePtc(
            @ShellOption(help = "Sender blockchain domain for tpbta crosschain lane") String senderDomain,
            @ShellOption(help = "Receiver blockchain domain for tpbta crosschain lane", defaultValue = "") String receiverDomain,
            @ShellOption(help = "Sender Identity for tpbta crosschain lane", defaultValue = "") String senderIdentity,
            @ShellOption(help = "Receiver Identity for tpbta crosschain lane", defaultValue = "") String receiverIdentity,
            @ShellOption(help = "The endorse root json configuration for your tpbta", valueProvider = FileValueProvider.class) String endorseRootFile
    ) {
        try {
            CrossChainLane tpbtaLane;
            if (!senderIdentity.isEmpty()) {
                tpbtaLane = new CrossChainLane(
                        new CrossChainDomain(senderDomain),
                        new CrossChainDomain(receiverDomain),
                        getIdentity(senderIdentity),
                        getIdentity(receiverIdentity)
                );
            } else if (!receiverDomain.isEmpty()) {
                tpbtaLane = new CrossChainLane(new CrossChainDomain(senderDomain), new CrossChainDomain(receiverDomain));
            } else {
                tpbtaLane = new CrossChainLane(new CrossChainDomain(senderDomain));
            }

            Path endorseRootPath = Paths.get(endorseRootFile);
            if (!Files.exists(endorseRootPath)) {
                return "endorse root file not exists";
            }
            VerifyBtaExtension verifyBtaExtension = new VerifyBtaExtension(
                    CommitteeEndorseRoot.decodeJson(new String(Files.readAllBytes(endorseRootPath))),
                    tpbtaLane
            );
            return Base64.encode(verifyBtaExtension.encode());
        } catch (Throwable t) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", t);
        }
    }

    @ShellMethod(value = "List all working ptc service infos")
    Object listPtcServices() {
        String res = queryAPI("listPtcServices");
        if (StrUtil.equalsIgnoreCase(res, "internal error") || StrUtil.isEmpty(res)) {
            return res;
        }

        return JSON.toJSONString(StrUtil.split(res, "@@@"));
    }
}
