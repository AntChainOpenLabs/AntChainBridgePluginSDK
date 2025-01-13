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

package com.alipay.antchain.bridge.relayer.core.types.network.response;

import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import com.alipay.antchain.bridge.relayer.commons.utils.json.CrossChainCertMapDeserializer;
import com.alipay.antchain.bridge.relayer.commons.utils.json.CrossChainCertMapSerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HelloStartRespPayload implements IResponsePayload {

    public static HelloStartRespPayload decodeFromJson(String json) {
        return JSON.parseObject(json, HelloStartRespPayload.class);
    }

    @JSONField(name = "remote_node_info")
    private String remoteNodeInfo;

    @JSONField(name = "domain_space_cert_path", serializeUsing = CrossChainCertMapSerializer.class, deserializeUsing = CrossChainCertMapDeserializer.class)
    private Map<String, AbstractCrossChainCertificate> domainSpaceCertPath;

    @JSONField(name = "sigAlgo")
    private SignAlgoEnum sigAlgo;

    @JSONField(name = "sig")
    private byte[] sig;

    @JSONField(name = "rand")
    private byte[] rand;

    @Override
    public String encode() {
        return JSON.toJSONString(this);
    }
}
