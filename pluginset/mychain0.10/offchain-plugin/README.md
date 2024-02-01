<div align="center">
  <img alt="am logo" src="https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/antchain.png" width="250" >
  <h1 align="center">Mychain010 Plugin</h1>
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

蚂蚁链插件（Mychain010 Plugin）是针对mychain_0.10版本开发的蚂蚁链链下插件，基于Mychain的Java SDK实现各种链上交互功能。

# 构建

**！！！目前存在部分内部依赖发布中，可能暂时无法成功构建，发布完成后即可使用以下命令进行构建**

在`pluginset/mychain0.10/offchain-plugin`目录运行maven命令，
在`pluginset/mychain0.10/offchain-plugin/target`目录下会生成插件jar包`plugin-testchain1-0.1-SNAPSHOT-plugin.jar`。

```
cd pluginset/mychain0.10/offchain-plugin && mvn package -Dmaven.test.skip=true
```

# 使用

将构建的插件包放置在插件服务的plugins目录下即可使用（具体参考[插件服务使用手册](https://github.com/AntChainOpenLabs/AntChainBridgePluginSDK/wiki/3.-AntChain-Bridge%E8%B7%A8%E9%93%BE%EF%BC%9A%E6%8F%92%E4%BB%B6%E6%9C%8D%E5%8A%A1)）。

## 插件类型
当前蚂蚁链插件的插件类型定义为`mychain010`，插件名称定义为`plugin-mychain010`，
您可以在`com/alipay/antchain/bridge/plugins/mychain/Mychain010BBCService.java:37`处进行自定义修改。

## 插件配置文件
使用插件需要特定的配置文件，蚂蚁链插件配置模板及介绍如下：

```shell
{
  "mychain_primary": "127.0.0.1 18130",
  "mychain_sslKey": "",
  "mychain_sslKeyPass": "",
  "mychain_sslCert": "",
  "mychain_trustStore": "",
  "mychain_trustStorePassword": "",
  "mychain_anchor_account": "",
  "mychain_anchor_account_pri_key": "",
  "mychain_anchor_account_pub_key": "",
  "wasm": "true",
  "contract_binary_version": "v1.5.0"
}
```

- mychain_primary：蚂蚁链节点地址，提供一个节点地址即可，包含节点网络IP和端口；
- mychain_sslKey：蚂蚁链SSL连接客户端私钥，直接读取PEM格式私钥即可（默认名称为`client.key`），可使用`cat client.key | tr '\n' '|' | sed 's/|/\\n/g' >client_key.txt`命令进行转换；
- mychain_sslKeyPass：蚂蚁链SSL连接客户端私钥解冻密码；
- mychain_sslCert：蚂蚁链SSL连接客户端证书，直接读取证书内容即可（默认名称为`client.crt`），可使用`cat client.crt | tr '\n' '|' | sed 's/|/\\n/g' >client_crt.txt`命令进行转换；
- mychain_trustStore：蚂蚁链SSL连接信任根信息，可根据`trustCa`或`ca.crt`进行转换，`trustCa`文件可直接使用base64编码转换即可，ca.crt信任根证书到trustCa信任根文件转换方式如下：
```shell
# 以下为非国密链转换方式，国密链请向相关技术人员请求支持
# keytool 为是Java提供的密钥（Key）和证书（Certificate）管理工具
keytool -import -file ca/ca.crt -noprompt -keystore trustCa -storepass mychain
keytool -exportcert -keystore ./trustCa -rfc -file ./ca.crt -alias ca
```
- mychain_trustStorePassword：蚂蚁链SSL连接信任根信息密码；
- mychain_anchor_account：插件账户名称；
- mychain_anchor_account_pri_key：插件账户私钥；
- mychain_anchor_account_pub_key：插件账户公钥；
- wasm：是否支持wasm合约部署，选填`true`或`false`，设置为`true`时插件部署合约时会自动部署wasm合约；
- contract_binary_version：蚂蚁链插件合约版本，默认为`v1.5.0`，建议使用默认版本。