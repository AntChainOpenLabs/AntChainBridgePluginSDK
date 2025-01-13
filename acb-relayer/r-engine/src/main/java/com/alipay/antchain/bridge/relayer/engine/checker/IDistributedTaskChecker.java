package com.alipay.antchain.bridge.relayer.engine.checker;

import java.util.concurrent.CompletableFuture;

public interface IDistributedTaskChecker {

    void addLocalFuture(String taskId, CompletableFuture<Void> future);

    boolean checkIfContinue(String taskId);
}
