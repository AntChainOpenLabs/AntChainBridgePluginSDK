package com.ali.antchain;

import com.ali.antchain.abstarct.IPluginTestTool;
import com.ali.antchain.core.*;
import com.ali.antchain.testers.EthTester;
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
    public void getcontextTest(){
        GetContextTest.run(inContext, bbcService);
    }

    @Override
    public void setupamcontractTest(){
        SetupAuthMessageContractTest.run(inContext, bbcService);
    }

    @Override
    public void setupsdpcontractTest(){
        SetupSDPMessageContractTest.run(inContext, bbcService);
    }

    @Override
    public void setprotocolTest(){
        StartUpTest.runBefore(inContext, bbcService);
        SetProtocolTest.run(bbcService, new EthTester(bbcService));
    }

    @Override
    public void querysdpmessageseqTest() {
        StartUpTest.runBefore(inContext, bbcService);
        QuerySDPMessageSeqTest.run(bbcService);
    }

    @Override
    public void setamcontractandlocaldomainTest() {
        StartUpTest.runBefore(inContext, bbcService);
        SetAMContractAndLocaldomainTest.run(bbcService);
    }

    @Override
    public void readcrosschainmessagereceiptTest() {

    }

    @Override
    public void relayauthmessageTest() {
        StartUpTest.runBefore(inContext, bbcService);
        RelayAuthMessageTest.run(bbcService, new EthTester(bbcService));
    }
}
