package com.alipay.antchain.bridge.bcdns.impl.bif.req;

import lombok.Data;

@Data
public class VcRevocationReqDto {

    private String credentialId;

    private String remark;
}
