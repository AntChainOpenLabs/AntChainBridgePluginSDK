package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

import lombok.Data;

@Data
public class VcTemplateDetailRespDto {

    private String templateName;
    private String industryName;
    private String categoryName;
    private String version;
    private String remark;
    private String data;
    private String userType;
    private String templateBid;
    private String issuerBid;
    private String issuerName;
    private Integer credentialType;


}
