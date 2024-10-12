package com.alipay.antchain.bridge.core;

import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
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

    public static void run(AbstractBBCContext context, AbstractBBCService service){
        SetupAuthMessageContractTest SetupAm = new SetupAuthMessageContractTest(service);
        SetupAm.setupAmContract_success(context);
    }

    public void setupAmContract_success(AbstractBBCContext context) {
        try {
            service.startup(context);

            // 部署AM合约前，上下文中合约状态为空
            AbstractBBCContext curCtx = service.getContext();
            log.info("before setup am contract, ctx: {}", curCtx);
            if (curCtx.getAuthMessageContract() != null) {
                throw new IllegalStateException("Expected AuthMessageContract to be null before setup.");
            }
            service.setupAuthMessageContract();

            // 部署AM合约后，上下文中合约状态为`CONTRACT_DEPLOYED`
            curCtx = service.getContext();
            log.info("after setup am contract, ctx: {}", curCtx);
            if (curCtx.getAuthMessageContract() == null) {
                throw new IllegalStateException("AuthMessageContract should not be null after setup.");
            }
            if (curCtx.getAuthMessageContract().getContractAddress() == null) {
                throw new IllegalStateException("Contract address should not be null after setup.");
            }
            if (!ContractStatusEnum.CONTRACT_DEPLOYED.equals(curCtx.getAuthMessageContract().getStatus())) {
                throw new IllegalStateException("Contract status should be CONTRACT_DEPLOYED after setup.");
            }
        } catch (Exception e) {
            log.error("Failed to setup authentication message contract", e);
        }
    }
}
