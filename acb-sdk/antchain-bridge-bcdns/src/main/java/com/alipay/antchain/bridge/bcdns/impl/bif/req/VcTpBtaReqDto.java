package com.alipay.antchain.bridge.bcdns.impl.bif.req;

import lombok.Data;

@Data
public class VcTpBtaReqDto {
    private String vcId;

    private byte[] tpbta;

    private Integer credentialType;

    private String publicKey;

    private String signAlgo;

    private byte[] sign;
}
