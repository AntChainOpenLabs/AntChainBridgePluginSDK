package org.bcdns.credential.dto.resp;

import lombok.Data;

import java.util.List;

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
