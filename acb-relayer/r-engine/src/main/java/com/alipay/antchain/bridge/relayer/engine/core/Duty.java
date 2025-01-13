package com.alipay.antchain.bridge.relayer.engine.core;

import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainDistributedTaskTypeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainDistributedTask;
import com.alipay.antchain.bridge.relayer.dal.repository.IScheduleRepository;
import com.alipay.antchain.bridge.relayer.engine.executor.BaseScheduleTaskExecutor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Duty会定时轮询任务表，读取当前时间片属于本节点的任务，交给系列职能线程池处理
 */
@Component
@Slf4j
public class Duty {

    @Resource
    private IScheduleRepository scheduleRepository;

    @Resource
    private ScheduleContext scheduleContext;

    @Getter
    @Value("${relayer.engine.schedule.duty.dt_task.time_slice:180000}")
    private long timeSliceLength;

    @Resource
    private Map<BlockchainDistributedTaskTypeEnum, BaseScheduleTaskExecutor> scheduleTaskExecutorMap;

    public void duty() {

        // 查询本节点的时间片任务
        List<BlockchainDistributedTask> tasks = scheduleRepository.getBlockchainDistributedTasksByNodeId(this.scheduleContext.getNodeId());
        if (tasks.isEmpty()) {
            log.debug("empty duty tasks");
        } else {
            log.debug("duty tasks size {}", tasks.size());
        }

        // 分配给各个职能线程池处理
        for (BlockchainDistributedTask task : tasks) {
            task.setTimeSliceLength(timeSliceLength);
            scheduleTaskExecutorMap.get(task.getTaskType()).execute(task);
        }
    }

}
