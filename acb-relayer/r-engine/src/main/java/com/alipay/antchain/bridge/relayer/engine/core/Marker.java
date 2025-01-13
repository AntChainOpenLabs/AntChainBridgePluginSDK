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

package com.alipay.antchain.bridge.relayer.engine.core;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.relayer.commons.constant.MarkDTTaskStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.ActiveNode;
import com.alipay.antchain.bridge.relayer.commons.model.MarkDTTask;
import com.alipay.antchain.bridge.relayer.dal.repository.IScheduleRepository;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Marker {

    @Resource
    private IScheduleRepository scheduleRepository;

    @Value("${relayer.engine.schedule.marker.batch_size:64}")
    private int markBatchSize;

    @Value("${relayer.engine.schedule.marker.task_timeout:30000}")
    private int taskTimeout;

    @Value("${relayer.engine.schedule.activate.ttl:3000}")
    private long nodeTimeToLive;

    public void mark() {
        Lock lock = getDistributeLock();
        if (!lock.tryLock()) {
            log.debug("not my mark lock.");
            return;
        }

        try {
            log.debug("process mark task now.");
            List<MarkDTTask> taskList = scheduleRepository.peekInitOrTimeoutMarkDTTask(markBatchSize);
            if (taskList.isEmpty()) {
                return;
            }
            doMark(taskList);
        } catch (Exception e) {
            log.error("failed to do mark task: ", e);
        } finally {
            lock.unlock();
        }
    }

    private Lock getDistributeLock() {
        return scheduleRepository.getMarkLock();
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

    private void doMark(List<MarkDTTask> tasks) {
        List<ActiveNode> onlineNodes = getOnlineNode();
        if (ObjectUtil.isEmpty(onlineNodes)) {
            log.warn("none online nodes!");
            return;
        }
        roundRobin(onlineNodes, tasks);
        scheduleRepository.batchUpdateMarkDTTasks(tasks);
    }

    private void roundRobin(List<ActiveNode> nodes, List<MarkDTTask> tasks) {
        Collections.shuffle(nodes);
        for (int i = 0; i < tasks.size(); ++i) {
            ActiveNode node = nodes.get(i % nodes.size());
            tasks.get(i).setNodeId(node.getNodeId());
            tasks.get(i).setEndTime(System.currentTimeMillis() + taskTimeout);
            tasks.get(i).setState(MarkDTTaskStateEnum.READY);
        }
    }
}
