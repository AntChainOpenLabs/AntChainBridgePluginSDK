package org.bcdns.credential.dto.req;

import cn.blockchain.common.validate.Validatable;
import lombok.Data;

@Data
public class IssuerAuditReqDto extends PageReqDto{

    @Validatable(value = "申请编号")
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
