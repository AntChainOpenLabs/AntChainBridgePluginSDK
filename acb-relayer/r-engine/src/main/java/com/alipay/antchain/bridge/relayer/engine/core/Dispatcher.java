package com.alipay.antchain.bridge.relayer.engine.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.relayer.commons.constant.BizDistributedTaskTypeEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainDistributedTaskTypeEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.PluginServerStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.*;
import com.alipay.antchain.bridge.relayer.core.manager.bbc.IBBCPluginManager;
import com.alipay.antchain.bridge.relayer.core.manager.blockchain.IBlockchainManager;
import com.alipay.antchain.bridge.relayer.dal.repository.IScheduleRepository;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Dispatcher负责拆分区块链任务，并根据节点心跳表获取在线节点排值班表
 */
@Component
@Slf4j
public class Dispatcher {

    @Resource
    private IBlockchainManager blockchainManager;

    @Resource
    private IBBCPluginManager bbcPluginManager;

    @Resource
    private IScheduleRepository scheduleRepository;

    @Value("#{duty.timeSliceLength}")
    private long timeSliceLength;

    @Getter
    @Value("${relayer.engine.schedule.activate.ttl:5000}")
    private long nodeTimeToLive;

//    @Value("${relayer.engine.schedule.dispatcher.task_diff_map:{anchor:5, committer:3, process:2}}")
//    private Map<String, Integer> taskTypeDiffMap;

    public void dispatch() {
        Lock lock = getDistributeLock();
        if (!lock.tryLock()) {
            log.debug("not my dispatch lock.");
            return;
        }

        try {
            log.debug("dispatch distributed tasks now.");

            List<IDistributedTask> tasks = new ArrayList<>(splitBlockchainTask(getRunningBlockchains()));
            tasks.add(
                    new BizDistributedTask(
                            BizDistributedTaskTypeEnum.DOMAIN_APPLICATION_QUERY,
                            BizDistributedTaskTypeEnum.DOMAIN_APPLICATION_QUERY.getCode()
                    )
            );

            // 剔除已分配过时间片的任务
            List<IDistributedTask> tasksToDispatch = filterTasksInTimeSlice(tasks);
            if (ObjectUtil.isEmpty(tasksToDispatch)) {
                log.debug("empty tasks to dispatch");
                return;
            }

            // 获取在线节点
            List<ActiveNode> onlineNodes = getOnlineNode();
            if (ObjectUtil.isEmpty(onlineNodes)) {
                log.warn("none online nodes!");
                return;
            }
            log.debug("size of online node : {}", onlineNodes.size());

            // 给剩余任务分配时间片
            doDispatch(onlineNodes, tasksToDispatch);
        } catch (Exception e) {
            log.error("failed to dispatch distributed task: ", e);
        } finally {
            lock.unlock();
        }
    }

    private Lock getDistributeLock() {
        return scheduleRepository.getDispatchLock();
    }

    @Synchronized
    private List<BlockchainMeta> getRunningBlockchains() {

        List<BlockchainMeta> blockchainMetas = blockchainManager.getAllServingBlockchains();
        if (ObjectUtil.isNull(blockchainMetas)) {
            return ListUtil.empty();
        }
        return blockchainMetas.stream().filter(
                blockchainMeta ->
                        PluginServerStateEnum.READY == bbcPluginManager.getPluginServerState(
                                blockchainMeta.getProperties().getPluginServerId()
                        )
        ).collect(Collectors.toList());
    }

    private List<BlockchainDistributedTask> splitBlockchainTask(List<BlockchainMeta> runningBlockchains) {
        return runningBlockchains.stream().map(
                blockchainMeta ->
                        ListUtil.toList(
                                new BlockchainDistributedTask(
                                        BlockchainDistributedTaskTypeEnum.ANCHOR_TASK,
                                        blockchainMeta.getProduct(),
                                        blockchainMeta.getBlockchainId()
                                ),
                                new BlockchainDistributedTask(
                                        BlockchainDistributedTaskTypeEnum.COMMIT_TASK,
                                        blockchainMeta.getProduct(),
                                        blockchainMeta.getBlockchainId()
                                ),
                                new BlockchainDistributedTask(
                                        BlockchainDistributedTaskTypeEnum.VALIDATION_TASK,
                                        blockchainMeta.getProduct(),
                                        blockchainMeta.getBlockchainId()
                                ),
                                new BlockchainDistributedTask(
                                        BlockchainDistributedTaskTypeEnum.PROCESS_TASK,
                                        blockchainMeta.getProduct(),
                                        blockchainMeta.getBlockchainId()
                                ),
                                new BlockchainDistributedTask(
                                        BlockchainDistributedTaskTypeEnum.AM_CONFIRM_TASK,
                                        blockchainMeta.getProduct(),
                                        blockchainMeta.getBlockchainId()
                                ),
                                new BlockchainDistributedTask(
                                        BlockchainDistributedTaskTypeEnum.RELIABLE_RELAY_TASK,
                                        blockchainMeta.getProduct(),
                                        blockchainMeta.getBlockchainId()
                                ),
                                new BlockchainDistributedTask(
                                        BlockchainDistributedTaskTypeEnum.ARCHIVE_TASK,
                                        blockchainMeta.getProduct(),
                                        blockchainMeta.getBlockchainId()
                                ),
                                new BlockchainDistributedTask(
                                        BlockchainDistributedTaskTypeEnum.DEPLOY_SERVICE_TASK,
                                        blockchainMeta.getProduct(),
                                        blockchainMeta.getBlockchainId()
                                )
                        )
        ).reduce((a, b) -> {
            a.addAll(b);
            return a;
        }).orElse(new ArrayList<>());
    }

    private List<IDistributedTask> filterTasksInTimeSlice(List<IDistributedTask> allTasks) {

        Map<String, IDistributedTask> allTasksMap = Maps.newHashMap();
        for (IDistributedTask task : allTasks) {
            task.setTimeSliceLength(timeSliceLength);
            allTasksMap.put(task.getUniqueTaskKey(), task);
        }

        List<BlockchainDistributedTask> timeSliceBlockchainTasks = scheduleRepository.getAllBlockchainDistributedTasks();
        Map<String, IDistributedTask> newTaskMap = Maps.newHashMap(allTasksMap);
        for (IDistributedTask existedTask : timeSliceBlockchainTasks) {
            existedTask.setTimeSliceLength(timeSliceLength);
            newTaskMap.remove(existedTask.getUniqueTaskKey());
            if (!existedTask.ifFinish()) {
                allTasksMap.remove(existedTask.getUniqueTaskKey());
            }
        }

        List<BizDistributedTask> bizDistributedTasks = scheduleRepository.getAllBizDistributedTasks();
        for (IDistributedTask existedTask : bizDistributedTasks) {
            existedTask.setTimeSliceLength(timeSliceLength);
            newTaskMap.remove(existedTask.getUniqueTaskKey());
            if (!existedTask.ifFinish()) {
                allTasksMap.remove(existedTask.getUniqueTaskKey());
            }
        }

        List<BlockchainDistributedTask> newBlockchainDistributedTasks = newTaskMap.values().stream()
                .filter(
                        task -> task instanceof BlockchainDistributedTask
                ).map(task -> (BlockchainDistributedTask) task)
                .collect(Collectors.toList());

        if (!newBlockchainDistributedTasks.isEmpty()) {
            scheduleRepository.batchInsertBlockchainDTTasks(
                    newBlockchainDistributedTasks
            );
        }

        List<BizDistributedTask> newBizDistributedTasks = newTaskMap.values().stream()
                .filter(
                        task -> task instanceof BizDistributedTask
                ).map(task -> (BizDistributedTask) task)
                .collect(Collectors.toList());

        if (!newBizDistributedTasks.isEmpty()) {
            scheduleRepository.batchInsertBizDTTasks(
                    newBizDistributedTasks
            );
        }

        return Lists.newArrayList(allTasksMap.values());
    }

    private List<ActiveNode> getOnlineNode() {
        List<ActiveNode> nodes = scheduleRepository.getAllActiveNodes();
        List<ActiveNode> onlineNodes = Lists.newArrayList();
        for (ActiveNode node : nodes) {
            if (node.ifActive(nodeTimeToLive)) {
                onlineNodes.add(node);
            }
        }
        return onlineNodes;
    }

    private void doDispatch(List<ActiveNode> nodes, List<IDistributedTask> tasks) {
        Collections.shuffle(nodes);
        roundRobin(nodes, tasks);
        // TODO: give a better algorithm for balancing tasks
        scheduleRepository.batchUpdateBlockchainDTTasks(
                tasks.stream()
                        .filter(
                                task -> task instanceof BlockchainDistributedTask
                        ).map(task -> (BlockchainDistributedTask) task)
                        .collect(Collectors.toList())
        );
        scheduleRepository.batchUpdateBizDTTasks(
                tasks.stream()
                        .filter(
                                task -> task instanceof BizDistributedTask
                        ).map(task -> (BizDistributedTask) task)
                        .collect(Collectors.toList())
        );
        log.info("dispatch tasks : {}", tasks.stream().map(IDistributedTask::getUniqueTaskKey).collect(Collectors.joining(" , ")));
    }

//    private void averageDiffPerBlockchainForEachNode(List<ActiveNode> nodes, List<BlockchainDistributedTask> tasks) {
//        Map<String, Map<String, Integer>> nodeCounterMap = nodes.stream().collect(Collectors.toMap(
//                ActiveNode::getNodeId,
//                node -> new HashMap<>()
//        ));
//        Map<String, ActiveNode> nodeMap = nodes.stream().collect(Collectors.toMap(
//                ActiveNode::getNodeId,
//                node -> node
//        ));
//
//        for (int i = 0; i < tasks.size(); ++i) {
//
//            BlockchainDistributedTask task = tasks.get(i);
//            int diffNum = taskTypeDiffMap.getOrDefault(task.getTaskType().getCode(), 1);
//
//            getTheMinDiffSumForBlockchain(nodeCounterMap, )
//
//            ActiveNode node = nodes.get(i % nodes.size());
//            tasks.get(i).setNodeId(node.getNodeId());
//            tasks.get(i).setStartTime(System.currentTimeMillis());
//        }
//    }
//
//    private String getTheMinDiffSumForBlockchain(Map<String, Map<String, Integer>> nodeCounterMap, String blockchainId) {
//        String nodeId = "";
//        Integer minDiff = Integer.MAX_VALUE;
//        for (Map.Entry<String, Map<String, Integer>> entry : nodeCounterMap.entrySet()) {
//            Integer diff = entry.getValue().getOrDefault(blockchainId, 0);
//            if (diff < minDiff) {
//                nodeId = entry.getKey();
//                minDiff = diff;
//            }
//        }
//
//        return nodeId;
//    }
//
//    private int calculateTotalDiff(Map<String, Map<String, Integer>> nodeCounterMap, String nodeId) {
//        return nodeCounterMap.entrySet().stream()
//                .collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        entry -> entry.getValue().values().stream().reduce(Integer::sum).orElse(0)
//                )).getOrDefault(nodeId, 0);
//    }

    private void roundRobin(List<ActiveNode> nodes, List<IDistributedTask> tasks) {
        Collections.shuffle(tasks);
        for (int i = 0; i < tasks.size(); ++i) {
            ActiveNode node = nodes.get(i % nodes.size());
            tasks.get(i).setNodeId(node.getNodeId());
            tasks.get(i).setStartTime(System.currentTimeMillis());
        }
    }
}
