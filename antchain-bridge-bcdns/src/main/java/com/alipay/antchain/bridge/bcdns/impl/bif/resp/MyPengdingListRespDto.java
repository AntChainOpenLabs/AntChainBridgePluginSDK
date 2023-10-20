package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

import java.util.List;

import lombok.Data;

@Data
public class MyPengdingListRespDto {

    private List<VcApplyBean> applyList;
    private PageBean page;

    @Data
    public static class VcApplyBean{
        private String applyNo;
        private String templateId;
        private String templateName;
        private String content;
        private String applyerBid;
    }
}
