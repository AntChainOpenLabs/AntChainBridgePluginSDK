package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

import lombok.Data;

@Data
public class BasicInfoRespDto {

    private String bid;
    private String certBid;
    private String company;
    private Integer userType;
    private String realName;
    private String publicKey;
    private String superNodeBid;
    private String userNumber;
    private boolean trustedFlag;
}
