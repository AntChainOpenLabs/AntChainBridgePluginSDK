package com.alipay.antchain.bridge.bcdns.impl.bif.req;

import lombok.Data;

@Data
public class MyPendingListReqDto {
    private String applyerBid;
    private Integer pageStart;
    private Integer pageSize;

}
