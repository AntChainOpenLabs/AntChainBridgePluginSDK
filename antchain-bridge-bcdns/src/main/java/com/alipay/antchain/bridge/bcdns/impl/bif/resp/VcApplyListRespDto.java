package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor
@Data
public class VcApplyListRespDto {
    private PageDTO page = new PageDTO();
    private List<IssueListDTO> dataList;

    @NoArgsConstructor
    @Data
    public static class PageDTO {
        /**
         * pageSize : 20
         * pageStart : 1
         * pageTotal : 4
         */

        private Integer pageSize;
        private Integer pageStart;
        private Integer pageTotal;
    }

    @NoArgsConstructor
    @Data
    public static class IssueListDTO {
        private String applyNo;
        private String credentialId;
        private String status;
        private byte[] userId;
        private Long createTime;
        private Integer credentialType;
        private Long auditTime;
        private Integer isDownload;
    }
}
