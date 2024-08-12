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

import com.alipay.antchain.bridge.bcdns.embedded.types.enums.ApplicationStateEnum;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateTypeEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@TableName("embedded_bcdns_cert_application")
public class CertApplicationEntity extends BaseEntity {

    @TableField("cert_type")
    private String certType;

    @TableField("raw_csr")
    private byte[] rawCsr;

    @TableField("receipt")
    private String receipt;

    @TableField("state")
    private String state;

    public ApplicationStateEnum getStateEnum() {
        return ApplicationStateEnum.valueOf(state);
    }

    public CrossChainCertificateTypeEnum getCertTypeEnum() {
        return CrossChainCertificateTypeEnum.valueOf(certType);
    }
}
