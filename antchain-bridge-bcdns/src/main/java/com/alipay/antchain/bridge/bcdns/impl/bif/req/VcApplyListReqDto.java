package com.alipay.antchain.bridge.bcdns.impl.bif.req;

import lombok.Data;

@Data
public class VcApplyListReqDto extends PageReqDto {
    private Integer[] status;
}
