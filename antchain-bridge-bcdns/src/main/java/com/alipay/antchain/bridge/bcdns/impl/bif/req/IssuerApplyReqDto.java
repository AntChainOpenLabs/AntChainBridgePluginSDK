package com.alipay.antchain.bridge.bcdns.impl.bif.req;

import lombok.Data;

@Data
public class IssuerApplyReqDto {


    private Integer joinType;

    private String bid;

    private String name;

    private String superNodeBid;

}
