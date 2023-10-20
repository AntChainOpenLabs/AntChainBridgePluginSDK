package org.bcdns.credential.dto.req;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

import static org.bcdns.credential.common.constant.MessageConstant.DESC_VALID_NULL;
import static org.bcdns.credential.common.constant.MessageConstant.DESC_VALID_STRING;

@Data
public class VcTemplateDetailReqDto {

    @NotBlank(message = DESC_VALID_NULL)
    @Length(min = 1,max = 64,message = DESC_VALID_STRING)
    private String templateBid;

}
