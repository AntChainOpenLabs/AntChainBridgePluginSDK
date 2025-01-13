package com.alipay.antchain.bridge.relayer.engine.executor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import cn.hutool.core.lang.Assert;
import com.alipay.antchain.bridge.relayer.commons.model.IDistributedTask;
import com.alipay.antchain.bridge.relayer.engine.checker.IDistributedTaskChecker;
import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

/**
 * 分布式任务基类，传入具体分布式任务，使用线程池异步执行
 */
@Slf4j
@Getter
public abstract class BaseScheduleTaskExecutor {

    private final ExecutorService executor;

    private final IDistributedTaskChecker distributedTaskChecker;

    public BaseScheduleTaskExecutor(ExecutorService executor, IDistributedTaskChecker distributedTaskChecker) {
        Assert.notNull(executor);
        this.executor = executor;
        this.distributedTaskChecker = distributedTaskChecker;
    }

    /**
     * 分布式任务执行基类
     *
     * @param task
     */
    @Synchronized
    public void execute(IDistributedTask task) {

        String taskId = task.getUniqueTaskKey();
        // 判断时间片是否结束
        if (task.ifFinish()) {
            log.debug("task out of time slice : {}", taskId);
            return;
        }

        // 该任务是否已经在执行
        if (!distributedTaskChecker.checkIfContinue(taskId)) {
            log.debug("task {} is running locally or on other node remotely", taskId);
            return;
        }

        // 触发执行
        log.debug("execute task : {}", taskId);
        distributedTaskChecker.addLocalFuture(taskId, CompletableFuture.runAsync(genTask(task), executor));
    }

    //*******************************************
    // 子类实现
    //*******************************************

    public abstract Runnable genTask(IDistributedTask task);
}
