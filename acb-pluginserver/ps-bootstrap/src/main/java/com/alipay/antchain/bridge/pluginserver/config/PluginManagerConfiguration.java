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

package com.alipay.antchain.bridge.pluginserver.config;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.pluginserver.server.PluginManagementServiceImpl;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

@Configuration
@Slf4j
public class PluginManagerConfiguration {

    @Value("${pluginserver.managerserver.host:localhost}")
    private String managementHost;

    @Value("${pluginserver.managerserver.port}")
    private String pluginServerMgrPort;

    @Bean
    public Server pluginMgrServer(@Autowired PluginManagementServiceImpl pluginManagementService) throws IOException {
        log.info("Starting plugin managing server on port " + pluginServerMgrPort);
        return NettyServerBuilder.forAddress(
                        new InetSocketAddress(
                                StrUtil.isEmpty(managementHost) ? InetAddress.getLoopbackAddress() : InetAddress.getByName(managementHost),
                                Integer.parseInt(pluginServerMgrPort)
                        )
                ).addService(pluginManagementService)
                .build()
                .start();
    }
}
