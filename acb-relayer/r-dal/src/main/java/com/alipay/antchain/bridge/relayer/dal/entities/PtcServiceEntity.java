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

package com.alipay.antchain.bridge.relayer.dal.entities;

import com.alipay.antchain.bridge.relayer.commons.constant.PtcServiceStateEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@TableName("ptc_service")
@AllArgsConstructor
@NoArgsConstructor
public class PtcServiceEntity extends BaseEntity {

    @TableField("service_id")
    private String serviceId;

    @TableField("type")
    private String type;

    @TableField("owner_id_hex")
    private String ownerIdHex;

    @TableField("issuer_bcdns_domain_space")
    private String issuerBcdnsDomainSpace;

    @TableField("state")
    private PtcServiceStateEnum state;

    @TableField("ptc_cert")
    private byte[] ptcCert;

    @TableField("client_config")
    private byte[] clientConfig;
}
