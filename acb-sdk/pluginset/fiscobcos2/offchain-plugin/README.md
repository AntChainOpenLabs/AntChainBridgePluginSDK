<div align="center">
  <img alt="am logo" src="https://gw.alipayobjects.com/zos/bmw-prod/3ee4adc7-1960-4dbf-982e-522ac135a0c0.svg" width="250" >
  <h1 align="center">FISCO-BCOS 2 Plugin</h1>
  <p align="center">
    <a href="http://makeapullrequest.com">
      <img alt="pull requests welcome badge" src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat">
    </a>
  </p>
</div>



| 说明                | 版本              |
|-------------------|-----------------|
| ⭐️ fisco-sdk-java | `2.8.0`         |
| ✅ 测试通过的 fisco     | `2.11.0`国密链、标准链 |

# 介绍

在本路径之下，实现了fisco-bcos的异构链接入插件，包括链下插件及链上插件部分

- **offchain-plugin**：链下插件，使用maven管理的Java工程，使用maven编译即可。基于fisco`2.8.0`版本的javasdk开发，在fisco`2.0`国密链及标准链上测试通过。

# 用法

## 构建

在offchain-plugin下通过`mvn package`编译插件Jar包，可以在target下找到`fiscobcos2-bbc-1.0-SNAPSHOT-plugin.jar`

## 使用

参考[插件服务](https://github.com/AntChainOpenLab/AntChainBridgePluginServer/blob/main/README.md)（PluginServer, PS）的使用，将Jar包放到指定路径，通过PS加载即可。

### 配置文件

当在AntChainBridge的Relayer服务注册fisco-bcos2.0时，需要指定PS和链类型（fiscobcos2），同时需要提交一个fisco链的配置。

fisco-bcos2.0链的配置文件`fiscobcos.json`主要包括链ssl证书信息和节点网络连接信息。

#### 标准链配置文件

当fisco链为标准链时，配置文件大致如下（配置文件中涉及证书路径均为绝对路径）：

```json
{
  "certPath": "/path/to/sdk",
  "caCert": "/path/to/sdk/ca.crt",
  "sslCert": "/path/to/sdk/sdk.crt",
  "sslKey": "/path/to/sdk/sdk.key",
  "connectPeer": "127.0.0.1:20200",
  "groupID": "1"
}
```

- certPath：sdk证书目录路径
- caCert：sdk ca证书路径
- sslCert：sdk ssl证书路径
- sslKey：sdl ssl私钥路径
- connectPeer：连接节点ip及端口
- groupID：连接节点所在groupId，默认为1


#### 国密链配置文件

当fisco链为国密链时，配置文件大致如下：

```json
{
  "certPath": "/path/to/sdk/gm",
  "caCert": "/path/to/sdk/gm/gmca.crt",
  "sslCert": "/path/to/sdk/gm/gmsdk.crt",
  "sslKey": "/path/to/sdk/gm/gmsdk.key",
  "enSslCert": "/path/to/sdk/gm/gmensdk.crt",
  "enSslKey": "/path/to/sdk/gm/gmensdk.key",
  "connectPeer": "127.0.0.1:20200",
  "groupID": "1",
  "useSMCrypto": "true"
}
```
国密链配置文件中多链以下几项：
- enSslCert：sdk 国密ssl证书路径
- enSslKey：sdk 国密ssl私钥路径
- useSMCrypto：国密链标识，国密链需要添加该标识，标准链默认为`false`

[参考fisco2.0官方安装文档](https://fisco-bcos-documentation.readthedocs.io/zh-cn/latest/docs/installation.html#:~:text=cp%20%2Dr%20nodes/127.0.0.1/sdk/*%20console/conf/)， 
这些证书均可以在链的安装目录`node/127.0.0.1/sdk`下找到，例如fisco2.0国密链的相应安装目录应如下：

```shell
 $ tree sdk
sdk
├── ca.crt
├── cert.cnf
├── gm
│   ├── gmca.crt
│   ├── gmensdk.crt
│   ├── gmensdk.key
│   ├── gmsdk.crt
│   ├── gmsdk.key
│   └── gmsdk.publickey
├── sdk.crt
└── sdk.key

2 directories, 10 files
```