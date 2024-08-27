package com.ali.antchain;

import com.ali.antchain.Test.GetContextTest;
import com.ali.antchain.Test.ShutDownTest;
import com.ali.antchain.Test.StartUpTest;
import com.ali.antchain.config.EthereumConfig;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;

public class EthPluginsTest extends PluginsTest {
    public EthPluginsTest(AbstractBBCContext context, AbstractBBCService service) {
        super(context, service);
    }
    EthereumConfig config;
    public void EthConfigInit(String url,String key,long gasPrice,long gasLimit) {
        this.config = new EthereumConfig();
        config.setUrl(url);
        config.setPrivateKey(key);
        config.setGasPrice(gasPrice);
        config.setGasLimit(gasLimit);
        this.context = new DefaultBBCContext();
        context.setConfForBlockchainClient(config.toJsonString().getBytes());
    }
}
