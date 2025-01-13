<div align="center">
  <img alt="am logo" src="https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/antchain.png" width="250" >
  <h1 align="center">Committee Supervisor </h1>
  <p align="center">
    <a href="http://makeapullrequest.com">
      <img alt="pull requests welcome badge" src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat">
    </a>
    <a href="https://www.java.com">
      <img alt="Language" src="https://img.shields.io/badge/Language-Java-blue.svg?style=flat">
    </a>
    <a href="https://github.com/AntChainOpenLab/AntChainBridgeRelayer/graphs/contributors">
      <img alt="GitHub contributors" src="https://img.shields.io/github/contributors/AntChainOpenLab/AntChainBridgeRelayer">
    </a>
    <a href="https://www.apache.org/licenses/LICENSE-2.0">
      <img alt="License" src="https://img.shields.io/github/license/AntChainOpenLab/AntChainBridgeRelayer?style=flat">
    </a>
  </p>
</div>

# 介绍

[AntChain Bridge蚂蚁链跨链桥技术产品](https://github.com/AntChainOpenLabs/AntChainBridgePluginSDK/wiki/1.-AntChain-Bridge%E8%B7%A8%E9%93%BE%EF%BC%9A%E4%BB%8B%E7%BB%8D)中证明转换组件（Proof Transformation Component, PTC）为区块链账本数据提供验证和背书能力。委员会PTC（Committee PTC）是PTC的一种证明方式，委员会PTC中的Supervisor节点提供将中继Relayer依赖的证明转换组件PTC向区块链域名服务系统BCDNS注册的能力。

委员会PTC注册到域名服务系统BCDNS，需要经过以下几个步骤：

- **生成Supervisor节点账户**：Supervisor节点生成账户身份；
- **准备PTC证书CSR**：Supervisor节点生成对PTC证书的证书签名请求（Certificate Signing Request, CSR）；
- **签署PTC证书**：BCDNS管理员签署PTC证书文件；
- **构造PTC信任根**：Supervisor节点收集委员会节点证书，生成信任根PTCTrustRoot；
- **上传PTC信任根**：Supervisor节点向区块链域名服务系统BCDNS提交信任根。

Supervisor节点CLI工具是用于管理委员会PTC的交互式命令行工具，它可以完成证明转换组件PTC的注册、停止服务等工作。

# 构建

**在开始之前，请您确保安装了maven和JDK，这里推荐使用[jdk-21](https://adoptium.net/zh-CN/temurin/releases/?version=21)版本*

**确保安装了AntChain Bridge Plugin SDK，详情请[见](https://github.com/AntChainOpenLabs/AntChainBridgePluginSDK?tab=readme-ov-file#%E6%9E%84%E5%BB%BA)**

## 本地安装

在committee-supervisor模块根目录运行maven命令即可：

```Shell
cd supervisor && mvn package -Dmaven.test.skip=true
```

这样，委员会PTC的Supervisor节点服务Jar包就被安装在本地了。

在`supervisor/target`目录下会生成一个压缩包`supervisor-cli-bin.tar.gz`，解压该压缩包即可使用。

解压编译生成包后可以看到文件如下：
```shell
target/supervisor-cli
├── README.md
├── bin
│   └── start.sh
└── lib
    └── supervisor-cli.jar

3 directories, 3 files
```

## 启动

查看脚本帮助信息：

```shell
$ ./supervisor-cli/bin/start.sh -h

start.sh - Start the Committee-ptc Supervisor Command Line Interface Tool

 Usage:
   start.sh <params>

 Examples:
  1. start with the default supervisor config file `supervisor-cli/conf/config.json`：
   start.sh
  2. start with specific supervisor config file:
   start.sh -c ${Path}/supervisor-cli-config.json}

 Options:
   -c         config path of Supervisor.
   -h         print help information.
```

启动命令执行情况如下：

```shell
$ ./bin/start.sh

   _____ __  ______  __________ _    ___________ ____  ____
  / ___// / / / __ \/ ____/ __ \ |  / /  _/ ___// __ \/ __ \
  \__ \/ / / / /_/ / __/ / /_/ / | / // / \__ \/ / / / /_/ /
 ___/ / /_/ / ____/ /___/ _, _/| |/ // / ___/ / /_/ / _, _/
/____/\____/_/   /_____/_/ |_| |___/___//____/\____/_/ |_|

                             CLI 0.1.0-SNAPSHOT

supervisor:>
```

启动成功后即可在`supervisor:>`启动符后执行cli命令。

## 1 Supervisor节点管理

Supervisor-CLI工具提供PTC管理相关指令，用于支持PTC向BCDNS申请注册、构造、上传、更新信任根证书等功能。

### 1.1 generatePTCAccount 生成Supervisor节点账户

生成Supervisor节点账户，与区块链域名服务系统BCDNS交互的超级节点，与BCDNS的交互需要该节点的签名

命令参数如下：

- `--keyAlgo`：（可选）Supervisor节点的账户生成算法类型，目前支持`Keccak256WithSecp256k1`(默认)、`SHA256withRSA`、`SHA256withECDSA`、`SM3withSM2`、`Ed25519`加密算法；
- `--outDir`：（可选）默认Supervisor根目录，Supervisor节点账户的公、私钥文件存储路径，后续用到Supervisor节点身份时从该路径获取；
- `--committeeId`：（必选）要申请的committee id；

用法如下：

```shell
relayer:> generate-ptc-account --committeeId default --keyAlgo Keccak256WithSecp256k1 --outDir /path/to/certs/
private key path: /path/to/certs/private_key.pem
public key path: /path/to/certs/public_key.pem
``` 

### 1.2 generatePtcCSR 准备PTC证书CSR

证书申请需要申请者构造好PTC证书的证书签名请求`Certificate Signing Request, CSR`。

命令：`generatePtcCSR`以 Base64 格式生成PTC证书签名请求。

命令参数如下：

- `--certVersion`：（可选）PTC跨链证书版本，通过版本提供PTC证书更新升级，默认值为1；
- `--credSubjectName`：（可选）跨链证书的PTC凭证主体名称，默认为myPTC；
- `--oidType`：（可选）拥有PTC跨链证书的对象身份类型，支持`X509_PUBLIC_KEY_INFO`和`BID`两种类型，该项默认为`X509_PUBLIC_KEY_INFO`；
- `--ptcType`：（可选）证明转换组件PTC服务类型，支持外部验证`EXTERNAL_VERIFIER`、委员会验证`COMMITTEE`、中继链验证`RELAY_CHAIN`，该项默认为`COMMITTEE`
- `--pubkeyFile`：（必选）Supervisor节点的公钥证书文件路径，即`1.1 generatePTCAccount`生成的账户公钥所存放的文件路径，例如`/path/to/certs/public_key.pem`

用法如下：

```shell
relayer:> generatePtcCSR --certVersion 2 --credSubjectName myPTC --oidType X509_PUBLIC_KEY_INFO --ptcType COMMITTEE --pubkeyFile /path/to/certs/public_key.pem
your CSR is 
AADGAAAAAAABAAAAMQIAAQAAAAIEAAgA...UglB72yehxIryiy2UQp+iC/xBAAAAAAA
``` 

### 1.3 签署证书

`1.2 generatePtcCSR`生成的证书签名请求CSR，通过线下交给区块链域名服务系统BCDNS，由BCDNS检验CSR并存证，返回BCDNS管理员签发的PTC证书`PTCCertificate`；

Supervisor节点线下从BCDNS管理员处拿到签发的PTC证书`PTCCertificate`，在本地Supervisor节点身份证书的存放路径保存。

### 1.4 startBCDNSClient 初始化并启动BCDNS客户端

Supervisor节点启动委员会PTC所背书区块链的链类型对应的BCDNS客户端，通过该客户端与BCDNS服务通信。

命令参数如下：

- `--domainSpace`：（必选）委员会PTC所背书的区块链域名空间名，如`.org`、`.com`等，一个域名空间名绑定一个BCDNS客户端服务，该项默认为空字符串，即当前中继的根域名空间名默认为空字符串；
- `--bcdnsType`：（必选）BCDNS服务类型，提供`bif`和 `embedded`和两种类型。其中`bif`为目前可用的星火链网BCDNS服务，为中继外部依赖服务，`embedded`为嵌入式BCDNS服务
- `--bcdnsClientConfigPath`：（必选）BCDNS客户端配置文件路径，该路径存放的BCDNS客户端配置文件，根据`--bcdnsType`BCDNS服务类型客户端对应的配置格式填写；

用法如下：

```shell
relayer:> startBCDNSClient --domainSpace .org --bcdnsType embedded --bcdnsClientConfigPath /path/to/bcdns-config.json
start bcdns client success, path/to/supervisor-config.json
``` 

BCDNS客户端配置文件示例：
```
EMBEDDED类型BCDNS客户端
{
  "server_address": "grpc://0.0.0.0:8090",
  "tls_client_cert": "",
  "tls_client_key": "",
  "tls_trust_cert_chain": "",
  "cross_chain_cert": "",
  "cross_chain_key": ""
}

BIF类型BCDNS客户端
{
  "certificationServiceConfig":{
    "authorizedKeyPem":"-----BEGIN PRIVATE KEY-----\nMFECAQEwB...WnSkTM4=\n-----END PRIVATE KEY-----\n",
    "authorizedPublicKeyPem":"-----BEGIN PUBLIC KEY-----\nMCowBQYDK2Vw...KDyWnSkTM4=\n-----END PUBLIC KEY-----\n",
    "authorizedSigAlgo":"Keccak256WithSecp256k1",
    "clientCrossChainCertPem":"-----BEGIN RELAYER CERTIFICATE-----\nAAAIA...DyLBh2ITiTQ4IVYlXkYjSBw==\n-----END RELAYER CERTIFICATE-----\n",
    "clientPrivateKeyPem":"-----BEGIN PRIVATE KEY-----\nMFECAQE...V+RqJKDyWnSkTM4=\n-----END PRIVATE KEY-----\n",
    "sigAlgo":"Keccak256WithSecp256k1",
    "url":"http://localhost:8112"
  },
  "chainConfig":{
    "bifAddress":"did:bid:efbThy5sb...5oQGX6LUGwg",
    "bifChainRpcUrl":"http://test.bifcore.bitfactory.cn",
    "bifPrivateKey":"priSPKgnr1a...JNaackZJUo",
    "domainGovernContract":"did:bid:efjQKy4HEshTueHGKzrJPATKoFeNgHKo",
    "ptcGovernContract":"did:bid:efgTq9DtP2zHAhmKp7M4BhN6AVYMVWV2",
    "relayerGovernContract":"did:bid:efSnvCFJSnpWiQiVhnh8Eimgyi4RoNpA"
  }
}
```

### 1.5 generatePTCTrustRoot 构造PTC信任根

Supervisor节点收集当前委员会PTC内的验证节点信息，共同构建该PTC的信任根`PTCTrustRoot`，上传至BCDNS作为该委员会背书的信任根证明

命令参数如下：

- `--domainSpace`：（必选）委员会PTC所背书的区块链域名空间名，如`.org`、`.com`等，一个域名空间名绑定一个BCDNS客户端服务，该项默认为空字符串，即当前中继的根域名空间名默认为空字符串；
- `--committeeNodesInfoDir`：（可选）存放委员会PTC所有验证节点信息的路径，如`dir/of/committeeNodes`；
- `--committeeNodeIds`：（必选）委员会PTC所有验证节点的id，根据该节点的id，在`--committeeNodesInfoDir`指出的路径下分别获得`nodeId.json`信息文件；

用法如下：

```shell
relayer:> generatePTCTrustRoot --domainSpace .org --committeeNodesInfoDir dir/of/committeeNodes/ --committeeNodeIds node1 node2 ...
your new generated PTCTrustRoot is 
AAABCAAAAAAEAAAALm9yZwEAoAAAAAAAmgAAAAAAAwAAADEuMAEADQAAAGNvbW1p
dHRlZS1wdGMCAAEAAAABAwBrAAAAAABlAAAAAAABAAAAAAEAWAAAADBWMBAGByqG
...
IEtFWS0tLS0tXG4ifV19XSwiY29tbWl0dGVlX2lkIjoiY29tbWl0dGVlMSJ9BAAW
AAAAS2VjY2FrMjU2V2l0aFNlY3AyNTZrMQUAQQAAAHYbc6qIL5xh9bLqWoCYob00
ghZ2qZppKUfHV4NOvKrXS8SXCGTn+mlqRnpW7WqhdKbXN7OKIjWzEP+udg3rP1kA
``` 

验证节点的配置文件示例：
```
{
  "endpoint_url": "grpc://0.0.0.0:8080",
  "keys": {
    "key1": "-----BEGIN PUBLIC KEY-----\n
    MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEC4Wuvhr7FFHJ4Fqa3HoxeuP0rzMJr3PB\n
    FI/ng5gxWxhbJcU5rwfdg4mcuJzlpjWYe6Oi4oifOpb78usUKQk/ww==\n
    -----END PUBLIC KEY-----\n"
  },
  "node_id": "node1",
  "tls_cert": "-----BEGIN CERTIFICATE-----\n
  MIIDnDCCAoSgAwIBAgIJANoR+ubebhQbMA0GCSqGSIb3DQEBCwUAMHwxETAPBgNV\n
  ...
  Q45YA8S2qdqNCWgo+vIFIJqhZf8ymw9VRHGFpgqufZRbkgAxMWkast2AXGaOjUvB\n
  N92eu9p3hyI/j1XOLD9CRA==\n
  -----END CERTIFICATE-----\n"
}
```

### 1.6 addPTCTrustRoot 上传PTC信任根

用于上传或更新委员会PTC的信任根，如果该委员会之前已经向BCDNS上传过PTC信任根，因为委员会PTC内的背书节点新增或退出，则需要向BCDNS服务上传并更新新的PTC信任根。

命令参数如下：

- `--domainSpace`：（必选）委员会PTC所背书的区块链域名空间名，如`.org`、`.com`等，一个域名空间名绑定一个BCDNS客户端服务，该项默认为空字符串，即当前中继的根域名空间名默认为空字符串；
- `--ptcTrustRootStr`：（必选）通过`1.5 generatePTCTrustRoot`构造生成的PTCTrustRoot，该参数为String类型；

用法如下：

```shell
relayer:> addPTCTrustRoot --domainSpace .org --ptcTrustRootStr AAABCAAAAAAEAAAALm9yZwEAoAAAAAAA...ghZ2qZppKUfHV4NOvKrXS8SXCGTn+mlq
add ptcTrustRoot to (domain:.org)'s BCDNS Service success
```

