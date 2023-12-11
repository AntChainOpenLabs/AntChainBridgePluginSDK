package com.alipay.antchain.bridge.bcdns.impl.bif.req;

import lombok.Data;

@Data
public class VcIssueAuditReqDto {
    private String applyNo;

    private Integer status;

    private String reason;
}
