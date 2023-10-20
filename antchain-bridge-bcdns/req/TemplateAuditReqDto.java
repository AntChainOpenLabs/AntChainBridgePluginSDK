package org.bcdns.credential.dto.req;

import lombok.Data;

@Data
public class TemplateAuditReqDto extends BlobReqDto{

    private String templateBid;
    private String remark;
    private Integer status;
    private String auditorBid;
}
