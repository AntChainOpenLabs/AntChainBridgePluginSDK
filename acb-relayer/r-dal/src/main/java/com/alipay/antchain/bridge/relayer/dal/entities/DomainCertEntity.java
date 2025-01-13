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

package com.alipay.antchain.bridge.relayer.dal.entities;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("domain_cert")
public class DomainCertEntity extends BaseEntity {

    @TableField("domain")
    private String domain;

    @TableField("blockchain_product")
    private String product;

    @TableField("instance")
    private String blockchainId;

    @TableField("subject_oid")
    private byte[] subjectOid;

    @TableField("issuer_oid")
    private byte[] issuerOid;

    @TableField("domain_space")
    private String domainSpace;

    @TableField("domain_cert")
    private byte[] domainCert;
}
