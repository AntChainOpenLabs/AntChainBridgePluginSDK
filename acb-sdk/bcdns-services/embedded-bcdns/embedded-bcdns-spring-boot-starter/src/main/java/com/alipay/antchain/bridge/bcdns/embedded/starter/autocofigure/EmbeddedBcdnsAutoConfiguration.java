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

package com.alipay.antchain.bridge.bcdns.embedded.starter.autocofigure;

import java.net.InetSocketAddress;

import cn.hutool.core.io.FileUtil;
import com.alipay.antchain.bridge.bcdns.embedded.server.GRpcEmbeddedBcdnsService;
import com.alipay.antchain.bridge.bcdns.embedded.server.IBcdnsState;
import com.alipay.antchain.bridge.bcdns.embedded.starter.config.EmbeddedBcdnsProperties;
import com.alipay.antchain.bridge.bcdns.embedded.starter.config.EmbeddedBcdnsSecurityEnum;
import com.alipay.antchain.bridge.bcdns.embedded.starter.config.EmbeddedBcdnsSecurityProperties;
import com.alipay.antchain.bridge.bcdns.embedded.starter.config.EmbeddedBcdnsTlsProperties;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import io.grpc.Server;
import io.grpc.TlsServerCredentials;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({EmbeddedBcdnsProperties.class, EmbeddedBcdnsSecurityProperties.class, EmbeddedBcdnsTlsProperties.class})
@ConditionalOnClass({GRpcEmbeddedBcdnsService.class})
@AutoConfigureOrder(100)
@Slf4j
public class EmbeddedBcdnsAutoConfiguration {

    @Bean
    @SneakyThrows
    @ConditionalOnBean(name = {"bcdnsState"})
    @ConditionalOnProperty(prefix = "acb.bcdns.embedded", name = "server-on", havingValue = "true")
    public Server embeddedBcdnsServer(
            EmbeddedBcdnsProperties embeddedBcdnsProperties,
            EmbeddedBcdnsSecurityProperties embeddedBcdnsSecurityProperties,
            EmbeddedBcdnsTlsProperties embeddedBcdnsTlsProperties,
            @Autowired IBcdnsState bcdnsState) {
        log.info("start embedded bcdns server on {}:{}",
                embeddedBcdnsProperties.getServerHost(), embeddedBcdnsProperties.getServerPort());

        GRpcEmbeddedBcdnsService service = new GRpcEmbeddedBcdnsService(
                bcdnsState,
                embeddedBcdnsProperties.getSignCertHashAlgo(),
                embeddedBcdnsProperties.getSignAlgo(),
                embeddedBcdnsProperties.getSignAlgo()
                        .getSigner()
                        .readPemPrivateKey(
                                FileUtil.readBytes(embeddedBcdnsProperties.getRootPrivateKeyFile().getFile())
                        ),
                CrossChainCertificateFactory.createCrossChainCertificateFromPem(FileUtil.readBytes(embeddedBcdnsProperties.getRootCertFile().getFile()))
        );

        if (embeddedBcdnsSecurityProperties.getMode() == EmbeddedBcdnsSecurityEnum.TLS) {
            log.info("embedded bcdns server start with tls security");
            return NettyServerBuilder.forAddress(
                            new InetSocketAddress(embeddedBcdnsProperties.getServerHost(), embeddedBcdnsProperties.getServerPort()),
                            TlsServerCredentials.newBuilder()
                                    .keyManager(
                                            embeddedBcdnsTlsProperties.getServerCertChain().getFile(),
                                            embeddedBcdnsTlsProperties.getServerKey().getFile()
                                    ).clientAuth(embeddedBcdnsTlsProperties.getClientAuth())
                                    .trustManager(embeddedBcdnsTlsProperties.getTrustCertCollection().getFile())
                                    .build()
                    ).addService(service)
                    .build()
                    .start();
        }

        return NettyServerBuilder.forAddress(
                        new InetSocketAddress(embeddedBcdnsProperties.getServerHost(), embeddedBcdnsProperties.getServerPort())
                ).addService(service)
                .build()
                .start();
    }
}
