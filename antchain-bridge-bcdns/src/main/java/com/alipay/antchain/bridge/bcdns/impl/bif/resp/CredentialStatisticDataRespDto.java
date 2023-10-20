package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

import lombok.Data;

@Data
public class CredentialStatisticDataRespDto {
    private Integer issuerCount;
    private Integer tempalteCount;
    private Integer credentialCount;

}
