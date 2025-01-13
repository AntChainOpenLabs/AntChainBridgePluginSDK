package com.alipay.antchain.bridge.plugins.ethereum.kms.req;

import lombok.Data;

@Data
public class KmsClientGetSecretReq {
    // 凭证名称
    private String name;
    // 凭证版本号
    private String version;
}
