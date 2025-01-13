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

package com.alipay.antchain.bridge.relayer.dal.repository;

import java.util.List;
import java.util.concurrent.locks.Lock;

import com.alipay.antchain.bridge.relayer.commons.constant.MarkDTTaskStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.MarkDTTaskTypeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.ActiveNode;
import com.alipay.antchain.bridge.relayer.commons.model.BizDistributedTask;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainDistributedTask;
import com.alipay.antchain.bridge.relayer.commons.model.MarkDTTask;

public interface IScheduleRepository {

    Lock getDispatchLock();

    Lock getMarkLock();

    void activate(String nodeId, String nodeIp);

    List<BlockchainDistributedTask> getAllBlockchainDistributedTasks();

    List<BizDistributedTask> getAllBizDistributedTasks();

    List<BlockchainDistributedTask> getBlockchainDistributedTasksByNodeId(String nodeId);

    List<BlockchainDistributedTask> getBlockchainDistributedTasksByBlockchain(String product, String blockchainId);

    List<BizDistributedTask> getBizDistributedTasksByNodeId(String nodeId);

    List<ActiveNode> getAllActiveNodes();

    void batchInsertBlockchainDTTasks(List<BlockchainDistributedTask> tasks);

    void batchInsertBizDTTasks(List<BizDistributedTask> tasks);

    void batchUpdateBlockchainDTTasks(List<BlockchainDistributedTask> tasks);

    void batchUpdateBizDTTasks(List<BizDistributedTask> tasks);

    void insertMarkDTTask(MarkDTTask markDTTask);

    void markForDomainRouterQuery(String senderDomain, String receiverDomain);

    boolean hasMarkDTTask(MarkDTTaskTypeEnum taskType, String uniqueKey);

    List<MarkDTTask> peekInitOrTimeoutMarkDTTask(int limit);

    void batchUpdateMarkDTTasks(List<MarkDTTask> tasks);

    List<MarkDTTask> peekReadyMarkDTTask(MarkDTTaskTypeEnum type, String nodeId, int limit);

    void updateMarkDTTaskState(MarkDTTaskTypeEnum type, String nodeId, String uniqueKey, MarkDTTaskStateEnum state);
}
