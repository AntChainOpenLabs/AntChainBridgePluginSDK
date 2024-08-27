package com.ali.antchain;

import com.ali.antchain.service.EthereumBBCService;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

public class EthTest extends TestCase {
    EthPluginsTest ethtest;
    AbstractBBCService service;
    AbstractBBCContext context;
    String url = "http://127.0.0.1:7545";
    String key = "0x45cfa3b6addf98274bfd9e3482a695406c7dcc99e1588bd6e0127b8fc06ae916";
    long gasPrice = 2300000000L;
    long gasLimit = 3000000;
    @Before
    public void setUp() throws Exception {
        service = new EthereumBBCService();
        context = new DefaultBBCContext();
        ethtest = new EthPluginsTest(context,service);
        ethtest.EthConfigInit(url,key,gasPrice,gasLimit);
    }
    @Test
    public void testStartup() throws Exception {
        ethtest.startup();
    }

    public void testShutdown() throws Exception {
        ethtest.shutdown();
    }

    public void testGetcontext() throws Exception {
        ethtest.getcontext();
    }

    public void testSetupAmContract() throws Exception {
        ethtest.setupamcontract();
    }

    public void testSetupSDPContract() throws Exception {
        ethtest.setupsdpcontract();
    }

    public void testQuerySDPMessageSeq() throws Exception {
        ethtest.querysdpmessageseq();
    }

    public void testSetAmContractAndLocalDomain() throws Exception {
        ethtest.setamcontractandlocaldomain();
    }

    public void testSetProtocol() throws Exception {
        ethtest.setamcontractandlocaldomain();
    }
}