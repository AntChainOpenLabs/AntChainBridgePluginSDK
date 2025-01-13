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

package com.alipay.antchain.bridge.relayer.dal.repository.impl;

import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.cache.Cache;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.constant.DTActiveNodeStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.MarkDTTaskStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.MarkDTTaskTypeEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.*;
import com.alipay.antchain.bridge.relayer.dal.entities.BizDTTaskEntity;
import com.alipay.antchain.bridge.relayer.dal.entities.BlockchainDTTaskEntity;
import com.alipay.antchain.bridge.relayer.dal.entities.DTActiveNodeEntity;
import com.alipay.antchain.bridge.relayer.dal.entities.MarkDTTaskEntity;
import com.alipay.antchain.bridge.relayer.dal.mapper.BizDTTaskMapper;
import com.alipay.antchain.bridge.relayer.dal.mapper.BlockchainDTTaskMapper;
import com.alipay.antchain.bridge.relayer.dal.mapper.DTActiveNodeMapper;
import com.alipay.antchain.bridge.relayer.dal.mapper.MarkDTTaskMapper;
import com.alipay.antchain.bridge.relayer.dal.repository.IScheduleRepository;
import com.alipay.antchain.bridge.relayer.dal.utils.ConvertUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class ScheduleRepository implements IScheduleRepository {

    private static final String SCHEDULE_LOCK_KEY = "RELAYER_SCHEDULE_LOCK";

    private static final String MARK_LOCK = "MARK_LOCK";

    @Resource
    private RedissonClient redisson;

    @Resource
    private DTActiveNodeMapper dtActiveNodeMapper;

    @Resource
    private BlockchainDTTaskMapper blockchainDtTaskMapper;

    @Resource
    private BizDTTaskMapper bizDTTaskMapper;

    @Resource
    private MarkDTTaskMapper markDTTaskMapper;

    @Resource
    private Cache<String, Boolean> markTaskCache;

    @Override
    public Lock getDispatchLock() {
        return redisson.getLock(SCHEDULE_LOCK_KEY);
    }

    @Override
    public Lock getMarkLock() {
        return redisson.getLock(MARK_LOCK);
    }

    @Override
    @Synchronized
    public void activate(String nodeId, String nodeIp) {
        try {
            if (
                    1 != dtActiveNodeMapper.update(
                            DTActiveNodeEntity.builder()
                                    .nodeId(nodeId)
                                    .nodeIp(nodeIp)
                                    .state(DTActiveNodeStateEnum.ONLINE)
                                    .build(),
                            new LambdaUpdateWrapper<DTActiveNodeEntity>()
                                    .eq(DTActiveNodeEntity::getNodeId, nodeId)
                    )
            ) {
                dtActiveNodeMapper.insert(
                        DTActiveNodeEntity.builder()
                                .nodeId(nodeId)
                                .nodeIp(nodeIp)
                                .state(DTActiveNodeStateEnum.ONLINE)
                                .build()
                );
            }
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    StrUtil.format("failed to activate node ( id: {}, ip: {} )", nodeId, nodeIp),
                    e
            );
        }
    }

    @Override
    public List<BlockchainDistributedTask> getAllBlockchainDistributedTasks() {
        try {
            return blockchainDtTaskMapper.selectList(null).stream()
                    .map(ConvertUtil::convertFromBlockchainDTTaskEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_TASK_ERROR,
                    "failed to get all distributed tasks",
                    e
            );
        }
    }

    @Override
    public List<BlockchainDistributedTask> getBlockchainDistributedTasksByNodeId(String nodeId) {
        try {
            return blockchainDtTaskMapper.selectList(
                            new LambdaQueryWrapper<BlockchainDTTaskEntity>()
                                    .eq(BlockchainDTTaskEntity::getNodeId, nodeId)
                    ).stream()
                    .map(ConvertUtil::convertFromBlockchainDTTaskEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_TASK_ERROR,
                    "failed to get distributed tasks for node " + nodeId,
                    e
            );
        }
    }

    @Override
    public List<BlockchainDistributedTask> getBlockchainDistributedTasksByBlockchain(String product, String blockchainId) {
        try {
            return blockchainDtTaskMapper.selectList(
                            new LambdaQueryWrapper<BlockchainDTTaskEntity>()
                                    .eq(BlockchainDTTaskEntity::getProduct, product)
                                    .eq(BlockchainDTTaskEntity::getBlockchainId, blockchainId)
                    ).stream()
                    .map(ConvertUtil::convertFromBlockchainDTTaskEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_TASK_ERROR,
                    e,
                    "failed to get distributed tasks for blockchain {}-{} ",
                    product, blockchainId
            );
        }
    }

    @Override
    public List<ActiveNode> getAllActiveNodes() {
        try {
            return dtActiveNodeMapper.selectList(null).stream()
                    .map(ConvertUtil::convertFromDTActiveNodeEntityActiveNode)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    "failed to get all active nodes",
                    e
            );
        }
    }

    @Override
    public void batchInsertBlockchainDTTasks(List<BlockchainDistributedTask> tasks) {
        try {
            blockchainDtTaskMapper.saveDTTasks(tasks);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    StrUtil.format(
                            "failed to save distributed tasks {}",
                            tasks.stream().map(BlockchainDistributedTask::getUniqueTaskKey)
                    ),
                    e
            );
        }
    }

    @Override
    @Transactional(rollbackFor = AntChainBridgeRelayerException.class)
    public void batchUpdateBlockchainDTTasks(List<BlockchainDistributedTask> tasks) {
        try {
            tasks.forEach(
                    task -> blockchainDtTaskMapper.update(
                            BlockchainDTTaskEntity.builder()
                                    .nodeId(task.getNodeId())
                                    .timeSlice(new Date(task.getStartTime()))
                                    .build(),
                            new LambdaUpdateWrapper<BlockchainDTTaskEntity>()
                                    .eq(BlockchainDTTaskEntity::getTaskType, task.getTaskType())
                                    .eq(BlockchainDTTaskEntity::getProduct, task.getBlockchainProduct())
                                    .eq(BlockchainDTTaskEntity::getBlockchainId, task.getBlockchainId())
                    )
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    StrUtil.format(
                            "failed to save distributed tasks {}",
                            tasks.stream().map(BlockchainDistributedTask::getUniqueTaskKey)
                    ),
                    e
            );
        }
    }

    @Override
    public List<BizDistributedTask> getAllBizDistributedTasks() {
        try {
            return bizDTTaskMapper.selectList(null).stream()
                    .map(ConvertUtil::convertFromBizDTTaskEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_TASK_ERROR,
                    "failed to get all biz distributed tasks",
                    e
            );
        }
    }

    @Override
    public List<BizDistributedTask> getBizDistributedTasksByNodeId(String nodeId) {
        try {
            return bizDTTaskMapper.selectList(
                            new LambdaQueryWrapper<BizDTTaskEntity>()
                                    .eq(BizDTTaskEntity::getNodeId, nodeId)
                    ).stream()
                    .map(ConvertUtil::convertFromBizDTTaskEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_TASK_ERROR,
                    "failed to get biz distributed tasks for node " + nodeId,
                    e
            );
        }
    }

    @Override
    @Transactional(rollbackFor = AntChainBridgeRelayerException.class)
    public void batchInsertBizDTTasks(List<BizDistributedTask> tasks) {
        try {
            tasks.forEach(
                    bizDistributedTask -> bizDTTaskMapper.insert(
                            ConvertUtil.convertFromBizDistributedTask(
                                    bizDistributedTask
                            )
                    )
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    StrUtil.format(
                            "failed to save distributed tasks {}",
                            tasks.stream().map(IDistributedTask::getUniqueTaskKey)
                    ),
                    e
            );
        }
    }

    @Override
    @Transactional(rollbackFor = AntChainBridgeRelayerException.class)
    public void batchUpdateBizDTTasks(List<BizDistributedTask> tasks) {
        try {
            tasks.forEach(
                    task -> bizDTTaskMapper.update(
                            BizDTTaskEntity.builder()
                                    .nodeId(task.getNodeId())
                                    .timeSlice(new Date(task.getStartTime()))
                                    .build(),
                            new LambdaUpdateWrapper<BizDTTaskEntity>()
                                    .eq(BizDTTaskEntity::getTaskType, task.getTaskType())
                                    .eq(BizDTTaskEntity::getUniqueKey, task.getUniqueKey())
                    )
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    StrUtil.format(
                            "failed to save distributed tasks {}",
                            tasks.stream().map(IDistributedTask::getUniqueTaskKey)
                    ),
                    e
            );
        }
    }

    public void markForDomainRouterQuery(String senderDomain, String receiverDomain) {
        if (!StrUtil.isAllNotEmpty(senderDomain, receiverDomain)) {
            throw new RuntimeException(StrUtil.format("empty sender domain {} or receiver domain {}", senderDomain, receiverDomain));
        }
        String uniqueKey = DomainRouterQueryMarkDTTask.generateDomainRouterQueryTaskUniqueKey(senderDomain, receiverDomain);
        log.info("try to start a mark task for domain router query: {} ", uniqueKey);
        if (hasMarkDTTask(MarkDTTaskTypeEnum.DOMAIN_ROUTER_QUERY, uniqueKey)) {
            log.warn("mark task for domain router query already exist: {} ", uniqueKey);
            return;
        }
        MarkDTTask task = new MarkDTTask(MarkDTTaskTypeEnum.DOMAIN_ROUTER_QUERY, uniqueKey);
        task.setState(MarkDTTaskStateEnum.INIT);
        insertMarkDTTask(task);
    }

    @Override
    public void insertMarkDTTask(MarkDTTask markDTTask) {
        try {
            markDTTaskMapper.insert(ConvertUtil.convertFromMarkDTTask(markDTTask));
            markTaskCache.put(generateMarkTaskCacheKey(markDTTask.getTaskType(), markDTTask.getUniqueKey()), true);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    StrUtil.format(
                            "failed to save mark task {}-{}",
                            markDTTask.getTaskType().name(), markDTTask.getUniqueKey()
                    ),
                    e
            );
        }
    }

    @Override
    public boolean hasMarkDTTask(MarkDTTaskTypeEnum taskType, String uniqueKey) {
        if (markTaskCache.containsKey(generateMarkTaskCacheKey(taskType, uniqueKey))) {
            return true;
        }
        boolean res = markDTTaskMapper.exists(
                new LambdaQueryWrapper<MarkDTTaskEntity>()
                        .eq(MarkDTTaskEntity::getTaskType, taskType)
                        .eq(MarkDTTaskEntity::getUniqueKey, uniqueKey)
        );
        if (res) {
            markTaskCache.put(generateMarkTaskCacheKey(taskType, uniqueKey), true);
        }
        return res;
    }

    @Override
    public List<MarkDTTask> peekInitOrTimeoutMarkDTTask(int limit) {
        try {
            List<MarkDTTaskEntity> entities = markDTTaskMapper.selectList(
                    new LambdaQueryWrapper<MarkDTTaskEntity>()
                            .eq(MarkDTTaskEntity::getState, MarkDTTaskStateEnum.INIT)
                            .or(
                                    wrapper -> wrapper.lt(MarkDTTaskEntity::getEndTime, new Date())
                                            .ne(MarkDTTaskEntity::getState, MarkDTTaskStateEnum.DONE)
                            )
            );
            if (ObjectUtil.isEmpty(entities)) {
                return ListUtil.empty();
            }
            return entities.stream().map(ConvertUtil::convertFromMarkDTTaskEntity).collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    "failed to peek init or timeout mark tasks",
                    e
            );
        }
    }

    @Override
    public void batchUpdateMarkDTTasks(List<MarkDTTask> tasks) {
        try {
            if (
                    tasks.stream().map(
                            task -> markDTTaskMapper.update(
                                    ConvertUtil.convertFromMarkDTTask(task),
                                    new LambdaUpdateWrapper<MarkDTTaskEntity>()
                                            .eq(MarkDTTaskEntity::getTaskType, task.getTaskType())
                                            .eq(MarkDTTaskEntity::getUniqueKey, task.getUniqueKey())
                            )
                    ).reduce(Integer::sum).orElse(0) != tasks.size()
            ) {
                throw new RuntimeException("failed to update multi mark tasks to DB");
            }
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    "failed to batch update mark tasks",
                    e
            );
        }
    }

    @Override
    public List<MarkDTTask> peekReadyMarkDTTask(MarkDTTaskTypeEnum type, String nodeId, int limit) {
        try {
            List<MarkDTTaskEntity> entities = markDTTaskMapper.selectList(
                    new LambdaQueryWrapper<MarkDTTaskEntity>()
                            .eq(MarkDTTaskEntity::getState, MarkDTTaskStateEnum.READY)
                            .eq(MarkDTTaskEntity::getTaskType, type)
                            .eq(MarkDTTaskEntity::getNodeId, nodeId)
                            .last("limit " + limit)
            );
            if (ObjectUtil.isEmpty(entities)) {
                return null;
            }
            return entities.stream().map(ConvertUtil::convertFromMarkDTTaskEntity).collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    e,
                    "failed to peek ready mark tasks for (type: {}, node_id: {}, limit: {})",
                    type.name(), nodeId, limit
            );
        }
    }

    @Override
    public void updateMarkDTTaskState(MarkDTTaskTypeEnum type, String nodeId, String uniqueKey, MarkDTTaskStateEnum state) {
        try {
            MarkDTTaskEntity entity = new MarkDTTaskEntity();
            entity.setState(state);
            if (
                    markDTTaskMapper.update(
                            entity,
                            new LambdaUpdateWrapper<MarkDTTaskEntity>()
                                    .eq(MarkDTTaskEntity::getTaskType, type)
                                    .eq(MarkDTTaskEntity::getUniqueKey, uniqueKey)
                                    .eq(MarkDTTaskEntity::getNodeId, nodeId)
                    ) != 1
            ) {
                throw new RuntimeException("update to DB failed");
            }
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    e,
                    "failed to batch update mark task state to {} for (type: {}, node_id: {}, unique_key: {})",
                    state.name(), type, nodeId, uniqueKey
            );
        }
    }

    private String generateMarkTaskCacheKey(MarkDTTaskTypeEnum taskType, String uniqueKey) {
        return StrUtil.format("{}-{}", taskType.name(), uniqueKey);
    }
}
