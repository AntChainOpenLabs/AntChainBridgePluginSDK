package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

import java.util.List;

public class VcCategoryListRespDto {


    private List<ListDTO> list;

    public List<ListDTO> getList() {
        return list;
    }

    public void setList(List<ListDTO> list) {
        this.list = list;
    }

    public static class ListDTO  {
        private String issuCategoName;
        private String issuCategoId;

        public String getIssuCategoName() {
            return issuCategoName;
        }

        public void setIssuCategoName(String issuCategoName) {
            this.issuCategoName = issuCategoName;
        }

        public String getIssuCategoId() {
            return issuCategoId;
        }

        public void setIssuCategoId(String issuCategoId) {
            this.issuCategoId = issuCategoId;
        }
    }
}
