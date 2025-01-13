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
import lombok.*;

@Getter
@Setter
@TableName("embedded_bcdns_ptc_trust_root")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PtcTrustRootEntity extends BaseEntity {

    @TableField("ptc_oid_hex")
    private String ptcOidHex;

    @TableField("latest_verify_anchor")
    private String latestVerifyAnchor;

    @TableField("issuer_bcdns_domain_space")
    private String issuerBcdnsDomainSpace;

    @TableField("network_info")
    private byte[] networkInfo;

    @TableField("ptc_crosschain_cert")
    private byte[] ptcCrosschainCert;

    @TableField("sign_algo")
    private String signAlgo;

    @TableField("sig")
    private byte[] sig;
}
