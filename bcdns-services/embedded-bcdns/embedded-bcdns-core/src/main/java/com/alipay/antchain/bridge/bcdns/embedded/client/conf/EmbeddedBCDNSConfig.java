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

package com.alipay.antchain.bridge.bcdns.embedded.client.conf;

import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.bcdns.embedded.types.endpoint.EndpointAddress;
import com.alipay.antchain.bridge.bcdns.embedded.types.endpoint.EndpointAddressDeserializer;
import com.alipay.antchain.bridge.bcdns.embedded.types.endpoint.EndpointAddressSerializer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmbeddedBCDNSConfig {

    @JSONField(name = "server_address", serializeUsing = EndpointAddressSerializer.class, deserializeUsing = EndpointAddressDeserializer.class)
    private EndpointAddress serverAddress;

    @JSONField(name = "tls_client_cert")
    private String tlsClientCert;

    @JSONField(name = "tls_client_key")
    private String tlsClientKey;

    @JSONField(name = "tls_trust_cert_chain")
    private String tlsTrustCertChain;

    @JSONField(name = "cross_chain_cert")
    private String crossChainCert;

    @JSONField(name = "cross_chain_key")
    private String crossChainKey;
}
