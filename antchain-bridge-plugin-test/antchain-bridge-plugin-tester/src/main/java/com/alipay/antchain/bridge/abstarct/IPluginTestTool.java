package com.alipay.antchain.bridge.abstarct;

// 和插件测试框架约定的接口
public interface IPluginTestTool {

    public void startupTest();

    public void shutdownTest();

    public void getContextTest();

    public void setupAmContractTest();

    public void setupSdpContractTest();

    public void setProtocolTest() throws Exception;

    public void querySdpMessageSeqTest();

    public void setAmContractAndLocalDomainTest();

    public void readCrossChainMessageReceiptTest();

    public void relayAuthMessageTest();

    public void readCrossChainMessageByHeightTest();
}
