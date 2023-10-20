package com.alipay.antchain.bridge.bcdns.impl.bif.req;

import lombok.Data;

@Data
public class RechageBlobReqDto {
    private String chainCode;
    private String address;
    private String amount;
}