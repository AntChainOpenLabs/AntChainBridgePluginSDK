package org.bcdns.credential.dto.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;

import static org.bcdns.credential.common.constant.MessageConstant.DESC_VALID_NULL;

@Data
public class MyPendingListReqDto {
    @NotBlank(message = DESC_VALID_NULL)
    private String applyerBid;
    private Integer pageStart;
    private Integer pageSize;

}
