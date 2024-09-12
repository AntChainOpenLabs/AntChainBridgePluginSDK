package com.ali.antchain;

import com.ali.antchain.Test.*;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;

public class PluginsTest {

    AbstractBBCContext context;
    AbstractBBCService service;
    String product;

    public PluginsTest(AbstractBBCContext context, AbstractBBCService service,String product) {
        this.context = context;
        this.service = service;
        this.product = product;
    }

    public void startup() throws Exception {
        StartUpTest.run(context, service);
    }

    public void shutdown() throws Exception {
        ShutDownTest.run(context,service);
    }

    public void getcontext() throws Exception {
        GetContextTest.run(context,service);
    }

    public void setupamcontract() throws Exception {}

    public void setupsdpcontract() throws Exception {}

    public void querysdpmessageseq() throws Exception {}


    public void setamcontractandlocaldomain() throws Exception {

    }
    public void relayamprepare() throws Exception {
        RelayAmPrepare.run(context,service);
    }

    public void readcrosschainmessagereceipt() throws Exception {
        ReadCrossChainMessageReceipt.run(context,service);
    }

    public void setprotocol() throws Exception {
        SetProtocolTest.run(context,service,product);
    }


//    public static void main(String[] args) throws Exception{
//        String url = "http://127.0.0.1:7545";
//        String key = "0x45cfa3b6addf98274bfd9e3482a695406c7dcc99e1588bd6e0127b8fc06ae916";
//        long gasPrice = 2300000000L;
//        long gasLimit = 3000000;
//        PlugsTest test = new PlugsTest(url,key,gasPrice,gasLimit);
////        test.ethinit.init();
//        test.startup();
//    }


}
