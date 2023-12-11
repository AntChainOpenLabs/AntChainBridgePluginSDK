package com.alipay.antchain.bridge.bcdns.impl.bif.req;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class AccessTokenReqDto {
    private String apiKey;

    private String apiSecret;

    private String issuerId;
}
