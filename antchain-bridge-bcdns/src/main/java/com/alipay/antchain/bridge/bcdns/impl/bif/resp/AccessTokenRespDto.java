package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AccessTokenRespDto {
    private String accessToken;
    private Integer expireIn;
}
