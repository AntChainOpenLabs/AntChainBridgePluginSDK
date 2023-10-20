package org.bcdns.credential.dto.req;

import cn.blockchain.common.validate.Validatable;
import lombok.Data;

@Data
public class IssuerAuditBlobReqDto extends BlobReqDto{

    @Validatable(value = "申请编号")
    private String applyNo;
    private String auditRemark;
    private Integer auditStatus;
    private String superNodeBid;
    private String hash;

}
