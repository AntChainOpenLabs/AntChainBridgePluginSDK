package com.alipay.antchain.bridge.core;
import com.alipay.antchain.bridge.commons.bbc.syscontract.AuthMessageContract;
import com.alipay.antchain.bridge.commons.bbc.syscontract.SDPContract;
import com.alipay.antchain.bridge.exception.PluginTestToolException;
import com.alipay.antchain.bridge.exception.PluginTestToolException.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;


public class StartUpTest {

    private static final Logger log = LoggerFactory.getLogger(StartUpTest.class);
    AbstractBBCService service;

    public StartUpTest(AbstractBBCService service) {
        this.service = service;
    }

    public static void runBefore(AbstractBBCContext context, AbstractBBCService service) throws PluginTestToolException {
        StartUpTest startUpTest = new StartUpTest(service);
        startUpTest.startup_success(context);
    }

    public static void run(AbstractBBCContext context, AbstractBBCService service) throws PluginTestToolException {
        StartUpTest startUpTest = new StartUpTest(service);
        startUpTest.startup_success(context);
    }

    public void startup_success(AbstractBBCContext context) throws PluginTestToolException {
        try {
            service.startup(context);
            AbstractBBCContext ctx = service.getContext();
            AuthMessageContract authMessageContract = ctx.getAuthMessageContract();
            if (authMessageContract != null) {
                throw new AuthMessageContractNotNullException("StartUpTest failed, authMessageContract is null");
            }
            SDPContract sdpContract = ctx.getSdpContract();
            if (sdpContract != null) {
                throw new AuthMessageContractNotNullException("StartUpTest failed, sdpContract is null");
            }
        } catch (Exception e) {
            throw new StartUpTestException("StartUpTest failed", e);
        }
    }
}
