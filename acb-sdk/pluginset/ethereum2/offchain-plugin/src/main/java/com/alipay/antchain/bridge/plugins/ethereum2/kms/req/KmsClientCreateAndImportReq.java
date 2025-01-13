package com.alipay.antchain.bridge.plugins.ethereum2.kms.req;

import com.alipay.antchain.bridge.plugins.ethereum2.kms.enums.SecretKeySpecEnum;
import com.alipay.antchain.bridge.plugins.ethereum2.kms.enums.SecretKeyUsageEnum;
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
