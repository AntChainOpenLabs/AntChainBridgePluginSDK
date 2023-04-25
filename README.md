<div align="center">
  <img alt="am logo" src="https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/antchain.png" width="250" >
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


# 介绍

AntChain Bridge将跨链互操作解释为两个层次：通信和可信，即跨链的目标在于实现区块链实体之间的可信通信。

在AntChain Bridge的架构中，中继需要与区块链进行交互，而异构链的通信协议各式各样，无法统一适配，因此AntChain Bridge抽象出了区块链桥接组件（Blockchain Bridge Component, BBC），来解决区块链和跨链网络的通信问题。

每种异构链要接入AntChain Bridge跨链网络，都需要实现一套标准的区块链桥接组件，可以分为链上和链下两部分，包括**链下插件**和**系统合约**。链下插件需要基于SDK完成开发，链上部分则通常是智能合约，要求实现特定的[接口](antchain-bridge-spi/README.md)和逻辑，为降低开发难度，我们提供了Solidity版本的[实现](./pluginset/ethereum/onchain-plugin/solidity)。

AntChain Bridge为开发者提供了SDK、手册和系统合约模板，来帮助开发者完成插件和合约的开发。同时，AntChain Bridge提供了插件服务（[PluginServer](https://github.com/AntChainOpenLab/AntChainBridgePluginServer)）来运行插件，插件服务是一个独立的服务，具备插件管理和响应中继请求的功能。

在当前的工程实现中，BBC链下部分是以插件的形式实现的。AntChain Bridge实现了一套SDK，通过实现SDK中规定的接口（SPI），经过简单的编译，即可生成插件包。插件服务（PluginServer, PS）可以加载BBC链下插件，详情可以参考插件服务的介绍[文档]()。

以下介绍了插件的一个集成架构：

![](https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/deploy_arch.png)

插件SDK共有四个部分，包括：

- **antchain-bridge-commons**：包含很多工具方法和数据结构，帮助BBC实现快速开发；

- **antchain-bridge-plugin-lib**：BBC插件化的依赖库，给出一个注解`@BBCService`，帮助插件开发者可以快速完成插件构建；

- **antchain-bridge-plugin-manager**：插件的管理库，提供插件的加载、生命周期管理等能力，插件服务依赖于这个库；

- **antchain-bridge-spi**：主要包含了接口`IBBCService`，描述了一个BBC实现类应该有的功能，开发者只要依次实现接口即可，详细接口介绍请[见](./antchain-bridge-spi/README.md)；

  

# 构建

**在开始之前，请您确保安装了maven和JDK，这里推荐使用openjdk-1.8版本*

在项目根目录下面的`scripts`路径下，运行脚本完成编译和打包：

```
./package.sh 
```

可以在`scripts`下，看到一个压缩包：`antchain-bridge-sdk.tar.gz`



# 安装

**后续会提供maven源，当前仅支持本地安装*

在项目根目录下面的`scripts`路径下，运行脚本完成SDK的安装：

```
./install_sdk.sh
```

这样，SDK的Jar包就被安装在本地了。

可以通过在maven的pom.xml配置依赖就可以了，比如下面一段配置，`${antchain-bridge.sdk.version}`为当前仓库的版本号，可以在`install_sdk.sh`中看到。

```xml
<dependency>
    <groupId>com.alipay.antchain.bridge</groupId>
    <artifactId>antchain-bridge-plugin-lib</artifactId>
    <version>${antchain-bridge.sdk.version}</version>
</dependency>
<dependency>
    <groupId>com.alipay.antchain.bridge</groupId>
    <artifactId>antchain-bridge-plugin-manager</artifactId>
    <version>${antchain-bridge.sdk.version}</version>
</dependency>
<dependency>
    <groupId>com.alipay.antchain.bridge</groupId>
    <artifactId>antchain-bridge-spi</artifactId>
    <version>${antchain-bridge.sdk.version}</version>
</dependency>
<dependency>
    <groupId>com.alipay.antchain.bridge</groupId>
    <artifactId>antchain-bridge-commons</artifactId>
    <version>${antchain-bridge.sdk.version}</version>
</dependency>
```



# 快速开始

## Testchain

[Testchain](pluginset/demo-testchain)是一个用于讲解如何开发BBC插件的demo工程，结合AntChain Bridge的文档，可以更好地理解BBC的开发过程。

详细的开发教程请参考本仓库的[Wiki]()。

## 以太坊

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