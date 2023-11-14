package org.bcdns.credential.dto.req;

import lombok.Data;

@Data
public class VcRevocationReqDto {
    private String credentialId;
    private String remark;

}
