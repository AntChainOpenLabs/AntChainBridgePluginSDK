package com.ali.antchain.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;

public class ShutDownTest {

    private static final Logger log = LoggerFactory.getLogger(ShutDownTest.class);
    AbstractBBCService service;

    public ShutDownTest(AbstractBBCService service) {
        this.service = service;
    }

    public static void run(AbstractBBCContext context, AbstractBBCService service){
        ShutDownTest shutdown = new ShutDownTest(service);
        shutdown.shutdown(context);
    }

    public void shutdown(AbstractBBCContext  context){
        try {
            // 调用 shutdown关闭服务
            service.shutdown();
        } catch (Exception e) {
            // 异常
            log.error("Failed to setup authentication message contract", e.getMessage());
        }
    }
}