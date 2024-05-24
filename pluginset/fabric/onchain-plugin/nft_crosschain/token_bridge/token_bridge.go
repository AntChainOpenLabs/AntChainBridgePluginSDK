package token_bridge

import (
	"encoding/hex"
	"encoding/json"
	"fabric_nft_crosschain/utils"
	"fmt"
	"github.com/ethereum/go-ethereum/accounts/abi"
	"github.com/hyperledger/fabric-chaincode-go/shim"
	"github.com/hyperledger/fabric-protos-go/peer"
	"math/big"
)

type TokenBridge struct{}

var _ ERC1155Receiver = (*TokenBridge)(nil)

const (
	ChAINCODE_NAME    = "TokenBridge"
	TOKEN_BRIDGE      = "%s-TokenBridges"
	ROUTE_TABLE       = "%s-%s-RouteTable" // srcAssetChaincodeNameHash, dstDomain -> dstAssetContractAddr
	CODE_NAME_MAP     = "%s-CodeNameMap"
	ASSET_LOCK_RECORD = "%s-%s-AssetLockRecord"
	IBC_MSG_ADDR      = "IbcMsgAddr"

	EVENT_CROSSCHAIN = "CrossChainEvent"
)

type CrossChainEvent struct {
	Domain           []byte     `json:"domain"`
	SrcContract      []byte     `json:"src_contract"`
	DestContract     []byte     `json:"dest_contract"`
	Ids              []*big.Int `json:"ids"`
	Amounts          []*big.Int `json:"amounts"`
	Holder           []byte     `json:"holder"`
	CrossChainStatus uint8      `json:"cross_chain_status"`
}

func (tb *TokenBridge) Init(stub shim.ChaincodeStubInterface) peer.Response {
	// 1 判断是否已经初始化
	rawName, _ := stub.GetState(ChAINCODE_NAME)
	if len(rawName) != 0 {
		return shim.Success(nil)
	}

	// 2 参数检查
	args := stub.GetArgs()
	if len(args) != 3 {
		return shim.Error("args number should be 2")
	}
	// 2.1 name
	if args[1] == nil {
		return shim.Error(fmt.Sprintf("token name can't be empty"))
	}
	// 2.2 sdp addr
	if args[2] == nil {
		return shim.Error(fmt.Sprintf("ibc msg addr can't be empty"))
	}

	// 3 存参数状态
	if err := stub.PutState(ChAINCODE_NAME, args[1]); err != nil {
		return shim.Error(fmt.Sprintf("failed To put token name: %v", err))
	}
	if err := stub.PutState(IBC_MSG_ADDR, args[2]); err != nil {
		return shim.Error(fmt.Sprintf("failed To put ibc msg addr: %v", err))
	}

	return shim.Success(nil)
}

func (tb *TokenBridge) Invoke(stub shim.ChaincodeStubInterface) peer.Response {
	fn, _ := stub.GetFunctionAndParameters()
	args := stub.GetArgs()
	if len(args) == 0 {
		return shim.Error("no args")
	}
	args = args[1:] // 下标0为函数名称

	switch fn {
	case "init":
		return tb.Init(stub)
	case "onERC1155Received":
		return tb.onERC1155Received(stub, args)
	case "onERC1155BatchReceived":
		return tb.onERC1155BatchReceived(stub, args)

	case "recvUnorderedMessage":
		return tb.recvUnorderedMessage(stub, args)

	case "setIbcMsgAddress":
		return tb.setIbcMsgAddress(stub, args)
	case "setDomainTokenBridgeAddress":
		return tb.setDomainTokenBridgeAddress(stub, args)
	case "registerRouter":
		return tb.registerRouter(stub, args)
	case "deregisterRouter":
		return tb.deregisterRouter(stub, args)

	case "batchLock":
		return tb.batchLock(stub)
	case "batchUnlock":
		return tb.batchUnlock(stub, args)

	// 自测接口
	case "testRecvUnorderedMessage_CrossReq":
		return tb.testRecvUnorderedMessage_CrossReq(stub, args)
	case "testRecvUnorderedMessage_CrossResp":
		return tb.testRecvUnorderedMessage_CrossResp(stub, args)
	}

	return shim.Error(fmt.Sprintf("no function name %s found", fn))
}

func (tb *TokenBridge) _emitCrosschainEvent(stub shim.ChaincodeStubInterface, event *CrossChainEvent) error {
	rawEvent, err := json.Marshal(event)
	if err != nil {
		return fmt.Errorf("failed to json marshal CrossChainEvent: %v", err)
	}
	if err := stub.SetEvent(EVENT_CROSSCHAIN, rawEvent); err != nil {
		return fmt.Errorf("failed to set cross chain event: %v", err)
	}

	fmt.Printf("crosschain event: %s\n", string(rawEvent))
	return nil
}

func (tb *TokenBridge) onERC1155Received(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	if len(args) != 6 {
		return shim.Error(fmt.Sprintf("args number should be 5 but %d", len(args)))
	}

	fmt.Printf("onERC1155Received, operatorAccount: %s, fromAccount: %s, fromContract: %s, id: %s, amount: %s, data: %s\n",
		args[0], args[1], args[2], big.NewInt(0).SetBytes(args[3]).String(), big.NewInt(0).SetBytes(args[4]).String(), args[5])

	// fromContractName是fabric链码合约名称，fromContract是资产合约名称的哈希（32个字节）
	fromContractName := args[2]
	rawFromContract := utils.GetChaincodeNameHash(fromContractName)
	if err := stub.PutState(chaincodeNameKey(rawFromContract), fromContractName); err != nil {
		return shim.Error(fmt.Sprintf("failed to update chaincode name map: %v", err))
	}

	// 从合约接收的byte参数
	id := big.NewInt(0).SetBytes(args[3])
	amt := big.NewInt(0).SetBytes(args[4])

	data := args[5]

	var transferDataABI abi.ABI
	abiStr := `[{"name":"DstDomain","type":"string"},{"name":"Holder","type":"[]byte"}]`
	if err := json.Unmarshal([]byte(abiStr), &transferDataABI); err != nil {
		return shim.Error(fmt.Sprintf("failed to get myABI: %v", err))
	}

	transferMsg, err := utils.UnpackTransferMsg(data)
	if err != nil {
		return shim.Error(fmt.Sprintf("failed to decode transfer msg: %v", err))
	}

	return tb._onERC1155Received(stub, rawFromContract, id, amt, transferMsg.DstDomain, transferMsg.Holder)
}

func (tb *TokenBridge) _onERC1155Received(stub shim.ChaincodeStubInterface, rawSrcContract []byte, id *big.Int, amt *big.Int, dstDomain []byte, holder [32]byte) peer.Response {
	// 获取目的链的TB合约
	rawDstTokenBridge, err := stub.GetState(tokenBridgeKey(dstDomain))
	if err != nil {
		return shim.Error(fmt.Sprintf("failed To get token bridges: %v", err))
	}
	if utils.IsZeroBytes(rawDstTokenBridge) {
		return shim.Error(fmt.Sprintf("unknow token bridge for domain:%s", dstDomain))
	}
	dstTokenBridge := []byte(hex.EncodeToString(rawDstTokenBridge))

	// 获取目的链的资产合约
	rawDstContract, err := stub.GetState(routeTableKey(rawSrcContract, dstDomain))
	if err != nil {
		return shim.Error(fmt.Sprintf("failed To get dst asset contract: %v", err))
	}
	if utils.IsZeroBytes(rawDstContract) {
		return shim.Error(fmt.Sprintf("unknow dst asset contract, srcContract: %s, dstDomain: %s", hex.EncodeToString(rawSrcContract), dstDomain))
	}

	// 锁定资产
	rawAssetLock, err := stub.GetState(assetLockKey(rawSrcContract, id))
	if err != nil {
		return shim.Error(fmt.Sprintf("failed To get asset-lock record: %v", err))
	}

	assetLock := big.NewInt(0).SetBytes(rawAssetLock)
	assetLock = assetLock.Add(assetLock, amt)

	if err := stub.PutState(assetLockKey(rawSrcContract, id), assetLock.Bytes()); err != nil {
		return shim.Error(fmt.Sprintf("failed To update asset lock record: %v", err))
	}

	// 获取跨链系统合约地址
	ibcMsgAddr, err := stub.GetState(IBC_MSG_ADDR)
	if err != nil || utils.IsZeroBytes(ibcMsgAddr) {
		return shim.Error(fmt.Sprintf("failed To get ibc msg address: %v", err))
	}

	// 编码跨链消息
	ids := utils.AsSingletonArray(id)
	amts := utils.AsSingletonArray(amt)
	packedData, err := utils.PackCrossChainMsg(ids, amts, utils.CopySliceToByte32(rawSrcContract), utils.CopySliceToByte32(rawDstContract), holder, uint8(utils.CROSSCHAIN_START))
	if err != nil {
		return shim.Error(fmt.Sprintf("failed To pack cross chain data: %v", err))
	}

	// 调用跨链系统合约发送跨链消息
	result := stub.InvokeChaincode(
		string(ibcMsgAddr),
		[][]byte{
			[]byte("sendUnorderedMessage"),
			dstDomain,
			dstTokenBridge,
			packedData,
		},
		stub.GetChannelID())
	if result.Status != shim.OK {
		return shim.Error(fmt.Sprintf("Failed to invoke chaincode %s: %s", string(ibcMsgAddr), result.String()))
	}

	// 抛出跨链事件
	if err := tb._emitCrosschainEvent(stub, &CrossChainEvent{
		Domain:           dstDomain,
		SrcContract:      []byte(hex.EncodeToString(rawSrcContract)),
		DestContract:     []byte(hex.EncodeToString(rawDstContract)),
		Ids:              ids,
		Amounts:          amts,
		Holder:           holder[:],
		CrossChainStatus: uint8(utils.CROSSCHAIN_START),
	}); err != nil {
		return shim.Error(fmt.Sprintf("failed to emit crosschain event: %v", err))
	}

	return shim.Success(nil)
}

func (tb *TokenBridge) onERC1155BatchReceived(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	//TODO implement me
	panic("implement me")
}

func (tb *TokenBridge) recvUnorderedMessage(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	if len(args) != 3 {
		return shim.Error("args number should be 3")
	}

	fmt.Printf("recvUnorderedMessage, fromDomain: %s, fromTBContract: %s, msgData:%s\n", args[0], args[1], args[2])

	fromDomain := args[0]

	fromTokenBridge, err := hex.DecodeString(string(args[1]))
	if err != nil {
		return shim.Error(fmt.Sprintf("failed to decode from tb: %v", err))
	}

	// 跨链合约直接调用，不需要hex
	packedMsg := args[2]
	//packedMsg, err := hex.DecodeString(string(args[2]))
	//if err != nil {
	//	return shim.Error(fmt.Sprintf("failed to decode packed msg: %v", err))
	//}
	crosschainMsg, err := utils.UnpackCrossChainMsg(packedMsg)
	if err != nil {
		return shim.Error(fmt.Sprintf("failed to unpack crosschain msg: %v, rawPackedMsg: %s", err, args[2]))
	}

	return tb._recvUnorderedMessage(stub, fromDomain, fromTokenBridge, crosschainMsg)
}

func (tb *TokenBridge) _recvUnorderedMessage(stub shim.ChaincodeStubInterface, fromDomain, fromContract []byte, msg *utils.CrossChainMsg) peer.Response {
	// msg.DstAssetContract是链码资产合约名称的hashhex，需要转为链码名称
	dstAssetCodeNameHash := msg.DstAssetContract[:]                                // route-table 组合键值的一部分, asset-lock的键值
	dstAssetCodeName, err := stub.GetState(chaincodeNameKey(dstAssetCodeNameHash)) //便于查看，传递给资产合约
	if err != nil {
		return shim.Error(fmt.Sprintf("failed to get dst asset code name by hashhex(%s): %v", msg.DstAssetContract, err))
	}

	switch msg.Status {
	case uint8(utils.CROSSCHAIN_SUCCESS):
		// 1 如果跨链状态为成功，应该是来源链收到跨链回执，直接抛出跨链成功事件即可结束
		fallthrough
	case uint8(utils.CROSSCHAIN_ERROR): // todo: 是不是应该包含crosschain error?
		// 1.1 抛出跨链事件
		if err := tb._emitCrosschainEvent(stub, &CrossChainEvent{
			Domain:           fromDomain,
			SrcContract:      msg.SrcAssetContract[:],
			DestContract:     dstAssetCodeName,
			Ids:              msg.Ids,
			Amounts:          msg.Amounts,
			Holder:           msg.Holder[:],
			CrossChainStatus: msg.Status,
		}); err != nil {
			return shim.Error(fmt.Sprintf("failed to emit crosschain event: %v", err))
		}

		// 1.2 跨链结束
		return shim.Success(nil)

	case uint8(utils.CROSSCHAIN_START):
		// 2 如果跨链状态是start，应该是目的链收到跨链请求，需要先解锁或铸造资产，然后给来源链回复跨链回执
		// 2.1 获取并验证来源链的TB合约存在 (正常情况下参数fromContract其实就是fromTB)
		rawSrcTokenBridge, err := stub.GetState(tokenBridgeKey(fromDomain))
		if err != nil {
			return shim.Error(fmt.Sprintf("failed to get from chain token bridges: %v", err))
		}
		if utils.IsZeroBytes(rawSrcTokenBridge) {
			return shim.Error(fmt.Sprintf("unknow token bridge for domain:%s", fromDomain))
		}

		// 2.2 获取并验证路由表中来源链的资产合约存在
		rawFromAssetContract, err := stub.GetState(routeTableKey(dstAssetCodeNameHash, fromDomain))
		if err != nil {
			return shim.Error(fmt.Sprintf("failed to get from asset contract: %v", err))
		}
		if utils.IsZeroBytes(rawFromAssetContract) {
			return shim.Error(fmt.Sprintf("unknow from asset contract(K:%s-%s)", dstAssetCodeName, fromDomain))
		}

		// 2.3 解锁或铸造资产
		if err := tb._mintOrUnlock(stub, msg.Ids, msg.Amounts, dstAssetCodeName, msg.Holder[:]); err != nil {
			return shim.Error(fmt.Sprintf("failed to mint or unlock asset: %v", err))
		}

		// todo: 链码暂不支持回调
		//// 2.4 发送跨链回执
		//srcTokenBridge := []byte(hex.EncodeToString(rawSrcTokenBridge))
		//// 2.4.1 获取跨链系统合约地址
		//ibcMsgAddr, err := stub.GetState(IBC_MSG_ADDR)
		//if err != nil {
		//	return shim.Error(fmt.Sprintf("failed to get ibc msg address: %v", err))
		//}
		//// 2.4.2 编码跨链回执消息
		//packedData, err := utils.PackCrossChainMsg(msg.Ids, msg.Amounts, msg.SrcAssetContract, msg.DstAssetContract, msg.Holder, uint8(utils.CROSSCHAIN_SUCCESS))
		//if err != nil {
		//	return shim.Error(fmt.Sprintf("failed to pack cross chain data: %v", err))
		//}
		//// 2.4.3 调用跨链系统合约发送跨链回执消息
		//fmt.Printf("invoke sendUnorderedMessage to send receipt, fromDomain: %s, srcTokenBridge: %s\n", fromDomain, srcTokenBridge)
		//result := stub.InvokeChaincode(
		//	string(ibcMsgAddr),
		//	[][]byte{
		//		[]byte("sendUnorderedMessage"),
		//		fromDomain,
		//		srcTokenBridge,
		//		packedData,
		//	},
		//	stub.GetChannelID())
		//if result.Status != shim.OK {
		//	return shim.Error(fmt.Sprintf("failed to invoke chaincode %s to send cross receipt: %s", string(ibcMsgAddr), result.String()))
		//}
	}

	return shim.Success(nil)
}

func (tb *TokenBridge) _mintOrUnlock(stub shim.ChaincodeStubInterface, ids, amts []*big.Int, dstChaincodeName, rawHolder []byte) error {
	mintIds := make([]*big.Int, len(ids))
	mintAmts := make([]*big.Int, len(amts))
	transferIds := make([]*big.Int, len(ids))
	transferAmts := make([]*big.Int, len(amts))

	mint := false
	transfer := false

	// 1. 获取获取解锁及铸造资产的情况
	for i := 0; i < len(ids); i++ {
		mintIds[i] = ids[i]
		transferIds[i] = ids[i]

		// 获取锁定资产
		rawAssetLock, err := stub.GetState(assetLockKey(utils.GetChaincodeNameHash(dstChaincodeName), ids[i]))
		if err != nil {
			return fmt.Errorf("failed To get asset-lock record: %v", err)
		}
		assetLock := big.NewInt(0)
		if !utils.IsZeroBytes(rawAssetLock) {
			assetLock = big.NewInt(0).SetBytes(rawAssetLock)
		}

		// 获取解锁及铸造资产的情况
		if assetLock.Cmp(amts[i]) >= 0 {
			assetLock = big.NewInt(0).Sub(assetLock, amts[i])
			transferAmts[i] = amts[i]
			transfer = true
		} else {
			// mint assets if the locked assets are insufficient， only in cross chain
			transferAmts[i] = assetLock
			if transferAmts[i].Cmp(big.NewInt(0)) == 1 {
				transfer = true
			}
			mintAmts[i] = big.NewInt(0).Sub(amts[i], assetLock)
			mint = true
		}

		// 更新锁定资产
		fmt.Printf("put assetLock: %v\n", assetLock)
		if err := stub.PutState(assetLockKey(utils.GetChaincodeNameHash(dstChaincodeName), ids[i]), assetLock.Bytes()); err != nil {
			return fmt.Errorf("failed to update asset lock record: %v", err)
		}
	}

	// 2 铸造锚定资产
	if mint {
		fmt.Printf("mint: invoke mintBatchByTB, holder: %s, ids: %s, amts: %s\n", hex.EncodeToString(rawHolder), utils.ConverBigintListToString(mintIds), utils.ConverBigintListToString(mintAmts))
		result := stub.InvokeChaincode(
			string(dstChaincodeName),
			[][]byte{
				[]byte("mintBatchByTB"),
				[]byte(hex.EncodeToString(rawHolder)),
				[]byte(utils.ConverBigintListToString(mintIds)),
				[]byte(utils.ConverBigintListToString(mintAmts)),
			},
			stub.GetChannelID())
		if result.Status != shim.OK {
			return fmt.Errorf("failed to invoke chaincode %s mint: %s", string(dstChaincodeName), result.String())
		}
	}

	// 3 解锁资产，将资产从tb合约转移到目的账户
	if transfer {
		fmt.Printf("unlock: invoke safeBatchTransferFrom, holder: %s, ids: %s, amts: %s\n", hex.EncodeToString(rawHolder), utils.ConverBigintListToString(transferIds), utils.ConverBigintListToString(transferAmts))
		rawCurChaincodeName, _ := stub.GetState(ChAINCODE_NAME)

		result := stub.InvokeChaincode(
			string(dstChaincodeName),
			[][]byte{
				[]byte("safeBatchTransferFrom"),
				rawCurChaincodeName,
				[]byte(hex.EncodeToString(rawHolder)),
				[]byte(utils.ConverBigintListToString(transferIds)),
				[]byte(utils.ConverBigintListToString(transferAmts)),
				nil, // to地址为合约时需要根据data进行跨合约调用，这里to地址是账户，data可以为nil
			},
			stub.GetChannelID())
		if result.Status != shim.OK {
			return fmt.Errorf("failed to invoke chaincode %s transfer: %s", string(dstChaincodeName), result.String())
		}
	}

	return nil
}

func (tb *TokenBridge) setIbcMsgAddress(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	// todo: 权限管理
	fmt.Printf("setIbcMsgAddress: %s\n", args[0])

	if len(args) != 1 {
		return shim.Error("args number should be 1")
	}

	// fabric链码合约，不需要hex
	ibcMsgAddr := args[0]
	if err := stub.PutState(IBC_MSG_ADDR, ibcMsgAddr); err != nil {
		return shim.Error(fmt.Sprintf("failed To update ibc msg addr: %v", err))
	}

	return shim.Success(nil)
}

func (tb *TokenBridge) setDomainTokenBridgeAddress(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	// todo: 权限管理

	fmt.Printf("setDomainTokenBridgeAddress, domain %s -> tb %s\n", args[0], args[1])

	if len(args) != 2 {
		return shim.Error("args number should be 2")
	}

	domain := args[0]
	tbAddr, err := hex.DecodeString(string(args[1]))
	if err != nil {
		tbAddr = args[0]
		//return shim.Error(fmt.Sprintf("failed to decode hex from tb addr: %v", err))
	}

	if err := stub.PutState(tokenBridgeKey(domain), tbAddr); err != nil {
		return shim.Error(fmt.Sprintf("failed To update token bridges: %v", err))
	}

	return shim.Success(nil)
}

func (tb *TokenBridge) registerRouter(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	// todo: 权限管理

	fmt.Printf("registerRouter, srcContractName: %s, dstDomain: %s, dstContract: %s\n", args[0], args[1], args[2])

	if len(args) != 3 {
		return shim.Error("args number should be 3")
	}

	// 来源合约是fabric链码的资产合约，不需要hex
	srcContractName := args[0]
	srcContractHash := utils.GetChaincodeNameHash(srcContractName)

	dstDomain := args[1]

	// 默认目的合约不是链码，需要hex
	dstContract, err := hex.DecodeString(string(args[2]))
	if err != nil {
		return shim.Error(fmt.Sprintf("failed to decode hex from dst asset contract addr: %v", err))
	}

	// 用hash注册route
	if err := stub.PutState(routeTableKey(srcContractHash, dstDomain), dstContract); err != nil {
		return shim.Error(fmt.Sprintf("failed To update route table: %v", err))
	}

	// 记录来源合约code-hash-map
	if err := stub.PutState(chaincodeNameKey(srcContractHash), srcContractName); err != nil {
		return shim.Error(fmt.Sprintf("failed to update chaincode name map: %v", err))
	}

	return shim.Success(nil)
}

func (tb *TokenBridge) deregisterRouter(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	//TODO implement me
	panic("implement me")
}

func (tb *TokenBridge) batchLock(stub shim.ChaincodeStubInterface) peer.Response {
	//TODO implement me
	panic("implement me")
}

func (tb *TokenBridge) batchUnlock(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	//TODO implement me
	panic("implement me")
}

// =================================================
func tokenBridgeKey(dstDomain []byte) string {
	return fmt.Sprintf(TOKEN_BRIDGE, string(dstDomain))
}

func routeTableKey(srcContract []byte, dstDomain []byte) string {
	return fmt.Sprintf(ROUTE_TABLE, hex.EncodeToString(srcContract), string(dstDomain))
}

func assetLockKey(assetContract []byte, assetId *big.Int) string {
	return fmt.Sprintf(ASSET_LOCK_RECORD, hex.EncodeToString(assetContract), assetId.String())
}

func chaincodeNameKey(chaincodeHashHex []byte) string {
	return fmt.Sprintf(CODE_NAME_MAP, hex.EncodeToString(chaincodeHashHex))
}

// test contract ============================
func (tb *TokenBridge) testRecvUnorderedMessage_CrossReq(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	// fromDomain, fromTBContract, (Ids, Amounts, SrcAssetContract, DstAssetCodeName,Holder)
	if len(args) != 7 {
		return shim.Error("args number should be 7")
	}

	rawSrcAssetContract, _ := hex.DecodeString(string(args[4]))
	dstAssetCodeNameHash := utils.GetChaincodeNameHash(args[5])
	rawHolder, _ := hex.DecodeString(string(args[6]))

	crosschainReq, err := utils.PackCrossChainMsg(
		utils.ConverStringToBigintList(string(args[2])),
		utils.ConverStringToBigintList(string(args[3])),
		utils.CopySliceToByte32(rawSrcAssetContract),
		utils.CopySliceToByte32(dstAssetCodeNameHash),
		utils.CopySliceToByte32(rawHolder),
		uint8(utils.CROSSCHAIN_START),
	)
	if err != nil {
		return shim.Error(fmt.Sprintf("failed to pack cross chain request msg: %v", err))
	}

	newArgs := [][]byte{args[0], args[1], crosschainReq}
	return tb.recvUnorderedMessage(stub, newArgs)
}

func (tb *TokenBridge) testRecvUnorderedMessage_CrossResp(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	// fromDomain, fromTBContract, (Ids, Amounts, SrcAssetContract, DstAssetCodeName,Holder)
	if len(args) != 7 {
		return shim.Error("args number should be 7")
	}

	rawSrcAssetContract, _ := hex.DecodeString(string(args[4]))
	dstAssetCodeNameHash := utils.GetChaincodeNameHash(args[5])
	rawHolder, _ := hex.DecodeString(string(args[6]))

	crosschainReq, err := utils.PackCrossChainMsg(
		utils.ConverStringToBigintList(string(args[2])),
		utils.ConverStringToBigintList(string(args[3])),
		utils.CopySliceToByte32(rawSrcAssetContract),
		utils.CopySliceToByte32(dstAssetCodeNameHash),
		utils.CopySliceToByte32(rawHolder),
		uint8(utils.CROSSCHAIN_SUCCESS),
	)
	if err != nil {
		return shim.Error(fmt.Sprintf("failed to pack cross chain request msg: %v", err))
	}

	newArgs := [][]byte{args[0], args[1], crosschainReq}
	return tb.recvUnorderedMessage(stub, newArgs)
}
