package com.alipay.antchain.bridge.relayer.core.service.deploy.task;

import javax.annotation.Resource;

import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import com.alipay.antchain.bridge.relayer.core.manager.blockchain.IBlockchainManager;
import lombok.Getter;

@Getter
public abstract class AbstractAsyncTaskExecutor {

    @Resource
    private IBlockchainManager blockchainManager;

    /**
     * 预检查该状态是否需要继续推进
     *
     * @param blockchainMeta@return
     */
    public abstract boolean preCheck(BlockchainMeta blockchainMeta);

    /**
     * 状态推进
     *
     * @param blockchainMeta
     * @return
     */
    public abstract boolean doAsyncTask(BlockchainMeta blockchainMeta);
}

