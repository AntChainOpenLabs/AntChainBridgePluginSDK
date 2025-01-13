<div align="center">
  <img alt="am logo" src="https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/antchain.png" width="250" >
  <h1 align="center">AntChain Bridge Plugin Server CLI</h1>
</div>

## 介绍

为了帮助管理插件服务，我们提供一个命令行工具(Command-Line Interface, CLI)，帮助进行一些插件服务的运维和管理工作，比如更新插件的之后，可以通过CLI重新加载插件。

## 快速开始

### 编译

在编译插件服务的时候，会同时编译CLI。在插件服务项目的根目录下，执行：

```
mvn clean package
```

编译之后，压缩包在路径`ps-cli/target/`之下。

当然，也可以在[release](https://github.com/AntChainOpenLab/AntChainBridgePluginServer/releases)页面下载安装包。

### 运行

首先，解压安装包，这里以`plugin-server-cli-0.2.0.tar.gz`为例。

```
tar -zxf plugin-server-cli-0.2.0.tar.gz
```

然后，进入文件夹`plugin-server-cli`，文件结构如下：

```
.
├── README.md
├── bin
│   └── start.sh
└── lib
    └── ps-cli-0.2.0.jar

2 directories, 3 files
```

通过运行`bin/start.sh -h`可以打印帮助信息，查看脚本使用方式：

```
╰─± bin/start.sh -h

 start.sh — Start the CLI tool to manage your plugin server

 Usage:
   start.sh <params>

 Examples:
  1. print help info:
   start.sh -h
  2. identify the port number to start CLI
   start.sh -p 9091
  3. identify the server IP to start CLI
   start.sh -H 0.0.0.0

 Options:
   -h         print help info
   -p         identify the port number to start CLI, default 9091
   -H         identify the server IP to start CLI, default 0.0.0.0

```

运行`bin/start.sh`以启动CLI，按下Tab键可以补全命令。

### 使用

#### 重新加载插件

场景：让插件服务，重新加载某个类型的插件。如果你修改了插件逻辑并重新编译，将插件的Jar包拷贝并覆盖原有文件，运行命令，插件服务会重新加载新的插件文件。

命令：`manage.reloadPlugin(String product)`

参数：参数`product`为插件中设置的区块链类型，比如我们提供的插件[demo](https://github.com/AntChainOpenLab/AntChainBridgePluginSDK/blob/df28f973a6f0ebdf204b86c2e49aa1be8f8d0c0c/pluginset/ethereum/offchain-plugin/src/main/java/com/alipay/antchain/bridge/plugins/ethereum/EthereumBBCService.java#L57)中的`simple-ethereum`。

```
    ___            __   ______ __            _           ____         _      __
   /   |   ____   / /_ / ____// /_   ____ _ (_)____     / __ ) _____ (_)____/ /____ _ ___
  / /| |  / __ \ / __// /    / __ \ / __ `// // __ \   / __  |/ ___// // __  // __ `// _ \
 / ___ | / / / // /_ / /___ / / / // /_/ // // / / /  / /_/ // /   / // /_/ // /_/ //  __/
/_/  |_|/_/ /_/ \__/ \____//_/ /_/ \__,_//_//_/ /_/  /_____//_/   /_/ \__,_/ \__, / \___/
                                                                            /____/
                          PLUGIN SERVER CLI 0.2.0

>>> type help to see all commands...
ps>
ps> manage.reloadPlugin("simple-ethereum")
```

#### 在新路径重新加载插件

场景：让插件服务，在一个新路径重新加载某个类型的插件。如果你修改了插件逻辑并重新编译，指定新的插件文件路径，运行命令，插件服务会重新加载新的插件文件。

命令：`manage.reloadPluginInNewPath(String product, String path)`

参数：参数`product`为插件中设置的区块链类型；参数`path`指定新的插件文件路径；

```
    ___            __   ______ __            _           ____         _      __
   /   |   ____   / /_ / ____// /_   ____ _ (_)____     / __ ) _____ (_)____/ /____ _ ___
  / /| |  / __ \ / __// /    / __ \ / __ `// // __ \   / __  |/ ___// // __  // __ `// _ \
 / ___ | / / / // /_ / /___ / / / // /_/ // // / / /  / /_/ // /   / // /_/ // /_/ //  __/
/_/  |_|/_/ /_/ \__/ \____//_/ /_/ \__,_//_//_/ /_/  /_____//_/   /_/ \__,_/ \__, / \___/
                                                                            /____/
                          PLUGIN SERVER CLI 0.2.0

>>> type help to see all commands...
ps>
ps> manage.reloadPluginInNewPath("simple-ethereum", "path/to/new/simple-ethereum-bbc-0.1-SNAPSHOT-plugin.jar")
```

#### 重启BBC实例

场景：当中继请求插件服务为某条链启动BBC实例之后，插件服务会从插件中读取相关Class，创建BBC实例，而更新了插件代码，经过插件的reload之后，需要重启BBC实例，才可以使用更新之后的代码提供服务。

命令：`manage.restartBBC(String product, String domain)`

参数：参数`product`为插件中设置的区块链类型；参数`domain`是你想让插件服务重新创建的BBC实例；

```
    ___            __   ______ __            _           ____         _      __
   /   |   ____   / /_ / ____// /_   ____ _ (_)____     / __ ) _____ (_)____/ /____ _ ___
  / /| |  / __ \ / __// /    / __ \ / __ `// // __ \   / __  |/ ___// // __  // __ `// _ \
 / ___ | / / / // /_ / /___ / / / // /_/ // // / / /  / /_/ // /   / // /_/ // /_/ //  __/
/_/  |_|/_/ /_/ \__/ \____//_/ /_/ \__,_//_//_/ /_/  /_____//_/   /_/ \__,_/ \__, / \___/
                                                                            /____/
                          PLUGIN SERVER CLI 0.2.0

>>> type help to see all commands...
ps>
ps> manage.restartBBC("simple-ethereum", "domainA.eth.org")
success
```

#### 加载新插件

场景：让插件服务，加载一个新插件，插件的类型和ID必须没有被加载过。

命令：`manage.loadPlugin(String path)`

参数：参数`path`指定新的插件文件路径，路径是相对插件服务进程的，相对路径和绝对路径都会被判断为相等；

```
    ___            __   ______ __            _           ____         _      __
   /   |   ____   / /_ / ____// /_   ____ _ (_)____     / __ ) _____ (_)____/ /____ _ ___
  / /| |  / __ \ / __// /    / __ \ / __ `// // __ \   / __  |/ ___// // __  // __ `// _ \
 / ___ | / / / // /_ / /___ / / / // /_/ // // / / /  / /_/ // /   / // /_/ // /_/ //  __/
/_/  |_|/_/ /_/ \__/ \____//_/ /_/ \__,_//_//_/ /_/  /_____//_/   /_/ \__,_/ \__, / \___/
                                                                            /____/
                          PLUGIN SERVER CLI 0.2.0

>>> type help to see all commands...
ps>
ps> manage.loadPlugin("path/to/simple-ethereum-bbc-0.1-SNAPSHOT-plugin.jar")
```

#### 启动新插件

场景：让插件服务，启动一个新加载的插件（重新加载的不需要），**只有启动之后的插件才可以用于创建BBC对象**。

命令：`manage.startPlugin(String path)`

参数：参数`path`指定该插件文件路径，路径是相对插件服务进程的，相对路径和绝对路径都会被判断为相等；

```
    ___            __   ______ __            _           ____         _      __
   /   |   ____   / /_ / ____// /_   ____ _ (_)____     / __ ) _____ (_)____/ /____ _ ___
  / /| |  / __ \ / __// /    / __ \ / __ `// // __ \   / __  |/ ___// // __  // __ `// _ \
 / ___ | / / / // /_ / /___ / / / // /_/ // // / / /  / /_/ // /   / // /_/ // /_/ //  __/
/_/  |_|/_/ /_/ \__/ \____//_/ /_/ \__,_//_//_/ /_/  /_____//_/   /_/ \__,_/ \__, / \___/
                                                                            /____/
                          PLUGIN SERVER CLI 0.2.0

>>> type help to see all commands...
ps>
ps> manage.startPlugin("path/to/simple-ethereum-bbc-0.1-SNAPSHOT-plugin.jar")
```

#### 停止插件

场景：让插件服务，停止使用某个插件，只有启动之后的插件才可以停止。

命令：`manage.stopPlugin(String product)`

参数：参数`product`为插件中设置的区块链类型；

```
    ___            __   ______ __            _           ____         _      __
   /   |   ____   / /_ / ____// /_   ____ _ (_)____     / __ ) _____ (_)____/ /____ _ ___
  / /| |  / __ \ / __// /    / __ \ / __ `// // __ \   / __  |/ ___// // __  // __ `// _ \
 / ___ | / / / // /_ / /___ / / / // /_/ // // / / /  / /_/ // /   / // /_/ // /_/ //  __/
/_/  |_|/_/ /_/ \__/ \____//_/ /_/ \__,_//_//_/ /_/  /_____//_/   /_/ \__,_/ \__, / \___/
                                                                            /____/
                          PLUGIN SERVER CLI 0.2.0

>>> type help to see all commands...
ps>
ps> manage.stopPlugin("simple-ethereum")
```

#### 启动已停止的插件

场景：让插件服务重新启动某个已经停止的插件，只有停止之后的插件才可以重新启动。

命令：`manage.startPluginFromStop(String product)`

参数：参数`product`为插件中设置的区块链类型；

```
    ___            __   ______ __            _           ____         _      __
   /   |   ____   / /_ / ____// /_   ____ _ (_)____     / __ ) _____ (_)____/ /____ _ ___
  / /| |  / __ \ / __// /    / __ \ / __ `// // __ \   / __  |/ ___// // __  // __ `// _ \
 / ___ | / / / // /_ / /___ / / / // /_/ // // / / /  / /_/ // /   / // /_/ // /_/ //  __/
/_/  |_|/_/ /_/ \__/ \____//_/ /_/ \__,_//_//_/ /_/  /_____//_/   /_/ \__,_/ \__, / \___/
                                                                            /____/
                          PLUGIN SERVER CLI 0.2.0

>>> type help to see all commands...
ps>
ps> manage.startPluginFromStop("simple-ethereum")
```

#### 查询当前所有已启动插件的类型列表

场景：让插件服务返回当前所有已经启动的插件类型列表。

命令：`manage.allPlugins()`

```
    ___            __   ______ __            _           ____         _      __
   /   |   ____   / /_ / ____// /_   ____ _ (_)____     / __ ) _____ (_)____/ /____ _ ___
  / /| |  / __ \ / __// /    / __ \ / __ `// // __ \   / __  |/ ___// // __  // __ `// _ \
 / ___ | / / / // /_ / /___ / / / // /_/ // // / / /  / /_/ // /   / // /_/ // /_/ //  __/
/_/  |_|/_/ /_/ \__/ \____//_/ /_/ \__,_//_//_/ /_/  /_____//_/   /_/ \__,_/ \__, / \___/
                                                                            /____/
                          PLUGIN SERVER CLI 0.2.0

>>> type help to see all commands...
ps> manage.allPlugins()
simple-ethereum, testchain
```

#### 查询当前所有服务中的区块链域名列表

场景：让插件服务返回当前所有服务中的区块链域名列表

命令：` manage.allDomains()`

```
    ___            __   ______ __            _           ____         _      __
   /   |   ____   / /_ / ____// /_   ____ _ (_)____     / __ ) _____ (_)____/ /____ _ ___
  / /| |  / __ \ / __// /    / __ \ / __ `// // __ \   / __  |/ ___// // __  // __ `// _ \
 / ___ | / / / // /_ / /___ / / / // /_/ // // / / /  / /_/ // /   / // /_/ // /_/ //  __/
/_/  |_|/_/ /_/ \__/ \____//_/ /_/ \__,_//_//_/ /_/  /_____//_/   /_/ \__,_/ \__, / \___/
                                                                            /____/
                          PLUGIN SERVER CLI 0.2.0

>>> type help to see all commands...
ps> manage.allDomains()
chaina, chainb
```

#### 查询某些区块链是否有启动的插件

场景：查询插件服务某些指定的区块链类型是否有已经启动的插件。

命令：` manage.hasPlugins(String[] products...)`

参数：`products`是区块链插件类型的数组；

```
    ___            __   ______ __            _           ____         _      __
   /   |   ____   / /_ / ____// /_   ____ _ (_)____     / __ ) _____ (_)____/ /____ _ ___
  / /| |  / __ \ / __// /    / __ \ / __ `// // __ \   / __  |/ ___// // __  // __ `// _ \
 / ___ | / / / // /_ / /___ / / / // /_/ // // / / /  / /_/ // /   / // /_/ // /_/ //  __/
/_/  |_|/_/ /_/ \__/ \____//_/ /_/ \__,_//_//_/ /_/  /_____//_/   /_/ \__,_/ \__, / \___/
                                                                            /____/
                          PLUGIN SERVER CLI 0.2.0

>>> type help to see all commands...
ps> manage.hasPlugins("simple-ethereum", "testchain", "notexist")
{
	"testchain":true,
	"notexist":false, # false 代表不存在
	"simple-ethereum":true # true 代表存在
}
```

#### 查询某些域名是否正在服务

场景：查询插件服务某些域名是否正在服务，即有正在运行的区块链BBC对象。

命令：` manage.hasDomains(String[] domains...)`

参数：`domains`是区块链插件类型的数组；

```
    ___            __   ______ __            _           ____         _      __
   /   |   ____   / /_ / ____// /_   ____ _ (_)____     / __ ) _____ (_)____/ /____ _ ___
  / /| |  / __ \ / __// /    / __ \ / __ `// // __ \   / __  |/ ___// // __  // __ `// _ \
 / ___ | / / / // /_ / /___ / / / // /_/ // // / / /  / /_/ // /   / // /_/ // /_/ //  __/
/_/  |_|/_/ /_/ \__/ \____//_/ /_/ \__,_//_//_/ /_/  /_____//_/   /_/ \__,_/ \__, / \___/
                                                                            /____/
                          PLUGIN SERVER CLI 0.2.0

>>> type help to see all commands...
ps> manage.hasDomains("chaina", "chainb")
{
	"chainb":true,
	"chaina":false
}
```

