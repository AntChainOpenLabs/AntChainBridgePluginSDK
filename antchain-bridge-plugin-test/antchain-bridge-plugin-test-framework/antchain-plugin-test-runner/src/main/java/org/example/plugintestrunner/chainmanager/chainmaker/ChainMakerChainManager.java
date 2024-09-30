package org.example.plugintestrunner.chainmanager.chainmaker;

import lombok.Getter;
import org.example.plugintestrunner.chainmanager.IChainManager;

@Getter
public class ChainMakerChainManager extends IChainManager {

    public ChainMakerChainManager(String sdk_config) throws Exception {
        this.config = ChainMakerConfigUtil.parseChainConfig(sdk_config);
    }

    @Override
    public void close() {

    }
}
