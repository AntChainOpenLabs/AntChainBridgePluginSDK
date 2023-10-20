package com.alipay.antchain.bridge.bcdns.impl.bif.req;

import lombok.Data;

@Data
public class TemplateAuditReqDto extends BlobReqDto{

    private String templateBid;
    private String remark;
    private Integer status;
    private String auditorBid;
}
