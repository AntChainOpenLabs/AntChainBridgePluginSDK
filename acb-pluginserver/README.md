<div align="center">
  <img alt="am logo" src="https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/antchain.png" width="250" >
  <h1 align="center">AntChain Bridge Plugin Server</h1>
  <p align="center">
    <a href="http://makeapullrequest.com">
      <img alt="pull requests welcome badge" src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat">
    </a>
    <a href="https://www.java.com">
      <img alt="Language" src="https://img.shields.io/badge/Language-Java-blue.svg?style=flat">
    </a>
    <a href="https://github.com/AntChainOpenLab/AntChainBridgePluginServer/graphs/contributors">
      <img alt="GitHub contributors" src="https://img.shields.io/github/contributors/AntChainOpenLab/AntChainBridgePluginServer">
    </a>
    <a href="https://www.apache.org/licenses/LICENSE-2.0">
      <img alt="License" src="https://img.shields.io/github/license/AntChainOpenLab/AntChainBridgePluginServer?style=flat">
    </a>
  </p>
</div>

# 介绍

AntChain Bridge 插件服务（PluginServer, PS）用于管理异构链插件、完成与中继通信的工作。

- **插件管理能力**

  在开发者完成一个异构链插件开发之后，将该插件打包放到PS的指定路径之下，启动PS即可加载该插件，PS将完成插件和区块链桥接组件（Blockchain Bridge Component, BBC）对象的管理等工作，PS还提供了CLI工具完成诸如插件重新加载、停止等工作。

- **与中继通信**

  将PS注册到一个中继之后，PS会作为一个RPC Server为中继提供服务，PS和中继之间会建立双向认证的TLS连接，确保身份互认和安全性，中继会发送请求要求PS完成BBC对象初始化等工作，并调用BBC的接口，完成与异构链的交互。

# 架构

下面介绍了中继服务的整体架构。

**以下插件BCOS和插件ChainMaker均为展示用例，代表不同的异构插件实现*

<img src="https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/1677744080321-3622114c-52c2-432c-9ceb-0f3d07c290e4.jpeg" style="zoom:33%;"  alt=""/>

# 快速开始

## 构建

进入代码的根目录，运行mvn编译即可：

```shell
mvn clean package
```

产生的安装包在`ps-bootstrap/target/plugin-server-x.x.x.tar.gz`。

## 配置

在获得安装包之后，执行解压缩操作，这里以`plugin-server-x.x.x.tar.gz`为例。

```
tar -zxf plugin-server-x.x.x.tar.gz
```

进入解压后的目录，可以看到：

```
cd plugin-server/
tree .
.
├── README.md
├── bin
│   ├── init_tls_certs.sh
│   ├── plugin-server.service
│   ├── print.sh
│   ├── start.sh
│   └── stop.sh
├── config
│   └── application.yml
└── lib
    └── ps-bootstrap-x.x.x.jar


3 directories, 8 files
```

首先，初始化PS的TLS秘钥和证书，该脚本将会在项目根目录下创建文件夹`certs`，下面存储了私钥`server.key`、证书`server.crt`、信任的证书`trust.crt`。

```
./bin/init_tls_certs.sh
```

然后，在和中继建立连接之前，中继的运维人员应该将中继的证书发送给了您，比如`relayer.crt`，您需要将这个证书添加到`trust.crt`中。

```
cat relayer.crt >> trust.crt
```

最后修改您的配置文件，将证书目录配置进去。

```
vi config/application.yml
```

依次替换`grpc.server.security.certificate-chain`、`grpc.server.security.private-key`和`grpc.server.security.trustCertCollection`的配置项。  
下面给出一个例子，示例中诸如`/path/to/certs/server.crt`的具体配置属性值请替换为您的实际路径，`file:`保留：

```yaml
grpc:
  server:
    security:
      # server certificate
      certificate-chain: file:/path/to/certs/server.crt
      # server key
      private-key: file:/path/to/certs/server.key
      # Mutual Certificate Authentication
      trustCertCollection: file:/path/to/certs/trust.crt
```

然后修改插件库路径，PS将加载这个路径下的插件。修改配置项`pluginserver.plugin.repo`，比如：

```yaml
pluginserver:
  plugin:
    # where to load the hetero-chain plugins
    repo: /path/to/plugins
```

将您的插件都放到这个路径下即可。

## 运行

### 作为系统服务运行

在解压包根目录之下，可以找到`start.sh`脚本。

如下可以打印帮助信息：

```
./bin/start.sh -h

 start.sh — Start the plugin server

 Usage:
   start.sh <params>

 Examples:
  1. start in system service mode：
   start.sh -s
  2. start in application mode:
   start.sh

 Options:
   -s         run in system service mode.
   -h         print help information.
```

运行如下命令，会自动将插件服务启动为系统服务：

```
./bin/start.sh -s
```

看到下面的输出即启动成功：

```
    ___            __   ______ __            _           ____         _      __
   /   |   ____   / /_ / ____// /_   ____ _ (_)____     / __ ) _____ (_)____/ /____ _ ___
  / /| |  / __ \ / __// /    / __ \ / __ `// // __ \   / __  |/ ___// // __  // __ `// _ \
 / ___ | / / / // /_ / /___ / / / // /_/ // // / / /  / /_/ // /   / // /_/ // /_/ //  __/
/_/  |_|/_/ /_/ \__/ \____//_/ /_/ \__,_//_//_/ /_/  /_____//_/   /_/ \__,_/ \__, / \___/
                                                                            /____/        

[ INFO ]_[ 2023-06-20 20:57:06.168 ] : running in system service mode
[ INFO ]_[ 2023-06-20 20:57:06.168 ] : plugin-server started successfully
```

可以通过下面命令查看系统服务状态：

```
systemctl status plugin-server.service
```

### 作为普通进程运行

在解压包根目录之下，运行一下命令即可：

```
./bin/start.sh
```

看到下面的输出即启动成功：

```
    ___            __   ______ __            _           ____         _      __
   /   |   ____   / /_ / ____// /_   ____ _ (_)____     / __ ) _____ (_)____/ /____ _ ___
  / /| |  / __ \ / __// /    / __ \ / __ `// // __ \   / __  |/ ___// // __  // __ `// _ \
 / ___ | / / / // /_ / /___ / / / // /_/ // // / / /  / /_/ // /   / // /_/ // /_/ //  __/
/_/  |_|/_/ /_/ \__/ \____//_/ /_/ \__,_//_//_/ /_/  /_____//_/   /_/ \__,_/ \__, / \___/
                                                                            /____/        

[ INFO ]_[ 2023-03-30 15:11:27.1680160287 ] : start plugin-server now...
[ INFO ]_[ 2023-03-30 15:11:27.1680160287 ] : plugin-server started successfully
```

可以通过`bin/stop.sh`关闭服务。

日志文件存储在`log`目录之下。

## 插件

将你的插件放在配置`pluginserver.plugin.repo`的路径之下，然后启动插件服务即可加载所有插件。

插件运行中使用`BBCLogger`打印的日志，比如[这里](https://github.com/AntChainOpenLabs/AntChainBridgePluginSDK/blob/604c5b97305ee77cfa1e14454f5b99f1f905f6ea/pluginset/ethereum/offchain-plugin/src/main/java/com/alipay/antchain/bridge/plugins/ethereum/EthereumBBCService.java#L74)，插件服务将会按照插件的`product`和BBCService对象的`domain`，打印到不同文件`logs/bbc/${product}/${domain}`，比如下面`product`为`simple-ethereum`和域名为`domainB`的日志文件。

```
logs/
├── antchain-bridge-pluginserver
│   ├── application.log
│   └── error.log
└── bbc
    └── simple-ethereum
        └── domainB.log

3 directories, 3 files
```

## 运行测试

在运行测试之前，请使用`ps-bootstrap/src/main/resources/scripts/init_tls_certs.sh`生成证书，并将`server.key`和`server.crt` 放到`ps-bootstrap/src/test/resources`之下，运行下述命令即可运行测试用例：

```
mvn test
```

# CLI

CLI工具在[v0.2.0](https://github.com/AntChainOpenLab/AntChainBridgePluginServer/releases/tag/v0.2.0)版本已经发布，相关教程请参考[文档](https://github.com/AntChainOpenLab/AntChainBridgePluginServer/blob/main/ps-cli/README.md)。


# 社区治理

AntChain Bridge 欢迎您以任何形式参与社区建设。

您可以通过以下方式参与社区讨论

- 钉钉

![scan dingding](https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/dingding2024.png?x-oss-process=image/resize,w_400/quality,Q_100)

- 邮件

发送邮件到`antchainbridge@service.alipay.com`

# License

详情参考[LICENSE](./LICENSE)。
