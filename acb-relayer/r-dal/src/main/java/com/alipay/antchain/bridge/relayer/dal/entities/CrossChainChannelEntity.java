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

import com.alipay.antchain.bridge.relayer.commons.constant.CrossChainChannelStateEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("crosschain_channel")
public class CrossChainChannelEntity extends BaseEntity {

    @TableField("local_domain")
    private String localDomain;

    @TableField("remote_domain")
    private String remoteDomain;

    @TableField("relayer_node_id")
    private String relayerNodeId;

    @TableField("state")
    private CrossChainChannelStateEnum state;
}
