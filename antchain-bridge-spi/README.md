<div align="center">
  <img alt="am logo" src="https://gw.alipayobjects.com/zos/bmw-prod/3ee4adc7-1960-4dbf-982e-522ac135a0c0.svg" width="250" >
  <h1 align="center">AntChainBridge 插件SPI库</h1>
  <p align="center">
    <a href="http://makeapullrequest.com">
      <img alt="pull requests welcome badge" src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat">
    </a>
  </p>
</div>
# 链下插件的SPI定义

_*以下接口均使用Java语言描述_

## IBBCService

​	为特定类型的区块链开发的插件中需要 IBBCService。 一个 IBBCService 实例对应一个特定的区块链网络。 IBBCService 处理与相应区块链网络的所有通信请求	

### 启动服务实例

```java
void startup(AbstractBBCContext context);
```

### 释放与服务关联的所有资源

```java
void shutdown();
```

### 获取服务的序列化的上下文

```java
AbstractBBCContext getContext();
```

## IAntChainBridgeDataWriter

中继可以通过 IAntChainBridgeDataWriter 将AntChainBridge数据写入区块链。

AntChainBridge数据是AntChainBridge系统在跨链过程中产生的数据。 例如，中继器将跨链消息从发送方链中继到接收方链，消息是一种AntChainBridge数据。 大多数情况下，IAntChainBridgeDataWriter 实例通过区块链 SDK 发送交易来发送 AntChainBridge数据。

### 设置AM合约

AM合约是通信协议栈的最底层合约，负责提供消息的可验证能力，如果存在PTC合约，AM合约会使用PTC合约验证消息证明，但是在本次开源版本中，我们将去掉这部分接口逻辑。

```java
void setupAuthMessageContract();
```

### 设置SDP合约

SDP合约是在协议栈中，是AM合约的上层协议。SDP合约调用AM合约的接口以发送消息，并且实现接口从AM合约接收消息，再传递到上层应用合约。

```java
void setupSDPMessageContract();
```

## IAMWriter

针对跨链可认证消息的功能接口。

这里实现和AuthMessage合约的交互能力，主要是配置上层协议合约地址、合法中继地址以及向区块链提交跨链消息。

### 配置上层协议

```java
void setProtocol(String protocolAddress, String protocolType);
```

### 转发跨链的可认证消息

```java
CrossChainMessageReceipt relayAuthMessage(byte[] rawMessage);
```

## ISDPWriter

消息推送合约（SDP）的管理接口。

### 设置可认证消息合约地址

```java
void setAmContract(String contractAddress);
```

### 设置本链的域名到SDP合约

```java
void setLocalDomain(String domain)
```

## IAntChainBridgeDataReader

我们可以通过 IAntChainBridgeDataReader 从区块链读取 AntChainBridge数据。

### 检查某个已经提交上链的跨链消息的状态

```java
boolean isCrossChainMessageConfirmed(CrossChainMessageReceipt receipt);
```

### 获取某高度区块中的跨链信息等，比如某些特定的合约事件等

在本次开源版本中，主要是AuthMessage合约通过某些手段写在账本中的数据，其中包含了要发出的跨链AuthMessage。

```java
List<CrossChainMessage> readCrossChainMessagesByHeight(long height);
```

## ISDPReader

用于读取消息推送合约的接口。

### 查询合约间推送的消息的序列号

SDP消息合约提供有序消息和无序消息，有序消息的顺序由【发送域名、发送合约、接收域名、接收合约】四者唯一确定，消息的序列号从零依次递增。

```java
long querySDPMessageSeq(String senderDomain, String fromAddress, String receiverDomain, String toAddress);
```

