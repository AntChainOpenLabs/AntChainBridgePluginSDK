package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

import lombok.Data;

@Data
public class QueryStatusRespDto {
    private String status;
    private String credentialId;
    private Integer type;
    private String userBid;
}
