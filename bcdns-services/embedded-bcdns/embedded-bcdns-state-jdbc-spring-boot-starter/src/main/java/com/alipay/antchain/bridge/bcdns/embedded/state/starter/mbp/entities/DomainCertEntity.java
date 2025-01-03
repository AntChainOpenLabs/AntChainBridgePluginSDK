/*
 * Copyright 2024 Ant Group
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

package com.alipay.antchain.bridge.bcdns.embedded.state.starter.mbp.entities;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@TableName("embedded_bcdns_domain_cert")
public class DomainCertEntity extends BaseEntity {

    @TableField("domain")
    private String domain;

    @TableField("domain_space")
    private String domainSpace;

    @TableField("subject_oid")
    private byte[] subjectOid;

    @TableField("issuer_oid")
    private byte[] issuerOid;

    @TableField("domain_cert")
    private byte[] domainCert;
}
