package com.alipay.antchain.bridge.plugintestrunner.chainmanager.chainmaker;

import com.alipay.antchain.bridge.plugintestrunner.chainmanager.IChainManager;
import lombok.Getter;

@Getter
public class ChainMakerChainManager extends IChainManager {

    public ChainMakerChainManager(String sdk_config) throws Exception {
        this.config = ChainMakerConfigUtil.parseChainConfig(sdk_config);
    }

    @Override
    public void close() {

    }
}
