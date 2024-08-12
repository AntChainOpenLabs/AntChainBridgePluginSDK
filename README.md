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

在当前的工程实现中，BBC链下部分是以插件的形式实现的。AntChain Bridge实现了一套SDK，通过实现SDK中规定的接口（SPI），经过简单的编译，即可生成插件包。插件服务（PluginServer, PS）可以加载BBC链下插件，详情可以参考插件服务的介绍[文档](https://github.com/AntChainOpenLab/AntChainBridgePluginServer/blob/main/README.md)。

区块链域名服务（BlockChain Domain Name Service, BCDNS）为AntChain Bridge跨链网络提供身份、路由、信任管理等服务，在v0.2.0版本之后，加入了BCDNS模块以及其他数据结构，比如区块链域名证书等类型和工具，并在`antchain-bridge-bcdns`中增加了基于[星火链网](https://bitfactory.cn/)的BCDNS服务的客户端实现，该BCDNS服务由[中国信息通信研究院](http://www.caict.ac.cn/)开发支持，详情请[见](https://github.com/caict-4iot-dev/BCDNS)。

在SDK中抽象了BCDNS服务的接口[IBlockChainDomainNameService](antchain-bridge-bcdns/src/main/java/com/alipay/antchain/bridge/bcdns/service/IBlockChainDomainNameService.java)，描述了BCDNS应该提供的功能，目前仅支持官方实现的BCDNS，支持的类型可[见](antchain-bridge-bcdns/src/main/java/com/alipay/antchain/bridge/bcdns/service/BCDNSTypeEnum.java)。

在v0.3.0之后，拆分出单独的BCDNS Factory模块，用于快速启动不同类型BCDNS的客户端，并剥离依赖。增加关于Embedded BCDNS的实现代码，并提供Spring Boot Starter方便快速启动Embedded BCDNS服务。

以下介绍了基于SDK的一个集成架构：

![](https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/deploy_arch_231228.png)

SDK共有六个部分，包括：

- **antchain-bridge-commons**：包含很多工具方法和数据结构，帮助BBC实现快速开发；

- **antchain-bridge-plugin-lib**：BBC插件化的依赖库，给出一个注解`@BBCService`，帮助插件开发者可以快速完成插件构建；

- **antchain-bridge-plugin-manager**：插件的管理库，提供插件的加载、生命周期管理等能力，插件服务依赖于这个库；

- **antchain-bridge-spi**：主要包含了接口`IBBCService`，描述了一个BBC实现类应该有的功能，开发者只要依次实现接口即可，详细接口介绍请[见](./antchain-bridge-spi/README.md)；

- **antchain-bridge-bcdns**：主要包含了接口`IBlockChainDomainNameService`，描述了一个BCDNS客户端应该有的功能，目前仅支持星火链网（BIF）的BCDNS客户端实现，详细使用可以参考[wiki]()中“如何实现跨链”的内容；

- **antchain-bridge-bcdns-factory**：包含一个工厂类，支持依据类型创建BCDNS客户端，目前支持星火链网（BIF）的BCDNS客户端和Embedded BCDNS的实现，详细使用可以参考[wiki](https://github.com/AntChainOpenLab/AntChainBridgePluginSDK/wiki)中“如何实现跨链”的内容；
  

# 构建

**在开始之前，请您确保安装了maven和JDK，这里推荐使用[jdk-1.8](https://adoptium.net/zh-CN/temurin/releases/?version=8)版本*

## 本地安装

在项目根目录下，直接使用maven编译即可：

```
mvn install -Dmaven.test.skip=true
```

这样，SDK的Jar包就被安装在本地了。

然后，可以通过在maven的pom.xml配置依赖就可以了，比如下面一段配置，`${antchain-bridge.sdk.version}`为当前仓库的版本号，可以在项目目录的[pom.xml](pom.xml)看到。

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
<dependency>
    <groupId>com.alipay.antchain.bridge</groupId>
    <artifactId>antchain-bridge-bcdns</artifactId>
    <version>${antchain-bridge.sdk.version}</version>
</dependency>
<dependency>
    <groupId>com.alipay.antchain.bridge</groupId>
    <artifactId>antchain-bridge-bcdns-factory</artifactId>
    <version>${antchain-bridge.sdk.version}</version>
</dependency>
```



# 快速开始

## BBC插件

### Testchain

[Testchain](pluginset/demo-testchain)是一个用于讲解如何开发BBC插件的demo工程，结合AntChain Bridge的文档，可以更好地理解BBC的开发过程。

详细的开发教程请参考本仓库的[Wiki](https://github.com/AntChainOpenLab/AntChainBridgePluginSDK/wiki)。

### 以太坊

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

### EOS

基于SDK，我们提供了一个打通EOS链的BBC[插件](pluginset/eos)。

- **offchain-plugin**工程下面实现了EOS的BBC插件的链下部分；
- **onchain-plugin**工程下面，主要分为两部分：
  - **合约代码**：合约代码放在[路径](pluginset/eos/onchain-plugin/cpp/sys/src)下面，包含AM合约、SDP合约、Demo合约，详情请[见](pluginset/eos/onchain-plugin/README.md)。

详细操作请[见](pluginset/ethereum/offchain-plugin/README.md)。

### Mychain

基于SDK我们给出了打通蚂蚁链（Mychain）的BBC[插件](pluginset/mychain0.10)，目前内部依赖发布中，发布之后即可编译使用。

## BCDNS

### 跨链身份

BCDNS为AntChain Bridge提供了身份服务，通过签发各类身份证书，实现区块链域名申请、域名验证、Relayer互认等流程。

BCDNS会为AntChain Bridge提供域名证书、域名空间证书、Relayer证书、PTC证书和信任根证书。

#### 证书结构介绍

证书的类型枚举可以参考[代码](antchain-bridge-commons/src/main/java/com/alipay/antchain/bridge/commons/bcdns/CrossChainCertificateTypeEnum.java)，证书的基本结构可以参考[代码](antchain-bridge-commons/src/main/java/com/alipay/antchain/bridge/commons/bcdns/AbstractCrossChainCertificate.java)。

整体类图如下：

```mermaid
classDiagram
direction LR
class CrossChainCertificateTypeEnum {
<<enumeration>>
  +  BCDNS_TRUST_ROOT_CERTIFICATE
  +  DOMAIN_NAME_CERTIFICATE
  +  RELAYER_CERTIFICATE
  +  PROOF_TRANSFORMATION_COMPONENT_CERTIFICATE
}

class AbstractCrossChainCertificate {
   CrossChainCertificateTypeEnum type
   String version
   String id
   ObjectIdentity issuer
   byte[] credentialSubject
   IssueProof proof
   long expirationDate
   long issuanceDate
}
class IssueProof {
   byte[] rawProof
   byte[] certHash
   String sigAlgo
   String hashAlgo
}

class ObjectIdentity {
  - byte[] rawId
  - ObjectIdentityType type
}
class ObjectIdentityType {
<<enumeration>>
  +  X509_PUBLIC_KEY_INFO
  +  BID
}

ObjectIdentity "1" *--> "type 1" ObjectIdentityType 
AbstractCrossChainCertificate "1" *--> "type 1" CrossChainCertificateTypeEnum 
AbstractCrossChainCertificate "1" *--> "proof 1" IssueProof 
AbstractCrossChainCertificate "1" *--> "issuer 1" ObjectIdentity 
AbstractCrossChainCertificate  -->  IssueProof 
```

从上图，可以看到证书的类型，域名和域名空间证书，都作为`DOMAIN_NAME_CERTIFICATE`类型。

`AbstractCrossChainCertificate`为证书的主要实现，包含下列字段：

- type：证书类型；
- version：证书结构的版本号；
- id：某一本证书的ID，由BCDNS签发时确定；
- issuer：签发该证书的主体ID，使用类型`ObjectIdentity`表示，下文会有所介绍；
- credentialSubject：持有该证书的主体信息，不同类型证书有不同的主体结构，下文会有所介绍；
- proof：签发主体对该证书作出的证明，目前支持非对称密钥的签名；
- expirationDate：该证书的超时的时间戳，以秒为单位。
- issuanceDate：签发该证书的时间戳，以秒为单位。

#### 主体结构介绍

上文中提到了主体ID类型`ObjectIdentity`，目前支持两种类型`X509_PUBLIC_KEY_INFO`和`BID`。

- X509_PUBLIC_KEY_INFO

  该[类型](antchain-bridge-commons/src/main/java/com/alipay/antchain/bridge/commons/core/base/X509PubkeyInfoObjectIdentity.java)使用了`X.509 Subject Public Key Info`作为主体ID，在`rawId`字段中存放了encoded的`X.509 Subject Public Key Info`，详细可以参考[RFC5280](https://datatracker.ietf.org/doc/html/rfc5280#section-4.1.2.7)。

- BID

  该[类型](antchain-bridge-commons/src/main/java/com/alipay/antchain/bridge/commons/core/base/BIDInfoObjectIdentity.java)使用了星火链的DID[方案](https://bif-doc.readthedocs.io/zh-cn/2.0.0/bid/%E6%98%9F%E7%81%AB%E9%93%BEBID%E4%BD%93%E7%B3%BB%E4%BB%8B%E7%BB%8D.html)。星火标识`BID`(`Blockchain-based Identifier`, `BID`)，`BID`标识是基于`W3C DID`标准开发的新型分布式标识，任意实体可自主生成星火标识，在`rawId`字段中存放了encoded的BID。

证书的`credentialSubject`字段可以按照证书类型反序列化为不同的类型：

- [域名主体](antchain-bridge-commons/src/main/java/com/alipay/antchain/bridge/commons/bcdns/DomainNameCredentialSubject.java)

  ```mermaid
  classDiagram
  direction LR
  class DomainNameCredentialSubject {
     String version
     DomainNameTypeEnum domainNameType
     CrossChainDomain domainName
     CrossChainDomain parentDomainSpace
     ObjectIdentity applicant
     byte[] subject
  }
  
  class DomainNameTypeEnum {
  <<enumeration>>
    +  DOMAIN_NAME
    +  DOMAIN_NAME_SPACE
  }
  
  DomainNameCredentialSubject "1" *--> "type 1" DomainNameTypeEnum 
  ```

  上图介绍了域名主体包含的字段：

  - version：该主体类型的版本号；
  - domainNameType：当前主体表达的类型，是域名的宣称还是域名空间的宣称；
  - domainName：域名或者域名空间的值；
  - parentDomainSpace：签发BCDNS的域名空间，规定根BCDNS的域名空间为空字符串，可见[代码](antchain-bridge-commons/src/main/java/com/alipay/antchain/bridge/commons/core/base/CrossChainDomain.java)的`ROOT_DOMAIN_SPACE`；l
  - applicant：主体ID；
  - subject：序列化的主体身份，根据主体ID类型，如果主体ID是`X509_PUBLIC_KEY_INFO`，则为空，如果是`BID`类型，则为[BID文档](https://bif-doc.readthedocs.io/zh-cn/2.0.0/bid/BID%E6%96%87%E6%A1%A3.html)（BID Document）内容，subject的主要作用是保存主体的公钥内容，BID文档的使用较为简单，主要是使用`publicKey`字段；

- 域名空间主体

  域名空间和域名的主体结构相同，只有`domainNameType`不同，为`DOMAIN_NAME_SPACE`。

  域名空间应当以点“.”开头，后接空间名字，比如‘’.web3“，如果是域名，则不应该以点开头。域名和域名空间的长度都不应该超过128字节，且建议使用小写英文字母表示，比如`antchain.web3`。

- [Relayer主体](antchain-bridge-commons/src/main/java/com/alipay/antchain/bridge/commons/bcdns/RelayerCredentialSubject.java)

  ```mermaid
  classDiagram
  direction BT
  class RelayerCredentialSubject {
     String version
     String name
     ObjectIdentity applicant
     byte[] subjectInfo
  }
  ```

  上图介绍了Relayer主体包含的字段：

  - version：该主体类型的版本号；
  - name：Relayer的名称，这里可以随意填写；
  - applicant：主体ID；
  - subject：序列化的主体身份；

  Relayer的运行需要依赖Relayer证书，详情请参考[AntChain Bridge Relayer](https://github.com/AntChainOpenLabs/AntChainBridgeRelayer)。

- [PTC证书](antchain-bridge-commons/src/main/java/com/alipay/antchain/bridge/commons/bcdns/PTCCredentialSubject.java)

  ```mermaid
  classDiagram
  direction LR
  class PTCCredentialSubject {
     String version
  	 String name
  	 PTCTypeEnum type
     ObjectIdentity applicant
     byte[] subjectInfo
  }
  
  class PTCTypeEnum {
  <<enumeration>>
    +  SIGNING_MACHINE
    +  BLOCKCHAIN
  }
  
  PTCCredentialSubject "1" *--> "type 1" PTCTypeEnum 
  ```

  证明转换服务（PTC）会为AntChain Bridge 提供异构链跨链消息存在性验证服务，以及提供背书证明，PTC需要向BCDNS申请身份并注册信任根，对每条链都会提供第三方区块链信任锚（TP-BTA），以供跨链中的接收链验证跨链消息证明（TP-Proof）。

  PTC将会在2024年进行开发并完成开源，目前计划支持多种类型的PTC，比如`PTCTypeEnum`中的签名机（SIGNING_MACHINE）、区块链（BLOCKCHAIN），区块链类型可以看作常见的中继链跨链方案。

- [信任根证书](antchain-bridge-commons/src/main/java/com/alipay/antchain/bridge/commons/bcdns/BCDNSTrustRootCredentialSubject.java)

  ```mermaid
  classDiagram
  direction BT
  class BCDNSTrustRootCredentialSubject {
     String name
  	 ObjectIdentity bcdnsRootOwner
     byte[] bcdnsRootSubjectInfo
  }
  ```

  最后是持有根域名的BCDNS信任根主体，上面字段包含主体的name，以及主体ID和主体信息。

  域名空间证书用于表示某个BCDNS持有该域名空间，该空间不可以是根空间，这些BCDNS为下级BCDNS，而根BCDNS为最高级BCDNS，比如当前的星火链BCDNS，上级BCDNS可以给下级签发域名空间证书，每个BCDNS可以签发自己空间的区块链域名。

#### 序列化介绍

跨链证书的序列化使用了AntChain Bridge 提供的一个TLV序列化工具（使用[介绍](antchain-bridge-commons/src/main/java/com/alipay/antchain/bridge/commons/utils/codec/tlv/annotation/TLVField.java)）。

跨链证书支持保存为PEM格式，证书对象可以通过[CrossChainCertificateUtil](antchain-bridge-commons/src/main/java/com/alipay/antchain/bridge/commons/bcdns/utils/CrossChainCertificateUtil.java)转化至PEM格式，比如：

```
-----BEGIN BCDNS TRUST ROOT CERTIFICATE-----
AADZAQAAAAABAAAAMQEABAAAAHRlc3QCAAEAAAAAAwA7AAAAAAA1AAAAAAABAAAA
AQEAKAAAAGRpZDpiaWQ6ZWZiVGh5NXNiRzdQM21GVXAyRVdONW9RR1g2TFVHd2cE
AAgAAAChN2RlAAAAAAUACAAAACFrRWcAAAAABgDQAAAAAADKAAAAAAADAAAAYmlm
AQA7AAAAAAA1AAAAAAABAAAAAQEAKAAAAGRpZDpiaWQ6ZWZiVGh5NXNiRzdQM21G
VXAyRVdONW9RR1g2TFVHd2cCAHoAAAB7InB1YmxpY0tleSI6W3sidHlwZSI6IkVE
MjU1MTkiLCJwdWJsaWNLZXlIZXgiOiJiMDY1NjZhZjY2NWVlNTUwNjM1ZTk4Mzc1
YzdiMzg4NzZhMmNjMzFlM2E5ZDgxODk1N2U0NmEyNGEwZjI1YTc0YTQ0Y2NlIn1d
fQcAiAAAAAAAggAAAAAAAwAAAFNNMwEAIAAAALKKKxzUTalyw2vp96hOJ3TuDsNZ
/pjgltTL+ip2bsRNAgAHAAAARWQyNTUxOQMAQAAAAMsFd4D9Gf3hiIebiq5MFTpw
IV4AUtT9d+Y8gRK/kmNySzlJ32Shw3FNj8Uvy3yjBxjO6vKOWH5Jhu936zMWOgk=
-----END BCDNS TRUST ROOT CERTIFICATE-----
```



### 星火链网（BIF）BCDNS

目前SDK支持星火链网的BCDNS服务客户端，这里介绍其配置项和如何实例化该客户端。

首先介绍配置，代码可[见](antchain-bridge-bcdns/src/main/java/com/alipay/antchain/bridge/bcdns/impl/bif/conf/BifBCNDSConfig.java)，主要分为两部分，一部分`certificationServiceConfig`是用于和颁证服务通信、鉴权，另一部分`chainConfig`用于和星火链网交互。

```json
{
	"certificationServiceConfig":{
		"authorizedKeyPem":"-----BEGIN PRIVATE KEY-----\nMFECAQEwB...WnSkTM4=\n-----END PRIVATE KEY-----\n",
		"authorizedPublicKeyPem":"-----BEGIN PUBLIC KEY-----\nMCowBQYDK2Vw...KDyWnSkTM4=\n-----END PUBLIC KEY-----\n",
		"authorizedSigAlgo":"Ed25519",
		"clientCrossChainCertPem":"-----BEGIN RELAYER CERTIFICATE-----\nAAAIA...DyLBh2ITiTQ4IVYlXkYjSBw==\n-----END RELAYER CERTIFICATE-----\n",
		"clientPrivateKeyPem":"-----BEGIN PRIVATE KEY-----\nMFECAQE...V+RqJKDyWnSkTM4=\n-----END PRIVATE KEY-----\n",
		"sigAlgo":"Ed25519",
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

下面对各个配置项给出解释：

首先是颁证服务的配置：

- authorizedKeyPem：有权限申请跨链身份（Relayer、PTC）的私钥，填入PEM格式的PKCS#8的私钥，目前仅支持Ed25519算法。
- authorizedPublicKeyPem：`authorizedKeyPem`对应的公钥，填入PEM格式的PKCS#8的公钥，目前仅支持Ed25519算法。
- authorizedSigAlgo：`authorizedKeyPem`私钥的签名算法，目前仅支持[Ed25519](http://oid-info.com/get/1.3.101.112)算法。
- clientCrossChainCertPem：Relayer的跨链证书，需要提前从BCDNS处获取。
- clientPrivateKeyPem：Relayer跨链证书持有者的私钥，填入PEM格式的PKCS#8的私钥，目前仅支持Ed25519算法。
- sigAlgo：`clientPrivateKeyPem`私钥的签名算法，目前仅支持Ed25519算法。
- url：颁证服务的URL。

然后是[星火链网](https://bif-doc.readthedocs.io/zh-cn/2.0.0/quickstart/%E5%BF%AB%E9%80%9F%E6%8E%A5%E5%85%A5%E6%98%9F%E7%81%AB%E9%93%BE.html)的配置：

- bifAddress：星火链网的账户地址，这里需要使用Relayer的公钥来生成该地址，可以参考[代码](https://github.com/AntChainOpenLabs/AntChainBridgeRelayer/blob/develop/r-cli/src/main/java/com/alipay/antchain/bridge/relayer/cli/command/UtilsCommands.java#L196)。
- bifChainRpcUrl：星火链网节点的RPC地址。
- bifChainRpcPort：星火链网节点的PRC端口，如果不需要则可以不填。
- clientPrivateKeyPem：星火链网账户的私钥，这里需要使用Relayer的私钥`clientPrivateKeyPem`来生成该地址，可以参考[代码](https://github.com/AntChainOpenLabs/AntChainBridgeRelayer/blob/6658dfa599b73b1aa4f3cf156e1fc1d72c5cb7c6/r-cli/src/main/java/com/alipay/antchain/bridge/relayer/cli/command/UtilsCommands.java#L203C20-L203C42)。
- domainGovernContract：域名管理合约，参考[星火链网BCDNS](https://github.com/caict-4iot-dev/BCDNS)，依赖的星火链网BCDNS应当有唯一一本域名管理合约。
- ptcGovernContract：PTC身份管理合约，依赖的星火链网BCDNS应当有唯一一本PTC身份管理合约。
- relayerGovernContract：Relayer身份管理合约，依赖的星火链网BCDNS应当有唯一一本Relayer身份管理合约。

准备完配置之后，可以根据[这里](antchain-bridge-bcdns/src/main/java/com/alipay/antchain/bridge/bcdns/impl/BlockChainDomainNameServiceFactory.java)创建一个BCDNS实例。

### Embedded BCDNS

Embedded BCDNS是内嵌在服务内部的BCDNS，提供中心化的权威服务，会使用一把私钥为跨链网络提供认证、准入等功能，按照服务端要求可以通过简单配置接入BCDNS，具体内容可以参考[这里](bcdns-services/embedded-bcdns/README.md)。

# 社区治理

AntChain Bridge 欢迎您以任何形式参与社区建设。

您可以通过以下方式参与社区讨论

- 钉钉

![scan dingding2024](https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/dingding2024.png?x-oss-process=image/resize,w_400/quality,Q_100)

- 邮件

发送邮件到`antchainbridge@service.alipay.com`

# License

详情参考[LICENSE](./LICENSE)。
