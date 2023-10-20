package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

import lombok.Data;

@Data
public class IssuerAuditDetailRespDto {

    private String applyNo;
    private String issuerBid;
    private String issuerName;
    private long applyTime;
    private String status;
    private String issuerScope;
    private String issuerTrusted;
    private String reason;

}
