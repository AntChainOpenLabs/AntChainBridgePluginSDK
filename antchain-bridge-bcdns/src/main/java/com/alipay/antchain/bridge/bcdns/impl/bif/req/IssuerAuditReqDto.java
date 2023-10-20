package com.alipay.antchain.bridge.bcdns.impl.bif.req;

import lombok.Data;

@Data
public class IssuerAuditReqDto extends PageReqDto{

    private String applyNo;
    private String superNodeBid;
    private String issuerName;
    private String issuerBid;
    private Integer status;
    private String applierName;
    private String applierBid;
    private String auditorBid;
    private String bid;
}
