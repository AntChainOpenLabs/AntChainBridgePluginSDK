package org.bcdns.credential.dto.resp;

import lombok.Data;

@Data
public class VcIssueRespDto {
    private String credentialId;
    private byte[] vcData;
}
