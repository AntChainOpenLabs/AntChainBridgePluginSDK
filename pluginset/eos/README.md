<div align="center">
  <img alt="am logo" src="https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/antchain.png" width="250" >
  <h1 align="center">AntChain Bridge EOS Plugin</h1>
</div>


# 介绍

在本路径之下，实现了EOS链的异构链接入插件，包括链上的智能合约和链下的插件。

- **offchain-plugin**：链下插件，使用maven管理的Java工程，使用maven编译即可。
- **onchain-plugin**：基于EOS的合约，实现了AM和SDP协议。

# 用法

## 链上合约

参考该[文档](./onchain-plugin/README.md)。

## 链下插件

### 构建

在offchain-plugin下，通过`mvn package`编译插件Jar包，可以在target下找到`eos-bbc-0.1-SNAPSHOT-plugin.jar`

### 使用

参考[插件服务](https://github.com/AntChainOpenLab/AntChainBridgePluginServer/blob/main/README.md)（PluginServer, PS）的使用，将Jar包放到指定路径，通过PS加载即可。

### 配置

当在AntChainBridge的Relayer服务注册EOS链时，需要指定PS和链类型（eos），同时需要提交一个EOS链的配置，用来初始化EOS插件的`BBCService`实例。

配置采用Json格式，这里给出配置的模板和解释：

```json
{
    "amContractAddressDeployed":"am",
    "maxIrreversibleWaitCount":30,
    "sdpContractAddressDeployed":"sdp",
    "url":"http://127.0.0.1:8888",
    "userName":"relayer1",
    "userPriKey":"5JzGkEmpSQWb92DASJi...aCWN4VZQc6uAxrQLAr",
    "waitTimeOnce":500,
    "waitUtilTxIrreversible":true
}
```

在注册链之前，需要提前部署AM和SDP合约，请参考链上合约一节。

- url：EOS节点的URL；
- amContractAddressDeployed：部署的AM合约的账户名字；
- sdpContractAddressDeployed：部署的SDP合约的账户名字；
- userName：`BBCService`实例使用的链账户名字，用于和EOS链交互；
- userPriKey：`BBCService`实例使用的链账户对应的私钥；
- waitUtilTxIrreversible：发送交易之后，是否等待交易不可逆，默认为false；
- waitTimeOnce：插件发送交易后，轮询交易结果的间隔时间，单位为ms，默认为500ms；
- maxIrreversibleWaitCount：最大轮询次数，默认为30次

