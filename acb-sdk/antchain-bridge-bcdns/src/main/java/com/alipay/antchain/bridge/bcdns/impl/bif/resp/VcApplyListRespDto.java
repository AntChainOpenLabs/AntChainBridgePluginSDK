/*
 * Copyright 2023 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
