package com.ali.antchain.core;

import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetupSDPMessageContractTest {

    private static final Logger log = LoggerFactory.getLogger(SetupAuthMessageContractTest.class);
    AbstractBBCService service;

    public SetupSDPMessageContractTest(AbstractBBCService service) {
        this.service = service;
    }

    public static void run(AbstractBBCContext context, AbstractBBCService service){
        SetupSDPMessageContractTest SetupSDP = new SetupSDPMessageContractTest(service);
        SetupSDP.setupsdpcontract_success(context);
    }

    public void setupsdpcontract_success(AbstractBBCContext context) {
        if (service == null) {
            throw new IllegalStateException("Service is not initialized.");
        }
        try {
            service.startup(context);
            service.setupAuthMessageContract();
            // set up sdp
            service.setupSDPMessageContract();
            // get context
            AbstractBBCContext ctx = service.getContext();
            log.info("SDP contract status: {}", ctx.getSdpContract().getStatus());
        } catch (Exception e) {
            log.error("Error setting up SDP contract", e);
        }
    }
}
