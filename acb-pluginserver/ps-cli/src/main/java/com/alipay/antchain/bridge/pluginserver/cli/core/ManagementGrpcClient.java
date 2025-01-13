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

package com.alipay.antchain.bridge.pluginserver.cli.core;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.pluginserver.managementservice.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ManagementGrpcClient {

    private final ManagedChannel channel;
    private ManagementServiceGrpc.ManagementServiceBlockingStub blockingStub;

    private final String host;

    private final int port;

    public ManagementGrpcClient(int port) {
        this("127.0.0.1", port);
    }

    public ManagementGrpcClient(String host, int port) {
        this.port = port;
        this.host = host;
        this.channel = ManagedChannelBuilder.forAddress(this.host, this.port)
                .usePlaintext()
                .build();
        this.blockingStub = ManagementServiceGrpc.newBlockingStub(channel);
    }

    public boolean checkServerStatus() {
        try {
            Socket socket = new Socket(host, port);
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public String managePlugin(PluginManageRequest.Type type, String product, String path) {
        ManageResponse response = this.blockingStub.managePlugin(
                PluginManageRequest.newBuilder()
                        .setType(type)
                        .setPath(path)
                        .setProduct(product)
                        .build()
        );
        return response.getCode() == 0 ? "success" : "failed with msg: " + response.getErrorMsg();
    }

    public String hasPlugins(List<String> productList) {
        ManageResponse response = this.blockingStub.hasPlugins(
                HasPluginsRequest.newBuilder()
                        .addAllProducts(productList)
                        .build()
        );
        if (response.getCode() != 0) {
            return "failed with msg: " + response.getErrorMsg();
        }

        return JSON.toJSONString(response.getHasPluginsResp().getResultsMap());
    }

    public String getAllPlugins() {
        ManageResponse response = this.blockingStub.allPlugins(AllPluginsRequest.newBuilder().build());
        if (response.getCode() != 0) {
            return "failed with msg: " + response.getErrorMsg();
        }

        return CollUtil.join(response.getAllPluginsResp().getProductsList(), ", ");
    }

    public String hasDomains(List<String> domains) {
        ManageResponse response = this.blockingStub.hasDomains(
                HasDomainsRequest.newBuilder()
                        .addAllDomains(domains)
                        .build()
        );
        if (response.getCode() != 0) {
            return "failed with msg: " + response.getErrorMsg();
        }

        return JSON.toJSONString(response.getHasDomainsResp().getResultsMap());
    }

    public String getAllDomains() {
        ManageResponse response = this.blockingStub.allDomains(AllDomainsRequest.newBuilder().build());
        if (response.getCode() != 0) {
            return "failed with msg: " + response.getErrorMsg();
        }

        return CollUtil.join(response.getAllDomainsResp().getDomainsList(), ", ");
    }

    public String restartBBC(String product, String domain) {
        ManageResponse response = this.blockingStub.restartBBC(
                RestartBBCRequest.newBuilder()
                        .setProduct(product)
                        .setDomain(domain)
                        .build()
        );
        if (response.getCode() != 0) {
            return "failed with msg: " + response.getErrorMsg();
        }
        return "success";
    }
}
