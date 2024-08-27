package com.ali.antchain;

import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import junit.framework.TestCase;

public class TestPlugs extends TestCase {

//    String url = "http://127.0.0.1:7545";
//    String key = "0x45cfa3b6addf98274bfd9e3482a695406c7dcc99e1588bd6e0127b8fc06ae916";
//    long gasPrice = 2300000000L;
//    long gasLimit = 3000000;
    AbstractBBCContext context;
    AbstractBBCService service;

    PluginsTest plugsTest = new PluginsTest(context,service);

    public void testStartup() throws Exception {
        plugsTest.startup();
    }

    public void testShutdown() throws Exception {
        plugsTest.shutdown();
    }

    public void testGetcontext() throws Exception {
        plugsTest.getcontext();
    }

    public void testSetupamcontract() throws Exception {
        plugsTest.setupamcontract();
    }

    public void testQuerysdpmessageseq() throws Exception {
        plugsTest.querysdpmessageseq();
    }

    public void testSetprotocol() throws Exception {
        plugsTest.setprotocol();
    }


    public void testSetamcontractandlocaldomain() throws Exception {
        plugsTest.setamcontractandlocaldomain();
    }
}