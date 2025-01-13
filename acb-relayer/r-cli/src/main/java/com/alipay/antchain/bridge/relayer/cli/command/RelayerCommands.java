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

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.cli.glclient.GrpcClient;
import lombok.Getter;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@Getter
@ShellCommandGroup(value = "Commands about Relayer Network")
@ShellComponent
public class RelayerCommands extends BaseCommands {

    @Resource
    private GrpcClient grpcClient;

    @Override
    public String name() {
        return "relayer";
    }

    @ShellMethod(value = "Set the endpoints represents the local relayer in network")
    Object setLocalEndpoints(@ShellOption(help = "Endpoints for local relayer, e.g. https://127.0.0.1:8082") String[] endpoints) {
        return queryAPI("setLocalEndpoints", StrUtil.join(",", ListUtil.toList(endpoints)));
    }

    @ShellMethod(value = "Get the endpoints represents the local relayer in network")
    Object getLocalEndpoints() {
        return queryAPI("getLocalEndpoints");
    }

    @ShellMethod(value = "Get the relayer ID represents the local relayer in network")
    Object getLocalRelayerId() {
        return queryAPI("getLocalRelayerId");
    }

    @ShellMethod(value = "Get the local relayer cross-chain certificate in PEM")
    Object getLocalRelayerCrossChainCertificate() {
        return queryAPI("getLocalRelayerCrossChainCertificate");
    }

    @ShellMethod(value = "Get the domain router from local storage")
    Object getLocalDomainRouter(@ShellOption(help = "Domain to query") String domain) {
        return queryAPI("getLocalDomainRouter", domain);
    }

    @ShellMethod(value = "Get the cross-chain channel from local domain to remote domain")
    Object getCrossChainChannel(
            @ShellOption(help = "Local domain") String localDomain,
            @ShellOption(help = "Remote domain") String remoteDomain
    ) {
        return queryAPI("getCrossChainChannel", localDomain, remoteDomain);
    }

    @ShellMethod(value = "Get the remote relayer information from local storage")
    Object getRemoteRelayerInfo(@ShellOption(help = "node ID for remote relayer") String nodeId) {
        return queryAPI("getRemoteRelayerInfo", nodeId);
    }
}
