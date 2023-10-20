package org.bcdns.credential.dto.req;

import lombok.Data;

@Data
public class VcTemplateListReqDto extends PageReqDto{

    private String templateName;
    private String templateBid;
    private String categoryId;
    private String issuerBid;
    private String industryId;
    private String applyNo;
    private Integer userType;
    private Integer status;
    private Integer auditStatus;
    private String issuerName;

}
