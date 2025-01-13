package com.alipay.antchain.bridge.relayer.engine.core;

import javax.annotation.Resource;

import com.alipay.antchain.bridge.relayer.dal.repository.IScheduleRepository;
import org.springframework.stereotype.Component;

/**
 * Activator负责分布式节点的定时心跳，往全局DB定时登记心跳，表示节点活性
 */
@Component
public class Activator {

    @Resource
    private IScheduleRepository scheduleRepository;

    @Resource
    private ScheduleContext scheduleContext;


    /**
     * 往全局DB定时登记心跳，表示节点活性
     */
    public void activate() {
        scheduleRepository.activate(
                scheduleContext.getNodeId(),
                scheduleContext.getNodeIp()
        );
    }
}
