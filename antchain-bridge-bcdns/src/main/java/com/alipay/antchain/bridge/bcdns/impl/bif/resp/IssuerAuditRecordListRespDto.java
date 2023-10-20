package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author skye
 * @version 1.0
 * @description:
 * @date 2021/7/26 17:07
 */
@NoArgsConstructor
@Data
public class IssuerAuditRecordListRespDto {

    private PageDTO page = new PageDTO();
    private List<IssuerAuditRecordListDTO> issuerAuditRecordList;

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
    public static class IssuerAuditRecordListDTO {

        private String applyNo;
        private String applierName;
        private String applierBid;
        private Long applyTime ;
        private Integer status;
        private Long auditTime ;
        private String auditName;
        private String auditBid;
    }
}
