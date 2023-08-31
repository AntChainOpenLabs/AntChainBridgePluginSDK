<div align="center">
  <img alt="am logo" src="https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/antchain.png" width="250" >
  <h1 align="center">AntChain Bridge EOS插件系统合约库</h1>
</div>

# 1 部署合约

部署合约过程中涉及的`kxjdrelayer1`账户为插件（中继）账户，需要有调用AM、SDP合约的权限

## 合约介绍

- AM合约：负责吐出发送的跨链消息，负责接收中继提交的消息。
- SDP合约：负责接收来自应用合约的消息，并且将消息传送给AM合约。
- Demo合约：这是一个Demo合约，实现了简单的消息接收、发送；

下面的图片介绍了合约之间的关系，主要他们之间的调用权限，在部署时添加这个权限。

<img src="https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/eos-contracts-arch.jpeg" style="zoom:50%;" />

## 部署AM合约

**以下内容均以cleos等命令行工具为例*

创建sys.am合约账户并编译部署AM合约，当然账户名字由您按照场景决定，需要将这些合约账户名字填到插件的配置文件中。

```shell
# 创建账户
$ cleos wallet create_key
Created new private key with a public key of: "EOS8H1etohrsde88GhH9RWjKeY54ZtP2nA9djU3c6Gwdg8q25cZj7"

$ cleos create account eosio sys.am EOS8H1etohrsde88GhH9RWjKeY54ZtP2nA9djU3c6Gwdg8q25cZj7 EOS8H1etohrsde88GhH9RWjKeY54ZtP2nA9djU3c6Gwdg8q25cZj7
executed transaction: 30d4c87156148bf439cb615f5802dee8e86ba4fa4b6d39d7768577ca7413935d  200 bytes  2118 us
#         eosio <= eosio::newaccount            {"creator":"eosio","name":"sys.am","owner":{"threshold":1,"keys":[{"key":"EOS8H1etohrsde88GhH9RWjKeY...
warning: transaction executed locally, but may not be confirmed by the network yet         ]

# 编译（需要预先创建bin/am/目录）
$ eosio-cpp -o bin/am/sys.am.wasm src/sys.am.cpp --abigen

# 部署
$ cleos set contract sys.am bin/am/ sys.am.wasm sys.am.abi -p sys.am@owner -p eosio@active -p sys.sdp@active -p test@active
Reading WASM from /Users/liyuan/work/antchain/odatspluginwork/pluginset/eos/onchain-plugin/cpp/sys/bin/am/sys.am.wasm...
Publishing contract...
executed transaction: 59218a8e9ca7bb8d87570a2b8d166e11cc19c016f034a144faf72a6a8e018bd8  23568 bytes  5948 us
#         eosio <= eosio::setcode               {"account":"sys.am","vmtype":0,"vmversion":0,"code":"0061736d0100000001ba022e60000060037f7f7f017f600...
#         eosio <= eosio::setabi                {"account":"sys.am","abi":"0e656f73696f3a3a6162692f312e32000a0a61646472656c61796572000207696e766f6b6...
warning: transaction executed locally, but may not be confirmed by the network yet         ]

```

## 部署SDP合约

创建sys.sdp合约账户并编译部署SDP合约

```shell
# 创建账户
$ cleos wallet create_key
Created new private key with a public key of: "EOS6go5jLUooxviFpsB4PRGvZYw5VkJnTcqRkxoUXWS4ngQaKw7QL"
$ cleos create account eosio sys.sdp EOS6go5jLUooxviFpsB4PRGvZYw5VkJnTcqRkxoUXWS4ngQaKw7QL EOS6go5jLUooxviFpsB4PRGvZYw5VkJnTcqRkxoUXWS4ngQaKw7QL
executed transaction: 873832a4568a6620789f64c97de9ff58ff1d9fd3cabe833f4ae1c0f0100a6bd4  200 bytes  455 us
#         eosio <= eosio::newaccount            {"creator":"eosio","name":"sys.sdp","owner":{"threshold":1,"keys":[{"key":"EOS6go5jLUooxviFpsB4PRGvZ...
warning: transaction executed locally, but may not be confirmed by the network yet         ]

# 编译
eosio-cpp -o bin/sdp/sys.sdp.wasm src/sys.sdp.cpp --abigen

# 部署
# 这里像demo、test等账户，视情况创建即可。
$ cleos set contract sys.sdp bin/sdp/ sys.sdp.wasm sys.sdp.abi -p sys.sdp@owner -p eosio@active -p sys.am@active -p demo@active -p test@active
Reading WASM from /Users/liyuan/work/antchain/odatspluginwork/pluginset/eos/onchain-plugin/cpp/sys/bin/sdp/sys.sdp.wasm...
Publishing contract...
executed transaction: 723aaf1e7215331668dec085749def52cad7b27b90ce454e693c6ef6ded8c695  25344 bytes  8270 us
#         eosio <= eosio::setcode               {"account":"sys.sdp","vmtype":0,"vmversion":0,"code":"0061736d0100000001b1022d60000060037f7f7f017f60...
#         eosio <= eosio::setabi                {"account":"sys.sdp","abi":"0e656f73696f3a3a6162692f312e32000a0a636f756e747461626c65000105636f756e74...
warning: transaction executed locally, but may not be confirmed by the network yet         ]

```

## 部署DEMO合约

**如果需要测试的话，可以部署Demo合约，如果不需要可以不部署。**

创建demo合约账户并编译部署DEMO合约。

```shell
#创建账户
$ cleos wallet create_key
Created new private key with a public key of: "EOS5e8zrR2u4Rqk5Pg3Sz7M6w1hN9dShjMUSuVFyyWybijamVCBEy"

$ cleos create account eosio demo EOS5e8zrR2u4Rqk5Pg3Sz7M6w1hN9dShjMUSuVFyyWybijamVCBEy EOS5e8zrR2u4Rqk5Pg3Sz7M6w1hN9dShjMUSuVFyyWybijamVCBEy
executed transaction: 93b4d97ae8a4304728ffea4990983dd1785ce08f25ba2cc28f4e55d6a6b90041  200 bytes  319 us
#         eosio <= eosio::newaccount            {"creator":"eosio","name":"demo","owner":{"threshold":1,"keys":[{"key":"EOS5e8zrR2u4Rqk5Pg3Sz7M6w1hN...
warning: transaction executed locally, but may not be confirmed by the network yet         ]

# 编译
$ eosio-cpp -o bin/demo/demo.wasm src/demo.cpp --abigen

# 部署
$ cleos set contract demo bin/demo/ demo.wasm demo.abi -p demo@owner -p eosio@active -p sys.sdp@active -p test@active
Reading WASM from /Users/liyuan/work/antchain/odatspluginwork/pluginset/eos/onchain-plugin/cpp/sys/bin/demo/demo.wasm...
Publishing contract...
executed transaction: 3a4e0e35643b6416656957edc2c111119891daa23cf772265390df58dea7d58f  19680 bytes  2515 us
#         eosio <= eosio::setcode               {"account":"demo","vmtype":0,"vmversion":0,"code":"0061736d01000000019d022b60000060037f7f7f017f60037...
#         eosio <= eosio::setabi                {"account":"demo","abi":"0e656f73696f3a3a6162692f312e32000507726563766d7367000407696e766f6b6572046e6...
warning: transaction executed locally, but may not be confirmed by the network yet         ]

```

## 合约账户授权

为了保证am、sdp和demo合约可以调用其他合约，对三个合约账户分别进行active授权。

```shell
~/work/antChain/odatspluginwork/pluginset/eos/onchain-plugin/cpp/sys on  feat/zhongchuan_eos/kms_support! ⌚ 23:42:32
$ cleos set account permission sys.am active --add-code -p sys.am@active
executed transaction: 99613fba7ef541adc1f3f3c59e303adcbfaf8aab68f3d759aa100098950c23dd  184 bytes  342 us
#         eosio <= eosio::updateauth            {"account":"sys.am","permission":"active","parent":"owner","auth":{"threshold":1,"keys":[{"key":"EOS...
warning: transaction executed locally, but may not be confirmed by the network yet         ]

~/work/antChain/odatspluginwork/pluginset/eos/onchain-plugin/cpp/sys on  feat/zhongchuan_eos/kms_support! ⌚ 23:42:36
$ cleos set account permission sys.sdp active --add-code -p sys.sdp@active
executed transaction: bc34ef90fb81180badc0b5d4156f302b45fa26101788edaf48d410a1b06c757b  184 bytes  214 us
#         eosio <= eosio::updateauth            {"account":"sys.sdp","permission":"active","parent":"owner","auth":{"threshold":1,"keys":[{"key":"EO...
warning: transaction executed locally, but may not be confirmed by the network yet         ]

~/work/antChain/odatspluginwork/pluginset/eos/onchain-plugin/cpp/sys on  feat/zhongchuan_eos/kms_support! ⌚ 23:42:51
$ cleos set account permission demo active --add-code -p demo@active
executed transaction: 26c58cf79568d47d4896c45a6f57a53d90bab54ff7625b242997f0a043262c02  184 bytes  347 us
#         eosio <= eosio::updateauth            {"account":"demo","permission":"active","parent":"owner","auth":{"threshold":1,"keys":[{"key":"EOS5e...
warning: transaction executed locally, but may not be confirmed by the network yet         ]

```

# 2 配置合约信息

调用合约进行合约中的参数配置，主要是下面一步，配置中继的账户到AM合约，以支持中继向AM合约提交跨链消息。

## 设置AM合约中的中继信息

- 假设kxjdrelayer1 为中继账户，需要有调用SDP、AM合约的权限。
- 调用AM合约的`addrelayer`接口，将账户`kxjdrelayer1`配置为中继，使其有权限提交跨链消息；

```shell
$ cleos push action sys.am addrelayer '["test","kxjdrelayer1"]' -p test@active
executed transaction: 47a413b0468789fa7bf2379478d1b54abbc6fe3585e820f76b235e7a4ee09fd7  112 bytes  159 us
#        sys.am <= sys.am::addrelayer           {"invoker":"test","relayer_account":"kxjdrelayer1"}
>> AMMSG_INFO: add relayer kxjdrelayer1
warning: transaction executed locally, but may not be confirmed by the network yet         ]

```



