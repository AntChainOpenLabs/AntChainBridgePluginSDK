package com.alipay.antchain.bridge;

import com.alipay.antchain.bridge.abstarct.IPluginTestTool;
import com.alipay.antchain.bridge.core.*;
import com.alipay.antchain.bridge.testers.EthTester;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;

public class EthPluginTestTool implements IPluginTestTool {

    AbstractBBCContext inContext;
    AbstractBBCService bbcService;

    public EthPluginTestTool(AbstractBBCContext _context, AbstractBBCService _service) {
        inContext = _context;
        bbcService = _service;
    }

    @Override
    public void startupTest() {
        StartUpTest.run(inContext, bbcService);
    }

    @Override
    public void shutdownTest() {
        ShutDownTest.run(inContext, bbcService);
    }

    @Override
    public void getContextTest(){
        GetContextTest.run(inContext, bbcService);
    }

    @Override
    public void setupAmContractTest(){
        SetupAuthMessageContractTest.run(inContext, bbcService);
    }

    @Override
    public void setupSdpContractTest(){
        SetupSDPMessageContractTest.run(inContext, bbcService);
    }

    @Override
    public void setProtocolTest() throws Exception {
        StartUpTest.runBefore(inContext, bbcService);
        SetProtocolTest.run(bbcService, new EthTester(bbcService));
    }

    @Override
    public void querySdpMessageSeqTest() {
        StartUpTest.runBefore(inContext, bbcService);
        QuerySDPMessageSeqTest.run(bbcService);
    }

    @Override
    public void setAmContractAndLocalDomainTest() {
        StartUpTest.runBefore(inContext, bbcService);
        SetAMContractAndLocalDomainTest.run(bbcService);
    }

    @Override
    public void readCrossChainMessageReceiptTest() {
        StartUpTest.runBefore(inContext, bbcService);
        ReadCrossChainMessageReceiptTest.run(bbcService, new EthTester(bbcService));
    }

    @Override
    public void readCrossChainMessageByHeightTest() {
        StartUpTest.runBefore(inContext, bbcService);
        ReadCrossChainMessageByHeightTest.run(bbcService, new EthTester(bbcService));
    }

    @Override
    public void relayAuthMessageTest() {
        StartUpTest.runBefore(inContext, bbcService);
        RelayAuthMessageTest.run(bbcService, new EthTester(bbcService));
    }

}
