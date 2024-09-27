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

//    public void setprotocol_success() {
//        tester.getBbcLogger().info("start setprotocol_success test ...");
//
//        // 1. prepare
////        bbcService.startup(inContext);
//        bbcService.setupAuthMessageContract();
//        bbcService.setupSDPMessageContract();
//
//        // 2. before set protocol
//        AbstractBBCContext curCtx = bbcService.getContext();
//        tester.getBbcLogger().info("before set_protocol, ctx: {}", curCtx);
//        Assert.assertNotNull(curCtx.getAuthMessageContract());
//        Assert.assertNotNull(curCtx.getAuthMessageContract().getContractAddress());
//        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, curCtx.getAuthMessageContract().getStatus());
//
//        // 3. execute set_protocol
//        bbcService.setProtocol(curCtx.getSdpContract().getContractAddress(),"0");
//
//        // 4. after set protocol
//        // 4.1 Check whether the protocol address is correct.
//        String protocolAddr = tester.getProtocol(curCtx.getAuthMessageContract().getContractAddress());
//        Assert.assertEquals(curCtx.getSdpContract().getContractAddress(), protocolAddr);
//
//        // 4.2 Check whether the contract status in the context is ready
//        curCtx = bbcService.getContext();
//        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, curCtx.getAuthMessageContract().getStatus());
//    }
    public void setprotocol_success() {
        tester.getBbcLogger().info("start setprotocol_success test ...");

        // 1. prepare
//        try {
//            bbcService.startup(inContext);
//            tester.getBbcLogger().info("After startup, ctx: {}", bbcService.getContext());
//        } catch (Exception e) {
//            tester.getBbcLogger().error("Failed to start up service", e);
//            throw e;
//        }

        setupContracts();

        // 2. before set protocol
        AbstractBBCContext curCtx = bbcService.getContext();
        logContextInfo(curCtx);
        checkContractStatus(curCtx);

        // 3. execute set_protocol
        try {
            bbcService.setProtocol(curCtx.getSdpContract().getContractAddress(), "0");
        } catch (Exception e) {
            tester.getBbcLogger().error("Failed to set protocol", e);
            throw e;
        }

        // 4. after set protocol
        // 4.1 Check whether the protocol address is correct.
        try {
            String protocolAddr = tester.getProtocol(curCtx.getAuthMessageContract().getContractAddress());
            Assert.assertEquals(curCtx.getSdpContract().getContractAddress(), protocolAddr);
        } catch (Exception e) {
            tester.getBbcLogger().error("Failed to get protocol address", e);
            throw e;
        }

        // 4.2 Check whether the contract status in the context is ready
        try {
            curCtx = bbcService.getContext();
            Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, curCtx.getAuthMessageContract().getStatus());
        } catch (Exception e) {
            tester.getBbcLogger().error("Failed to check contract status", e);
            throw e;
        }
    }

    private void setupContracts() {
        try {
            bbcService.setupAuthMessageContract();
            bbcService.setupSDPMessageContract();
            tester.getBbcLogger().info("Contracts setup completed.");
        } catch (Exception e) {
            tester.getBbcLogger().error("Failed to setup contracts", e);
            throw e;
        }
    }

    private void logContextInfo(AbstractBBCContext ctx) {
        tester.getBbcLogger().info("Current context: {}", ctx);
    }

    private void checkContractStatus(AbstractBBCContext ctx) {
        try {
            Assert.assertNotNull(ctx.getAuthMessageContract());
            Assert.assertNotNull(ctx.getAuthMessageContract().getContractAddress());
            Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
        } catch (AssertionError e) {
            tester.getBbcLogger().error("Contract status assertion failed", e);
            throw e;
        }
    }

}
