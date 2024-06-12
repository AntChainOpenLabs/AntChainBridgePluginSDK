<div align="center">
  <img alt="am logo" src="https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/antchain.png" width="250" >
  <h1 align="center">AntChain Bridge BIF Plugin</h1>
</div>



# 介绍

在本路径之下，实现了星火BIF链的异构链链下接入插件（**offchain-plugin**），是使用maven管理的Java工程，使用maven编译即可。

# 用法

## 链上合约

星火BIF链支持EVM，可以采用和以太坊完全相同的链上合约，合约内容参考[文档](../ethereum/onchain-plugin/README.md)。

## 链下插件

### 构建

在offchain-plugin下，通过`mvn package`编译插件Jar包，可以在target下找到`bif-bbc-plugin-0.1-SNAPSHOT-plugin.jar`

### 使用

参考[插件服务](https://github.com/AntChainOpenLab/AntChainBridgePluginServer/blob/main/README.md)（PluginServer, PS）的使用，将Jar包放到指定路径，通过PS加载即可。

### 配置

当在AntChainBridge的Relayer服务注册bif链时，需要指定PS和链类型（simple-bifchain），同时需要提交一个bif链的配置，用来初始化bif插件的`BBCService`实例。

配置采用Json格式，这里给出配置的模板和解释：

```json
{
    "url": "http://test.bifcore.bitfactory.cn",
    "privateKey": "priSPKt9wkwEwc4suujrhf5Rxwdpxryx5QjBkuQyHjejKAwUBm",
    "address": "did:bid:efkdYHzcgLiHHCq1SKayMtqVHpxveSDD"
}
```

- url：bif节点的URL；
- privateKey：`BBCService`实例使用的链账户对应的私钥；
- address：`BBCService`实例使用的链账户对应的地址；

