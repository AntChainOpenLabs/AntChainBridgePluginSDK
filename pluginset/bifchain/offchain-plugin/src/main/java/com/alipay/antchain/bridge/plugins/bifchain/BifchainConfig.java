package com.alipay.antchain.bridge.plugins.bifchain;

import java.io.IOException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
/**
 * Bifchain's configuration information
 * - Url for Bifchain node rpc
 * - Private key
 */
@Getter
@Setter
public class BifchainConfig {

    /**
     * 从json字符串反序列化
     *
     * @param jsonString raw json
     */
    public static BifchainConfig fromJsonString(String jsonString) throws IOException {
        return JSON.parseObject(jsonString, BifchainConfig.class);
    }

    @JSONField
    String url;

    @JSONField
    private String privateKey;

    @JSONField
    private String address;

    @JSONField
    private long gasLimit = 50000000L;

    @JSONField
    private long gasPrice = 1L;

    @JSONField
    private long amount = 0L;

    @JSONField
    private String amContractAddressDeployed;

    @JSONField
    private String sdpContractAddressDeployed;

    @JSONField
    private String ptcContractAddressDeployed;

    @JSONField
    private String ptcContractInitInput;

    @JSONField
    private short csVersion;

    @JSONField
    private String domainName;


    /**
     * json序列化为字符串
     */
    public String toJsonString() {
        return JSON.toJSONString(this);
    }
}