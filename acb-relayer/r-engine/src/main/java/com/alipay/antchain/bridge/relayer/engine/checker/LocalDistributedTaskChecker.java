package com.alipay.antchain.bridge.relayer.engine.checker;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Getter
@Component("localDistributedTaskChecker")
@Slf4j
public class LocalDistributedTaskChecker implements IDistributedTaskChecker {

    private final Map<String, Future> localRunningTasks = Maps.newConcurrentMap();

    @Override
    public void addLocalFuture(String taskId, CompletableFuture<Void> future) {
        localRunningTasks.put(taskId, future);
    }

    @Override
    public boolean checkIfContinue(String taskId) {
        if (localRunningTasks.containsKey(taskId)) {
            if (!localRunningTasks.get(taskId).isDone()) {
                log.debug("local task is running : {}", taskId);
                return false;
            }
            log.info("local task finish : {}", taskId);
            localRunningTasks.remove(taskId);
        }
        return true;
    }
}
