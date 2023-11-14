package org.bcdns.credential.dto.resp;

import lombok.Data;

import java.util.List;

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
