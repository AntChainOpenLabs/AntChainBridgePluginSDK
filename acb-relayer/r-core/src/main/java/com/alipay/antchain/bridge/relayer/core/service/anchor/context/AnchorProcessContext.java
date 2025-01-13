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

package com.alipay.antchain.bridge.relayer.core.service.anchor.context;

import java.util.concurrent.ExecutorService;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import com.alipay.antchain.bridge.relayer.core.manager.ptc.IPtcManager;
import com.alipay.antchain.bridge.relayer.core.service.anchor.tasks.CachedBlockQueue;
import com.alipay.antchain.bridge.relayer.core.service.anchor.tasks.IBlockQueue;
import com.alipay.antchain.bridge.relayer.core.service.receiver.ReceiverService;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.AbstractBlockchainClient;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.BlockchainClientPool;
import com.alipay.antchain.bridge.relayer.dal.repository.IBlockchainRepository;
import lombok.Getter;
import lombok.Setter;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.support.TransactionTemplate;

@Getter
@Setter
public class AnchorProcessContext {

    private String anchorProduct;

    private String anchorBlockchainId;

    private BlockchainClientPool blockchainClientPool;

    private ReceiverService receiverService;

    private IPtcManager ptcManager;

    private IBlockQueue blockQueue;

    private TransactionTemplate transactionTemplate;

    private RedissonClient redisson;

    private ExecutorService blockSyncTaskThreadsPool;

    private int blockCacheCapacity;

    private int blockCacheTTL;

    private int syncBatchSize;

    private int syncAsyncQuerySize;

    private int maxDiffBetweenSyncAndNotify;

    private int notifyBatchSize;

    private long heightDelayAlarmThreshold;

    public AnchorProcessContext(
            BlockchainMeta blockchainMeta,
            TransactionTemplate transactionTemplate,
            BlockchainClientPool blockchainClientPool,
            RedissonClient redisson,
            ExecutorService blockSyncTaskThreadsPool,
            ReceiverService receiverService,
            IPtcManager ptcManager,
            int blockCacheCapacity,
            int blockCacheTTL,
            int syncBatchSize,
            int syncAsyncQuerySize,
            int maxDiffBetweenSyncAndNotify,
            int notifyBatchSize,
            long heightDelayAlarmThreshold
    ) {
        this.blockchainClientPool = blockchainClientPool;
        this.anchorProduct = blockchainMeta.getProduct();
        this.anchorBlockchainId = blockchainMeta.getBlockchainId();
        this.blockQueue = new CachedBlockQueue(
                this,
                redisson,
                blockCacheCapacity,
                blockCacheTTL
        );
        this.transactionTemplate = transactionTemplate;
        this.redisson = redisson;
        this.blockSyncTaskThreadsPool = blockSyncTaskThreadsPool;
        this.receiverService = receiverService;
        this.ptcManager = ptcManager;
        this.blockCacheCapacity = blockCacheCapacity;
        this.blockCacheTTL = blockCacheTTL;
        this.syncBatchSize = syncBatchSize;
        this.syncAsyncQuerySize = syncAsyncQuerySize;
        this.maxDiffBetweenSyncAndNotify = maxDiffBetweenSyncAndNotify;
        this.notifyBatchSize = notifyBatchSize;
        this.heightDelayAlarmThreshold = heightDelayAlarmThreshold;

        // init blockchain client
        blockchainClientPool.createClient(blockchainMeta);
    }

    public AbstractBlockchainClient getBlockchainClient() {
        return blockchainClientPool.getClient(anchorProduct, anchorBlockchainId);
    }

    public IBlockchainRepository getBlockchainRepository() {
        return blockchainClientPool.getBlockchainRepository();
    }

    public BlockchainMeta getBlockchainMeta() {
        return blockchainClientPool.getClient(anchorProduct, anchorBlockchainId).getBlockchainMeta();
    }

    public String getBlockchainDomain() {
        return blockchainClientPool.getClient(anchorProduct, anchorBlockchainId).getDomain();
    }

    public boolean isPtcSupport() {
        return ObjectUtil.isNotNull(blockchainClientPool.getClient(anchorProduct, anchorBlockchainId).getPtcContract())
                && ObjectUtil.isNotNull(getBlockchainMeta().getProperties().getBbcContext().getPtcContract())
                && getBlockchainMeta().getProperties().getBbcContext().getPtcContract().getStatus() == ContractStatusEnum.CONTRACT_READY;
    }
}