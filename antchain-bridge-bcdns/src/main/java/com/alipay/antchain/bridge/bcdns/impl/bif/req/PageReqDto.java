package com.alipay.antchain.bridge.bcdns.impl.bif.req;

import lombok.Data;

@Data
public class PageReqDto {
    private int pageStart = 1;
    private int pageSize = 10;
    private int startNum;
}
