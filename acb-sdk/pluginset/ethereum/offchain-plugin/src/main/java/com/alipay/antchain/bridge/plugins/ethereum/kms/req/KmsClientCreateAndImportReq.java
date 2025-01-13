package com.alipay.antchain.bridge.plugins.ethereum.kms.req;

import com.alipay.antchain.bridge.plugins.ethereum.kms.enums.SecretKeySpecEnum;
import com.alipay.antchain.bridge.plugins.ethereum.kms.enums.SecretKeyUsageEnum;
import lombok.Getter;

@Getter
public class KmsClientCreateAndImportReq {
    /**
     * key内容
     */
    private String keyPlaintext;

    /**
     * 密钥规格
     */
    private SecretKeySpecEnum keySpec;


    /**
     * 密钥的用途
     */
    private SecretKeyUsageEnum keyUsage;
}
