package com.alipay.antchain.bridge;

import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;

import com.alipay.antchain.bridge.plugins.ethereum.EthereumBBCService;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import org.junit.Before;
import org.junit.Test;

public class EthPluginTestToolTest {

    // 测试入参
    AbstractBBCContext inContext;
    AbstractBBCService bbcService;

    // 测试主体结构
    EthPluginTestTool ethTestTool;
    String product;
    String url = "http://127.0.0.1:8545";
    String key = "0xef56b373888bae5e370852bfc8a0e7b98bc4a75d32f7428eca76c2bd7e8a2a56";
    long gasPrice = 2300000000L;
    long gasLimit = 3000000;

    String testConfig = "{\"gasLimit\":"+gasLimit+",\"gasPrice\":"+gasPrice+",\"privateKey\":\""+key+"\",\"url\":\""+url+"\"}";

    @Before
    public void setUp() throws Exception {
        inContext = new DefaultBBCContext();
        inContext.setConfForBlockchainClient(testConfig.getBytes());
        bbcService = new EthereumBBCService();

        ethTestTool = new EthPluginTestTool(inContext, bbcService);
    }

    @Test
    public void testStartupTest() {
        ethTestTool.startupTest();
    }

    @Test
    public void testShutdownTest() {
        ethTestTool.shutdownTest();
    }

    @Test
    public void testGetContextTest() {
        ethTestTool.getContextTest();
    }

    @Test
    public void testSetupAmContractTest() {
        ethTestTool.setupAmContractTest();
    }

    @Test
    public void testSetupSdpContractTest() {
        ethTestTool.setupSdpContractTest();
    }

    @Test
    public void testSetProtocolTest() throws Exception {
        ethTestTool.setProtocolTest();
    }

    @Test
    public void testQuerySdpMessageSeqTest() {
        ethTestTool.querySdpMessageSeqTest();
    }

    @Test
    public void testSetAmContractAndLocalDomainTest() {
        ethTestTool.setAmContractAndLocalDomainTest();
    }

    @Test
    public void testReadCrossChainMessageReceiptTest() {
        ethTestTool.readCrossChainMessageReceiptTest();
    }

    @Test
    public void testReadCrossChainMessageByHeightTest() {
        ethTestTool.readCrossChainMessageByHeightTest();
    }

    @Test
    public void testRelayAuthMessageTest() {
        ethTestTool.relayAuthMessageTest();
    }
}