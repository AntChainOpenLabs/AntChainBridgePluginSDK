package org.bcdns.credential.dto.resp;

import lombok.Data;

@Data
public class IssueAuditRespDto {

    private String payload;

    private String payloadId;

    private String bcTxBlob;


}