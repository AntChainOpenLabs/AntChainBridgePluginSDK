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

package com.alipay.antchain.bridge.plugins.citacloud;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CITACloudConfig {

    public static final String CITA_CRYPTO_SUITE_TYPE_SM = "sm";

    public static final String CITA_CRYPTO_SUITE_TYPE_DEFAULT = "default";

    public static CITACloudConfig parseFrom(byte[] rawConfig) {
        return JSON.parseObject(rawConfig, CITACloudConfig.class);
    }

    @JSONField(name = "node_host")
    private String nodeHost;

    @JSONField(name = "tx_port")
    private int txPort;

    @JSONField(name = "query_port")
    private int queryPort;

    @JSONField(name = "chain_code")
    private String chainCode;

    @JSONField(name = "gateway_host")
    private String gatewayHost;

    @JSONField(name = "app_id")
    private String appId;

    @JSONField(name = "app_secret")
    private String appSecret;

    @JSONField(name = "seconds_before_token_expired")
    private long secondsBeforeAuthTokenExpired = 300;

    @JSONField(name = "am_address")
    private String amContractAddressDeployed;

    @JSONField(name = "sdp_address")
    private String sdpContractAddressDeployed;

    @JSONField(name = "private_key")
    private String privateKey;

    @JSONField(name = "address")
    private String address;

    @JSONField(name = "crypto_suite")
    private String cryptoSuite = CITA_CRYPTO_SUITE_TYPE_SM;

    @JSONField(name = "sys_contract_version")
    private String sysContractVersion = "v0.1";
}
