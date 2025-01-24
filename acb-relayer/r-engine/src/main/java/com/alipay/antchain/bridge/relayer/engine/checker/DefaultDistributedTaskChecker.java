package com.alipay.antchain.bridge.relayer.engine.checker;

import java.util.concurrent.CompletableFuture;
import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component("defaultDistributedTaskChecker")
public class DefaultDistributedTaskChecker extends LocalDistributedTaskChecker {

    @Resource
    private RedissonClient redisson;

    @Override
    public void addLocalFuture(String taskId, CompletableFuture<Void> future) {
        super.addLocalFuture(taskId, future);
        future.whenComplete((unused, throwable) -> {
            if (ObjectUtil.isNotNull(throwable)) {
                log.error("failed to process task : {}", taskId, throwable);
            }
            try {
                RLock lock = new SingleThreadRedissonLock(redisson, getLockKey(taskId));
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("local task {} finished, unlock it", taskId);
                } else {
                    // Could be the lock used to locked with this node has been deleted somehow.
                    // For example, over the TTL and watchdog not work as usual.
                    log.error("local task {} is not locked by current node but this node run the task somehow 😨!", taskId);
                }
            } catch (Throwable t) {
                log.error("failed to unlock task : {}", taskId, t);
            }
        });
    }

    @Override
    public boolean checkIfContinue(String taskId) {
        if (!super.checkIfContinue(taskId)) {
            return false;
        }
        RLock lock = new SingleThreadRedissonLock(redisson, getLockKey(taskId));
        return lock.tryLock();
    }

    private String getLockKey(String taskId) {
        return String.format("relayer:task:%s", taskId);
    }
}
