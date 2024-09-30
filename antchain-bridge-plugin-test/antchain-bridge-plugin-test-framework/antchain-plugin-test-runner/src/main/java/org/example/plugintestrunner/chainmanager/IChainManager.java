package org.example.plugintestrunner.chainmanager;

import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
import lombok.Getter;


@Getter
public abstract class IChainManager {

    public String config;

    public AbstractBBCContext getBBCContext() {
        AbstractBBCContext bbcContext = new DefaultBBCContext();
        bbcContext.setConfForBlockchainClient(this.config.getBytes());
        return bbcContext;
    }

    public abstract void close();
}
