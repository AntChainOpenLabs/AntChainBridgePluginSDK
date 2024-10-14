package com.alipay.antchain.bridge.core;

import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.AuthMessageContract;
import com.alipay.antchain.bridge.exception.PluginTestToolException;
import com.alipay.antchain.bridge.exception.PluginTestToolException.*;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetContextTest {

    private static final Logger log = LoggerFactory.getLogger(GetContextTest.class);
    AbstractBBCService service;

    public GetContextTest(AbstractBBCService service) {
        this.service = service;
    }

    public static void run(AbstractBBCContext context, AbstractBBCService service) throws PluginTestToolException {
        GetContextTest getContextTest = new GetContextTest(service);
        getContextTest.getcontext(context);
    }

    public void getcontext(AbstractBBCContext context) throws PluginTestToolException {
        if (service == null) {
            throw new ServiceNullException("GetContextTest failed, service is not initialized.");
        }
        try {
            service.startup(context);
            AbstractBBCContext ctx = service.getContext();
            if (ctx == null) {
                throw new ContextNullException("GetContextTest failed, context is null.");
            }
        } catch (Exception e) {
            throw new GetContextTestException("GetContextTest failed.", e);
        }
    }
}