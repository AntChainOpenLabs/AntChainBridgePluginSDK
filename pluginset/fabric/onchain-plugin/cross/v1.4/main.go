package main

import (
	"encoding/hex"
	"encoding/json"
	"fmt"
	"github.com/hyperledger/fabric/core/chaincode/shim"
	"github.com/hyperledger/fabric/core/chaincode/shim/shimtest"
	pb "github.com/hyperledger/fabric/protos/peer"
	"oraclelogic"
	"strconv"
	"wrapstub"
)

// 实例化合约
func main() {
	if err := shim.Start(NewCrossChain()); err != nil {
		fmt.Printf("Error starting Biz chaincode: %s", err)
	}
}

// 跨链合约数据结构
type CrossChain struct {
	// 需要包含oraclelogic和amlogic的两个对象
	Os *oraclelogic.OracleService
}

// 构造胡跨链合约
func NewCrossChain() *CrossChain {
	return &CrossChain{
		// 初始化oraclelogic/amlogic对象
		Os: oraclelogic.NewOracleLogic(),
	}
}

// 初始化Init函数
// 建议在初始化跨链合约时设置oraclelogic的管理员证书
func (bs *CrossChain) Init(stub shim.ChaincodeStubInterface) pb.Response {
	return shim.Success([]byte("Init success"))
}

/*
 * 合约调用
 */
func (bs *CrossChain) Invoke(originStub shim.ChaincodeStubInterface) pb.Response {
	fn, args := originStub.GetFunctionAndParameters()
	fmt.Println("CrossChain Invoked func ", fn)
	var stub shim.ChaincodeStubInterface
	cstub, ok := (originStub).(*shim.ChaincodeStub)
	if ok {
		stub = wrapstub.NewWrapStub(cstub)
	}
	mstub, ok2 := (originStub).(*shimtest.MockStub)
	if ok2 {
		stub = wrapstub.NewMockWrapStub(mstub)
	}

	switch fn {

	// 初始化函数，目前暂无用途
	case "Init":
		return bs.Init(stub)

	// 检查是否未设置admin
	case "hasNotSetAdmin":
		return bs.Os.HasNotSetAdmin(stub)

	// 跨链服务设置管理员权限
	// @param 跨链服务账号证书(必选)，x509公钥证书
	case "setAdmin":
		// 配置admin
		if ret := bs.Os.SetAdmin(stub, []byte(args[0])); ret.Status != shim.OK {
			fmt.Printf("Set Oracle Admin failed %s\n", ret.Message)
			return ret
		}
		return shim.Success(nil)

	// 设置domain parser。
	// parser应该是product的枚举值，比如fabric_14，不同parser对应于不同的函数。
	case "setDomainParser":
		if ret := bs.Os.AdminManage(stub, "checkAdmin", []string{}); ret.Status != shim.OK {
			return shim.Error("[setDomainParser] " + ret.Message)
		}
		return bs.setDomainParser(stub, args)

	// 客户链码 invoke 跨链链码发送「有序」消息
	// args[0] 目的地的域名(必选)
	// args[1] 目的地账号(必选)，byte32 hexstring
	// args[2] 消息内容(必选), string
	// args[3] 消息nounce(可选)，区分同一笔交易内发送多个消息, string
	case "sendMessage":
		re := bs.sendMessage(stub, args, oraclelogic.K_MSG_TYPE_ORDERED)
		if re.Status != shim.OK {
			return shim.Error("[sendMessage] " + re.Message)
		}
		return re

	// 客户链码 invoke 跨链链码发送「无序」消息
	// args[0] 目的地的域名(必选)
	// args[1] 目的地账号(必选)，byte32 hexstring
	// args[2] 消息内容(必选), string
	// args[3] 消息nounce(可选)，区分同一笔交易内发送多个消息, string
	case "sendUnorderedMessage":
		re := bs.sendMessage(stub, args, oraclelogic.K_MSG_TYPE_UNORDERED)
		if re.Status != shim.OK {
			return shim.Error("[sendUnorderedMessage] " + re.Message)
		}
		return re

	// 客户链码 invoke 跨链链码发送「无序」消息
	// args[0] 目的地的域名(必选)
	// args[1] 目的地账号(必选)，byte32 hexstring
	// args[2..] 消息内容(必选), string，N个消息就N个参数
	case "batchSendUnorderedMessage":
		if len(args) < 3 {
			fmt.Println("Unexpected args len")
			return shim.Error("Unexpected args len")
		}
		for i := 2; i < len(args); i++ {
			nounce := "nounce" + strconv.Itoa(i)
			newargs := []string{args[0], args[1], args[i], nounce}
			re := bs.sendMessage(stub, newargs, oraclelogic.K_MSG_TYPE_UNORDERED)
			if re.Status != shim.OK {
				return shim.Error("[batchSendUnorderedMessage] " + re.Message)
			}
		}
		return shim.Success([]byte("success"))

	// 跨链服务上传跨链消息的接口
	case "recvMessage":
		if ret := bs.Os.AdminManage(stub, "checkAdmin", []string{}); ret.Status != shim.OK {
			return shim.Error("[recvMessage] " + ret.Message)
		}
		return bs.recvMessage(stub, args)

	// 跨链服务管理接口
	case "oracleAdminManage":
		fmt.Printf("go to oracleAdminManage\n")
		ret := bs.Os.AdminManage(stub, args[0], args[1:])
		return ret

	//////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////

	// 测试回调biz链码
	case "testCallbackBizChaincode":
		if ret := bs.Os.AdminManage(stub, "checkAdmin", []string{}); ret.Status != shim.OK {
			return ret
		}
		return bs.callbackBizChaincode(stub, []byte(args[0]))

	default:
		return shim.Error("Method not found")
	}
}

func (bs *CrossChain) setDomainParser(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 2 {
		return shim.Error(fmt.Sprintf("Wrong length of args: %v", len(args)))
	}

	senderDomain := args[0]
	parser := args[1]

	if senderDomain == "" || parser == "" {
		return shim.Error(fmt.Sprintf("Wrong args: %s, %s", senderDomain, parser))
	}

	if err := bs.Os.PutState(stub, false, fmt.Sprintf("%s_%s", oraclelogic.KMychainParserInfo, senderDomain), []byte(parser)); err != nil {
		return shim.Error(fmt.Sprintf("failed to put parser: %v", err))
	}

	return shim.Success(nil)
}

// 用户发送消息示例
func (bs *CrossChain) sendMessage(stub shim.ChaincodeStubInterface, args []string, msgType string) pb.Response {
	/**************************/
	/*      USER DEFINE       */
	/**************************/
	// 构造对外发送的消息，准备目的域名、接收账号、消息内容, nounce
	if len(args) != 4 && len(args) != 3 {
		fmt.Println("Unexpected args len")
		return shim.Error(fmt.Sprintf("Unexpected args len: %d", len(args)))
	}
	var (
		destDomain    = args[0]
		receiver, err = hex.DecodeString(args[1])
		msg           = []byte(args[2])
	)

	if err != nil {
		return shim.Error(fmt.Sprintf("receiver(%s) format error: %v", args[1], err))
	}

	if len(msg) > oraclelogic.K_SEND_MESSAGE_LENGTH_LIMIT {
		return shim.Error(fmt.Sprintf(" message exceed length limit (%d)", len(msg)))
	}

	msgnounce := ""
	if len(args) == 4 {
		msgnounce = args[3]
	}

	collection := "" // 非隐私消息，应该使用空字符串

	/**************************/
	/*      DONOT MODIFY      */
	/**************************/
	// 调用oraclelogic发送消息
	res := bs.Os.SendMessage(stub, destDomain, receiver, msg, msgnounce, collection, msgType)
	if res.Status != shim.OK {
		fmt.Printf("Orale SendMessage failed, message:%s\n", res.Message)
		return res
	}

	fmt.Printf("sendMessage success\n")
	return shim.Success(nil)
}

func (bs *CrossChain) recvMessage(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	/**************************/
	/*  DONOT MODIFY (BEGIN)  */
	/**************************/
	// 兼容隐私消息(指定collection)和非隐私消息
	// 非隐私消息：
	//           args[0]: oracle service id
	//           args[1]: 提交的TEE验证过的原始信息
	//
	// 隐私消息：
	//           args只传oracle service id一个参数，其他参数通过transient map传递
	if len(args) == 1 {
		// args[0] : oralce service id
		// 从transient中获取机密信息
		trans, _ := stub.GetTransient()
		rawdata := trans["rawdata"]
		args = []string{args[0], string(rawdata)}
		fmt.Printf("recvMessage with transient data : %s ", rawdata)
	}

	//  调用跨链接收消息接口     */
	// recvmsg: 消息处理的结果状态，还有解析收到的报文
	// eventmsg: 通知Oracle链下服务回执，表示交易已经执行成功
	recvmsg := bs.Os.RecvBatchMychainMessage(stub, args)
	if recvmsg.Status != shim.OK {
		// 返回错误信息
		return recvmsg
	}

	fmt.Printf("Crosschain recevive message:%s\n", recvmsg.Payload) // json结构，src domain/ src id/ msg

	return bs.callbackBizChaincode(stub, recvmsg.Payload)
}

func (bs *CrossChain) callbackBizChaincode(stub shim.ChaincodeStubInterface, messages []byte) pb.Response {
	// 解析收到的消息
	var msgs oraclelogic.RecvAuthMessages
	_ = json.Unmarshal(messages, &msgs)

	for i := 0; i < len(msgs.Message); i++ {
		msg := msgs.Message[i]
		cc_hash := msg.Receiver
		cc_name := bs.Os.QuerySha256Invert(stub, []string{hex.EncodeToString(cc_hash[:])})
		if cc_name.Status != shim.OK {
			return shim.Error(fmt.Sprintf("receiver chaincode(recHash: %s) not exist!", hex.EncodeToString(cc_hash[:])))
		}

		// 回调用户合约
		var (
			bizcc = string(cc_name.Payload) // 收到消息的链码
		)
		var cbFn string
		if msg.MsgType == oraclelogic.K_MSG_TYPE_ORDERED {
			cbFn = "recvMessage"
		} else if msg.MsgType == oraclelogic.K_MSG_TYPE_UNORDERED {
			cbFn = "recvUnorderedMessage"
		}

		var args_cb = [][]byte{
			[]byte(cbFn), // 接收消息的客户合约要实现一个接口
			//      recvMessage(
			//             sourceDomain stirng,   // 消息来源区块链的域名
			//             sourceIdentity string, // 消息发送者身份
			//             message string)        // 消息内容
			//      pb.Response                   // 回调用户连码返回值
			[]byte(msg.From), // source domain
			[]byte(hex.EncodeToString(msg.Identity[:])), // source identity  hex串
			[]byte(msg.Content),                         // message
		}
		re := stub.InvokeChaincode(bizcc, args_cb, stub.GetChannelID())
		if re.Status != shim.OK {
			fmt.Printf("call %s.%s failed: %s\n", bizcc, cbFn, re.Message)
			return shim.Error(fmt.Sprintf("recv message and callback chaincode %s failed", bizcc))
		}
		fmt.Printf("call %s.%s success: %s\n", bizcc, cbFn, re.Message)
	}
	return shim.Success([]byte("callback biz chaincode success"))
}
