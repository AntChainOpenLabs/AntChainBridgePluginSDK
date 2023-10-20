package org.bcdns.credential.dto.req;

import lombok.Data;

@Data
public class VcIssueAuditSignReqDto {


    private String auditBid;

    private String payloadId;

    private String signPayload;

    private String signBcTxBlob;

    private String publicKey;

}
