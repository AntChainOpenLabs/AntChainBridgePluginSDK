package com.ali.antchain;


import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;

import com.alipay.antchain.bridge.plugins.ethereum.EthereumBBCService;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

public class EthPluginTestToolTest extends TestCase {

    // 测试入参
    AbstractBBCContext inContext;
    AbstractBBCService bbcService;

    // 测试主体结构
    EthPluginTestTool ethTestTool;
    String product;
    String url = "http://127.0.0.1:7545";
    String key = "0x36c40fd6a40ce7b37089fa40c79527243720fbcb5393145be689aca5af5719e2";
    long gasPrice = 2300000000L;
    long gasLimit = 3000000;

    String testConfig = "{\"gasLimit\":3000000,\"gasPrice\":4100000000,\"privateKey\":\"0x36c40fd6a40ce7b37089fa40c79527243720fbcb5393145be689aca5af5719e2\",\"url\":\"http://127.0.0.1:7545\"}";

    @Before
    public void setUp() throws Exception {
        inContext = new DefaultBBCContext();
        inContext.setConfForBlockchainClient(testConfig.getBytes());
        bbcService = new EthereumBBCService();

        ethTestTool = new EthPluginTestTool(inContext, bbcService);
    }

    public void testStartupTest() {
        ethTestTool.startupTest();
    }

    public void testShutdownTest() {
        ethTestTool.shutdownTest();
    }

    public void testGetcontextTest() {
        ethTestTool.getcontextTest();
    }

    public void testSetupamcontractTest() {
        ethTestTool.setupamcontractTest();
    }

    public void testSetupsdpcontractTest() {
        ethTestTool.setupsdpcontractTest();
    }

    @Test
    public void testSetprotocolTest() {
        ethTestTool.setprotocolTest();
    }
@Test
    public void testQuerysdpmessageseqTest() {
        ethTestTool.querysdpmessageseqTest();
    }

    public void testSetamcontractandlocaldomainTest() {
        ethTestTool.setamcontractandlocaldomainTest();
    }

    public void testReadcrosschainmessagereceiptTest() {
        ethTestTool.readcrosschainmessagereceiptTest();
    }

    public void testReadcrosschainmessagebyheightTest() {
        ethTestTool.readcrosschainmessagebyheightTest();
    }

    public void testRelayauthmessageTest() {
        ethTestTool.relayauthmessageTest();
    }
}