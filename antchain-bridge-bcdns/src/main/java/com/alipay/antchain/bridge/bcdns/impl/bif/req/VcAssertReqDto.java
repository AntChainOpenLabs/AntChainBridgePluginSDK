package com.alipay.antchain.bridge.bcdns.impl.bif.req;

import lombok.Data;

@Data
public class VcAssertReqDto {
    private String bid;

    private String templateId;

    private int hold;//0未持有1持有
}
