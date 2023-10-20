package org.bcdns.credential.dto.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class QueryMyCredentialAuditingParam {
    @NotBlank(message = "userBid不能为空")
    private String userBid;
}
