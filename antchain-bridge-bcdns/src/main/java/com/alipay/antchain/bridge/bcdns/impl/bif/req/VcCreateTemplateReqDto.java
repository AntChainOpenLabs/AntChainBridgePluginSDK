package com.alipay.antchain.bridge.bcdns.impl.bif.req;

import lombok.Data;

@Data
public class VcCreateTemplateReqDto extends BlobReqDto{

    private String name;
    private String industryId;
    private String categoryId;
    private String version;
    private String remark;
    private String data;
    private String userType;
    private String issuerBid;
    private String templateBid;

}
