package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

import lombok.Data;

@Data
public class IssuerApplyRespDto {

    private String applyNo;

    private Long applyTime;

    private String bid;

    private String name;

    /**
     * 申请状态 0-待审核，1-审核通过，2_审核不通过
     */
    private Integer applyStatus;

    private Long auditTime;

    private String auditorBid;


}
