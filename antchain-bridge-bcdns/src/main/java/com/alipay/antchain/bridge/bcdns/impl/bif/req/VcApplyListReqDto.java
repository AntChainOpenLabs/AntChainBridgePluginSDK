package com.alipay.antchain.bridge.bcdns.impl.bif.req;

import lombok.Data;

/**
 * @author skye
 * @version 1.0
 * @description:
 * @date 2021/6/3 14:33
 */
@Data
public class VcApplyListReqDto extends PageReqDto {

    private String name;
    private String issuerBid;
    private Integer credentialType;
    private String superNode;
    private String userName;
    private String category;
    private Integer[] status;
    private String userBid;
    private String pdpBid;
    private String applyNo;
}
