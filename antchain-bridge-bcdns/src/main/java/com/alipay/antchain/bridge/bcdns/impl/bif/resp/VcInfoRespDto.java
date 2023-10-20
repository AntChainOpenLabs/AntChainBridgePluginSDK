package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

import lombok.Data;

@Data
public class VcInfoRespDto {

    private String jws;
    private String vc;
    private String issueBid;
    private String issueName;
}
