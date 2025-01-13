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

package com.alipay.antchain.bridge.plugins.ethereum;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.plugins.ethereum.helper.EthNoncePolicyEnum;
import com.alipay.antchain.bridge.plugins.ethereum.helper.GasLimitPolicyEnum;
import com.alipay.antchain.bridge.plugins.ethereum.helper.GasPricePolicyEnum;
import com.alipay.antchain.bridge.plugins.ethereum.helper.GasPriceProviderSupplierEnum;
import lombok.Getter;
import lombok.Setter;
import org.web3j.tx.gas.DefaultGasProvider;

import java.io.IOException;

/**
 * Ethereum's configuration information
 * - Url for Ethereum node rpc
 * - Private key
 */
@Getter
@Setter
public class EthereumConfig {

    /**
     * 从json字符串反序列化
     *
     * @param jsonString raw json
     */
    public static EthereumConfig fromJsonString(String jsonString) throws IOException {
        return JSON.parseObject(jsonString, EthereumConfig.class);
    }

    public enum CrossChainMessageScanPolicyEnum {
        LOG_FILTER,
        BLOCK_SCAN
    }

    @JSONField
    private String url;

    @JSONField
    private String privateKey;

    @JSONField
    private String chainName;

    @JSONField
    private long gasLimit = DefaultGasProvider.GAS_LIMIT.longValue();

    @JSONField
    private long gasPrice = DefaultGasProvider.GAS_PRICE.longValue();

    @JSONField
    private String amContractAddressDeployed;

    @JSONField
    private String proxyAdmin;

    @JSONField
    private boolean upgradableContracts = true;

    @JSONField
    private String sdpContractAddressDeployed;

    @JSONField
    private BlockHeightPolicyEnum blockHeightPolicy = BlockHeightPolicyEnum.LATEST;

    @JSONField
    private CrossChainMessageScanPolicyEnum msgScanPolicy = CrossChainMessageScanPolicyEnum.LOG_FILTER;

    @JSONField
    private GasLimitPolicyEnum gasLimitPolicy = GasLimitPolicyEnum.STATIC;

    @JSONField
    private long extraGasLimit = 0L;

    @JSONField
    private GasPricePolicyEnum gasPricePolicy = GasPricePolicyEnum.STATIC;

    @JSONField
    private EthNoncePolicyEnum ethNoncePolicy = EthNoncePolicyEnum.NORMAL;

    @JSONField
    private GasPriceProviderSupplierEnum gasPriceProviderSupplier;

    @JSONField
    private String gasProviderUrl;

    @JSONField
    private String gasProviderApiKey;

    @JSONField
    private long gasUpdateInterval = 15000;

    // --------------KMS Service Config--------------
    @JSONField
    private boolean kmsService = false;

    @JSONField
    private String kmsEndpoint;

    @JSONField
    private String kmsAccessKeyId;

    @JSONField
    private String kmsAccessKeySecret;

    @JSONField
    private String kmsPrivateKeyId;

    /**
     * json序列化为字符串
     */
    public String toJsonString() {
        return JSON.toJSONString(this);
    }
}
