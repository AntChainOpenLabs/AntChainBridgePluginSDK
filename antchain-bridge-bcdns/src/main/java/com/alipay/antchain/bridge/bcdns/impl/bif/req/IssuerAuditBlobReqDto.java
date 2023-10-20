package com.alipay.antchain.bridge.bcdns.impl.bif.req;

import lombok.Data;

@Data
public class IssuerAuditBlobReqDto extends BlobReqDto{

    private String applyNo;
    private String auditRemark;
    private Integer auditStatus;
    private String superNodeBid;
    private String hash;

}
