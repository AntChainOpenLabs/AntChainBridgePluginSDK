<div align="center">
  <img alt="am logo" src="https://gw.alipayobjects.com/zos/bmw-prod/3ee4adc7-1960-4dbf-982e-522ac135a0c0.svg" width="250" >
  <h1 align="center">AntChain Bridge Plugin SDK</h1>
  <p align="center">
    <a href="http://makeapullrequest.com">
      <img alt="pull requests welcome badge" src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat">
    </a>
    <a href="https://www.java.com">
      <img alt="Language" src="https://img.shields.io/badge/Language-Java-blue.svg?style=flat">
    </a>
    <a href="https://github.com/AntChainOpenLab/AntChainBridgePluginSDK/graphs/contributors">
      <img alt="GitHub contributors" src="https://img.shields.io/github/contributors/AntChainOpenLab/AntChainBridgePluginSDK">
    </a>
    <a href="https://www.apache.org/licenses/LICENSE-2.0">
      <img alt="License" src="https://img.shields.io/github/license/AntChainOpenLab/AntChainBridgePluginSDK?style=flat">
    </a>
  </p>
</div>


# Introduction

在AntChain Bridge的跨链架构中，中继和区块链之间的交互，需要通过区块链桥接组件（Blockchain Bridge Component, BBC）来完成。

在当前的工程实现中，BBC是以插件的形式实现的。AntChain Bridge实现了一套SDK，通过实现SDK中规定的接口（SPI），经过简单的编译，即可生成插件包。AntChain Bridge提供了插件服务（PluginServer, PS）用来加载BBC插件，详情可以参考插件服务的介绍文档。

插件工程共有四个部分，包括：

- **antchain-bridge-commons**：包含很多工具方法和数据结构，帮助BBC实现快速开发；

- **antchain-bridge-plugin-lib**：BBC插件化的依赖库，给出一个注解`@BBCService`，帮助插件开发者可以快速完成插件构建；

- **antchain-bridge-plugin-manager**：插件的管理库，提供插件的加载、生命周期管理等能力，插件服务依赖于这个库；

- **antchain-bridge-spi**：主要包含了接口`IBBCService`，描述了一个BBC实现类应该有的功能，开发者只要依次实现接口即可；

  

# Build

在项目根目录下面的`scripts`路径下，运行脚本完成编译和打包：

```
./package.sh 
```

可以在`scripts`下，看到一个压缩包：`antchain-bridge-sdk.tar.gz`



# Install

**后续会提供maven源，当前仅支持本地安装*

在项目根目录下面的`scripts`路径下，运行脚本完成SDK的安装：

```
./install_sdk.sh
```

这样，SDK的Jar包就被安装在本地了。



# Demo

## Testchain

[Testchain](pluginset/demo-testchain)是一个用于讲解如何开发BBC插件的demo工程，结合AntChain Bridge的文档，可以更好地理解BBC的开发过程。

## Ethereum

基于SDK，我们开发了一个打通以太坊的BBC[插件](./pluginset/ethereum)。

进入以太坊插件的路径下，可以看到以下文件：

```
# tree -L 4 .        
.
├── offchain-plugin
│   ├── README.md
│   ├── pom.xml
│   └── src
└── onchain-plugin
    ├── README.md
    └── solidity
        ├── scenarios
        │   └── nft_crosschain
        └── sys
            ├── AppContract.sol
            ├── AuthMsg.sol
            ├── SDPMsg.sol
            ├── interfaces
            └── lib
```

- **offchain-plugin**工程下面，我们基于`Web3j`，实现了以太坊的BBC插件的链下部分；
- **onchain-plugin**工程下面，主要分为两部分：
  - **sys**：包含以太坊的BBC链上部分，实现了AM、SDP等逻辑。
  - **scenarios**：本路径下的`nft_crosschain`中，我们实现了一套跨链桥方案，用于ERC1155资产的跨链。

详细操作请[见](pluginset/ethereum/offchain-plugin/README.md)。

# Community

AntChain Bridge 欢迎您以任何形式参与社区建设。

您可以通过以下方式参与社区讨论

- 钉钉

![scan dingding](https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/dingding.png)

- 微信

![scan_wechat](https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/wechat.png)

- 邮件

发送邮件到`antchainbridge@service.alipay.com`

# License

详情参考[LICENSE](./LICENSE)。