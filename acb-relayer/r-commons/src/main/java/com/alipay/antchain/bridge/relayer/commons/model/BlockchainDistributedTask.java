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

package com.alipay.antchain.bridge.relayer.commons.model;

import java.util.Date;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainDistributedTaskTypeEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BlockchainDistributedTask implements IDistributedTask {

    private String nodeId = StrUtil.EMPTY;

    private BlockchainDistributedTaskTypeEnum taskType;

    private String blockchainProduct;

    private String blockchainId;

    private String ext = StrUtil.EMPTY;

    private long startTime = 0;

    private long timeSliceLength = 0;

    public BlockchainDistributedTask(BlockchainDistributedTaskTypeEnum taskType, String blockchainProduct, String blockchainId) {
        this.taskType = taskType;
        this.blockchainProduct = blockchainProduct;
        this.blockchainId = blockchainId;
    }

    public BlockchainDistributedTask(
            String nodeId,
            BlockchainDistributedTaskTypeEnum taskType,
            String blockchainProduct,
            String blockchainId,
            String ext,
            long startTime
    ) {
        this.nodeId = nodeId;
        this.taskType = taskType;
        this.blockchainProduct = blockchainProduct;
        this.blockchainId = blockchainId;
        this.ext = ext;
        this.startTime = startTime;
    }

    public boolean ifFinish() {
        return (System.currentTimeMillis() - this.startTime) > timeSliceLength;
    }

    public String getUniqueTaskKey() {
        return taskType.getCode() + "_" + blockchainId;
    }

    public Date getTimeSlice() {
        return new Date(startTime);
    }
}
