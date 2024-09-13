package com.ali.antchain;

import com.ali.antchain.EthPluginTestTool;
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
    String key = "0xc52e50920ed5e3548e22152fb0e5d5fd13d49ab5bad98212a73027d6e1f1e7ef";
    long gasPrice = 2300000000L;
    long gasLimit = 3000000;

    String testConfig = "{\"gasLimit\":3000000,\"gasPrice\":4100000000,\"privateKey\":\"0xc52e50920ed5e3548e22152fb0e5d5fd13d49ab5bad98212a73027d6e1f1e7ef\",\"url\":\"http://127.0.0.1:7545\"}";

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
    }

    public void testGetcontextTest() {
    }

    public void testSetupamcontractTest() {
    }

    public void testSetupsdpcontractTest() {
    }

    @Test
    public void testSetprotocolTest() {
        ethTestTool.setprotocolTest();
    }
@Test
    public void testQuerysdpmessageseqTest() {
    }

    public void testSetamcontractandlocaldomainTest() {
        ethTestTool.setamcontractandlocaldomainTest();
    }

    public void testReadcrosschainmessagereceiptTest() {
    }

    public void testRelayauthmessageTest() {
        ethTestTool.relayauthmessageTest();
    }
}