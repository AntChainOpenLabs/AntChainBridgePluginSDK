<div align="center">
  <img alt="am logo" src="https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/antchain.png" width="250" >
  <h1 align="center">Hyperchain2.0 Plugin</h1>
  <p align="center">
    <a href="http://makeapullrequest.com">
      <img alt="pull requests welcome badge" src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat">
    </a>
    <a href="https://www.java.com">
      <img alt="Language" src="https://img.shields.io/badge/Language-Java-blue.svg?style=flat">
    </a>
    <a href="https://www.apache.org/licenses/LICENSE-2.0">
      <img alt="License" src="https://img.shields.io/github/license/AntChainOpenLab/AntChainBridgeRelayer?style=flat">
    </a>
  </p>
</div>

# 介绍

趣链插件（Hyperchain2.0 Plugin）是针对hyperchain_2.0版本开发的链下插件，基于hyperchain的Java SDK实现各种链上交互功能。

# 构建

在`pluginset/hyperchain2.0/offchain-plugin`目录运行maven命令，
在`pluginset/hyperchain2.0/offchain-plugin/target`目录下会生成插件jar包`hyperchain2-bbc-0.1.0-plugin.jar`。

```
cd pluginset/hyperchain2.0/offchain-plugin && mvn package -Dmaven.test.skip=true
```

# 使用

将构建的插件包放置在插件服务的plugins目录下即可使用（具体参考[插件服务使用手册](https://github.com/AntChainOpenLabs/AntChainBridgePluginSDK/wiki/3.-AntChain-Bridge%E8%B7%A8%E9%93%BE%EF%BC%9A%E6%8F%92%E4%BB%B6%E6%9C%8D%E5%8A%A1)）。

## 插件类型
当前蚂蚁链插件的插件类型定义为`hyperchain2`，插件名称定义为`plugin-hyperchain2`，
您可以在`com/alipay/antchain/bridge/plugins/hyperchain/HyperchainBBCService.java:42`处进行自定义修改。

## 插件配置文件
使用插件需要特定的配置文件，趣链插件配置模板`hyperchain2_template.json`介绍如下：

```shell
{
  "url": "127.0.0.1:43221",
  "accountJson": "{\"address\":\"9c5415f760e31712ae69315f9ea1e8c769ce2290\",\"publicKey\":\"0471a8c8……58ef0f9e\",\"privateKey\":\"a3352ecf……7044a868\",\"version\":\"4.0\",\"algo\":\"0x13\"}",
  "password": ""
}
```

- url：hyperchain节点地址，提供一个节点地址即可，包含节点网络IP和端口；
- accountJson：hyperchain账户信息，包含账户公私钥信。如果需要新创建一个新链上账户，可以将`hyperchain2_withoutAccount.json`配置文件中的链url修改为您的实际链url后，执行`com.alipay.antchain.bridge.plugins.hyperchain.HyperchainBBCServiceTest.genAccount`单测方法创建一个新的账户，该单测方法会打印出账户信息json字符串；
- password：hyperchain账户相应的密码。如果账户为`com.alipay.antchain.bridge.plugins.hyperchain.HyperchainBBCServiceTest.genAccount`单测方法生成，该方法的默认账户密码为空字符串。