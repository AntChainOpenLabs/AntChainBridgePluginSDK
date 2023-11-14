package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

public  class PageBean {
        /**
         * pageSize : 20
         * pageStart : 1
         * pageTotal : 4
         */

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