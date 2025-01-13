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

import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgTrustLevelEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.UpperProtocolTypeBeyondAMEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("auth_msg_archive")
public class AuthMsgArchiveEntity extends BaseEntity {

    @TableField("ucp_id")
    private String ucpId;

    @TableField("blockchain_product")
    private String product;

    @TableField("blockchain_id")
    private String blockchainId;

    @TableField("domain_name")
    private String domain;

    @TableField("amclient_contract_address")
    private String amClientContractAddress;

    @TableField("version")
    private Integer version;

    @TableField("msg_sender")
    private String msgSender;

    @TableField("protocol_type")
    private UpperProtocolTypeBeyondAMEnum protocolType;

    @TableField("trust_level")
    private AuthMsgTrustLevelEnum trustLevel;

    @TableField("payload")
    private byte[] payload;

    @TableField("process_state")
    private AuthMsgProcessStateEnum processState;

    @TableField("ext")
    private byte[] ext;
}
