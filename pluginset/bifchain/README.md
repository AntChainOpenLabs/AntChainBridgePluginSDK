<div align="center">
  <img alt="am logo" src="https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/antchain.png" width="250" >
  <h1 align="center">AntChain Bridge BIF Plugin</h1>
</div>



# 介绍

在本路径之下，实现了星火BIF链的异构链链下接入插件（**offchain-plugin**），是使用maven管理的Java工程，使用maven编译即可。

# 用法

## 链上合约

包括AuthMsg合约、SDPMsg合约和PtcHub合约，采用solidity语言编写，编译后的abi在`offchain-plugin\src\main\resources`目录下。

## 链下插件

### 构建

在offchain-plugin下，通过`mvn package`编译插件Jar包（最好跳过单元测试），可以在target下找到`bif-bbc-plugin-0.1-SNAPSHOT-plugin.jar`。

`注意：在项目maven时，若出现找不到bif-chain-sdk的1.1.0组件时，在星火插件根目录下放着已经编译好的组件，请安装到本地即可使用。`

### 使用

参考[插件服务](https://github.com/AntChainOpenLab/AntChainBridgePluginServer/blob/main/README.md)（PluginServer, PS）的使用，将Jar包放到指定路径，通过PS加载即可。

### 配置

当在AntChainBridge的Relayer服务注册星火链时，需要指定链product为`bifchain`，同时需要提交一个星火链的配置，用来初始化bif插件的`BBCService`实例。

配置采用Json格式，这里给出配置的模板和解释：

```json
{
    "url": "http://test.bifcore.bitfactory.cn",
    "privateKey": "priSPKt9wkwEwc4suujrhf5Rxwdpxryx5QjBkuQyHjejKAwUBm",
    "address": "did:bid:efkdYHzcgLiHHCq1SKayMtqVHpxveSDD",
    "ptcContractInitInput": "000014020000000001000000310100280000006469643a6269643a6566433551766f5239557776586f75734a42324d6f723172336a5744665341430200010000000003003b000000000035000000000001000000010100280000006469643a6269643a656631366d61755739756b42624c715970625a38623762617658545065474d43040008000000b6a124670000000005000800000036d50569000000000600e70000000000e100000000001a000000726f6f745f76657269666961626c655f63726564656e7469616c01003b000000000035000000000001000000010100280000006469643a6269643a65666b6459487a63674c694848437131534b61794d747156487078766553444402007a0000007b227075626c69634b6579223a5b7b2274797065223a2245443235353139222c227075626c69634b6579486578223a2262303635363662383735353463653138383034613133313537663030323764393336633633373762666263626632343435373865393364373631646135356234353931633239227d5d7d070088000000000082000000000003000000534d33010020000000928a49223a69832e612f3308f7b8562fb070159024a46d22d608fd31665c9e2202000700000045643235353139030040000000ffc14376db14849e4b13232a14a6279ba2a5ce8d143c8971b8165cbac9889193c18e6e17b66bef2366f959b858b0ccededf797cf5e87068309fa47663221f80f"
}
```

- url：bif节点的URL；
- privateKey：`BBCService`实例使用的链账户对应的私钥；
- address：`BBCService`实例使用的链账户对应的地址；
- ptcContractInitInput：部署ptc合约时的初始化参数，实际为星火BCDNS的根证书十六进制表示。



