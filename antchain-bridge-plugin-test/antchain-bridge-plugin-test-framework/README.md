# 插件测试框架

AntChain Bridge将跨链互操作解释为两个层次：通信和可信，即跨链的目标在于实现区块链实体之间的可信通信。

在AntChain Bridge的架构中，中继需要与区块链进行交互，而异构链的通信协议各式各样，无法统一适配，因此 AntChain Bridge 抽象出了区块链桥接组件（Blockchain Bridge Component, BBC），来解决区块链和跨链网络的通信问题。

每种异构链要接入 AntChain Bridge 跨链网络，都需要实现一套标准的区块链桥接组件，可以分为链上和链下两部分，包括链下插件和系统合约。链下插件需要基于 SDK 完成开发，链上部分则通常是智能合约，要求实现特定的接口和逻辑，为降低开发难度，我们提供了 Solidity 版本的实现。

AntChain Bridge为开发者提供了 SDK、手册和系统合约模板，来帮助开发者完成插件和合约的开发。同时，AntChain Bridge 提供了插件服务（PluginServer）来运行插件，插件服务是一个独立的服务，具备插件管理和响应中继请求的功能。

在当前的工程实现中，BBC 链下部分是以插件的形式实现的。AntChain Bridge 实现了一 套 SDK，通过实现 SDK 中规定的接口（SPI），经过简单的编译，即可生成插件包。插件服务（PluginServer, PS）可以加载 BBC 链下插件，详情可以参考插件服务的介绍文档。

本项目为用户编写的插件提供了一个快捷的测试方法，主要包含两个功能模块：

- `antchain-plugin-test-cli`：命令行工具，通过命令行执行异构链启动关闭、插件加载启动、插件功能测试；
- `antchain-plugin-test-runner`：实现了异构链启动关闭、插件加载启动、插件功能测试；



## 运行命令行

编译 `antchain-plugin-test-runner`

```shell
cd antchain-plugin-test-runner && maven clean install -Dmaven.test.skip=true
```

编译完成之后，进入 `antchain-plugin-test-runner` 目录下，运行 `App.java` ，输入 `help` 可以查看帮助信息。

```shell
  ____  _             _         _____         _  _____           _ 
 |  _ \| |_   _  __ _(_)_ __   |_   _|__  ___| ||_   _|__   ___ | |
 | |_) | | | | |/ _` | | '_ \    | |/ _ \/ __| __|| |/ _ \ / _ \| |
 |  __/| | |_| | (_| | | | | |   | |  __/\__ \ |_ | | (_) | (_) | |
 |_|   |_|\__,_|\__, |_|_| |_|   |_|\___||___/\__||_|\___/ \___/|_|
                |___/                                               
                        Plugin TestTool CLI 0.1.0
>>> type help to see all commands...
cmd> 
```



## 异构链启动关闭

异构链启动关闭模块相关的资源位于 `src/main/resources` 下，`scripts` 目录下包含对 chainmaker、eos、fabric、fiscobcos、hyperchain、ethereum 测试环境的启动（`_startup.sh`）关闭（`_shutdown.sh`）脚本，以及插件功能测试部分所需要使用到的配置文件。`config.properties` 包函了异构链启动时必要的配置信息。在使用测试框架前，请确认本地能够通过 sh 脚本正常地启动、关闭测试环境。

```shell
tree src/main/resources/ -L 3
src/main/resources/
├── config.properties
├── scripts
│   ├── chainmaker
│   │   ├── chainmaker_shutdown.sh
│   │   ├── chainmaker_startup.sh
│   │   ├── contractcollection-2.0-SNAPSHOT.jar
│   │   └── sdk_config.yml
│   ├── eos
│   │   ├── config.ini
│   │   ├── eos_shutdown.sh
│   │   ├── eos_startup.sh
│   │   └── protocol_features
│   ├── fabric
│   │   ├── bootstrap.sh
│   │   ├── conf.json
│   │   ├── fabric_shutdown.sh
│   │   ├── fabric_startup.sh
│   │   ├── fill_args.py
│   │   └── install-fabric.sh
│   ├── fiscobcos
│   │   ├── build_chain.sh
│   │   ├── config.toml
│   │   ├── fiscobcos_shutdown.sh
│   │   └── fiscobcos_startup.sh
│   ├── hyperchain2
│   │   ├── hyperchain2_shutdown.sh
│   │   ├── hyperchain2_startup.sh
│   │   ├── hyperchain2_template.json
│   │   ├── LICENSE
│   │   └── trial.tar.gz
│   ├── simple-ethereum
│   │   ├── simple-ethereum_shutdown.sh
│   │   └── simple-ethereum_startup.sh
│   └── utils.sh
└── ...
```



### 命令行

- 启动 chainmaker 测试链（大约需要 1 分钟）

```shell
chain-manager start -p chainmaker
```

执行成功将输出

```shell
Successfully started chain: chainmaker
```

>  其他支持的链名为:
>
> - fiscobcos
> - hyperchain2
> - fabric
> - eos
> - simple-ethereum



- 查看已经启动的测试链列表

```shell
chain-manager show
```

执行成功将输出

```shell
[chainmaker]
```



- 关闭 chainmaker 测试链

```shell
chain-manager stop -p chainmaker
```

执行成功将输出

```shell
Successfully stopped chain: chainmaker
```





## 插件加载启动模块

### 命令行

- 加载 chainmaker 插件

```shell
plugin-manager load -j chainmaker-bbc-0.1.0-plugin.jar
```

执行成功将输出

```shell
Successfully loaded plugin: chainmaker-bbc-0.1.0-plugin.jar
```



- 启动 chainmaker 插件

```shell
plugin-manager start -j chainmaker-bbc-0.1.0-plugin.jar
```

执行成功将输出

```shell
Successfully loaded plugin: chainmaker-bbc-0.1.0-plugin.jar
```



- 创建 BBC 服务

```shell
plugin-manager create-bbc -p chainmaker -d domain-chainmaker
```

执行成功将输出

```shell
Successfully created BBC service for chainmaker with domain domain-chainmaker
```





## 插件测试模块

> 还未联调，可以单独运行 `antchain-plugin-test-runner `下的单元测试
