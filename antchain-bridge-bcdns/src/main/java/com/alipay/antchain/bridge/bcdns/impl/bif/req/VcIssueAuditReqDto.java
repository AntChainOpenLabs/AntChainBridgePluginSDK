package org.bcdns.credential.dto.req;

import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import static org.bcdns.credential.common.constant.MessageConstant.*;

@Data
public class VcIssueAuditReqDto {
    @NotBlank(message = DESC_VALID_NULL)
    @Length(min = 1, max = 64, message = DESC_VALID_STRING)
    private String applyNo;

    @NotNull(message = DESC_VALID_NULL)
    @Range(min = 2, max = 3, message = DESC_VALID_NUMBER)
    private Integer status;

    private String reason;
}
