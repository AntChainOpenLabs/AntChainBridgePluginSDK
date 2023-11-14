package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

import java.util.List;

import lombok.Data;

@Data
public class PageRespDto<T> {
    private List<T> dataList;
    private PageBean page;

    public PageRespDto(List<T> dataList, PageBean pageDto) {
        this.dataList = dataList;
        this.page = pageDto;
    }

    public PageRespDto() {
    }
}
