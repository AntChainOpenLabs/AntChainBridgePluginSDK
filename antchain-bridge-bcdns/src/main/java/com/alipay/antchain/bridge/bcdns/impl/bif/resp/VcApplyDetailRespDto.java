package com.alipay.antchain.bridge.bcdns.impl.bif.resp;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class VcApplyDetailRespDto {
    private byte[] content;
    private String status;
    private String applyNo;
    private byte[] applyUser;
    private Long applyTime;
    private byte[] auditId;
    private Long auditTime;
    private String auditRemark;
}