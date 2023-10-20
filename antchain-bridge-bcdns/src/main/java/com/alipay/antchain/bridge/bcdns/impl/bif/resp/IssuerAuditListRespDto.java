package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

import java.util.List;


public class IssuerAuditListRespDto {

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

    public static class PageDTO {
        private int pageSize;
        private int pageStart;
        private int pageTotal;

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public int getPageStart() {
            return pageStart;
        }

        public void setPageStart(int pageStart) {
            this.pageStart = pageStart;
        }

        public int getPageTotal() {
            return pageTotal;
        }

        public void setPageTotal(int pageTotal) {
            this.pageTotal = pageTotal;
        }
    }

    public static class ListDTO {
        private String applyNo;
        private String issuerName;
        private long applyTime;
        private String status;
        private String issuerBid;

        public String getIssuerBid() {
            return issuerBid;
        }

        public void setIssuerBid(String issuerBid) {
            this.issuerBid = issuerBid;
        }

        public String getApplyNo() {
            return applyNo;
        }

        public void setApplyNo(String applyNo) {
            this.applyNo = applyNo;
        }

        public String getIssuerName() {
            return issuerName;
        }

        public void setIssuerName(String issuerName) {
            this.issuerName = issuerName;
        }

        public long getApplyTime() {
            return applyTime;
        }

        public void setApplyTime(long applyTime) {
            this.applyTime = applyTime;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }


}
