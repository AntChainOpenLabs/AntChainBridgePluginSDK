package com.ali.antchain.core;

import com.ali.antchain.abstarct.AbstractTester;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Stack;

public class SetProtocolTest {

    AbstractTester tester;
    AbstractBBCService bbcService;

    public SetProtocolTest(AbstractBBCService _bbcService, AbstractTester _tester) {
        bbcService = _bbcService;
        tester = _tester;
    }

    public static void run(AbstractBBCService _bbcService, AbstractTester _tester) {
        SetProtocolTest setProtocolTest = new SetProtocolTest(_bbcService, _tester);

        setProtocolTest.setprotocol_success();
    }

    public void setprotocol_success() {
        tester.getBbcLogger().info("start setprotocol_success test ...");

        // 1. prepare
//        bbcService.startup(inContext);
        bbcService.setupAuthMessageContract();
        bbcService.setupSDPMessageContract();

        // 2. before set protocol
        AbstractBBCContext curCtx = bbcService.getContext();
        tester.getBbcLogger().info("before set_protocol, ctx: {}", curCtx);
        Assert.assertNotNull(curCtx.getAuthMessageContract());
        Assert.assertNotNull(curCtx.getAuthMessageContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, curCtx.getAuthMessageContract().getStatus());

        // 3. execute set_protocol
        bbcService.setProtocol(curCtx.getSdpContract().getContractAddress(),"0");

        // 4. after set protocol
        // 4.1 Check whether the protocol address is correct.
        String protocolAddr = tester.getProtocol(curCtx.getAuthMessageContract().getContractAddress());
        Assert.assertEquals(curCtx.getSdpContract().getContractAddress(), protocolAddr);

        // 4.2 Check whether the contract status in the context is ready
        curCtx = bbcService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, curCtx.getAuthMessageContract().getStatus());
    }

}
