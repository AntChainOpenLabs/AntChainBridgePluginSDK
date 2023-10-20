package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

import lombok.Data;

@Data
public class AuditBlobRespDto {
    private String blobId;
    private String blob;
    private String txHash;

}
