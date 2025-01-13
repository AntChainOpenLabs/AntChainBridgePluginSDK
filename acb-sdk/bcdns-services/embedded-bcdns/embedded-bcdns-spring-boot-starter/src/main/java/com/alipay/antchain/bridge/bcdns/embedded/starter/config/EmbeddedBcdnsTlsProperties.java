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

package com.alipay.antchain.bridge.bcdns.embedded.starter.config;

import io.grpc.TlsServerCredentials;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConditionalOnProperty(prefix = "acb.bcdns.embedded.security", name = "mode", havingValue = "TLS")
@ConfigurationProperties("acb.bcdns.embedded.security.tls")
@Data
public class EmbeddedBcdnsTlsProperties {

    private Resource serverCertChain;

    private Resource serverKey;

    private Resource trustCertCollection;

    private TlsServerCredentials.ClientAuth clientAuth = TlsServerCredentials.ClientAuth.REQUIRE;
}
