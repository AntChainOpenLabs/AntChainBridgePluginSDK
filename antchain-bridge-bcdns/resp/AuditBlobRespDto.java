package org.bcdns.credential.dto.resp;

import lombok.Data;

@Data
public class AuditBlobRespDto {
    private String blobId;
    private String blob;
    private String txHash;

}
