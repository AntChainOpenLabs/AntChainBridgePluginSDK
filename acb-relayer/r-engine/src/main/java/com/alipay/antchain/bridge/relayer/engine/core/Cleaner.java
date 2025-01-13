package com.alipay.antchain.bridge.relayer.engine.core;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainDistributedTask;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import com.alipay.antchain.bridge.relayer.core.manager.blockchain.IBlockchainManager;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.BlockchainClientPool;
import com.alipay.antchain.bridge.relayer.dal.repository.IScheduleRepository;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cleaner负责清理区块链
 */
@Component
@Slf4j
public class Cleaner {

    @Resource
    private IBlockchainManager blockchainManager;

    @Resource
    private BlockchainClientPool blockchainClientPool;

    @Resource
    private IScheduleRepository scheduleRepository;

    @Value("#{duty.timeSliceLength}")
    private long timeSliceLength;

    public void clean() {

        log.debug("begin clean.");
        log.debug("all running clients : {}", StrUtil.join(", ", blockchainClientPool.getAllClient()));

        List<BlockchainMeta> stopBlockchains = getStopBlockchains();

        for (BlockchainMeta blockchain : stopBlockchains) {
            log.debug("begin to clean the stopped blockchain {}-{}", blockchain.getProduct(), blockchain.getBlockchainId());
            try {
                boolean ifRunning = blockchainClientPool.hasClient(blockchain.getProduct(), blockchain.getBlockchainId());
                log.debug("blockchain's client {} is running or not: {} ", blockchain.getBlockchainId(), ifRunning);
                if (!ifRunning) {
                    continue;
                }

                log.info("begin to clean the stopped blockchain {}-{}", blockchain.getProduct(), blockchain.getBlockchainId());
                blockchainClientPool.shutdownClient(blockchain.getProduct(), blockchain.getBlockchainId());
                log.info("blockchain's client {} shutdown success.", blockchain.getBlockchainId());
            } catch (Throwable e) {
                log.error("clean stopped blockchain {} fail.", blockchain.getBlockchainId(), e);
            }
        }
    }

    @Synchronized
    private List<BlockchainMeta> getStopBlockchains() {
        List<BlockchainMeta> blockchainMetas = blockchainManager.getAllStoppedBlockchains();
        if (ObjectUtil.isNull(blockchainMetas)) {
            return ListUtil.empty();
        }
        return blockchainMetas.stream().filter(
                blockchainMeta -> !isAnyDTTaskRunning(blockchainMeta.getProduct(), blockchainMeta.getBlockchainId())
        ).collect(Collectors.toList());
    }

    private boolean isAnyDTTaskRunning(String product, String blockchainId) {
        List<BlockchainDistributedTask> tasks = scheduleRepository.getBlockchainDistributedTasksByBlockchain(product, blockchainId);
        if (ObjectUtil.isEmpty(tasks)) {
            return false;
        }
        return tasks.stream().anyMatch(task -> {
            task.setTimeSliceLength(timeSliceLength);
            return !task.ifFinish();
        });
    }
}
