package com.alipay.antchain.bridge.bcdns.impl.bif.req;

import lombok.Data;

@Data
public class VcIssueAuditSignReqDto {

    private String auditBid;

    private String payloadId;

    private String signPayload;

    private String signBcTxBlob;

    private String publicKey;

}
