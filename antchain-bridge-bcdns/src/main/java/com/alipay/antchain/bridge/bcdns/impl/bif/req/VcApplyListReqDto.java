package org.bcdns.credential.dto.req;

import lombok.Data;
@Data
public class VcApplyListReqDto extends PageReqDto {
    private Integer[] status;
}
