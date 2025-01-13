以下所有操作都在 `fabric-samples/test-network` 目录下，fabric为2.*版本（自测过2.2和2.5.0）

# 1 启动fabric网络
```shell
# 启动网络并创建通道
cd fabric-samples/test-network
./network.sh up createChannel -ca
```

# 2 部署合约  

- **关于cross链码**

cross链码依赖1.4版本的fabric，故直接依赖代码中已有的vendor，执行合约部署时将`scripts/deployCC.sh`中的 `GO111MODULE=on go mod vendor` 注释掉。如果不注释使用2.*版本的fabric安装cross会出现报错

- **关于tb和asset链码**

这两个链码均依赖2.*版本的fabric和1.20+的go环境  

如果本地是mac环境，执行链码安装时可能会缺少部分文件，比如报错`No such file or directory #include "libsecp256k1/include/secp256k1.h"`，这是mac环境编译和docker中的linux环境不一致所致  

可以直接从go mod依赖库中手动拷贝`libsecp256k1`目录到vendor中相应的位置，然后将`scripts/deployCC.sh`中的`GO111MODULE=on go mod vendor`注释掉，再重新执行链码安装命令即可

```shell
# 部署资产合约 $ERC1155 为自定义合约名称
./network.sh deployCC -ccn $ERC1155 -ccp $repo/src/nft_crosschain/assets/token/cmd -ccl go

# 部署TB合约 $TOKEN_BRIDGE 为自定义合约名称
./network.sh deployCC -ccn $TOKEN_BRIDGE -ccp $repo/src/nft_crosschain/token_bridge/cmd/ -ccl go

# 部署跨链合约 $CROSS_CHAIN 为自定义合约名称
./network.sh deployCC -ccn $CROSS_CHAIN -ccp $repo/src/cross/ -ccl go
```

# 3 配置环境变量
```shell
cd fabric-samples/test-network
# 注意fabric的bin目录位置是否在fabric-samples下，同时需要保证当前目录下有go 1.20环境
export PATH=${PWD}/../bin:$PATH
export FABRIC_CFG_PATH=${PWD}/../config/

export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_ADDRESS=localhost:7051
export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp

# 参数简化
export TARGET_TLS_OPTIONS=(-o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem" --peerAddresses localhost:7051 --tlsRootCertFiles "${PWD}/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt" --peerAddresses localhost:9051 --tlsRootCertFiles "${PWD}/organizations/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt")
```

# 4 调用合约进行测试

## 4.1 B链信息配置
```shell
# 按实际情况配置
# B链域名
export B_DOMAIN="OtherDomain" 
# B链TB合约
export B_TB="111767e597a7d803e97c796aa4faf05f5601c6d48e0353167c842a990e2f9fc8"
# B链资产合约
export B_ASSET="222767e597a7d803e97c796aa4faf05f5601c6d48e0353167c842a990e2f9fc8"
# B链账户
export B_ACCOUNT="333767e597a7d803e97c796aa4faf05f5601c6d48e0353167c842a990e2f9fc8"
```

## 4.2 资产合约初始化

```shell
# 资产合约初始化：设置资产合约名称为 $ERC1155
peer chaincode invoke "${TARGET_TLS_OPTIONS[@]}" -C mychannel -n $ERC1155 -c '{"Args":["init", "'$ERC1155'", "T1155"]}'
# 查看当前client账户地址：查看结果为 $ACCOUNT
peer chaincode query -C mychannel -n $ERC1155 -c '{"function":"clientAccountID","Args":[]}'
# 铸造原始资产：为当前账户mint 10 个token
peer chaincode invoke "${TARGET_TLS_OPTIONS[@]}" -C mychannel -n $ERC1155 -c '{"Args":["mint", "'$ACCOUNT'", "1", "10", "data"]}'

# 查询当前账户余额（10）
peer chaincode query -C mychannel -n $ERC1155 -c '{"function":"balanceOf","Args":["'$ACCOUNT'","1"]}'
# 查询tb合约锁定余额（0）
peer chaincode query -C mychannel -n $ERC1155 -c '{"function":"balanceOf","Args":["'$TOKEN_BRIDGE'","1"]}'
```

## 4.3 TB合约初始化

```shell
# tb合约初始化：设置tb合约名称为 tokenBridge 设置跨链合约名称为 crosschain
peer chaincode invoke "${TARGET_TLS_OPTIONS[@]}" -C mychannel -n $TOKEN_BRIDGE -c '{"Args":["init", "'$TOKEN_BRIDGE'", "'$CROSS_CHAIN'"]}'
# tb合约设置 tbmap
peer chaincode invoke "${TARGET_TLS_OPTIONS[@]}" -C mychannel -n $TOKEN_BRIDGE -c '{"Args":["setDomainTokenBridgeAddress", "'$B_DOMAIN'", "'$B_TB'"]}'
# tb合约设置 assetRoute
peer chaincode invoke "${TARGET_TLS_OPTIONS[@]}" -C mychannel -n $TOKEN_BRIDGE -c '{"Args":["registerRouter", "'$ERC1155'", "'$B_DOMAIN'", "'$B_ASSET'"]}'

# (更新时可能用得上) tb合约设置cc地址
peer chaincode invoke "${TARGET_TLS_OPTIONS[@]}" -C mychannel -n $TOKEN_BRIDGE -c '{"Args":["setIbcMsgAddress", "'$CROSS_CHAIN'"]}'

```

## 4.4 CC合约初始化
当前步骤仅用于无中继环境的自测，正式环境中由中继自动执行，可直接跳过本步骤
```shell

# 注册TB合约链码名称
peer chaincode invoke "${TARGET_TLS_OPTIONS[@]}" -C mychannel -n $CROSS_CHAIN -c '{"Args":["oracleAdminManage", "registerSha256Invert", "'$TOKEN_BRIDGE'"]}'
# 注册自己的domain
export DOMAIN="MyDomain"
peer chaincode invoke "${TARGET_TLS_OPTIONS[@]}" -C mychannel -n $CROSS_CHAIN -c '{"Args":["oracleAdminManage", "setExpectedDomain", "'$DOMAIN'"]}'
# 注册B链消息的parse
export B_PARSER="OtherParser"
peer chaincode invoke "${TARGET_TLS_OPTIONS[@]}" -C mychannel -n $CROSS_CHAIN -c '{"Args":["setDomainParser", "'$B_DOMAIN'", "'$B_PARSER'"]}'

```


## 4.5 资产转移自测

发送资产
```shell
# 调用 safeTransferFrom 发起跨链交易，$TRANSFER_MSG_HEX 为已pack的跨链请求的hex字符串，需自行构造
peer chaincode invoke "${TARGET_TLS_OPTIONS[@]}" -C mychannel -n $ERC1155 -c '{"Args":["safeTransferFrom", "'$ACCOUNT'", "'$TOKEN_BRIDGE'", "1", "1", "'$TRANSFER_MSG_HEX'"]}'
# 为方便构造参数，也可以调用 testSafeTransferFrom 进行交易发送
peer chaincode invoke "${TARGET_TLS_OPTIONS[@]}" -C mychannel -n $ERC1155 -c '{"Args":["testSafeTransferFrom", "'$ACCOUNT'", "'$TOKEN_BRIDGE'", "1", "1", "'$B_DOMAIN'", "'$B_ACCOUNT'"]}'
``` 

接收资产，仅用于自测（自测时建议【先发送资产再接收资产】【先接收资产再发送资产】两个流程均走一遍），正式环境中被动接收合约调用
```shell
# 用于【TB->资产】的自测，调用tb合约 testRecvUnorderedMessage_CrossReq 测试方法
peer chaincode invoke "${TARGET_TLS_OPTIONS[@]}" -C mychannel -n $TOKEN_BRIDGE -c '{"Args":["testRecvUnorderedMessage_CrossReq", "'$B_DOMAIN'", "'$B_TB'", "1", "1", "'$B_ASSET'", "'$ERC1155'", "'$ACCOUNT'"]}'

# 用于【CC->TB->资产】的自测，自行构造好 $HEX_MSG ，调用cc合约的 recvMessage 测试方法
export HEX_MSG="000000000000025100004b02000005003402000000002e0200000000280200000000000800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006a87b6e4d79446f6d61696e0000000000000000000000000000000000000000ffffffff7aa5ae9ecf82f6fc5208ecd0940fd71d50cd4ac596fccac2ede05af8000000000000000000000000000000000000000000000000000000000000014000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000100222767e597a7d803e97c796aa4faf05f5601c6d48e0353167c842a990e2f9fc838b46e7e6dfe3fd5f26c4b8de4eeda5cd0b82d57c9145415ad326367416cbccaae3b756b862137fb017a97acb0ab1f98441b337871cf1e03fcd2646f5a5dfe950000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000001c400000000111767e597a7d803e97c796aa4faf05f5601c6d48e0353167c842a990e2f9fc80000000109000b0000004f74686572446f6d61696e"
peer chaincode invoke "${TARGET_TLS_OPTIONS[@]}" -C mychannel -n $CROSS_CHAIN -c '{"Args":["recvMessage", "serviceID", "'$HEX_MSG'"]}'
```