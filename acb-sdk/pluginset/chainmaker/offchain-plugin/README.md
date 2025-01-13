<div align="center">
  <img alt="am logo" src="https://gw.alipayobjects.com/zos/bmw-prod/3ee4adc7-1960-4dbf-982e-522ac135a0c0.svg" width="250" >
  <h1 align="center">ChainMaker Plugin</h1>
  <p align="center">
    <a href="http://makeapullrequest.com">
      <img alt="pull requests welcome badge" src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat">
    </a>
  </p>
</div>


# 介绍

在本路径之下，实现了长安链的异构链接入插件，主要包括链下插件部分，链上插件合约可复用以太坊链上合约。

- **offchain-plugin**：链下插件，使用maven管理的Java工程，使用maven编译即可。基于长安链2.3.2版本的javasdk开发，在长安链3.0上测试通过。

# 用法

## 构建

在offchain-plugin下通过`mvn package`编译插件Jar包，可以在target下找到`chainmaker-bbc-0.1.0-plugin.jar`

## 使用

参考[插件服务](https://github.com/AntChainOpenLab/AntChainBridgePluginServer/blob/main/README.md)（PluginServer, PS）的使用，将Jar包放到指定路径，通过PS加载即可。

### 配置文件

当在AntChainBridge的Relayer服务注册长安链时，需要指定PS和链类型（chainmaker），同时需要提交一个长安链的配置，用来初始化长安链插件的`BBCService`实例。

长安链的配置文件`chainmaker.json`主要包括管理员信息和sdk配置信息，根据链证书和sdk配生成。
其中链证书可以在链安装目录找到，
sdk配置模板可参考`src/test/resources/sdk_config.yml`。
最终配置文件`chainmaker.json`的生成方法可以参考单元测试`com.alipay.antchain.bridge.plugins.chainmaker.ConfigUtilsTest`。


- 将链证书`crypto-config`和sdk配置文件`sdk_config.yml`一同放在`test/resources`目录，大致如下（具体证书文件省略）：
```
├── crypto-config
│   ├── wx-org1.chainmaker.org
│   │   ├── ca
│   │   ├── node
│   │   └── user
│   ├── wx-org2.chainmaker.org
│   │   ├── ca
│   │   ├── node
│   │   └── user
│   ├── wx-org3.chainmaker.org
│   │   ├── ca
│   │   ├── node
│   │   └── user
│   └── wx-org4.chainmaker.org
│       ├── ca
│       ├── node
│       └── user
└── sdk_config.yml

18 directories, 1 file
```

- 执行`com.alipay.antchain.bridge.plugins.chainmaker.ConfigUtilsTest.testGeneratorConfig`测试方法，
即可在根目录下生成链注册时需要用到的配置文件`chainmaker.json`，文件模板大致如下：
```json
{
  "adminCertPaths": [
    "LS0tLS1C...LS0tCg==",
    "LS0tLS1C...LS0tLS0K",
    "LS0tLS1C...LS0tLS0K"
  ],
  "adminKeyPaths": [
    "LS0tLS1C...LS0tLQo=",
    "LS0tLS1C...LS0tLQo=",
    "LS0tLS1C...LS0tLQo="
  ],
  "adminTlsCertPaths": [
    "LS0tLS1C...LS0tLS0K",
    "LS0tLS1C...LS0tLS0K",
    "LS0tLS1C...LS0tLS0K"
  ],
  "adminTlsKeyPaths": [
    "LS0tLS1C...LS0tLQo=",
    "LS0tLS1C...LS0tLQo=",
    "LS0tLS1C...LS0tLQo="
  ],
  "orgIds": [
    "wx-org1.chainmaker.org",
    "wx-org2.chainmaker.org",
    "wx-org3.chainmaker.org"
  ],
  "sdkConfig": "{\"chainClient\":{\"chainId\":\"chain1\",\"orgId\":\"wx-org1.chainmaker.org\",\"userKeyFilePath\":\"src/main/resources/crypto-config/wx-org1.chainmaker.org/user/client1/client1.tls.key\",\"userCrtFilePath\":\"src/main/resources/crypto-config/wx-org1.chainmaker.org/user/client1/client1.tls.crt\",\"userSignKeyFilePath\":\"src/main/resources/crypto-config/wx-org1.chainmaker.org/user/client1/client1.sign.key\",\"userSignCrtFilePath\":\"src/main/resources/crypto-config/wx-org1.chainmaker.org/user/client1/client1.sign.crt\",\"userKeyBytes\":[45,45,45,45,...,45,45,45,10],\"userCrtBytes\":[45,45,45,45,...,45,45,45,10],\"userSignKeyBytes\":[45,45,45,45,...,45,45,45,10],\"userSignCrtBytes\":[45,45,45,45,...,45,45,45,10],\"authType\":\"permissionedWithcert\",\"retryLimit\":10,\"retryInterval\":500,\"nodes\":[{\"nodeAddr\":\"127.0.0.1:12301\",\"connCnt\":10,\"enableTls\":true,\"trustRootPaths\":[\"src/main/resources/crypto-config/wx-org1.chainmaker.org/ca\"],\"trustRootBytes\":[[45,45,45,45,...,45,45,45,10]],\"tlsHostName\":\"chainmaker.org\"}],\"rpcClient\":{\"maxReceiveMessageSize\":100},\"archiveCenterQueryFirst\":false,\"pkcs11\":{\"enabled\":false},\"enableTxResultDispatcher\":true}}"
}
```