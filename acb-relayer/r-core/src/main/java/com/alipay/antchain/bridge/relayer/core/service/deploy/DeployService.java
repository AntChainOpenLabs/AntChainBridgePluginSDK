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

package com.alipay.antchain.bridge.relayer.core.service.deploy;

import java.util.concurrent.locks.Lock;
import javax.annotation.Resource;

import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import com.alipay.antchain.bridge.relayer.core.manager.blockchain.IBlockchainManager;
import com.alipay.antchain.bridge.relayer.core.service.deploy.task.AbstractAsyncTaskExecutor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class DeployService {

    public static String ROW_LOCK_NAME = "DEPLOY_SERVICE_TASK_LOCK_";

    @Resource
    private IBlockchainManager blockchainManager;

    @Resource(name = "amServiceAsyncTaskExecutor")
    private AbstractAsyncTaskExecutor amServiceAsyncTaskExecutor;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private RedissonClient redisson;

    public void process(String product, String blockchainId){
        BlockchainMeta blockchainMeta = blockchainManager.getBlockchainMeta(product, blockchainId);

        // 判断是否有am服务部署任务需要执行
        if (amServiceAsyncTaskExecutor.preCheck(blockchainMeta)) {
            try {
                log.info("do deploy am service task {}-{}", blockchainMeta.getProduct(), blockchainMeta.getBlockchainId());
                processDeployAMService(blockchainMeta);
            } catch (Exception e) {
                log.error("processDeployAMService failed for blockchain {}", blockchainMeta.getMetaKey(), e);
            }
        }
    }

    public void processDeployAMService(BlockchainMeta blockchainMeta) {
        Lock lock = getDeployServiceLock(blockchainMeta.getProduct(), blockchainMeta.getBlockchainId());
        if (!lock.tryLock()) {
            log.info("deploy service get lock failed: {}-{} ", blockchainMeta.getProduct(), blockchainMeta.getBlockchainId());
            return;
        }
        try {
            // 真正执行任务时，加排它锁，确保不会有多并发线程安全问题
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    // 执行任务
                    amServiceAsyncTaskExecutor.doAsyncTask(blockchainMeta);
                }
            });
        } finally {
            lock.unlock();
        }
    }

    private String getRowLockName(String product, String blockchainId) {
        return ROW_LOCK_NAME + product + "_" + blockchainId;
    }

    private Lock getDeployServiceLock(String product, String blockchainId) {
        return redisson.getLock(getRowLockName(product, blockchainId));
    }
}
