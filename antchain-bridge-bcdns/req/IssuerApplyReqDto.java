package org.bcdns.credential.dto.req;

import lombok.Data;
import org.bcdns.credential.common.constant.MessageConstant;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

@Data
public class IssuerApplyReqDto {


    @NotBlank(message = MessageConstant.DESC_VALID_NULL)
    @Length(min = 1,max = 10,message = MessageConstant.DESC_VALID_STRING)
    private Integer joinType;

    @NotBlank(message = MessageConstant.DESC_VALID_NULL)
    @Length(min = 1,max = 64,message = MessageConstant.DESC_VALID_STRING)
    private String bid;

    private String name;

    @NotBlank(message = MessageConstant.DESC_VALID_NULL)
    @Length(min = 1,max = 64,message = MessageConstant.DESC_VALID_STRING)
    private String superNodeBid;

}
