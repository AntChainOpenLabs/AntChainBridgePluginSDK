# Embedded BCDNS

## 介绍

区块链域名服务（BCDNS）作为AntChain Bridge的核心组件之一，负责签发、认证跨链中继（Relayer）、验证组件（PTC）的身份，以及区块链在网络中的唯一标识，即区块链域名，同时维护了跨链域名与中继服务的绑定关系，即跨链路由。

Embedded BCDNS作为AntChain Bridge定义并提供的一种BCDNS实现，提供了BCDNS的所有功能，支持中继、PTC等的跨链身份证书签发与查询，支持区块链域名证书认证，支持跨链路由的注册和查询。Embedded BCDNS可以简单地在普通的Java程序中拉起服务端，以及初始化客户端，这样方便在特定的测试或者商业化场景中使用AntChain Bridge跨链。

如下图，在服务端通过启动一个AntChain Bridge SDK提供的GRpc服务端，可以快速启动Embedded BCDNS，类似的可以通过输入客户端的配置Json，简单快速地启动Embedded BCDNS的客户端，可以在SDK代码中找到模块 **embedded-bcdns-core**，在这里我们提供了服务端和客户端的核心实现代码。由于Embedded BCDNS是有状态的服务，所以需要开发者需要实现一个接口`IBcdnsState`，为服务端提供状态。除此之外，AntChain Bridge为SpringBoot应用提供了starter，可以通过引用和简单的配置，就可以启动Embedded BCDNS的服务端，`embedded-bcdns-spring-boot-starter` 提供了服务端的包装，而`embedded-bcdns-state-jdbc-spring-boot-starter`提供了`IBcdnsState`的实现，它要求您配置了`spring.datasource`。

<img src="https://raw.githubusercontent.com/AntChainOpenLabs/AntChainBridge/main/.doc_resouces/bcdns/embedded_models.png" alt="image-20240805141637618" style="zoom:40%;" />

## 快速开始

### 安装

> [!IMPORTANT]  
> Embedded BCDNS要求AntChain Bridge SDK版本大于等于 v0.3.0

首先完成AntChain Bridge SDK的安装，请参考[这里](../../README.md#构建)。

然后，安装模块 **embedded-bcdns-core**，进入目录`bcdns-services/embedded-bcdns/embedded-bcdns-core`下，执行下面命令，完成安装。

```
mvn clean install
```

这样就可以在自己的项目中引用**embedded-bcdns-core**，并在代码中启动Embedded BCDNS服务端或者客户端了，这里仅介绍maven引用方式，`${embedded.bcdns.core.version}`填入你使用的版本。

```xml
<dependency>
    <groupId>com.alipay.antchain.bridge</groupId>
    <artifactId>embedded-bcdns-core</artifactId>
    <version>${embedded.bcdns.core.version}</version>
</dependency>
```

如果你的程序使用了SpringBoot框架，推荐使用提供的Starter来启动服务端，

类似地，进入`bcdns-services/embedded-bcdns/embedded-bcdns-spring-boot-starter`，执行命令编译安装：

```
mvn clean install
```

由于BCDNS要求实现IBcdnsState接口，我们提供了使用`spinrg.datasource`的state实现，基于[MybatisPlus](https://github.com/baomidou/mybatis-plus)实现，进入`bcdns-services/embedded-bcdns/embedded-bcdns-state-jdbc-spring-boot-starter`，执行编译安装：

```
mvn clean install
```

在自己的工程中完成引用，`${acb-embedded-bcdns.version}`填入当前使用的版本：

```xml
<dependency>
    <groupId>com.alipay.antchain.bridge</groupId>
    <artifactId>embedded-bcdns-state-jdbc-spring-boot-starter</artifactId>
    <version>${acb-embedded-bcdns.version}</version>
</dependency>
<dependency>
    <groupId>com.alipay.antchain.bridge</groupId>
    <artifactId>embedded-bcdns-spring-boot-starter</artifactId>
    <version>${acb-embedded-bcdns.version}</version>
</dependency>
```

### 使用方法

#### 直接使用

##### **服务端**

这一节介绍直接使用**embedded-bcdns-core**来完成BCDNS服务端和客户端的启动。

首先服务端需要实现这个接口IBcdnsState，这里就不介绍具体的实现方式了，核心思路是对某个数据源包一层，比如MySQL、LevelDB等。

然后，按照构造函数创建`GRpcEmbeddedBcdnsService`的对象:

```java
new GRpcEmbeddedBcdnsService(
        bcdnsState,
        HashAlgoEnum.KECCAK_256,
        SignAlgoEnum.KECCAK256_WITH_SECP256K1,
        embeddedBcdnsProperties.getSignAlgo().getSigner().generateKeyPair().getPrivate(),
        new AbstractCrossChainCertificate()
);
```

- `bcdnsState`是上面实现的状态对象；
- `signCertHashAlgo`是对证书执行的hash算法，可以参考[HashAlgoEnum](../../antchain-bridge-commons/src/main/java/com/alipay/antchain/bridge/commons/utils/crypto/HashAlgoEnum.java)；
- `bcdnsSignAlgo`是server对证书签名的算法，可以参考[SignAlgoEnum](../../antchain-bridge-commons/src/main/java/com/alipay/antchain/bridge/commons/utils/crypto/SignAlgoEnum.java)；
- `bcdnsRootKey`是server执行证书签发的私钥；
- `bcdnsRootCert`是server的BCDNS根证书，可以参考[README](../../README.md#跨链身份)，以及附录

初始化之后，为服务启动一个server即可：

```java
NettyServerBuilder.forAddress(
        new InetSocketAddress(embeddedBcdnsProperties.getServerHost(), embeddedBcdnsProperties.getServerPort())
).addService(service)
.build()
.start();
```

##### **客户端**

这里需要在项目引入下面依赖，`${acb-sdk.version}`填入SDK的版本：

```
<dependency>
    <groupId>com.alipay.antchain.bridge</groupId>
    <artifactId>antchain-bridge-bcdns-factory</artifactId>
    <version>${acb-sdk.version}</version>
</dependency>
```

上面依赖会提供一个工厂类，负责从配置文件创建特定类型的客户端：

```java
BlockChainDomainNameServiceFactory.create(
        BCDNSTypeEnum.EMBEDDED,
        ("{\n" +
                "  \"server_address\":\"grpcs://0.0.0.0:8090\",\n" +
                "  \"tls_client_cert\":\"-----BEGIN CERTIFICATE-----\\nMIID...n3A==\\n-----END CERTIFICATE-----\\n\",\n" +
                "  \"tls_client_key\":\"-----BEGIN PRIVATE KEY-----\\nMIIEv...Sw==\\n-----END PRIVATE KEY-----\"\n" +
                "}").getBytes()
);
```

上面json即为客户端配置：

- server_address：服务端的地址，grpcs代表GRpc over TLS，要求配置TLS证书和私钥，grpc代表plaintext；
- tls_client_cert：客户端的TLS证书；
- tls_client_key：客户端的TLS私钥；

### Spring Boot Starter

##### 服务端

在工程中引用上面提到的starter依赖，要求有可以使用的datasource，这里推荐MySQL。

首先，需要初始化你的数据库，在模块`embedded-bcdns-state-jdbc-spring-boot-starter`中，我们提供了[DDL](embedded-bcdns-state-jdbc-spring-boot-starter/src/main/resources/ddl/mysql/ddl.sql)，在MySQL上运行良好，如果您使用了其他数据库，请自行修改脚本。

通过下面配置即可启动一个plaintext的GRpc服务端：

```yaml
acb:
  bcdns:
    embedded:
      server-on: true
      server-host: 0.0.0.0
      server-port: 8090
      sign-algo: keccak256_with_secp256k1
      sign-cert-hash-algo: keccak_256
      root-cert-file: file:root.crt
      root-private-key-file: file:root.key
```

- server-on：代表是否启动Embedded BCDNS server，默认为false；
- server-host：指定server启动的host地址，默认`0.0.0.0`；
- server-port：指定server启动的端口，默认`8090`；
- root-cert-file：代表BCDNS根证书，可以参考[README](../../README.md#跨链身份)，以及附录来生成自签的证书；
- sign-algo：是server对证书签名的算法，可以参考[SignAlgoEnum](../../antchain-bridge-commons/src/main/java/com/alipay/antchain/bridge/commons/utils/crypto/SignAlgoEnum.java)；
- sign-cert-hash-algo：是对证书执行的hash算法，可以参考[HashAlgoEnum](../../antchain-bridge-commons/src/main/java/com/alipay/antchain/bridge/commons/utils/crypto/HashAlgoEnum.java)；
-  root-private-key-file：是server执行证书签发的私钥文件，PEM格式PKCS#1私钥即可，要和签名算法对齐，生成方式可以参考附录；

也可以通过下面配置要求TLS链接，以及TLS双向链接等：

```yaml
acb:
  bcdns:
    embedded:
      security:
        mode: tls
        tls:
          server-key: file:node_keys/relayer/node_tls.key
          server-cert-chain: file:node_keys/relayer/node_tls.crt
          trust-cert-collection: file:node_keys/relayer/trust.crt
          client-auth: require
```

- mode: 支持TLS和NONE，默认是none；
- tls.server-key：server的TLS私钥；
- tls.server-cert-chain：server的证书链；
- tls.trust-cert-collection：server信任的证书集合，一个文件内可以有多本证书；
- tls.client-auth：是否验证客户端证书，默认为require，支持optional、none，可以参考`io.grpc.TlsServerCredentials.ClientAuth`；

##### 客户端

这里需要在项目引入下面依赖，`${acb-sdk.version}`填入SDK的版本：

```
<dependency>
    <groupId>com.alipay.antchain.bridge</groupId>
    <artifactId>antchain-bridge-bcdns-factory</artifactId>
    <version>${acb-sdk.version}</version>
</dependency>
```

上面依赖会提供一个工厂类，负责从配置文件创建特定类型的客户端：

```java
BlockChainDomainNameServiceFactory.create(
        BCDNSTypeEnum.EMBEDDED,
        ("{\n" +
                "  \"server_address\":\"grpcs://0.0.0.0:8090\",\n" +
                "  \"tls_client_cert\":\"-----BEGIN CERTIFICATE-----\\nMIID...n3A==\\n-----END CERTIFICATE-----\\n\",\n" +
                "  \"tls_client_key\":\"-----BEGIN PRIVATE KEY-----\\nMIIEv...Sw==\\n-----END PRIVATE KEY-----\"\n" +
                "}").getBytes()
);
```

上面json即为客户端配置：

- server_address：服务端的地址，grpcs代表GRpc over TLS，要求配置TLS证书和私钥，grpc代表plaintext；
- tls_client_cert：客户端的TLS证书；
- tls_client_key：客户端的TLS私钥；



## 附录

- 生成BCDNS根证书的代码，按需要填入自己的参数即可

  ```java
  KeyPair keyPair = SignAlgoEnum.KECCAK256_WITH_SECP256K1.getSigner().generateKeyPair();
  
  AbstractCrossChainCertificate certificate = CrossChainCertificateFactory.createCrossChainCertificate(
          CrossChainCertificateV1.MY_VERSION,
          "test",
          new X509PubkeyInfoObjectIdentity(keyPair.getPublic().getEncoded()),
          DateUtil.currentSeconds(),
          DateUtil.offsetDay(new Date(), 365).getTime() / 1000,
          new BCDNSTrustRootCredentialSubject(
                  "test",
                  new X509PubkeyInfoObjectIdentity(keyPair.getPublic().getEncoded()),
                  new byte[]{}
          )
  );
  
  certificate.setProof(
          new AbstractCrossChainCertificate.IssueProof(
                  HashAlgoEnum.KECCAK_256.getName(),
                  HashAlgoEnum.KECCAK_256.hash(certificate.getEncodedToSign()),
                  SignAlgoEnum.KECCAK256_WITH_SECP256K1.getName(),
                  signer.sign(keyPair.getPrivate(), certificate.getEncodedToSign())
          )
  );
  System.out.println(CrossChainCertificateUtil.formatCrossChainCertificateToPem(certificate));
  ```

- 生成AntChain Bridge要求的私钥，打印出PKCS#1的PEM格式私钥

  ```java
  KeyPair keyPair = SignAlgoEnum.KECCAK256_WITH_SECP256K1.getSigner().generateKeyPair();
  // dump the private key into pem
  StringWriter stringWriter = new StringWriter(256);
  JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
  jcaPEMWriter.writeObject(keyPair.getPrivate());
  jcaPEMWriter.close();
  String privatePem = stringWriter.toString();
  System.out.println(privatePem);
  ```

  