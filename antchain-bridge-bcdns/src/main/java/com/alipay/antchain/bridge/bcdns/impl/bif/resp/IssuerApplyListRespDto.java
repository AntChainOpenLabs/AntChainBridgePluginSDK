package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

import java.util.List;

import lombok.Data;

public class IssuerApplyListRespDto {

    private PageDTO page;
    private List<ApplyListDTO> list;

    public PageDTO getPage() {
        return page;
    }

    public void setPage(PageDTO page) {
        this.page = page;
    }

    public List<ApplyListDTO> getList() {
        return list;
    }

    public void setList(List<ApplyListDTO> list) {
        this.list = list;
    }

    @Data
    public static class PageDTO {
        private int pageSize;
        private int pageStart;
        private int pageTotal;

    }
    @Data
    public static class ApplyListDTO {

        private String applyNo;

        private Long applyTime;

        private String bid;

        private String name;

        /**
         * 申请状态 0-待审核，1-审核通过，2_审核不通过
         */
        private Integer applyStatus;

        private Long auditTime;

        private String auditorBid;

    }
}
