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

package com.alipay.antchain.bridge.relayer.core.service.anchor.tasks;

import java.util.concurrent.TimeUnit;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.core.service.anchor.context.AnchorProcessContext;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.AbstractBlock;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.HeterogeneousBlock;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.ByteArrayCodec;

@Getter
@Setter
@Slf4j
public class CachedBlockQueue implements IBlockQueue {

    private AnchorProcessContext processContext;

    private final Cache<String, AbstractBlock> blockCache;

    private final RedissonClient redisson;

    private final int blockCacheTTL;

    private long latestBlockHeightFetched = Long.MAX_VALUE;

    public CachedBlockQueue(
            AnchorProcessContext processContext,
            RedissonClient redisson,
            int blockCacheCapacity,
            int blockCacheTTL
    ) {
        this.processContext = processContext;
        this.blockCache = CacheUtil.newFIFOCache(blockCacheCapacity);
        this.redisson = redisson;
        this.blockCacheTTL = blockCacheTTL;
    }

    public void putBlockIntoQueue(AbstractBlock block) {
        putBlockIntoCache(block);
    }

    public AbstractBlock getBlockFromQueue(long height) {
        if (blockCache.containsKey(getMemCacheKey(height))) {
            return blockCache.get(getMemCacheKey(height), false);
        }

        if (height <= latestBlockHeightFetched) {
            // 从redis二级缓存读取
            try {
                log.debug("try to get block from redis {}-{}", processContext.getBlockchainMeta().getMetaKey(), height);
                RBucket<byte[]> bucket = redisson.getBucket(
                        getRedisCacheKey(processContext.getAnchorProduct(), processContext.getAnchorBlockchainId(), height),
                        ByteArrayCodec.INSTANCE
                );
                byte[] blockBin = bucket.get();
                if (ObjectUtil.isNotEmpty(blockBin)) {
                    HeterogeneousBlock heterogeneousBlock = new HeterogeneousBlock();
                    heterogeneousBlock.decode(blockBin);
                    log.info("get block from redis {}-{}", processContext.getBlockchainMeta().getMetaKey(), height);
                    return heterogeneousBlock;
                }
            } catch (Exception e) {
                log.error(
                        "failed to read block ( product: {}, blockchain_id: {}, height: {} ) from redis.",
                        processContext.getAnchorProduct(), processContext.getAnchorBlockchainId(), height,
                        e
                );
            }
        }

        AbstractBlock block = processContext.getBlockchainClient().getEssentialBlockByHeight(height);
        putBlockIntoCache(block);
        if (height > latestBlockHeightFetched || latestBlockHeightFetched == Long.MAX_VALUE) {
            latestBlockHeightFetched = height;
        }

        return block;
    }

    private void putBlockIntoCache(AbstractBlock block) {

        log.debug("put block {} from blockchain {}-{} into cache", block.getHeight(), block.getProduct(), block.getBlockchainId());

        String key = getMemCacheKey(block.getHeight());
        blockCache.put(key, block);

        log.info("put block into redis {}-{}-{}", block.getProduct(), block.getBlockchainId(), block.getHeight());
        redisson.getBucket(
                getRedisCacheKey(block.getProduct(), block.getBlockchainId(), block.getHeight()),
                ByteArrayCodec.INSTANCE
        ).setAsync(
                block.encode(),
                blockCacheTTL,
                TimeUnit.MILLISECONDS
        ).exceptionally(throwable -> {
            log.error("failed to put block into redis, please check redis server", throwable);
            return null;
        });
    }

    private String getMemCacheKey(long blockHeight) {
        return Long.toString(blockHeight);
    }

    private String getRedisCacheKey(String product, String blockchainId, long blockHeight) {
        return StrUtil.format("{}^{}^{}", product, blockchainId, blockHeight);
    }
}
