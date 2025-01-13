package com.alipay.antchain.bridge.relayer.engine.checker;

import org.redisson.Redisson;
import org.redisson.RedissonLock;
import org.redisson.api.RFuture;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;

public class SingleThreadRedissonLock extends RedissonLock {

    public SingleThreadRedissonLock(RedissonClient redisson, String lockName) {
        super(((Redisson) redisson).getCommandExecutor(), lockName);
    }

    @Override
    public RFuture<Boolean> tryLockAsync() {
        return tryLockAsync(-1);
    }

    @Override
    public void unlock() {
        try {
            get(unlockAsync(-1));
        } catch (RedisException e) {
            if (e.getCause() instanceof IllegalMonitorStateException) {
                throw (IllegalMonitorStateException) e.getCause();
            } else {
                throw e;
            }
        }
    }

    @Override
    public boolean isHeldByCurrentThread() {
        return super.isHeldByThread(-1);
    }
}
