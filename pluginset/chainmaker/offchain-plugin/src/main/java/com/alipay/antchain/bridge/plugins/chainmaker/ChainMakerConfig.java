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

package com.alipay.antchain.bridge.plugins.chainmaker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChainMakerConfig {
    // chain configuration
    @JSONField
    private String sdkConfig;
    // info of admins in chain organizations
    @JSONField
    private List<byte[]> adminTlsKeyPaths = new ArrayList<>();
    @JSONField
    private List<byte[]> adminTlsCertPaths = new ArrayList<>();
    @JSONField
    private List<byte[]> adminKeyPaths = new ArrayList<>();
    @JSONField
    private List<byte[]> adminCertPaths = new ArrayList<>();
    @JSONField
    private List<String> orgIds = new ArrayList<>();
    @JSONField
    private String amContractAddressDeployed;
    @JSONField
    private String sdpContractAddressDeployed;

    // json string to java object
    public static ChainMakerConfig fromJsonString(String jsonString) throws IOException {
        return JSON.parseObject(jsonString, ChainMakerConfig.class);
    }
    // java object to json string
    public String toJsonString() {
        return JSON.toJSONString(this);
    }
}
