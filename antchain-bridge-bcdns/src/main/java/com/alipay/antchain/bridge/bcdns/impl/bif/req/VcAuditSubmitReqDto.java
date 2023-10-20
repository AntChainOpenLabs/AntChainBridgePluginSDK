package com.alipay.antchain.bridge.bcdns.impl.bif.req;

import lombok.Data;

@Data
public class VcAuditSubmitReqDto {

    private String blobId;
    private String signBlob;
    private String publicKey;


}
