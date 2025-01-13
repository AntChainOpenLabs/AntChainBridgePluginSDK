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

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@TableName("anchor_process")
@AllArgsConstructor
@NoArgsConstructor
public class AnchorProcessEntity extends BaseEntity {

    public AnchorProcessEntity(String product, String blockchainId, String task, Long blockHeight) {
        this.product = product;
        this.blockchainId = blockchainId;
        this.task = task;
        this.blockHeight = blockHeight;
    }

    @TableField("blockchain_product")
    private String product;

    @TableField("instance")
    private String blockchainId;

    @TableField("task")
    private String task;

    @TableField("tpbta_lane_key")
    private String tpbtaLaneKey;

    @TableField("block_height")
    private Long blockHeight;

    @TableField("block_timestamp")
    //TODO: 还没有应用到逻辑中
    private Date blockTimestamp;
}
