//package com.ali.antchain;
//
//import com.ali.antchain.EthPluginTestTool;
//import com.ali.antchain.lib.service.EthereumBBCService;
//import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
//import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
//import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
//import junit.framework.TestCase;
//import org.junit.Before;
//import org.junit.Test;
//
//public class EthTest extends TestCase {
//    EthPluginTestTool ethtest;
//    AbstractBBCService service;
//    AbstractBBCContext context;
//    String product;
//    String url = "http://127.0.0.1:7545";
//    String key = "0xc52e50920ed5e3548e22152fb0e5d5fd13d49ab5bad98212a73027d6e1f1e7ef";
//    long gasPrice = 2300000000L;
//    long gasLimit = 3000000;
//    @Before
//    public void setUp() throws Exception {
//        service = new EthereumBBCService();
//        context = new DefaultBBCContext();
//        product = "simple-ethereum";
//        ethtest = new EthPluginTestTool(context,service,product);
//        ethtest.EthConfigInit(url,key,gasPrice,gasLimit);
//    }
//    @Test
//    public void testStartup() throws Exception {
//        ethtest.startup();
//    }
//
//    public void testShutdown() throws Exception {
//        ethtest.shutdown();
//    }
//
//    public void testGetcontext() throws Exception {
//        ethtest.getcontext();
//    }
//
//    public void testSetupAmContract() throws Exception {
//        ethtest.setupamcontract();
//    }
//
//    public void testSetupSDPContract() throws Exception {
//        ethtest.setupsdpcontract();
//    }
//
//    public void testQuerySDPMessageSeq() throws Exception {
//        ethtest.querysdpmessageseq();
//    }
//
//    public void testSetAmContractAndLocalDomain() throws Exception {
//        ethtest.setamcontractandlocaldomain();
//    }
//
//    public void testSetProtocol() throws Exception {
//        ethtest.setamcontractandlocaldomain();
//    }
//
//    public void testRelayamprepare() throws Exception {
//        ethtest.relayamprepare();
//    }
//    public void testReadcrosschainmessagereceipt() throws Exception {
//        ethtest.readcrosschainmessagereceipt();
//    }
//
//    public void testSetprotocol() throws Exception {
//        ethtest.setprotocol();
//    }
//}