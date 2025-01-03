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

package com.alipay.antchain.bridge.plugins.hyperchain;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Ethereum's configuration information
 * - Url for Ethereum node rpc
 * - Private key
 */
@Getter
@Setter
public class HyperchainConfig {

    /**
     * 从json字符串反序列化
     *
     * @param jsonString raw json
     */
    public static HyperchainConfig fromJsonString(String jsonString) throws IOException {
        return JSON.parseObject(jsonString, HyperchainConfig.class);
    }

    @JSONField
    private String url;

    @JSONField
    private String accountJson;

    @JSONField
    private String password;

    @JSONField
    private String amContractAddressDeployed;

    @JSONField
    private String sdpContractAddressDeployed;

    /**
     * json序列化为字符串
     */
    public String toJsonString() {
        return JSON.toJSONString(this);
    }

    public static String readFileJson(String fileName) {
        StringBuilder jsonStringBuilder = new StringBuilder();

        try {
            // 使用ClassLoader获取资源文件的输入流
            InputStream inputStream = HyperchainConfig.class.getClassLoader().getResourceAsStream(fileName);

            // 使用BufferedReader逐行读取文件内容
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }

            // 关闭资源
            reader.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return jsonStringBuilder.toString();
    }
}
