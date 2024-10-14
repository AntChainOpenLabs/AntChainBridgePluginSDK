package com.alipay.antchain.bridge.core;

import com.alipay.antchain.bridge.exception.PluginTestToolException;
import com.alipay.antchain.bridge.exception.PluginTestToolException.*;
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

    public static void run(AbstractBBCContext context, AbstractBBCService service) throws PluginTestToolException {
        ShutDownTest shutdown = new ShutDownTest(service);
        shutdown.shutdown_success(context);
    }

    public void shutdown_success(AbstractBBCContext context) throws PluginTestToolException {
        try {
            service.startup(context);
            service.shutdown();
        } catch (Exception e) {
            throw new ShutDownTestException("ShutDownTest failed", e);
        }
    }
}
