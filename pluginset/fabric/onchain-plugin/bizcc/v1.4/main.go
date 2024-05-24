package main

import (
	"fmt"
	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

// 实例化合约
func main() {
	if err := shim.Start(NewCrossChainTest()); err != nil {
		fmt.Printf("Error starting Biz chaincode: %s", err)
	}
}

const (
	PREFIX             = "bizcc_"
	LASTMSG            = PREFIX + "last_msg"
	LAST_UNORDERED_MSG = PREFIX + "last_unordered_msg"
)

// 跨链合约数据结构
type CrossChainTest struct {
}

// 构造胡跨链合约
func NewCrossChainTest() *CrossChainTest {
	return &CrossChainTest{}
}

// 初始化Init函数
func (bs *CrossChainTest) Init(stub shim.ChaincodeStubInterface) pb.Response {
	return shim.Success([]byte("Init success"))
}

/*
 * 合约调用
 */
func (bs *CrossChainTest) Invoke(stub shim.ChaincodeStubInterface) pb.Response {
	fn, args := stub.GetFunctionAndParameters()
	fmt.Println("CrossChainTest Invoked func ", fn)
	fmt.Println("CrossChainTest Invoked args ", args)

	switch fn {

	// 用户自定义方法
	case "testSendMessage":
		// 对外发送的消息
		// args[0]: crosscc名字
		// args[1]: 目的区块链域名
		// args[2]: 接收者身份
		//          如果目的区块链是蚂蚁区块链，账号为账号地址(32字节)hex字符串，不要加0x开头
		//          如果目的区块链时Fabric，账号为接收消息的链码名字进行sha256后哈希值的hex字符串
		// args[3]: 发送消息内容
		// args[4]: 发送消息内容nounce
		if len(args) != 5 {
			fmt.Println("Unexpected args len")
			return shim.Error("Unexpected args len")
		}
		fmt.Printf("CrossChainTest send message to %s::%s, content is %s\n", args[0], args[1], args[2])

		// 调用跨链utitlity合约示例
		var (
			cc = args[0] // 跨链utility链码名字
		)
		var args_cross = [][]byte{
			[]byte("sendMessage"), // 发送跨链消息
			[]byte(args[1]),       // 目标区块链域名
			[]byte(args[2]),       // 接收消息的mychain客户合约地址
			[]byte(args[3]),       // 发送的消息
			[]byte(args[4]),       // 发送的消息nounce
		}
		re := stub.InvokeChaincode(cc, args_cross, stub.GetChannelID())

		// 检查跨链utitlity链码返回值

		return re

		// 用户自定义方法
	case "testSendUnorderedMessage":
		fmt.Printf("CrossChainTest send message to %s::%s, content is %s\n", args[0], args[1], args[2])
		// 对外发送的消息
		// args[0]: crosscc名字
		// args[1]: 目的区块链域名
		// args[2]: 接收者身份
		//          如果目的区块链是蚂蚁区块链，账号为账号地址(32字节)hex字符串，不要加0x开头
		//          如果目的区块链时Fabric，账号为接收消息的链码名字进行sha256后哈希值的hex字符串
		// args[3]: 发送消息内容
		// args[4]: 发送消息内容nounce
		if len(args) != 5 {
			fmt.Println("Unexpected args len")
			return shim.Error("Unexpected args len")
		}
		fmt.Printf("CrossChainTest send message to %s::%s, content is %s\n", args[0], args[1], args[2])

		// 调用跨链utitlity合约示例
		var (
			cc = args[0] // 跨链utility链码名字
		)
		var args_cross = [][]byte{
			[]byte("sendUnorderedMessage"), // 发送跨链消息
			[]byte(args[1]),                // 目标区块链域名
			[]byte(args[2]),                // 接收消息的mychain客户合约地址
			[]byte(args[3]),                // 发送的消息
			[]byte(args[4]),                // 发送的消息nounce
		}
		re := stub.InvokeChaincode(cc, args_cross, stub.GetChannelID())

		// 检查跨链utitlity链码返回值

		return re

	case "testSendUnorderedMessageMulti":
		fmt.Printf("CrossChainTest send message to %s::%s, content is %s\n", args[0], args[1], args[2])
		// 对外发送的消息
		// args[0]: crosscc名字
		// args[1]: 目的区块链域名
		// args[2]: 接收者身份
		//          如果目的区块链是蚂蚁区块链，账号为账号地址(32字节)hex字符串，不要加0x开头
		//          如果目的区块链时Fabric，账号为接收消息的链码名字进行sha256后哈希值的hex字符串
		// args[3]: 发送消息内容
		// args[4]: 发送消息内容nounce
		if len(args) != 5 {
			fmt.Println("Unexpected args len")
			return shim.Error("Unexpected args len")
		}
		fmt.Printf("CrossChainTest send message to %s::%s, content is %s\n", args[0], args[1], args[2])

		// 调用跨链utitlity合约示例
		var (
			cc = args[0] // 跨链utility链码名字
		)
		var args_cross = [][]byte{
			[]byte("sendUnorderedMessage"), // 发送跨链消息
			[]byte(args[1]),                // 目标区块链域名
			[]byte(args[2]),                // 接收消息的mychain客户合约地址
			[]byte(args[3]),                // 发送的消息
			[]byte(args[4]),                // 发送的消息nounce
		}

		var re pb.Response
		for i := 0; i < 20; i++ {
			args_cross[4] = []byte(args[4] + "-" + string(i))
			re = stub.InvokeChaincode(cc, args_cross, stub.GetChannelID())
		}

		// 检查跨链utitlity链码返回值

		return re

		// 用户自定义方法
	case "testSendBatchUnorderedMessage":
		fmt.Printf("CrossChainTest send message to %s::%s, content is %s\n", args[0], args[1], args[2])
		// 对外发送的消息
		// args[0]: crosscc名字
		// args[1]: 目的区块链域名
		// args[2]: 接收者身份
		//          如果目的区块链是蚂蚁区块链，账号为账号地址(32字节)hex字符串，不要加0x开头
		//          如果目的区块链时Fabric，账号为接收消息的链码名字进行sha256后哈希值的hex字符串
		// args[3]: 发送消息内容
		if len(args) < 4 {
			fmt.Println("Unexpected args len")
			return shim.Error("Unexpected args len")
		}
		fmt.Printf("CrossChainTest send message to %s::%s, content is %s\n", args[0], args[1], args[2])

		// 调用跨链utitlity合约示例
		var (
			cc = args[0] // 跨链utility链码名字
		)
		var args_cross = [][]byte{
			[]byte("batchSendUnorderedMessage"), // 发送跨链消息
			[]byte(args[1]),                     // 目标区块链域名
			[]byte(args[2]),                     // 接收消息的mychain客户合约地址
			[]byte(args[3]),                     // 发送的消息
		}
		for i := 4; i < len(args); i++ {
			args_cross = append(args_cross, []byte(args[i]))
		}

		re := stub.InvokeChaincode(cc, args_cross, stub.GetChannelID())

		// 检查跨链utitlity链码返回值

		return re

	//客户合约实现接收有序消息接口
	case "recvMessage": // 接收消息
		return bs.recvMessage(stub, args[0], args[1], args[2])

	//客户合约实现接收有序消息接口
	case "recvUnorderedMessage": // 接收消息
		return bs.recvUnorderedMessage(stub, args[0], args[1], args[2])

	case "getLastMsg":
		msg, _ := stub.GetState(LASTMSG)
		return shim.Success(msg)

	case "getLastUnorderedMsg":
		msg, _ := stub.GetState(LAST_UNORDERED_MSG)
		return shim.Success(msg)

	default:
		return shim.Error("Method not found")
	}
}

//客户合约必须实现接口
func (bs *CrossChainTest) recvMessage(stub shim.ChaincodeStubInterface, sourceDomain string, sourceIdentity string, message string) pb.Response {
	//  sourceDomain stirng,   // 消息来源区块链的域名
	//  sourceIdentity string, // 消息发送者身份
	//  message string)        // 消息内容
	//  补充具体实现
	fmt.Printf("CrossChainTest recv message from domain:%s, identity:%s, msg:%s\n", sourceDomain, sourceIdentity, message)

	stub.PutState(LASTMSG, []byte(sourceDomain+"::"+sourceIdentity+":"+message))
	return shim.Success(nil)
}

//客户合约必须实现接口
func (bs *CrossChainTest) recvUnorderedMessage(stub shim.ChaincodeStubInterface, sourceDomain string, sourceIdentity string, message string) pb.Response {
	//  sourceDomain stirng,   // 消息来源区块链的域名
	//  sourceIdentity string, // 消息发送者身份
	//  message string)        // 消息内容
	//  补充具体实现
	fmt.Printf("CrossChainTest recv message from domain:%s, identity:%s, msg:%s\n", sourceDomain, sourceIdentity, message)

	stub.PutState(LAST_UNORDERED_MSG, []byte(sourceDomain+"::"+sourceIdentity+":"+message))
	return shim.Success(nil)
}
