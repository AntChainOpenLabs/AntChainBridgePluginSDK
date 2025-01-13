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

import com.alipay.antchain.bridge.relayer.cli.glclient.GrpcClient;
import lombok.Getter;
import org.springframework.shell.standard.*;

@ShellCommandGroup(value = "Service Control")
@ShellComponent
@Getter
public class ServiceManagerCommands extends BaseCommands {

    @Override
    public String name() {
        return "service";
    }

    @Resource
    private GrpcClient grpcClient;

    @ShellMethod(value = "Add new item to cross-chain message access control list")
    public Object addCrossChainMsgACL(
            @ShellOption(help = "Blockchain domain applying permission to send msg") String grantDomain,
            @ShellOption(help = "Blockchain account identity applying permission to send msg") String grantIdentity,
            @ShellOption(help = "Blockchain domain receiving msg") String ownerDomain,
            @ShellOption(help = "Blockchain domain account identity receiving msg") String ownerIdentity
    ) {
        return queryAPI("addCrossChainMsgACL", grantDomain, grantIdentity, ownerDomain, ownerIdentity);
    }

    @ShellMethod(value = "Get a item from cross-chain message access control list")
    Object getCrossChainMsgACL(@ShellOption(help = "The ID for cross-chain msg ACL item") String bizId) {
        return queryAPI("getCrossChainMsgACL", bizId);
    }

    @ShellMethod(value = "Get the ACL item matched the input")
    Object getMatchedCrossChainACLItems(
            @ShellOption(help = "Blockchain domain applying permission to send msg") String grantDomain,
            @ShellOption(help = "Blockchain account identity applying permission to send msg") String grantIdentity,
            @ShellOption(help = "Blockchain domain receiving msg") String ownerDomain,
            @ShellOption(help = "Blockchain domain account identity receiving msg") String ownerIdentity
    ) {
        return queryAPI("getCrossChainMsgACL", grantDomain, grantIdentity, ownerDomain, ownerIdentity);
    }

    @ShellMethod(value = "Delete the ACL item with the specified biz ID")
    Object deleteCrossChainMsgACL(
            @ShellOption(help = "You can get the ID by calling `getMatchedCrossChainACLItems`") String bizId
    ) {
        return queryAPI("deleteCrossChainMsgACL", bizId);
    }

    @ShellMethod(value = "Register a plugin server into Relayer")
    Object registerPluginServer(
            @ShellOption(help = "unique ID for your plugin server") String pluginServerId,
            @ShellOption(help = "plugin server's URL e.g. 127.0.0.1:9090") String address,
            @ShellOption(valueProvider = FileValueProvider.class, help = "file path for TLS CA for plugin server e.g. /path/to/certs/server.crt") String pluginServerCAPath
    ) {
        return queryAPI("registerPluginServer", pluginServerId, address, pluginServerCAPath);
    }

    @ShellMethod(value = "Stop the plugin server")
    Object stopPluginServer(@ShellOption(help = "ID of the plugin server you want to stop") String pluginServerId) {
        return queryAPI("stopPluginServer", pluginServerId);
    }

    @ShellMethod(value = "Start the plugin server from stop")
    Object startPluginServer(@ShellOption(help = "ID of the plugin server you want to start") String pluginServerId) {
        return queryAPI("startPluginServer", pluginServerId);
    }

    @ShellMethod(value = "Query the nodes status of local relayer")
    Object queryCurrActiveNodes() {
        return queryAPI("queryCurrActiveNodes");
    }

    @ShellMethod(value = "End the mark distributed task, e.g. domain query tasks")
    Object endMarkDtTask(
            @ShellOption(help = "The type of the mark distributed task, default DOMAIN_ROUTER_QUERY", defaultValue = "DOMAIN_ROUTER_QUERY")
            String taskType,
            @ShellOption(help = "The unique ID of the mark distributed task, field 'unique_key' in DB table 'mark_dt_task'. ")
            String uniqueId
    ) {
        return queryAPI("endMarkDTTask", taskType, uniqueId);
    }
}
