package com.ali.antchain.abstarct;

// 和插件测试框架约定的接口
public interface IPluginTestTool {

    public void startupTest();

    public void shutdownTest();

    public void getcontextTest();

    public void setupamcontractTest();

    public void setupsdpcontractTest();

    public void setprotocolTest();

    public void querysdpmessageseqTest();

    public void setamcontractandlocaldomainTest();

//    public void relayamprepare() throws Exception {
//        RelayAmPrepare.run(context, service);
//    }

    public void readcrosschainmessagereceiptTest();

    public void relayauthmessageTest();

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
