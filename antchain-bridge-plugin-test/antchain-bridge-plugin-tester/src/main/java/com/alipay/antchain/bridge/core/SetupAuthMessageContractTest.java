package com.alipay.antchain.bridge.core;

import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.exception.PluginTestToolException;
import com.alipay.antchain.bridge.exception.PluginTestToolException.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;


public class SetupAuthMessageContractTest {

    private static final Logger log = LoggerFactory.getLogger(SetupAuthMessageContractTest.class);
    AbstractBBCService service;

    public SetupAuthMessageContractTest(AbstractBBCService service) {
        this.service = service;
    }

    public static void run(AbstractBBCContext context, AbstractBBCService service) throws PluginTestToolException {
        SetupAuthMessageContractTest SetupAm = new SetupAuthMessageContractTest(service);
        SetupAm.setupAmContract_success(context);
    }

    public void setupAmContract_success(AbstractBBCContext context) throws PluginTestToolException {
        try {
            service.startup(context);
            AbstractBBCContext curCtx = service.getContext();
            if (curCtx.getAuthMessageContract() != null) {
                throw new AuthMessageContractNotNullException("SetupAuthMessageContractTest failed, AuthMessageContract should be null before setup.");
            }
            service.setupAuthMessageContract();
            // 部署AM合约后，上下文中合约状态为`CONTRACT_DEPLOYED`
            curCtx = service.getContext();
            if (curCtx.getAuthMessageContract() == null) {
                throw new AuthMessageContractNullException("SetupAuthMessageContractTest failed, AuthMessageContract should not be null after setup.");
            }
            if (curCtx.getAuthMessageContract().getContractAddress() == null) {
                throw new AuthMessageContractAddressNullException("SetupAuthMessageContractTest failed, AuthMessageContract address should not be null after setup.");
            }
            if (!ContractStatusEnum.CONTRACT_DEPLOYED.equals(curCtx.getAuthMessageContract().getStatus())) {
                throw new AuthMessageContractStatusException("SetupAuthMessageContractTest failed, AuthMessageContract status should be CONTRACT_DEPLOYED after setup.");
            }
        } catch (Exception e) {
            throw new SetupAuthMessageContractTestException("SetupAuthMessageContractTest failed, exception occurred.", e);
        }
    }
}
