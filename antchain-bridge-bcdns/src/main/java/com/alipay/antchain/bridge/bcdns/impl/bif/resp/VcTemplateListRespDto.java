package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

import java.util.List;

import lombok.Data;

public class VcTemplateListRespDto {

    private PageDTO page;
    private List<ListDTO> list;

    public PageDTO getPage() {
        return page;
    }

    public void setPage(PageDTO page) {
        this.page = page;
    }

    public List<ListDTO> getList() {
        return list;
    }

    public void setList(List<ListDTO> list) {
        this.list = list;
    }

    @Data
    public static class PageDTO {
        private int pageSize;
        private int pageStart;
        private int pageTotal;

    }

    @Data
    public static class ListDTO {
        private String templateName;
        private String templateId;
        private String industryName;
        private String categoryName;
        private String version;
        private String remark;
        private String userType;
        private String templateBid;
        private String applyNo;
        private Integer auditStatus;
        private Long auditTime;
        private Long createTime;


    }
}
