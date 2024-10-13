package com.alipay.antchain.bridge.core;

import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.exception.PluginTestToolException;
import com.alipay.antchain.bridge.exception.PluginTestToolException.*;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetupSDPMessageContractTest {

    private static final Logger log = LoggerFactory.getLogger(SetupAuthMessageContractTest.class);
    AbstractBBCService service;

    public SetupSDPMessageContractTest(AbstractBBCService service) {
        this.service = service;
    }

    public static void run(AbstractBBCContext context, AbstractBBCService service) throws PluginTestToolException{
        SetupSDPMessageContractTest SetupSDP = new SetupSDPMessageContractTest(service);
        SetupSDP.setupSdpContract_success(context);
    }
    public void setupSdpContract_success(AbstractBBCContext context) throws PluginTestToolException {
        if (service == null) {
            throw new IllegalStateException("Service is not initialized.");
        }
        try {

            service.startup(context);

            AbstractBBCContext curCtx = service.getContext();
            if (curCtx.getSdpContract() != null) {
                throw new SDPContractNotNullException("SetupSDPMessageContractTest failed, SDPContract should be null before setup.");
            }

            service.setupAuthMessageContract();

            service.setupSDPMessageContract();

            curCtx = service.getContext();

            if (curCtx.getSdpContract() == null) {
                throw new SDPContractNullException("SetupSDPMessageContractTest failed, SDPContract should not be null after setup.");
            }
            if (!ContractStatusEnum.CONTRACT_DEPLOYED.equals(curCtx.getSdpContract().getStatus())) {
                throw new SDPContractStatusException("SetupSDPMessageContractTest failed, SDPContract status should be CONTRACT_DEPLOYED after setup.");
            }
        } catch (Exception e) {
            throw new SetupSDPMessageContractTestException("SetupSDPMessageContractTest failed, exception occurred.", e);
        }
    }
}
