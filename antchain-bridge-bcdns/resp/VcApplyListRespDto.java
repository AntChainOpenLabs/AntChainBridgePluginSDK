package org.bcdns.credential.dto.resp;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author skye
 * @version 1.0
 * @description:
 * @date 2021/6/3 15:00
 */
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
        private String credentialBid;
        private String name;
        private String categoryName;
        private String status;
        private String userName;
        private Long createTime;
        private String templateBid;
        private String auditName;
        private Long auditTime;
        private Long dealTime;
        private String userBid;
        private String version;
        private Integer isDownload;
    }
}
