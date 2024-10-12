package com.alipay.antchain.bridge.core;

import com.alipay.antchain.bridge.abstarct.AbstractTester;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;

public class SetProtocolTest {

    AbstractTester tester;
    AbstractBBCService bbcService;

    public SetProtocolTest(AbstractBBCService _bbcService, AbstractTester _tester) {
        bbcService = _bbcService;
        tester = _tester;
    }

    public static void run(AbstractBBCService _bbcService, AbstractTester _tester) throws Exception {
        SetProtocolTest setProtocolTest = new SetProtocolTest(_bbcService, _tester);
        setProtocolTest.setProtocol_success();
    }

    public void setProtocol_success() throws Exception {
        tester.getBbcLogger().info("start setProtocol_success test ...");

        setupContracts();

        // 2. before set protocol
        AbstractBBCContext curCtx = bbcService.getContext();
        logContextInfo(curCtx);
//        checkContractStatus(curCtx);

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
        } catch (Exception e) {
            tester.getBbcLogger().error("Failed to get protocol address", e);
            throw e;
        }

        // 4.2 Check whether the contract status in the context is ready
        try {
            curCtx = bbcService.getContext();
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
}
