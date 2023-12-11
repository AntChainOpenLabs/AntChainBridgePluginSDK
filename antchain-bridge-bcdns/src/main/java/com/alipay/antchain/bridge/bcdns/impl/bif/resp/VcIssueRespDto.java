package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

import lombok.Data;

@Data
public class VcIssueRespDto {
    private String credentialId;
    private byte[] vcData;
}
