package com.alipay.antchain.bridge.abstarct;

import com.alipay.antchain.bridge.exception.PluginTestToolException;

// 和插件测试框架约定的接口
public interface IPluginTestTool {

    public void startupTest() throws PluginTestToolException;

    public void shutdownTest() throws PluginTestToolException;

    public void getContextTest() throws PluginTestToolException;

    public void setupAmContractTest() throws PluginTestToolException;

    public void setupSdpContractTest() throws PluginTestToolException;

    public void setProtocolTest() throws Exception;

    public void querySdpMessageSeqTest() throws PluginTestToolException;

    public void setAmContractAndLocalDomainTest() throws PluginTestToolException;

    public void readCrossChainMessageReceiptTest() throws PluginTestToolException;

    public void relayAuthMessageTest() throws PluginTestToolException;

    public void readCrossChainMessageByHeightTest() throws PluginTestToolException;
}
