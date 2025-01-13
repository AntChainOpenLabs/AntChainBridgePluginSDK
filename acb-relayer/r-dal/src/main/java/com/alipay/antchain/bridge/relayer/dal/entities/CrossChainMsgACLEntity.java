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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("cross_chain_msg_acl")
public class CrossChainMsgACLEntity extends BaseEntity {

    @TableField("biz_id")
    private String bizId;

    @TableField("owner_domain")
    private String ownerDomain;

    @TableField("owner_identity")
    private String ownerId;

    @TableField("owner_identity_hex")
    private String ownerIdHex;

    @TableField("grant_domain")
    private String grantDomain;

    @TableField("grant_identity")
    private String grantId;

    @TableField("grant_identity_hex")
    private String grantIdHex;

    @TableField("is_deleted")
    private Integer isDeleted;
}
