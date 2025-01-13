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

import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainDistributedTaskTypeEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Getter
@Setter
@TableName("blockchain_dt_task")
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class BlockchainDTTaskEntity extends BaseEntity {

    @TableField("node_id")
    private String nodeId;

    @TableField("task_type")
    private BlockchainDistributedTaskTypeEnum taskType;

    @TableField("blockchain_product")
    private String product;

    @TableField("blockchain_id")
    private String blockchainId;

    @TableField("ext")
    private String ext;

    @TableField("timeslice")
    private Date timeSlice;
}
