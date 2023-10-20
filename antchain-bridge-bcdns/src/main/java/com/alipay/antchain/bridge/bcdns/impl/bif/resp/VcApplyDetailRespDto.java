package com.alipay.antchain.bridge.bcdns.impl.bif.resp;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class VcApplyDetailRespDto {

    private String content;
    private String status;
    private String applyNo ;
    private String applyUser ;
    private Long applyTime ;
    private String auditBid ;
    private Long auditTime ;
    private String auditName ;
    private String auditRemark ;
    private String userBid ;

    private Long startTime;
    private Long endTime;
    private String userType;
}