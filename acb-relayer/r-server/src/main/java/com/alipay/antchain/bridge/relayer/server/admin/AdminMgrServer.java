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

package com.alipay.antchain.bridge.relayer.server.admin;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.annotation.Resource;

import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Order
public class AdminMgrServer implements ApplicationRunner {

    @Value("${relayer.admin_server.host:127.0.0.1}")
    private String adminServerHost;

    @Value("${relayer.admin_server.port:8088}")
    private Integer adminServerPort;

    @Resource
    private AdminRpcServerImpl adminRpcServer;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting admin managing server on {}:{}", adminServerHost, adminServerPort);
        NettyServerBuilder.forAddress(
                        new InetSocketAddress(
                                InetAddress.getByName(adminServerHost),
                                adminServerPort
                        )
                ).addService(adminRpcServer)
                .build()
                .start();
    }
}
