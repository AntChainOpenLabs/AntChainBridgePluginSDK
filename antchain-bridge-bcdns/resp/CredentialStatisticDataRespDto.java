package org.bcdns.credential.dto.resp;

import lombok.Data;

@Data
public class CredentialStatisticDataRespDto {
    private Integer issuerCount;
    private Integer tempalteCount;
    private Integer credentialCount;

}
