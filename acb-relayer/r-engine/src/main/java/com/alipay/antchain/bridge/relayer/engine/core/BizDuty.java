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

import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

import com.alipay.antchain.bridge.relayer.commons.constant.BizDistributedTaskTypeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.BizDistributedTask;
import com.alipay.antchain.bridge.relayer.dal.repository.IScheduleRepository;
import com.alipay.antchain.bridge.relayer.engine.executor.BaseScheduleTaskExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BizDuty {

    @Resource
    private ScheduleContext scheduleContext;

    @Resource
    private IScheduleRepository scheduleRepository;

    @Value("#{duty.timeSliceLength}")
    private long timeSliceLength;

    @Resource
    private Map<BizDistributedTaskTypeEnum, BaseScheduleTaskExecutor> scheduleBizTaskExecutorMap;

    public void duty() {
        List<BizDistributedTask> tasks = scheduleRepository.getBizDistributedTasksByNodeId(this.scheduleContext.getNodeId());
        if (tasks.isEmpty()) {
            log.debug("empty duty tasks");
        } else {
            log.debug("biz duty tasks size {}", tasks.size());
        }

        for (BizDistributedTask task : tasks) {
            task.setTimeSliceLength(timeSliceLength);
            scheduleBizTaskExecutorMap.get(task.getTaskType()).execute(task);
        }
    }
}
