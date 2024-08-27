package com.ali.antchain.Test;

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
        SetupAm.setupamcontract(context);
    }

    public void setupamcontract(AbstractBBCContext  context) {
        try {
            service.startup(context);
            service.setupAuthMessageContract();

            // 获取上下文
            AbstractBBCContext ctx = service.getContext();

            // 打印AM合约状态
            log.info("The status of the auth message contract is: {}", ctx.getAuthMessageContract().getStatus());
        } catch (Exception e) {
            // 异常信息
            log.error("Failed to setup authentication message contract", e);
        }
    }
}
