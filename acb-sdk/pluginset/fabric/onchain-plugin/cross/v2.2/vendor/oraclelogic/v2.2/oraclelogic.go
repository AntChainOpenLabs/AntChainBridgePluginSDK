package oraclelogic

import (
	"chaincodepb"
	"crypto"
	"crypto/rsa"
	"crypto/sha256"
	"crypto/x509"
	"encoding/base64"
	"encoding/binary"
	"encoding/hex"
	"encoding/json"
	"encoding/pem"
	"errors"
	"fmt"
	"github.com/golang/protobuf/proto"
	"github.com/hyperledger/fabric-chaincode-go/pkg/cid"
	"github.com/hyperledger/fabric-chaincode-go/shim"
	comm "github.com/hyperledger/fabric-protos-go/common"
	pb "github.com/hyperledger/fabric-protos-go/peer"
	sysos "os"
	"strconv"
	"strings"
)

// -------------------------------------------- Oracle Service -------------------------------------------------
// OracleService implements Chaincode interface
type OracleService struct {
}

type RecvAuthMessage struct {
	From     string   `json:"From"`
	Identity [32]byte `json:"Identity"`
	Content  []byte   `json:"Content"`
	Receiver [32]byte `json:"Receiver"`
	MsgType  string   `json:"MsgType"`
}

type RecvAuthMessages struct {
	Message []RecvAuthMessage `json:"Message"`
}

// State keys
const (
	PREFIX = "oraclelogic_"

	// Value is mashalled bytes of map<string, SGXOracleCluster> of which is keyed by oracleBizId
	K_ORACLE_CLUSTERS = PREFIX + "oracle_clusters"

	K_PK_DOMAINS         = PREFIX + "pk_domains"
	K_DOMAIN_SERVICE_IDS = PREFIX + "domain_service_ids"
	K_ORACLE_EVENT       = PREFIX + "event"

	K_ADMIN_CERT = PREFIX + "admin_cert"

	// Value is mashalled bytes of `Counters`
	K_COUNTERS = PREFIX + "counters"

	// Value is marshalled bytes of `OracleNodePks`
	K_ORACLE_NODE_PKS = PREFIX + "oracle_node_pks"

	// Value is marshalled bytes of map<string, OracleService>
	K_ORACLE_SERVICE = PREFIX + "oracle_services"

	K_DEFAULT_CURL_ORACLE_SERVICE_ID = PREFIX + "default_curl_oracle_service_id"

	K_REQUESTS = PREFIX + "requests"

	K_REQUESTS_NUM = PREFIX + "requests_num"

	K_RESPONSES = PREFIX + "responses"

	K_EXPECTED_DOMAIN = PREFIX + "expected_domain"

	K_SOLIDITY_AMCLIENT_PREFIX = PREFIX + "amclient_solidity_addr_"

	K_WASM_AMCLIENT_PREFIX = PREFIX + "amclient_wasm_addr_"

	K_STATE_STABLIZED_SUFFIX = "<<<<<<"

	K_CROSSCHAIN_CCNAME = PREFIX + "crosschain_ccname"

	// 完整一条消息的key: oraclelogic_crosschain_msg_${txid}_${nounce}
	K_CROSSCHAIN_MSG_PREFIX = PREFIX + "crosschain_msg_"

	K_SHA256_INVERT_PREFIX = PREFIX + "sha256_invert_"

	K_RECV_MSGS_SUCCESS = PREFIX + "recv_messages_success"

	K_MSG_TYPE_ORDERED = PREFIX + "ordered_msg"

	K_MSG_TYPE_UNORDERED = PREFIX + "unordered_msg"

	K_UNORDERED_MSG_SEQ = 0xffffffff

	K_SEND_MESSAGE_LENGTH_LIMIT = 10000 // 发送消息单条消息长度限制10K

	DEBUG = false
)

// 构造跨链合约
func NewOracleLogic() *OracleService {
	return &OracleService{}
}

/*
 * 配置Oracle管理员
 * @certPEM: Oracle管理员账号的证书，Oracle管理员负责调用管理接口
 */
func (os *OracleService) SetAdmin(stub shim.ChaincodeStubInterface, certPEM []byte) pb.Response {
	fmt.Printf("set admin with pem %s\n", string(certPEM))

	// 检查pem格式，如果格式非法则panic
	getCertFromPEM(certPEM)

	cert, err := os.GetState(stub, true, K_ADMIN_CERT)
	if err != nil {
		return shimErr("admin not set yet")
	}

	if cert == nil {
		// 全新未配置
		err := os.PutState(stub, true, K_ADMIN_CERT, certPEM)
		if err != nil {
			return shimErr("set oracle service admin failed")
		}

		// 设置当前跨链链码名字
		// selfCCName := os.getSignedProposalChaincode(stub)
		// err = os.PutState(stub, false, K_CROSSCHAIN_CCNAME, []byte(selfCCName))
		//if err != nil {
		//	return shimErr("set oracle service cc name failed")
		//}
	} else {
		// 更新配置
		if err := os.checkAdmin(stub); (!DEBUG) && err.Status != shim.OK {
			fmt.Printf("OracleService::SetAdmin checkAdmin failed\n")
			return err
		}

		err := os.PutState(stub, true, K_ADMIN_CERT, certPEM)
		if err != nil {
			return shimErr("set oracle service admin failed")
		}
	}

	return shim.Success(nil)
}

func (os *OracleService) HasNotSetAdmin(stub shim.ChaincodeStubInterface) pb.Response {
	cert, err := os.GetState(stub, true, K_ADMIN_CERT)
	if err != nil {
		return shimErr("admin not set yet")
	}

	if cert == nil {
		return shim.Success([]byte("yes"))
	} else {
		return shimErr("oracle chaincode is set already")
	}
}

/*
 * oracle管理员管理配置接口
 */
func (os *OracleService) AdminManage(stub shim.ChaincodeStubInterface, fn string, args []string) pb.Response {
	fmt.Printf("enter oracleAdminManage\n")

	fmt.Printf("OracleService::AdminManage, call %s with args:\n", fn)

	for i, arg := range args {
		fmt.Printf("arg[%d]:%s\n", i, arg)
	}

	// check admin
	if err := os.checkAdmin(stub); (!DEBUG) && err.Status != shim.OK {
		fmt.Printf("OracleService::AdminManage checkAdmin failed\n")
		return err
	}
	fmt.Println("OracleService::AdminManage checkAdmin success\n")

	fmt.Printf("Begin to exec %s\n", fn)
	ret := pb.Response{}
	switch fn {

	case "batchDeployService":
		argslen := 9
		offset := 0
		argsAddSGXOracleCluster := args[offset : offset+argslen]
		if bret := os.addSGXOracleCluster(stub, argsAddSGXOracleCluster); bret.Status != shim.OK {
			fmt.Printf("addSGXOracleCluster failed with message:%s\n", bret.Message)
			ret = bret
			break
		}
		offset += argslen

		argslen = 5
		argsRegisterSGXOracleNode := args[offset : offset+argslen]
		if bret := os.registerSGXOracleNode(stub, argsRegisterSGXOracleNode); bret.Status != shim.OK {
			fmt.Printf("registerSGXOracleNode failed with message:%s\n", bret.Message)
			ret = bret
			break
		}
		offset += argslen

		argslen = 7
		argsAddOracleService := args[offset : offset+argslen]
		if bret := os.addOracleService(stub, argsAddOracleService); bret.Status != shim.OK {
			fmt.Printf("addOracleService failed with message:%s\n", bret.Message)
			ret = bret
			break
		}
		offset += argslen

		ret = shim.Success(nil)
		break

	// - Oracle cluster operations
	case "addSGXOracleCluster":
		ret = os.addSGXOracleCluster(stub, args)
		break
	case "hasOracle":
		ret = os.hasOracle(stub, args)
		break
	case "queryOracleBasicInfo":
		ret = os.queryOracleBasicInfo(stub, args)
		break

	// - Oracle node operations
	case "registerSGXOracleNode":
		ret = os.registerSGXOracleNode(stub, args)
		break
	case "hasOracleNode":
		ret = os.hasOracleNode(stub, args)
		break
	case "registerUDNSDomain":
		ret = os.registerUDNSDomain(stub, args)
		break
	case "hasUDNSDomain":
		ret = os.hasUDNSDomain(stub, args)
		break

	// - Oracle service operations
	case "addOracleService":
		ret = os.addOracleService(stub, args)
		break
	case "hasOracleService":
		ret = os.hasOracleService(stub, args)
		break
	case "getMyChainDomainAMClient":
		ret = os.getMyChainDomainAMClient(stub, args)
		break
	case "setMyChainDomainAMClient":
		ret = os.setMyChainDomainAMClient(stub, args)
		break

	case "queryRecvP2PMsgSeq":
		ret = os.queryRecvP2PMsgSeq(stub, args)
		break
	case "querySendP2PMsgSeq":
		ret = os.querySendP2PMsgSeq(stub, args)
		break
	case "setRecvP2PMsgSeq":
		ret = os.setRecvP2PMsgSeq(stub, args)
		break
	case "setSendP2PMsgSeq":
		ret = os.setSendP2PMsgSeq(stub, args)
		break

	case "rejectP2PMessage":
		ret = os.rejectP2PMessage(stub, args)
		break

	case "setExpectedDomain":
		if len(args) != 0 {
			ret = shimErr("setExpectedDomain need exactly 1 args")
		}
		os.PutState(stub, true, K_EXPECTED_DOMAIN, []byte(args[0]))
		ret = shim.Success([]byte("set expected domain success"))
		break

		//
	case "registerSha256Invert":
		fmt.Printf("registerSha256Invert for image:%s\n", args[0])
		image := args[0] // image 原像
		imgHash := sha256.Sum256([]byte(image))
		imgHashHex := hex.EncodeToString(imgHash[:])
		stub.PutState(imgHashHex, []byte(image))
		return shim.Success([]byte("register sha256 invert success"))

	// 预留给Oracle管理员的Invoke接口
	case "querySha256Invert":
		fmt.Printf("querySha256Invert for hex:%s\n", args[0])
		return os.QuerySha256Invert(stub, args)

	case "checkAdmin":
		// 单纯只是检查admin权限
		return shim.Success(nil)

	case "setDomainServiceId":
		ret = os.setDomainServiceId(stub, args)
		break

	case "getDomainServiceId":
		ret = os.getDomainServiceId(stub, args)
		break

	default:
		ret = shim.Error(fmt.Sprintf("Invalid fn %s for invoke", fn))
		break
	}
	fmt.Printf("Finish execing %s, ret Status:%d, Message:%s, Payload:%s\n", fn, ret.Status, ret.Message, ret.Payload)
	return ret
}

type RecvMsg struct {
	Message   []string `json:"Message"`
	SrcDomain string   `json:"SrcDomain"`
	From      string   `json:"From"`
}

func (os *OracleService) QuerySha256Invert(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	imgHashHex := args[0] // image 哈希
	col, err := stub.GetState(imgHashHex)

	if err != nil {
		fmt.Printf("image not exist\n")
		return shim.Error("image not exist")
	}

	if col == nil {
		fmt.Printf("image is null\n")
		return shim.Error("image is null")
	}

	fmt.Printf("image is %s\n", col)
	return shim.Success(col)
}

/*
 * oracle管理员提交收到的信息
 * 返回给crosschain合约使用
 */
func (os *OracleService) RecvBatchMychainMessage(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	// check admin
	{
		if err := os.checkAdmin(stub); (!DEBUG) && err.Status != shim.OK {
			fmt.Printf("OracleService::RecvBatchMychainMessage checkAdmin failed\n")
			return shimErr("Oracle checkAdmin failed")
		}

	}

	if len(args) != 2 {
		fmt.Printf("Unexpected args length %d\n", len(args))
		return shimErr(fmt.Sprintf("Unexpected args length %d", len(args)))
	}
	fmt.Printf("RecvBatchMychainMessage called with args:\nargs[0]:%s\nargs[1]:%s\n", args[0], args[1])
	serviceId := args[0]
	rawdata, err := hex.DecodeString(args[1])
	if err != nil {
		return shimErr(fmt.Sprintf("rawdata format error"))
	}

	pkglen := len(rawdata)
	offset := 0

	var msgs RecvAuthMessages

	//TOD: 只处理第一个消息
	for offset < pkglen {
		offset += 4
		hintlen := bytesToUint32(uint32(offset-1), rawdata)
		hint := make([]byte, hintlen)
		copyBytesWithLen(hint, rawdata, 0, uint64(offset), uint64(hintlen))
		offset += int(hintlen)

		offset += 4
		prooflen := bytesToUint32(uint32(offset-1), rawdata)
		proof := make([]byte, prooflen)
		copyBytesWithLen(proof, rawdata, 0, uint64(offset), uint64(prooflen))
		offset += int(prooflen)

		ret := os.recvMychainMessage(stub, []string{serviceId, string(proof), string(hint)})
		if ret.Status == shim.OK && ret.Payload != nil {
			var msg RecvAuthMessage
			err := json.Unmarshal(ret.Payload, &msg)
			if err == nil {
				messages := append(msgs.Message, msg)
				msgs.Message = messages
			}
		} else {
			return shimErr("Process AM message failed")
		}
	}

	msgsStr, _ := json.Marshal(msgs)

	// 记录到写集合中，表示
	// os.PutState(stub, false, K_RECV_MSGS_SUCCESS, []byte("yes"))

	return shim.Success([]byte(msgsStr))
}

/*
 * oracle管理员提交收到的信息
 * 返回给crosschain合约使用
 */
func (os *OracleService) recvMychainMessage(stub shim.ChaincodeStubInterface, args []string) (recvmsg pb.Response) {
	serviceId := args[0]
	rawdata := []byte(args[1])
	hints := args[2]

	resp := decodeResponse(rawdata)
	resp.ServiceId = serviceId
	if len(hints) > 0 {
		fmt.Printf("begin to verify resp\n")
		if !os.verifyResponse(stub, &resp) {
			fmt.Printf("verify resp failed\n")
			return shimErr("response verify failed")
		}
		fmt.Printf("verify resp success\n")

		domain := os.GetUDAGDomain(stub, &resp)
		fmt.Printf("GetUDAGDomain %s\n", domain)

		recvmsg = os.recvMychainRawData(stub, domain, resp.ResBody, hints)
	} else {
		recvmsg = os.recvMychainRawData(stub, resp.Domain, resp.ResBody, hints)
	}

	fmt.Printf("receive am message: status:%d, message:%s, payload: %s\n", recvmsg.Status, recvmsg.Message, recvmsg.Payload)

	return recvmsg
}

/*
 * 对外发送消息接口
 * @stub
 * @destdomain，目标区块链域名
 * @receiver, 接收消息的身份
 * @message, 发送消息内容
 * @msgnouce, 同一笔交易多次调用跨链链码发送消息，需要使用不同msgnounce区分
 * @collection, 指定collection，如果不指定传""空字符串，暂时不开放collection功能
 *
 * 返回值：pb.Response, Status为shim.OK代表成功，Message填失败的的提示，Payload填要写入event的内容
 */
func (os *OracleService) SendMessage(stub shim.ChaincodeStubInterface,
	destDomain string,
	receiver []byte,
	message []byte,
	msgnounce string,
	collection string,
	msgType string) pb.Response {
	return os.sendMessage(stub, destDomain, CopySliceToByte32(receiver), message, msgnounce, collection, msgType)
}

// *********************** 内部方法 ***********************
// *********************** 内部方法 ***********************
// *********************** 内部方法 ***********************
// *********************** 内部方法 ***********************
// *********************** 内部方法 ***********************

func (os *OracleService) checkAdmin(stub shim.ChaincodeStubInterface) pb.Response {
	cert, err := os.GetState(stub, true, K_ADMIN_CERT)
	if err != nil {
		return shimErr("admin not set yet")
	}

	fmt.Printf("checkAdmin: cert is %s\n", cert)

	cert_adim := getCertFromPEM(cert)
	cur_cert := os.getIdentityCert(stub)
	if cur_cert != nil && cert_adim.Equal(cur_cert) {
		return shim.Success(nil)
	}
	return shimErr("current user is not oracle service admin")
}

func getCertFromPEM(certPEM []byte) *x509.Certificate {
	block, _ := pem.Decode(certPEM)
	if block == nil {
		panic("failed to parse certificate PEM")
	}
	cert, err := x509.ParseCertificate(block.Bytes)
	if err != nil {
		panic("set oracle service admin failed")
	}
	return cert
}

func (os *OracleService) getIdentityCert(stub shim.ChaincodeStubInterface) *x509.Certificate {
	ci, err := cid.New(stub)
	if err != nil {
		return nil
	}
	// id, _ := ci.GetID()
	// fmt.Printf("getIdentity id %s\n", id)
	// mid, _ := ci.GetMSPID()
	// fmt.Printf("getIdentity mspid %s\n", mid)

	cert, err := ci.GetX509Certificate()
	if err != nil {
		return nil
	}
	// fmt.Printf("getIdentity cert %s\n", cert.Raw)

	return cert
}

// addSGXOracleCluster adds an oracle cluster in chaincode with args:
// - oracleBizId
// - oracleName
// - oracleDesc
// - iasDesc
// - iasPubKey
// - rootCA
// - mrEnclave
// - ifPswSupport
// - extInfoJson
func (os *OracleService) addSGXOracleCluster(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	fmt.Println("addSGXOracleCluster called")
	// sanity check
	if len(args) != 9 {
		return shimErr(fmt.Sprintf("Unexpected args length %d", len(args)))
	}
	var (
		oracleBizId = args[0]
		oracleName  = args[1]
		oracleDesc  = args[2]
		iasDesc     = args[3]
		iasPubKey   = args[4]
		rootCA      = args[5]
		mrEnclave   = args[6]
		pswSupport  = args[7]
		//extInfoJson = args[8]
	)
	fmt.Println("addSGXOracleCluster args check success")

	// TODO: sanity check for args empty value
	ifPswSupport, err := strconv.ParseBool(pswSupport)
	if err != nil {
		return shimErr("addSGXOracleCluster: invalid args")
	}

	fmt.Println("addSGXOracleCluster pswSupport:", ifPswSupport)

	oracleClusters, err := os.getStateOracleClusters(stub)
	if err != nil {
		return shimErr("addSGXOracleCluster: get oracle clusters state failed")
	}
	if oracleClusters == nil {
		oracleClusters = &chaincodepb.SGXOracleClusters{}
	}
	if oracleClusters.Clusters == nil {
		oracleClusters.Clusters = make(map[string]*chaincodepb.SGXOracleCluster)
	}
	//if _, has := oracleClusters.Clusters[oracleBizId]; has {
	//	fmt.Println("addSGXOracleCluster already has oracle with id:", oracleBizId)
	//	return shim.Success([]byte(fmt.Sprintf("addSGXOracleCluster: oracle cluster %s has already existed", oracleBizId)))
	//}
	fmt.Println("addSGXOracleCluster add a new oracle cluster")

	// Add a new oracle cluster
	var oracleCluster chaincodepb.SGXOracleCluster
	oracleCluster.OracleBasicInfo = &chaincodepb.OracleBasicInfo{
		// TODO: OracleHashId not set
		OracleBizId:  oracleBizId,
		OracleName:   oracleName,
		OracleDesc:   oracleDesc,
		TotalNodeNum: 0,
		IfExists:     true,
	}
	oracleCluster.SgxTrustRoot = &chaincodepb.SGXTrustRoot{
		IasDesc:      iasDesc,
		IasPubKey:    iasPubKey,
		RootCa:       rootCA,
		MrEnclave:    []byte(mrEnclave),
		IfPswSupport: ifPswSupport,
		IfExists:     true,
	}
	oracleClusters.Clusters[oracleBizId] = &oracleCluster

	fmt.Println("addSGXOracleCluster add new oracle cluster to state")

	if err := os.putStateOracleClusters(stub, oracleClusters); err != nil {
		return shimErr("addSGXOracleCluster: oracle clusters put state failed")
	}

	fmt.Println("addSGXOracleCluster add new oracle cluster to state success")

	return shim.Success(nil)
}

// - oracleBizId
type HasOracleRes struct {
	Result bool `json:"result"`
}

func (os *OracleService) hasOracle(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 1 {
		return shimErr("Unexpected args length")
	}
	oracleBizId := args[0]
	fmt.Println("query hasOracle with oracleBizId ", oracleBizId)

	oracleClusters, err := os.getStateOracleClusters(stub)
	if err != nil {
		return shimErr("hasOracle: get oracle clusters state failed")
	}

	fmt.Println("hasOracle cluster state get success")

	var hasOracleRes HasOracleRes // default result is false
	if oracleClusters != nil && oracleClusters.Clusters != nil {
		if _, has := oracleClusters.Clusters[oracleBizId]; has {
			hasOracleRes.Result = true
			fmt.Println("hasOracle cluster for ", oracleBizId, " true")
		}
	}

	bs, err := json.Marshal(hasOracleRes)
	if err != nil {
		return shimErr("hasOracle: json marshal failed")
	}
	fmt.Println("Oracle cluster for ", oracleBizId, " content: ", string(bs))
	return shim.Success(bs)
}

// TODO: implement
// - queryOracleIDList
// - queryOracleSGXRoot

// - oracleBizId
func (os *OracleService) queryOracleBasicInfo(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 1 {
		return shimErr("Unexpected args length")
	}
	oracleBizId := args[0]

	oracleClusters, err := os.getStateOracleClusters(stub)
	if err != nil || oracleClusters == nil {
		return shimErr("hasOracle: get oracle clusters state failed")
	}

	oracleCluster, has := oracleClusters.Clusters[oracleBizId]
	if !has {
		return shim.Success(nil)
	}
	bs, err := json.Marshal(oracleCluster.OracleBasicInfo)
	if err != nil {
		return shimErr("queryOracleBasicInfo: json marshal failed")
	}
	return shim.Success(bs)
}

// registerSGXOracleNode registers oracle node entity in chaincode with args:
// - oracleBizId
// - nodeBizId
// - nodeName
// - nodeDesc
// - AVR
func (os *OracleService) registerSGXOracleNode(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	fmt.Printf("registerSGXOracleNode called")
	if len(args) != 5 {
		return shimErr("Unexpected args length")
	}

	// TODO: sanity check args not empty
	var (
		oracleBizId = args[0]
		nodeBizId   = args[1]
		nodeName    = args[2]
		nodeDesc    = args[3]
		avr         = args[4]
	)
	oracleClusters, err := os.getStateOracleClusters(stub)
	if err != nil {
		return shimErr("registerSGXOracleNode: get state of oracle clusters failed")
	}
	oracleCluster, has := oracleClusters.Clusters[oracleBizId]
	if !has || oracleCluster == nil {
		return shimErr(fmt.Sprintf("registerSGXOracleNode: oracle cluster %s not exists", oracleBizId))
	}
	_, has = oracleCluster.OracleNodes[nodeBizId]
	//if has {
	//	return shim.Success([]byte(fmt.Sprintf("registerSGXOracleNode: oracle cluster %s - oracle node %s exists", oracleBizId, nodeBizId)))
	//}

	// New SGX oracle node
	newOracleNode := &chaincodepb.SGXOracleNode{
		OracleNodeBasicInfo: &chaincodepb.OracleNodeBasicInfo{
			NodeBizId: nodeBizId,
			NodeName:  nodeName,
			NodeDesc:  nodeDesc,
			// other values set below
		},
		SgxProof: nil, // set below
		UdnsInfo: nil, // set below
	}

	if err = os.parseAVRAndUpdateOracleNode(newOracleNode, avr); err != nil {
		return shimErr("registerSGXOracleNode: parse avr data failed")
	}
	if err = os.parseRADataAndUpdateOracleNode(newOracleNode); err != nil {
		return shimErr("registerSGXOracleNode: parse raData failed")
	}

	//return shim.Success(nil)

	// Verify oracle node
	iasPubKey := oracleCluster.SgxTrustRoot.IasPubKey
	mrEnclave := oracleCluster.SgxTrustRoot.MrEnclave
	isPswSupport := oracleCluster.SgxTrustRoot.IfPswSupport
	if !os.verifySGXOracleNode(stub, newOracleNode, iasPubKey, mrEnclave, isPswSupport) {
		return shimErr("registerSGXOracleNode: oracle node verify failed")
	}

	//// Saved oracle clusters
	oracleCluster.NodeBizIdList = append(oracleCluster.NodeBizIdList, newOracleNode.OracleNodeBasicInfo.NodeBizId)
	oracleCluster.OracleBasicInfo.TotalNodeNum += 1
	// TODO: add verify_timestamp
	if oracleCluster.OracleNodes == nil {
		oracleCluster.OracleNodes = make(map[string]*chaincodepb.SGXOracleNode)
	}
	oracleCluster.OracleNodes[nodeBizId] = newOracleNode

	if err := os.putStateOracleClusters(stub, oracleClusters); err != nil {
		return shimErr("registerSGXOracleNode: oracle clusters put state failed")
	}

	fmt.Println("registerSGXOracleNode success")

	return shim.Success(nil)
}

// AVR json schema
type AVR struct {
	Report                       string `json:"report"`
	XIasreportSignature          string `json:"xIasreportSignature"`
	XIasreportSigningCertificate string `json:"xIasreportSigningCertificate"`
	RaData                       string `json:"raData"`
}

// Parse avr json data and update sgx oracle node struct
// ATTENTION: this will modify the `oracleNode`
func (os *OracleService) parseAVRAndUpdateOracleNode(oracleNode *chaincodepb.SGXOracleNode, avr string) error {
	decodedAVR, err := base64.StdEncoding.DecodeString(avr)
	if err != nil {
		fmt.Printf("parse avr, decode base64 failed: %s\n", err)
		return err
	}

	avrJson := AVR{}
	err = json.Unmarshal(decodedAVR, &avrJson)
	if err != nil {
		fmt.Printf("parse avr, unmarshal json data failed: %s\n", err)
		return err
	}

	// sanity check for avrJson
	if avrJson.Report == "" ||
		avrJson.XIasreportSignature == "" ||
		avrJson.XIasreportSigningCertificate == "" ||
		avrJson.RaData == "" {
		fmt.Printf("parse avr, invalid avr data\n")
		return errors.New("invalid avr")
	}
	decodeRAData, err := base64.StdEncoding.DecodeString(avrJson.RaData)
	if err != nil {
		fmt.Printf("parse avr, decode base64 ra data failed: %s\n", err)
		return err
	}

	sgxProof := chaincodepb.SGXProof{
		AVR:          avrJson.Report,
		AVRSig:       avrJson.XIasreportSignature,
		IasCertChain: avrJson.XIasreportSigningCertificate,
		RaData:       hex.EncodeToString(decodeRAData), // hex encoding
	}
	oracleNode.SgxProof = &sgxProof
	return nil
}

// Parse raData of avr json and update sgx oracle node struct
// ATTENTION: this will modify the `oracleNode`
func (os *OracleService) parseRADataAndUpdateOracleNode(oracleNode *chaincodepb.SGXOracleNode) error {
	raDataBytes, err := hex.DecodeString(oracleNode.SgxProof.RaData)
	if err != nil {
		fmt.Printf("parse RAData, hex decode radata error: %s\n", err)
		return err
	}
	fmt.Printf("RAData bytes length: %d\n", len(raDataBytes))

	// NOTE: version(2) + length(4)
	var offset uint32 = 6

	for offset < uint32(len(raDataBytes)) {
		// TLV encoding schema:
		// - type<2>
		// - valueLength<4>
		// - value<valueLength>
		//
		t := readUint16(raDataBytes[offset : offset+2])
		offset += 2
		vLen := readUint32(raDataBytes[offset : offset+4])
		offset += 4
		v := raDataBytes[offset : offset+vLen]
		offset += vLen

		fmt.Printf("parse RAData, type: %d, length: %d\n", t, vLen)

		if t == 0 { // rsa pub key
			oracleNode.OracleNodeBasicInfo.RsaPubKey = v
		} else if t == 1 { // ecdsa pub key
			oracleNode.OracleNodeBasicInfo.EcdsaPubKey = v
		} else if t == 2 { // counter flag
			oracleNode.OracleNodeBasicInfo.CounterFlag = readUint32(v)
		} else if t == 3 { // counter id hash
			oracleNode.OracleNodeBasicInfo.CounterIdHash = v
		} else if t == 4 { // counter value
			oracleNode.OracleNodeBasicInfo.CounterValue = readUint32(v)
		} else if t == 5 {
			// udns ca pub key
			oracleNode.UdnsInfo = &chaincodepb.UDNSInfo{
				UdnsCaPubKey:   v,
				TotalDomainNum: 0,
				// no domain registered now
			}
		}
	}
	return nil
}

// EnclaveQuote json schema of avr report
type EnclaveQuote struct {
	IsvEnclaveQuoteStatus string `json:"isvEnclaveQuoteStatus"`
	IsvEnclaveQuoteBody   string `json:"isvEnclaveQuoteBody"`
}

// Verify SGX remote attestation AVR with params setting on `OracleCluster`:
// - iasPubKey
// - mrEnclave
// - isPswSupport
func (os *OracleService) verifySGXOracleNode(stub shim.ChaincodeStubInterface, oracleNode *chaincodepb.SGXOracleNode,
	iasPubKey string, mrEnclave []byte, isPswSupport bool) bool {
	avr := oracleNode.SgxProof.AVR
	quote := EnclaveQuote{}
	if err := json.Unmarshal([]byte(avr), &quote); err != nil {
		fmt.Printf("verifySGXOracleNode: unmarshal avr report failed: %s\n", err)
		return false
	}

	// TODO: no checking for `isvEnclaveQuoteStatus`

	// Verify enclave quote body
	if !os.verifyEnclaveQuote(oracleNode, quote.IsvEnclaveQuoteBody, mrEnclave) {
		return false
	}

	// Verify monotonic counter
	// NOTE: this will update Counters value
	if !os.verifyMonotonicCounter(stub, oracleNode, isPswSupport) {
		return false
	}

	// Verify AVR signature
	if !os.verifyAVRSig(oracleNode, iasPubKey) {
		return false
	}

	// Verify if oracle keys exist, if not, register oracle keys
	// NOTE: this will update OracleNodePks value
	if !os.verifyOracleNodePks(stub, oracleNode) {
		return false
	}
	return true
}

// Verify enclave quote
func (os *OracleService) verifyEnclaveQuote(oracleNode *chaincodepb.SGXOracleNode, quote string, mrEnclave []byte) bool {
	decodedQuote, err := base64.StdEncoding.DecodeString(quote)
	if err != nil {
		fmt.Printf("verifyEnclaveQuote: failed to base64 decode quote: %s\n", err)
		return false
	}

	// Verify the hash of radata in avr matching
	raDataHashInQuote := hex.EncodeToString(decodedQuote[400 : 400+32])
	decodeRaData, err := hex.DecodeString(oracleNode.SgxProof.RaData)
	if err != nil {
		fmt.Printf("verifyEnclaveQuote, hex decode radata error: %s\n", err)
	}
	raDataHash := bytesToHash(decodeRaData)

	fmt.Printf("verifyEnclaveQuote, raDataHashInQuote: %s, raDataHash: %s\n",
		raDataHashInQuote,
		raDataHash)

	if raDataHashInQuote != raDataHash {
		fmt.Printf("verifyEnclaveQuote: failed to check radata hash\n")
		return false
	}

	// Verify if the BASENAME is in SGX whitelist
	// TODO

	// Verify if the MRENCLAVE is in SGX whitelist
	mrEnclaveInQuote := decodedQuote[112 : 112+32]
	mrEnclave_bin, err := hex.DecodeString(string(mrEnclave))

	if !isSameBytes(mrEnclaveInQuote, mrEnclave_bin) {
		fmt.Printf("verifyEnclaveQuote: failed to verify mrenclave\n")
		return false
	}
	return true
}

// Verify monotonic counter
func (os *OracleService) verifyMonotonicCounter(stub shim.ChaincodeStubInterface, oracleNode *chaincodepb.SGXOracleNode,
	ifPswSupport bool) bool {

	if !ifPswSupport {
		return true
	}

	if oracleNode.OracleNodeBasicInfo.CounterFlag != 1 { // counter flag should be 1
		fmt.Printf("verifyMonotonicCounter: counter flag should be 1\n")
		return false
	}

	counterIdHash := string(oracleNode.OracleNodeBasicInfo.CounterIdHash)
	counterValue := oracleNode.OracleNodeBasicInfo.CounterValue

	counters, err := os.getStateCounters(stub)
	if err != nil {
		fmt.Printf("verifyMonotonicCounter: failed to get state counters %s\n", err)
		return false
	}
	counterSaved, has := counters.Counters[counterIdHash]
	if has {
		if counterValue <= counterSaved {
			fmt.Printf("verifyMonotonicCounter: verify counter value failed, counterValue(%d) - counterSaved(%d)\n",
				counterValue, counterSaved)
			return false
		}
	}

	// Save counter value
	counters.Counters[counterIdHash] = counterValue
	if err := os.putStateCounters(stub, counters); err != nil {
		fmt.Printf("verifyMonotonicCounter: failed to put state counters %s\n", err)
		return false
	}
	return true
}

// Verify AVR signature
// - iasPubKey is base64 encoded
func (os *OracleService) verifyAVRSig(oracleNode *chaincodepb.SGXOracleNode, iasPubKey string) bool {
	decodedPk, err := base64.StdEncoding.DecodeString(iasPubKey)
	if err != nil {
		fmt.Printf("verifyAVRSig: failed to decode ias pubkey: %s\n", err)
		return false
	}

	decodedSig, err := base64.StdEncoding.DecodeString(oracleNode.SgxProof.AVRSig)
	if err != nil {
		fmt.Printf("verifyAVRSig: failed to decode avr sig: %s\n", err)
		return false
	}

	body := oracleNode.SgxProof.AVR
	fmt.Printf("verifyAVRSig:\niasPubKey: %s\nsignBody: %s\nsig: %s\n",
		iasPubKey, body, oracleNode.SgxProof.AVRSig)
	return verifySigRsa(decodedPk, body, decodedSig)
}

// Verify oracle node pks not registered
func (os *OracleService) verifyOracleNodePks(stub shim.ChaincodeStubInterface, oracleNode *chaincodepb.SGXOracleNode) bool {
	oracleNodePks, err := os.getStateOracleNodePks(stub)
	if err != nil {
		return false
	}

	rsaPk := bytesToHash(oracleNode.OracleNodeBasicInfo.RsaPubKey)
	ecdsaPk := bytesToHash(oracleNode.OracleNodeBasicInfo.EcdsaPubKey)

	if oracleNodePks == nil {
		oracleNodePks = &chaincodepb.OracleNodePks{}
	}
	if oracleNodePks.Pks == nil {
		oracleNodePks.Pks = make(map[string]string)
	}

	if nodeBizId, has := oracleNodePks.Pks[rsaPk]; has {
		fmt.Printf("verifyOracleNodePks: oracle node %s key has registered\n", nodeBizId)
		// return false
	}
	if nodeBizId, has := oracleNodePks.Pks[ecdsaPk]; has {
		fmt.Printf("verifyOracleNodePks: oracle node %s key has registered\n", nodeBizId)
		// return false
	}

	// Save oracle node pks
	oracleNodePks.Pks[rsaPk] = oracleNode.OracleNodeBasicInfo.NodeBizId
	oracleNodePks.Pks[ecdsaPk] = oracleNode.OracleNodeBasicInfo.NodeBizId
	if err := os.putStateOracleNodePks(stub, oracleNodePks); err != nil {
		return false
	}
	return true
}

type HasOracleNodeRes struct {
	Result bool `json:"result"`
}

// Check whether oracle node exists with args:
// - oracleBizId
// - oracleNodeBizId
func (os *OracleService) hasOracleNode(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 2 {
		return shimErr(" Unexpected args length")
	}
	var (
		oracleBizId = args[0]
		nodeBizId   = args[1]
	)

	oracleClusters, err := os.getStateOracleClusters(stub)
	if err != nil {
		return shimErr("hasOracleNode: get state oracle clusters failed")
	}
	var hasOracleNodeRes HasOracleNodeRes
	if oracleCluster, has := oracleClusters.Clusters[oracleBizId]; has {
		if _, has := oracleCluster.OracleNodes[nodeBizId]; has {
			hasOracleNodeRes.Result = true
		}
	}
	res, err := json.Marshal(&hasOracleNodeRes)
	if err != nil {
		return shimErr(fmt.Sprintf("hasOracleNode: json marshal fatal error: %s", err))
	}
	return shim.Success(res)
}

// Register UNDS for specific domain in oracle node with args:
// - oracleBizId
// - oracleNodeBizId
// - domainName
// - udns
func (os *OracleService) registerUDNSDomain(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 4 {
		return shimErr("registerUDNSDomain: unexpected args length")
	}
	var (
		oracleBizId = args[0]
		nodeBizId   = args[1]
		domainName  = args[2]
		udns        = args[3]
	)

	fmt.Println("registerUDNSDomain args check success")
	// Basic checking
	oracleClusters, err := os.getStateOracleClusters(stub)
	if err != nil {
		return shimErr("registerUDNSDomain: get state oracle clusters failed")
	}
	if oracleClusters == nil || oracleClusters.Clusters == nil {
		return shimErr("registerUDNSDomain: oracle not added yet")
	}
	oracleCluster, has := oracleClusters.Clusters[oracleBizId]
	if !has {
		return shimErr("registerUDNSDomain: oracle not added yet")
	}
	fmt.Println("registerUDNSDomain oracle cluster found")

	if oracleCluster.OracleNodes == nil {
		return shimErr("registerUDNSDomain: oracle node not registered yet")
	}
	oracleNode, has := oracleCluster.OracleNodes[nodeBizId]
	if !has {
		return shimErr("registerUDNSDomain: oracle node not registered yet")
	}
	fmt.Println("registerUDNSDomain oracle node found")

	// NOTE: if oracleNode added, oracleNode.UdnsInfo MUST has set
	if oracleNode.UdnsInfo.UdnsDomains != nil && oracleNode.UdnsInfo.UdnsDomains[domainName] != nil {
		return shimErr("registerUDNSDomain: UDNS domain already registered")
	}
	fmt.Println("registerUDNSDomain oracle node not yet registered domain:", domainName)

	if oracleNode.UdnsInfo.UdnsDomains == nil {
		oracleNode.UdnsInfo.UdnsDomains = make(map[string]*chaincodepb.UDNSDomainInfo)
	}

	var udnsDomainInfo chaincodepb.UDNSDomainInfo
	if err := os.decodeUDNSTLVData(&udnsDomainInfo, domainName, udns); err != nil {
		return shimErr("registerUDNSDomain: decode udns tlv data failed")
	}

	// Verify UDNS domain
	if !os.verifyUDNSDomain(stub, oracleNode, &udnsDomainInfo) {
		return shimErr("registerUDNSDomain: failed to verify udns domain info")
	}

	fmt.Printf("registerUDNSDomain oracle node verify domain %s success\n", domainName)

	// Saved oracle cluster
	oracleNode.UdnsInfo.UdnsDomains[domainName] = &udnsDomainInfo
	if err := os.putStateOracleClusters(stub, oracleClusters); err != nil {
		return shimErr("registerUDNSDomain: put state oracle clusters failed")
	}

	fmt.Printf("registerUDNSDomain oracle node save domain %s success\n", domainName)

	return shim.Success(nil)
}

type HasUDNSDomainRes struct {
	Result bool `json:"result"`
}

// Check UDNS domain has registered or not, with args:
// - OracleBizId
// - OracleNodeBizId
// - domainName
func (os *OracleService) hasUDNSDomain(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 3 {
		return shimErr("hasUDNSDomain: unexpected args length")
	}
	var (
		oracleBizId = args[0]
		nodeBizId   = args[1]
		domainName  = args[2]
	)

	oracleClusters, err := os.getStateOracleClusters(stub)
	if err != nil {
		return shimErr("hasUDNSDomain: get state oracle clusters failed")
	}

	var res HasUDNSDomainRes
	if oracleClusters != nil && oracleClusters.Clusters != nil {
		oracleCluster, has := oracleClusters.Clusters[oracleBizId]
		if has && oracleCluster.OracleNodes != nil { // oracle cluster exists
			oracleNode, has := oracleCluster.OracleNodes[nodeBizId]
			if has { // oracle node exists

				if oracleNode.UdnsInfo.UdnsDomains != nil &&
					oracleNode.UdnsInfo.UdnsDomains[domainName] != nil {
					res.Result = true
				}
			}
		}
	}

	bs, err := json.Marshal(res)
	if err != nil {
		return shimErr("hasUDNSDomain: json marshal fatal error")
	}
	return shim.Success(bs)
}

// Decode UDNS tlv schema data
// NOTE: this will update `UDNSDomainInfo`
func (os *OracleService) decodeUDNSTLVData(udnsDomainInfo *chaincodepb.UDNSDomainInfo,
	domainName string, udns string) error {

	udnsBytes, err := base64.StdEncoding.DecodeString(udns)
	if err != nil {
		fmt.Printf("decodeUDNSTLVData, base64 decode udns error: %s\n", err)
		return err
	}

	var offset uint32 = 6
	for offset < uint32(len(udnsBytes)) {
		// TLV encoding schema:
		// - type<2>
		// - valueLength<4>
		// - value<valueLength>
		//
		t := readUint16(udnsBytes[offset : offset+2])
		//TODO: check t == 5 (set signing_body)
		if t == 5 {
			udnsDomainInfo.SigningBody = udnsBytes[6:offset]
		}

		offset += 2
		l := readUint32(udnsBytes[offset : offset+4])
		offset += 4
		v := udnsBytes[offset : offset+l]
		offset += l

		if t == 1 { // domain cert
			udnsDomainInfo.DomainCert = v
		} else if t == 2 { // udns cert
			udnsDomainInfo.UdnsCert = v
		} else if t == 3 { // domain name
			domain := string(v)
			fmt.Printf("decodeUDNSTLVData, parsed domain name: %s\n", domain)
			if domain != domainName {
				fmt.Printf("decodeUDNSTLVData, domain(tlv): %s and domain name: %s dismatch\n",
					domain, domainName)
				return fmt.Errorf("decodeUDNSTLVData, domain(tlv): %s and domain name: %s dismatch",
					domain, domainName)
			}
			udnsDomainInfo.DomainName = domain
		} else if t == 4 { // pks
			// Decode pks tlv data
			var (
				i          = 0
				oft uint32 = 6
			)
			for i < 2 {
				t2 := readUint16(v[oft : oft+2])
				oft += 2
				l2 := readUint32(v[oft : oft+4])
				oft += 4
				v2 := v[oft : oft+l2]
				oft += l2

				if t2 == 0 { // udns rsa pub key
					udnsDomainInfo.UdnsRsaPubKey = v2
				} else if t2 == 1 { // udns ecdsa pub key
					udnsDomainInfo.UdnsEcdsaPubKey = v2
				}
				i++
			}

		} else if t == 5 { // pk hash
			fmt.Printf("decode tlv udns, oracle node pk hash length: %d; value: %s\n", len(v), hex.EncodeToString(v))
			udnsDomainInfo.PubKeyHash = v
		} else if t == 6 {
			fmt.Printf("decode tlv udns, sig %s\n", hex.EncodeToString(v))
			udnsDomainInfo.Sig = v
		}
	}

	return nil
}

func (os *OracleService) verifyUDNSDomain(stub shim.ChaincodeStubInterface,
	oracleNode *chaincodepb.SGXOracleNode,
	udnsDomainInfo *chaincodepb.UDNSDomainInfo) bool {

	// Verify udnsDomainInfo.PubKeyHash
	pkHash := udnsDomainInfo.PubKeyHash
	oracleNodePks, err := os.getStateOracleNodePks(stub)
	if err != nil {
		fmt.Printf("verifyUDNSDomain: get state oracle node pks failed\n")
		return false
	}
	if oracleNodePks == nil || oracleNodePks.Pks == nil {
		fmt.Printf("verifyUDNSDomain: oracle node pubkey not registered, pks not exist\n")
		return false
	}
	fmt.Println(oracleNodePks.Pks)
	fmt.Println(hex.EncodeToString(pkHash))

	nodeBizId, has := oracleNodePks.Pks[hex.EncodeToString(pkHash)]
	//fmt.Printf("verify PubkeyHash, has: %s, nodeBizId: %s, pubkeyhash: %s\n", has, nodeBizId, string(pkHash))
	if !has || nodeBizId != oracleNode.OracleNodeBasicInfo.NodeBizId {
		fmt.Printf("verifyUDNSDomain: oracle node pubkey not registered\n")
		return false
	}

	// Verify udns domain info signature
	if !verifySigRsa(
		oracleNode.OracleNodeBasicInfo.RsaPubKey,
		string(udnsDomainInfo.SigningBody),
		udnsDomainInfo.Sig) {
		fmt.Printf("verifyUDNSDomain: verify udns signature failed\n")
		return false
	}

	// Verify domain pks registered on oracle cluster
	udnsRSAPkHash := bytesToHash(udnsDomainInfo.UdnsRsaPubKey)
	info, err := os.getStatePkDomainsByPk(stub, udnsRSAPkHash)
	if err != nil {
		fmt.Printf("verifyUDNSDomain: check pkDomain info failed")
		return false
	}
	if info != nil {
		fmt.Printf("verifyUDNSDomain: domain key has been registered")
		return false
	}

	udnsECDSAPkHash := bytesToHash(udnsDomainInfo.UdnsEcdsaPubKey)
	info, err = os.getStatePkDomainsByPk(stub, udnsECDSAPkHash)
	if err != nil {
		fmt.Printf("verifyUDNSDomain: check pkDomain info failed")
		return false
	}
	if info != nil {
		fmt.Printf("verifyUDNSDomain: domain key has been registered")
		return false
	}

	// Update oracle cluster and oracle node
	var (
		domainRSAPkInfo   chaincodepb.UDNSDomainPKHashInfo
		domainECDSAPkInfo chaincodepb.UDNSDomainPKHashInfo
	)
	domainRSAPkInfo.NodeBizId = nodeBizId
	domainRSAPkInfo.DomainName = udnsDomainInfo.DomainName
	domainECDSAPkInfo.NodeBizId = nodeBizId
	domainECDSAPkInfo.DomainName = udnsDomainInfo.DomainName
	// TODO: refactor all map assign
	if err = os.putStatePkDomainsWithPk(stub, udnsRSAPkHash, &domainRSAPkInfo); err != nil {
		fmt.Printf("verifyUDNSDomain: saving pkDomain info failed")
		return false
	}

	if err = os.putStatePkDomainsWithPk(stub, udnsECDSAPkHash, &domainECDSAPkInfo); err != nil {
		fmt.Printf("verifyUDNSDomain: saving pkDomain info failed")
		return false
	}

	oracleNode.UdnsInfo.TotalDomainNum += 1
	oracleNode.UdnsInfo.DomainIdList = append(oracleNode.UdnsInfo.DomainIdList, udnsDomainInfo.DomainName)
	return true
}

// Add oracle service in chaincode with args:
// - oracleServiceBizId
// - oracleServiceName
// - oracleServiceDesc
// - oracleBizId
// - dataSource
// - permissionPolicy
// - status
func (os *OracleService) addOracleService(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	fmt.Printf("addOracleService called")
	if len(args) != 7 {
		return shimErr("addOracleService: unexpected args length")
	}
	var (
		oracleServiceBizId = args[0]
		oracleServiceName  = args[1]
		oracleServiceDesc  = args[2]
		oracleBizId        = args[3]
		dataSource         = args[4]
		permissionPolicy   = args[5]
		status             = args[6]
	)
	// Arguments checking
	if dataSource != "HTTPS" && dataSource != "UDAG" {
		return shimErr(fmt.Sprintf("addOracleService: invalid dataSource %s", dataSource))
	}
	if permissionPolicy != "WHITELIST" && permissionPolicy != "BLACKLIST" {
		return shimErr(fmt.Sprintf("addOracleService: invalid permission policy: %s", permissionPolicy))
	}
	if status != "OPEN" && status != "CLOSE" {
		return shimErr(fmt.Sprintf("addOracleService: invalid service status: %s", status))
	}

	oracleClusters, err := os.getStateOracleClusters(stub)
	if err != nil {
		return shimErr("addOracleService: get state oracle clusters failed")
	}
	if oracleClusters == nil ||
		oracleClusters.Clusters == nil ||
		oracleClusters.Clusters[oracleBizId] == nil {
		return shimErr(fmt.Sprintf("addOracleService: oracle cluster %s has not registered", oracleBizId))
	}

	// Register an oracle service and save
	var oracleService chaincodepb.OracleService
	oracleService.OracleServiceBasicInfo = &chaincodepb.OracleServiceBasicInfo{
		ServiceBizId:     oracleServiceBizId,
		ServiceName:      oracleServiceName,
		ServiceDesc:      oracleServiceDesc,
		OracleBizId:      oracleBizId,
		ServiceStatus:    status,
		DataSource:       dataSource,
		PermissionPolicy: permissionPolicy,
	}
	oracleServices, err := os.getStateOracleServices(stub)
	if err != nil {
		return shimErr("addOracleService: get state oracle services failed")
	}
	if oracleServices == nil {
		oracleServices = &chaincodepb.OracleServices{}
	}
	if oracleServices.Services == nil {
		oracleServices.Services = make(map[string]*chaincodepb.OracleService)
	}
	oracleServices.Services[oracleServiceBizId] = &oracleService

	if err := os.putStateOracleServices(stub, oracleServices); err != nil {
		return shimErr("addOracleService: put state oracle services failed")
	}

	fmt.Printf("addOracleService called success")
	return shim.Success(nil)
}

type HasOracleServiceRes struct {
	Result bool `json:"result"`
}

func (os *OracleService) hasOracleService(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 1 {
		return shimErr("hasOracleService: unexpected args length")
	}
	oracleServiceId := args[0]

	oracleServices, err := os.getStateOracleServices(stub)
	if err != nil {
		return shimErr("hasOracleService: get state oracle services failed")
	}
	var hasOracleServiceRes HasOracleRes
	if oracleServices.Services != nil && oracleServices.Services[oracleServiceId] != nil {
		hasOracleServiceRes.Result = true
	}

	res, err := json.Marshal(&hasOracleServiceRes)
	if err != nil {
		return shimErr("hasOracleService: json marshal fatal error")
	}
	return shim.Success(res)
}

// TODO: callback chaincode set on oracle service chaincode
//

func decodeResponse(res []byte) chaincodepb.Response {
	resp := chaincodepb.Response{}

	var oft uint32 = 6
	for oft < uint32(len(res)) {
		// parse TLV item
		t := readUint16(res[oft : oft+2])
		oft += 2
		l := readUint32(res[oft : oft+4])
		oft += 4
		v := res[oft : oft+l]
		oft += l

		if t == 4 { // parse reqId and sigType
			var (
				oft2 uint32 = 6
				i           = 0
			)
			for i < 3 {
				t2 := readUint16(v[oft2 : oft2+2])
				oft2 += 2
				l2 := readUint32(v[oft2 : oft2+4])
				oft2 += 4
				v2 := v[oft2 : oft2+l2]
				oft2 += l2
				if t2 == 1 {
					// no need to check req exist !! 因为可能是am消息，请求是在OS里产生的
					resp.ReqId = string(v2) // reqId is hexstring
				} else if t2 == 2 {
					// request body
					resp.ResBody = v2
				} else if t2 == 3 {
					resp.SigType = uint32(readUint8(v2))
					// confirm response callback sigType
					fmt.Printf("responseCallback, parsed signType: %d\n", resp.SigType)
				}
				i++
			}
		} else if t == 0 { // parse oracle node pubkey hash: raw byte32
			resp.PubKeyHash = hex.EncodeToString(v)
		} else if t == 5 { // parse resp header & resp body & signing body
			// signing body
			resp.SigningBody = res[6:oft]
			resp.ResHeader, resp.ResBody, resp.HttpStatus = decodeUdagResp(v)
		} else if t == 6 { // parse sig
			resp.Sig = v
		} else if t == 7 { // parse errcode
			resp.ErrorCode = readUint32(v[:])
			fmt.Printf("responseCallback, errcode: %d\n", resp.ErrorCode)
		} else if t == 8 { // parse errmsg
			resp.ErrorMsg = string(v)
			fmt.Printf("responseCallback, errmsg: %s\n", resp.ErrorMsg)
		} else if t == 9 { // parse domain
			resp.Domain = string(v)
			fmt.Printf("responseCallback, doamin: %s\n", resp.Domain)
		} else if t == 10 { // parse version
			resp.Version = uint32(readUint16(v[:]))
			fmt.Printf("responseCallback, version: %s\n", resp.Version)
		}
	}

	return resp
}

func (os *OracleService) oracleServiceRejectRequest(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	//TODO:
	return shim.Success(nil)
}

func (os *OracleService) GetUDAGDomain(stub shim.ChaincodeStubInterface, resp *chaincodepb.Response) string {
	/*
		req := os.getRequestById(stub, resp.ReqId)
		if req == nil {
			fmt.Printf("verifyResponse, request %s not found\n", resp.ReqId)
			return ""
		}*/

	if resp.ErrorCode == 12306 ||
		resp.ErrorCode == 0 ||
		resp.ErrorCode == 12290 ||
		resp.ErrorCode == 5122 {
		udnsDomainPkInfo, err := os.getStatePkDomainsByPk(stub, resp.PubKeyHash)
		if err != nil {
			fmt.Printf("GetUDAGDomain: getting pkDomain info failed")
			return ""
		}
		if udnsDomainPkInfo == nil {
			fmt.Printf("verifyResponse, UDNS domain key not exists\n")
			return "" // UDNS domain key not exists
		}
		return udnsDomainPkInfo.DomainName

	}
	return ""
}

func (os *OracleService) verifyResponse(stub shim.ChaincodeStubInterface, resp *chaincodepb.Response) bool {
	/*
		req := os.getRequestById(stub, resp.ReqId)
		if req == nil {
			fmt.Printf("verifyResponse, request %s not found\n", resp.ReqId)
			return false
		}
	*/
	fmt.Printf("=============resp.pkHash: %v\n", resp.PubKeyHash)
	udnsDomainPkInfo, err := os.getStatePkDomainsByPk(stub, resp.PubKeyHash)
	if err != nil {
		fmt.Printf("GetUDAGDomain: getting pkDomain info failed")
		return false
	}
	if udnsDomainPkInfo == nil {
		fmt.Printf("verifyResponse, UDNS domain key not exists\n")
		return false // UDNS domain key not exists
	}
	var (
		nodeBizId  = udnsDomainPkInfo.NodeBizId
		domainName = udnsDomainPkInfo.DomainName
	)

	if resp.ErrorCode == 12306 ||
		resp.ErrorCode == 0 ||
		resp.ErrorCode == 12290 ||
		resp.ErrorCode == 5122 {
		resDomainTrustedServiceId := os.getDomainServiceId(stub, []string{domainName})
		if resDomainTrustedServiceId.Status != shim.OK {
			fmt.Printf("failed to get domain trusted service id")
			return false
		}

		oracleService, err := os.getOracleServiceById(stub, string(resDomainTrustedServiceId.Payload))
		if err != nil || oracleService == nil {
			fmt.Printf("getOracleServiceById failed with service id:%s \n", resp.ServiceId)
			return false
		}
		oracleCluster, err := os.getOracleClusterById(stub, oracleService.OracleServiceBasicInfo.OracleBizId)
		if err != nil || oracleCluster == nil {
			fmt.Printf("getOracle cluster failed with oracle id %s \n", oracleService.OracleServiceBasicInfo.OracleBizId)
			return false
		}

		oralceNode := oracleCluster.OracleNodes[nodeBizId]
		udnsDomainInfo := oralceNode.UdnsInfo.UdnsDomains[domainName]

		fmt.Printf("%p\n", udnsDomainInfo.UdnsRsaPubKey)

		// TODO(yichen): add back 检查
		if !verifySigRsa(udnsDomainInfo.UdnsRsaPubKey, string(resp.SigningBody), resp.Sig) {
			fmt.Printf("verifyResponse, verify sig failed\n")
			return false // verify sig failed
		}
	}

	//} else {
	//	// TODO
	//}
	//} else {
	//	return false // data source not exists
	//}

	return true
}

//func decodeCurlResp(res []byte) (header []byte, body []byte, status uint32) {
//	fmt.Printf("decodeCurlResp, data len: %d\n", len(res))
//	var (
//		oft uint32 = 6
//		i          = 0
//	)
//	for i < 3 {
//		t := readUint16(res[oft : oft+2])
//		oft += 2
//		l := readUint32(res[oft : oft+4])
//		oft += 4
//		v := res[oft : oft+l]
//		oft += l // shit! don't forget this
//
//		if t == 0 { // http status
//			var oft2 uint32 = 6
//			t2 := readUint16(v[oft2 : oft2+2])
//			oft2 += 2
//			l2 := readUint32(v[oft2 : oft2+4])
//			oft2 += 4
//			v2 := v[oft2 : oft2+l2]
//			if t2 == 0 {
//				status = readUint32(v2[:])
//			}
//		} else if t == 1 {
//			header = v
//		} else if t == 2 {
//			body = v
//		}
//		i++
//	}
//	fmt.Printf("decodeCurlResp, http status: %d, header len: %d, body len: %d, body: %s\n",
//		status, len(header), len(body), string(body))
//	return
//}

func decodeUdagResp(res []byte) (header []byte, body []byte, status uint32) {
	fmt.Printf("decodeUdagResp, data len: %d\n", len(res))
	var oft uint32 = 8
	l := readUint32(res[oft : oft+4])
	oft += 4
	v := res[oft : oft+l]
	oft += l

	header = []byte("6e756c6c")
	body = v
	return
}

// TODO: Define internal errors type
type RequestNotFoundErr struct {
	Msg string
}

func NewRequestNotFoundErr(msg string) RequestNotFoundErr {
	return RequestNotFoundErr{
		Msg: msg,
	}
}

func (rnf RequestNotFoundErr) Error() string {
	return rnf.Msg
}

func (os *OracleService) getRequestType(stub shim.ChaincodeStubInterface, reqId string) (string, error) {
	reqs, err := os.getStateRequests(stub)
	if err != nil {
		return "", err
	}
	if reqs == nil || reqs.Reqs == nil {
		return "", NewRequestNotFoundErr("Requests not exist")
	}
	req, has := reqs.Reqs[reqId]
	if !has {
		return "", NewRequestNotFoundErr(fmt.Sprintf("Request %s not exists", reqId))
	}

	return req.DataSource, nil
}

// getStateOracleClusters get state with key 'K_ORACLE_CLUSTERS' returned a reference of `SGXOracleClusters` and and error
// TODO: use cache in code??
func (os *OracleService) getStateOracleClusters(stub shim.ChaincodeStubInterface) (*chaincodepb.SGXOracleClusters, error) {
	bs, err := os.GetState(stub, false, K_ORACLE_CLUSTERS)
	if err != nil {
		fmt.Printf("getStateOracleClusters: get state fatal error: %s\n", err)
		return nil, err
	}

	var oracleClusters chaincodepb.SGXOracleClusters
	err = proto.Unmarshal(bs, &oracleClusters)
	if err != nil {
		fmt.Printf("getStateOracleClusters: unmarshal failed %s\n", err)
		return nil, err
	}

	return &oracleClusters, nil
}

func (os *OracleService) getOracleClusterById(stub shim.ChaincodeStubInterface, id string) (*chaincodepb.SGXOracleCluster, error) {
	ocs, err := os.getStateOracleClusters(stub)
	if err != nil {
		return nil, err
	}
	if ocs == nil || ocs.Clusters == nil {
		return nil, nil
	}
	return ocs.Clusters[id], nil
}

func pbDeterministicMarshal(pb proto.Message) ([]byte, error) {
	buf := proto.NewBuffer(nil)
	buf.SetDeterministic(true)
	err := buf.Marshal(pb)
	output := buf.Bytes()
	return output, err
}

func (os *OracleService) putStateOracleClusters(
	stub shim.ChaincodeStubInterface, oracleClusters *chaincodepb.SGXOracleClusters) error {

	bs, err := pbDeterministicMarshal(oracleClusters)
	if err != nil {
		fmt.Printf("putStateOracleClusters: unmarshal failed %s\n", err)
		return err
	}
	err = os.PutState(stub, false, K_ORACLE_CLUSTERS, bs)
	if err != nil {
		fmt.Printf("putStateOracleClusters: put state fatal error: %s\n", err)
		return err
	}
	return nil
}

func (os *OracleService) getStateCounters(stub shim.ChaincodeStubInterface) (*chaincodepb.Counters, error) {
	bs, err := os.GetState(stub, false, K_COUNTERS)
	if err != nil {
		fmt.Printf("getStateCounters: get state fatal error: %s\n", err)
		return nil, err
	}

	var counters chaincodepb.Counters
	err = proto.Unmarshal(bs, &counters)
	if err != nil {
		fmt.Printf("getStateCounters: unmarshal failed %s\n", err)
		return nil, err
	}
	return &counters, nil
}

func (os *OracleService) putStateCounters(
	stub shim.ChaincodeStubInterface, counters *chaincodepb.Counters) error {

	bs, err := pbDeterministicMarshal(counters)
	if err != nil {
		fmt.Printf("putStateCounters: unmarshal failed %s\n", err)
		return err
	}
	err = os.PutState(stub, false, K_COUNTERS, bs)
	if err != nil {
		fmt.Printf("putStateCounters: put state fatal error: %s\n", err)
		return err
	}
	return nil
}

func (os *OracleService) getStateOracleNodePks(stub shim.ChaincodeStubInterface) (*chaincodepb.OracleNodePks, error) {
	bs, err := os.GetState(stub, false, K_ORACLE_NODE_PKS)
	if err != nil {
		fmt.Printf("getStateOracleNodePks: get state fatal error: %s\n", err)
		return nil, err
	}

	var pks chaincodepb.OracleNodePks
	err = proto.Unmarshal(bs, &pks)
	if err != nil {
		fmt.Printf("getStateOracleNodePks: unmarshal failed %s\n", err)
		return nil, err
	}
	return &pks, nil
}

func (os *OracleService) putStateOracleNodePks(
	stub shim.ChaincodeStubInterface, pks *chaincodepb.OracleNodePks) error {

	bs, err := pbDeterministicMarshal(pks)
	if err != nil {
		fmt.Printf("putStateOracleNodePks: marshal failed %s\n", err)
		return err
	}
	err = os.PutState(stub, false, K_ORACLE_NODE_PKS, bs)
	if err != nil {
		fmt.Printf("putStateOracleNodePks: put state fatal error: %s\n", err)
		return err
	}
	return nil
}

func (os *OracleService) getStatePkDomains(stub shim.ChaincodeStubInterface) (*chaincodepb.DomainPks, error) {
	bs, err := os.GetState(stub, false, K_PK_DOMAINS)
	if err != nil {
		fmt.Printf("getStatePkDomains: get state fatal error: %s\n", err)
		return nil, err
	}

	var pks chaincodepb.DomainPks
	err = proto.Unmarshal(bs, &pks)
	if err != nil {
		fmt.Printf("getStatePkDomains: unmarshal failed %s\n", err)
		return nil, err
	}
	return &pks, nil
}

func (os *OracleService) getStatePkDomainsByPk(stub shim.ChaincodeStubInterface, hash string) (*chaincodepb.UDNSDomainPKHashInfo, error) {
	pks, err := os.getStatePkDomains(stub)
	if err != nil {
		return nil, err
	}
	if pks == nil || pks.DomainPks == nil {
		return nil, nil
	}
	return pks.DomainPks[hash], nil
}

func (os *OracleService) putStatePkDomains(stub shim.ChaincodeStubInterface, pks *chaincodepb.DomainPks) error {
	bs, err := pbDeterministicMarshal(pks)
	if err != nil {
		fmt.Printf("putStatePkDomains: marshal failed %s\n", err)
		return err
	}
	err = os.PutState(stub, false, K_PK_DOMAINS, bs)
	if err != nil {
		fmt.Printf("putStatePkDomains: put state fatal error: %s\n", err)
		return err
	}
	return nil
}

func (os *OracleService) putStatePkDomainsWithPk(stub shim.ChaincodeStubInterface, hash string, info *chaincodepb.UDNSDomainPKHashInfo) error {
	pks, err := os.getStatePkDomains(stub)
	if err != nil {
		return err
	}
	if pks.DomainPks == nil {
		pks.DomainPks = make(map[string]*chaincodepb.UDNSDomainPKHashInfo)
	}
	pks.DomainPks[hash] = info

	err = os.putStatePkDomains(stub, pks)
	return err
}

func (os *OracleService) getStateOracleServices(stub shim.ChaincodeStubInterface) (*chaincodepb.OracleServices, error) {
	bs, err := os.GetState(stub, false, K_ORACLE_SERVICE)
	if err != nil {
		fmt.Printf("getStateOracleServices: get state fatal error: %s\n", err)
		return nil, err
	}

	var oracleServices chaincodepb.OracleServices
	err = proto.Unmarshal(bs, &oracleServices)
	if err != nil {
		fmt.Printf("getStateOracleServices: unmarshal failed %s\n", err)
		return nil, err
	}
	return &oracleServices, nil
}

func (os *OracleService) getOracleServiceById(stub shim.ChaincodeStubInterface, serviceId string) (
	*chaincodepb.OracleService, error) {

	oss, err := os.getStateOracleServices(stub)
	if err != nil {
		return nil, err
	}
	if oss == nil || oss.Services == nil {
		return nil, nil
	}
	return oss.Services[serviceId], nil
}

func (os *OracleService) putStateOracleServices(
	stub shim.ChaincodeStubInterface, oracleServices *chaincodepb.OracleServices) error {

	bs, err := pbDeterministicMarshal(oracleServices)
	if err != nil {
		fmt.Printf("putStateOracleServices: unmarshal failed %s\n", err)
		return err
	}
	err = os.PutState(stub, false, K_ORACLE_SERVICE, bs)
	if err != nil {
		fmt.Printf("putStateOracleServices: put state fatal error: %s\n", err)
		return err
	}
	return nil
}

func (os *OracleService) getStateRequests(stub shim.ChaincodeStubInterface) (*chaincodepb.Requests, error) {
	bs, err := os.GetState(stub, false, K_REQUESTS)
	if err != nil {
		fmt.Printf("getStateRequests: get state fatal error: %s\n", err)
		return nil, err
	}

	var reqs chaincodepb.Requests
	err = proto.Unmarshal(bs, &reqs)
	if err != nil {
		fmt.Printf("getStateRequests: unmarshal failed %s\n", err)
		return nil, err
	}
	return &reqs, nil
}

func (os *OracleService) getRequestById(stub shim.ChaincodeStubInterface, reqId string) *chaincodepb.Request {
	reqs, err := os.getStateRequests(stub)
	if err != nil {
		return nil
	}
	if reqs == nil || reqs.Reqs == nil {
		return nil
	}
	return reqs.Reqs[reqId]
}

// ccen TOCHECK: 这个函数没被调用过
func (os *OracleService) putStateRequests(
	stub shim.ChaincodeStubInterface, reqs *chaincodepb.Requests) error {

	bs, err := pbDeterministicMarshal(reqs)
	if err != nil {
		fmt.Printf("putStateRequests: unmarshal failed %s\n", err)
		return err
	}
	err = os.PutState(stub, false, K_REQUESTS, bs)
	if err != nil {
		fmt.Printf("putStateRequests: put state fatal error: %s\n", err)
		return err
	}
	return nil
}

func (os *OracleService) getStateRequestsNum(stub shim.ChaincodeStubInterface) (int, error) {
	bs, err := os.GetState(stub, true, K_REQUESTS_NUM)
	if err != nil {
		fmt.Printf("getStateRequestsNum: get state fatal error: %s\n", err)
		return 0, err
	}

	// -- initial value 0
	if len(bs) == 0 {
		return 0, nil
	}
	num, err := strconv.Atoi(string(bs))
	if err != nil {
		fmt.Printf("getStateRequestsNum: unmarshal failed %s\n", err)
		return 0, err
	}
	return int(num), nil
}

func (os *OracleService) putStateRequestsNum(
	stub shim.ChaincodeStubInterface, num int) error {

	err := os.PutState(stub, true, K_REQUESTS_NUM, []byte(strconv.Itoa(num)))
	if err != nil {
		fmt.Printf("putStateRequestsNum: put state fatal error: %s\n", err)
		return err
	}
	return nil
}

func (os *OracleService) getStateResponses(stub shim.ChaincodeStubInterface) (*chaincodepb.Responses, error) {
	bs, err := os.GetState(stub, false, K_RESPONSES)
	if err != nil {
		fmt.Printf("getStateResponses: get state fatal error: %s\n", err)
		return nil, err
	}

	var resps chaincodepb.Responses
	err = proto.Unmarshal(bs, &resps)
	if err != nil {
		fmt.Printf("getStateResponses: unmarshal failed %s\n", err)
		return nil, err
	}
	return &resps, nil
}

func (os *OracleService) putStateResponses(
	stub shim.ChaincodeStubInterface, resps *chaincodepb.Responses) error {

	bs, err := pbDeterministicMarshal(resps)
	if err != nil {
		fmt.Printf("putStateResponses: unmarshal failed %s\n", err)
		return err
	}
	err = os.PutState(stub, false, K_RESPONSES, bs)
	if err != nil {
		fmt.Printf("putStateResponses: put state fatal error: %s\n", err)
		return err
	}
	return nil
}

func readUint8(s []byte) uint8 {
	a := uint8(s[1])
	return a
}

func readUint16(s []byte) uint16 {
	return binary.LittleEndian.Uint16(s)
}

func readUint32(s []byte) uint32 {
	return binary.LittleEndian.Uint32(s)
}

// https://gist.github.com/jedy/5963633
func verifySigRsa(key []byte, body string, sig []byte) bool {
	re, err := x509.ParsePKIXPublicKey(key)
	if err != nil {
		fmt.Printf("verifySigRsa, parse pubkey error: %s\n", err)
		return false
	}
	pub := re.(*rsa.PublicKey)

	h := sha256.New()
	h.Write([]byte(body))
	digest := h.Sum(nil) // sha256 hash

	err = rsa.VerifyPKCS1v15(pub, crypto.SHA256, digest, sig)
	if err != nil {
		fmt.Printf("verifySigRsa, verify error: %s\n", err)
		return false
	}

	return true
}

func isSameBytes(a []byte, b []byte) bool {
	lena := len(a)
	lenb := len(b)
	if lena != lenb {
		return false
	}
	for i := 0; i < lena; i++ {
		if a[i] != b[i] {
			return false
		}
	}
	return true
}

/////////////////// AM logic //////////////////////////////////////////////////////////////////////////////
/////////////////// AM logic //////////////////////////////////////////////////////////////////////////////
/////////////////// AM logic //////////////////////////////////////////////////////////////////////////////
/////////////////// AM logic //////////////////////////////////////////////////////////////////////////////
/////////////////// AM logic //////////////////////////////////////////////////////////////////////////////

type LogEntriesType struct {
	From    string   `json:"From"`
	To      string   `json:"To"`
	Topics  []string `json:"Topics"`
	LogData string   `json:"LogData"`
}

type MychainReceipt struct {
	Offset     int              `json:"TxIndex"`
	Result     int              `json:"Result"`
	GasUsed    int              `json:"GasUsed"`
	Output     string           `json:"Output"`
	LogEntries []LogEntriesType `json:"LogEntries"`
}

type FabricWriteEntry struct {
	Key      string `json:"key"`
	IsDelete uint   `json:"is_delete"`
	Value    string `json:"value"`
}

//type AuthMessage struct {
//	Collection string `json:"Collection"`
//	Content    string `json:"Content"`// AM消息全文，hex编码
//	Digest     string `json:"Digest"`
//	Receipt    string `json:"Receipt"`
//}
//
//// MarshalJSON : 自定义对象转换到 json
//func (j *AuthMessage ) MarshalJSON() (data []byte, err error) {
//	var buf bytes.Buffer
//	buf.WriteString(fmt.Sprintf(
//		"{\"Digest\":\"%s\",\"Content\":\"%s\",\"Collection\":\"%s\",\"Receipt\":\"%s\"}",
//		j.Digest, j.Content, j.Collection, j.Receipt))
//	return buf.Bytes(), nil
//}
//
//
//type OracleEvent struct {
//	Message *AuthMessage `json:"Message"`
//	Request chaincodepb.Request `json:"Request"`
//	BizExtension []byte `json:"BizExtension"`
//	Extension []byte `json:"Extension"`
//}
//
//// MarshalJSON : 自定义对象转换到 json
//func (j *OracleEvent ) MarshalJSON() (data []byte, err error) {
//	var buf bytes.Buffer
//	j1, _ := json.Marshal(j.Message)
//	j2, _ := json.Marshal(j.Request)
//	j3, _ := json.Marshal(j.BizExtension)
//	j4, _ := json.Marshal(j.Extension)
//	buf.WriteString(fmt.Sprintf(
//		"{\"Message\":\"%s\",\"Request\":\"%s\",\"BizExtension\":%s,\"Extension\":%s}", j1, j2, j3, j4 ))
//	return buf.Bytes(), nil
//}

// ---------------------------------- state/private data key ---------------------------------------

var (
	AMPREFIX = PREFIX

	KMychainParserInfo = AMPREFIX + "parser_info"

	K_RECV_SEQ_PREFIX = AMPREFIX + "recv_seq_"

	K_SEND_SEQ_PREFIX = AMPREFIX + "send_seq_"

	K_COLLECTION_SALT = AMPREFIX + "salt"

	K_AM_CONTENT_PREFIX = AMPREFIX + "am_content_"

	K_AM_TRANSIENT = AMPREFIX + "transient"

	FABRIC_PARSER = "fabric_1.4"

	MYCHAIN_PARSER = "mychain_0.10"
)

var (
	P2P_MSG_PROTOCOL_TYPE = uint32(0) // 默认 0 for P2P
)

var (
	EMPTY_BYTES32                = [32]byte{0}
	DEFAULT_SOLIDITY_AMMSG_TOPIC = "79b7516b1b7a6a39fb4b7b22e8667cd3744e5c27425292f8a9f49d1042c0c651"
	DEFAULT_WASM_AMMSG_TOPIC     = "53656e64417574684d657373616765"
	TEE_UNENCRYPT                = "756e656e6372797074"
)

func (os *OracleService) setMyChainDomainAMClient(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 3 {
		return shimErr(fmt.Sprintf("Unexpected args length %d", len(args)))
	}
	var (
		domain            = args[0]
		solidity_amclient = args[1]
		wasm_amclient     = args[2]
	)

	if err := os.PutState(stub, true, K_SOLIDITY_AMCLIENT_PREFIX+domain, []byte(solidity_amclient)); err != nil {
		return shimErr("setMyChainDomainAMClient: solidity save failed")
	}

	if err := os.PutState(stub, true, K_WASM_AMCLIENT_PREFIX+domain, []byte(wasm_amclient)); err != nil {
		return shimErr("setMyChainDomainAMClient: wasm save failed")
	}

	return shim.Success([]byte("success"))
}

func (os *OracleService) getMyChainDomainAMClient(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 1 {
		return shim.Success([]byte(""))
	}
	var (
		domain = args[0]
	)

	solidity_amclient, err := os.GetState(stub, true, K_SOLIDITY_AMCLIENT_PREFIX+domain)
	if err != nil {
		solidity_amclient = []byte("")
	}
	wasm_amclient, err := os.GetState(stub, true, K_WASM_AMCLIENT_PREFIX+domain)
	if err != nil {
		wasm_amclient = []byte("")
	}

	var amclient []byte
	amclient = append(amclient, solidity_amclient...)
	amclient = append(amclient, '\000') // one byte of '\0'
	amclient = append(amclient, wasm_amclient...)
	return shim.Success(amclient)
}

func (os *OracleService) recvMychainRawData(stub shim.ChaincodeStubInterface,
	domain string,
	data []byte,
	hint string) pb.Response {

	// TODO: 增加其他区块链的parse支持
	rawParser, err := os.GetState(stub, false, fmt.Sprintf("%s_%s", KMychainParserInfo, domain))
	if err != nil {
		return shimErr(fmt.Sprintf("failed to get parser for domain %s: %v", domain, err))
	}
	if rawParser == nil || len(rawParser) == 0 {
		rawParser = []byte("defaultParse")
		//return shimErr(fmt.Sprintf("empty parser for domain %s", domain))
	}

	var amPkt string
	var resp pb.Response
	switch string(rawParser) {
	case MYCHAIN_PARSER:
		amPkt, resp = os.parseMyChainRawData(stub, domain, data, hint)
	case FABRIC_PARSER:
		amPkt, resp = os.parseFabricRawData(stub, domain, data, hint)
	default:
		amPkt = hex.EncodeToString(data)
		//return shimErr(fmt.Sprintf("your parser %s is not supported. ", string(rawParser)))
	}

	if amPkt == "" {
		fmt.Printf("Parser[%s] get empty amPkt, with err %s\n", string(rawParser), resp.Message)
		return resp
	}

	fmt.Printf("parseMyChainRawData success with amPkt %s\n", amPkt)

	return os.recvAMMessage(stub, amPkt, domain)
}

func (os *OracleService) RecvMychainRawData(stub shim.ChaincodeStubInterface,
	domain string,
	data []byte,
	hint string) pb.Response {
	return os.recvMychainRawData(stub, domain, data, hint)
}

func (os *OracleService) parseMyChainTeeRawData(stub shim.ChaincodeStubInterface, domain string, data []byte, hint string) (string, pb.Response) {
	hintsSplit := strings.Split(hint, ",")
	if len(hintsSplit) != 2 {
		fmt.Printf("tee hint format error %s\n", hint)
		return "", shimErr("parseMyChainTeeRawData: parse tee hint fail")
	}
	plainPkg := hintsSplit[1]
	plainBin, err := hex.DecodeString(plainPkg)
	if err != nil {
		fmt.Printf("parseMyChainTeeRawData get plain error %s\n", plainPkg)
		return "", shimErr("parseMyChainTeeRawData get plain error")
	}

	logIndex, err := strconv.Atoi(hintsSplit[0])
	if err != nil {
		fmt.Printf("parseMyChainTeeRawData get logIndex failed err message %s\n", err)
		return "", shimErr("parseMyChainTeeRawData: parse tee hint index fail")
	}

	receipt := MychainReceipt{}
	err = json.Unmarshal(data, &receipt)
	if err != nil {
		fmt.Printf("parseMyChainTeeRawData parse receipt error %s, %s\n", err, data)
		return "", shimErr("parseMyChainTeeRawData: parse receipt error")
	}

	if TEE_UNENCRYPT != receipt.LogEntries[logIndex].Topics[0] {
		fmt.Printf("parseMyChainTeeRawData check topic0 error %s\n", receipt.LogEntries[logIndex].Topics[0])
		return "", shimErr("parseMyChainTeeRawData check topict0 error")
	}

	if DEFAULT_WASM_AMMSG_TOPIC != receipt.LogEntries[logIndex].Topics[1] {
		fmt.Printf("parseMyChainTeeRawData check topic1 error %s\n", receipt.LogEntries[logIndex].Topics[1])
		return "", shimErr("parseMyChainTeeRawData check topic1 error")
	}

	if bytesToHash(plainBin) != receipt.LogEntries[logIndex].Topics[2] {
		fmt.Printf("parseMyChainTeeRawData check hash error %s %s\n", receipt.LogEntries[logIndex].Topics[2], plainPkg)
		return "", shimErr("parseMyChainTeeRawData check hash error")
	}

	// amClient的检查
	amClients := strings.Split(string(os.getMyChainDomainAMClient(stub, []string{domain}).Payload), string('\000'))
	if len(amClients) != 2 {
		return "", shimErr(fmt.Sprintf("Unexpected am clients number = %d", len(amClients)))
	}
	wasmAmClient := amClients[1]
	if wasmAmClient == "" {
		fmt.Printf("getMyChainDomainAMClient error for domain %s, wasm empty string\n", domain)
		return "", shimErr("wasm client for this domain is not set yet, domain " + domain)
	}
	if receipt.LogEntries[logIndex].Topics[3] != wasmAmClient {
		fmt.Printf("parseMyChainTeeRawData check client error %s %s\n", receipt.LogEntries[logIndex].Topics[3], wasmAmClient)
		return "", shimErr("parseMyChainTeeRawData check client error " + receipt.LogEntries[logIndex].Topics[3] + " " + wasmAmClient)
	}

	plainBin = getBytesFromRLP(plainBin)
	if plainBin == nil || len(plainBin) == 0 {
		return "", shimErr(fmt.Sprintf("getBytesFromRLP failed with result %v", plainBin))
	}

	fmt.Printf("getMyChainDomainAMClient get tee amPkt success\n")
	return hex.EncodeToString(plainBin), shim.Success(nil)
}

// domain: mychain域名
// data是取回来的receipt的json格式
// hint是LogEntries的序列，格式是"[?]"
// update: data可能来自tee链或非tee链，在本函数内进行对应的解析
//
//	当前tee/非tee链的data最大区别在于hints。格式与OS严格对齐
//	非tee格式:     [logIndex]
//	tee格式:       logIndex,plainAmPkg
func (os *OracleService) parseMyChainRawData(stub shim.ChaincodeStubInterface, domain string, data []byte, hint string) (string, pb.Response) {
	if hint[0] != '[' {
		fmt.Println("get teechain raw data, domain " + domain)
		return os.parseMyChainTeeRawData(stub, domain, data, hint)
	}
	receipt := MychainReceipt{}
	_ = json.Unmarshal(data, &receipt)

	// amclient的检查
	amclients := strings.Split(string(os.getMyChainDomainAMClient(stub, []string{domain}).Payload), string('\000'))
	if len(amclients) != 2 {
		return "", shimErr(fmt.Sprintf("Unexpected am clients number = %d", len(amclients)))
	}

	solidity_amclient := amclients[0]
	wasm_amclient := amclients[1]
	if solidity_amclient == "" && wasm_amclient == "" {
		fmt.Printf("getMyChainDomainAMClient error for domain %s, both are empty string\n", domain)
		return "", shimErr("amclient for this domain is not set yet")
	}

	logIndex, err2 := strconv.Atoi(hint[1 : len(hint)-1])
	if err2 != nil {
		fmt.Printf("getMyChainDomainAMClient get logIndex failed err message %s\n", err2)
		return "", shimErr("parseMyChainRawData: parse hint fail")
	}

	fmt.Printf("receipt.LogEntries[logIndex].To=%s\n\nsolidity_amclient=%s\n\n", receipt.LogEntries[logIndex].To, solidity_amclient)
	fmt.Printf("receipt.LogEntries[logIndex].To=%s\n\nwasm_amclient=%s\n\n", receipt.LogEntries[logIndex].To, wasm_amclient)

	if receipt.LogEntries[logIndex].To != solidity_amclient && receipt.LogEntries[logIndex].To != wasm_amclient {
		return "", shimErr("parseMyChainRawData: not from expected am client")
	}

	var amPkt string
	switch receipt.LogEntries[logIndex].Topics[0] {
	case DEFAULT_SOLIDITY_AMMSG_TOPIC:
		amPkt = receipt.LogEntries[logIndex].LogData
	case DEFAULT_WASM_AMMSG_TOPIC:
		amPkt = receipt.LogEntries[logIndex].Topics[1]
	default:
		return "", shimErr("parseMyChainRawData: not found expected topic")
	}

	packet, err := hex.DecodeString(amPkt)
	if err != nil {
		return "", shimErr("recvAMMessage hex decode packet failed")
	}

	// 收到的是RLP编码后的数组
	packet = getBytesFromRLP(packet)
	if packet == nil || len(packet) == 0 {
		return "", shimErr(fmt.Sprintf("getBytesFromRLP failed with result %v", packet))
	}

	fmt.Printf("parseMyChainRawData get amPkt success\n")
	return hex.EncodeToString(packet), shim.Success(nil)
}

func (os *OracleService) parseFabricRawData(stub shim.ChaincodeStubInterface, domain string, data []byte, hint string) (string, pb.Response) {
	wset := &FabricWriteEntry{}
	if err := json.Unmarshal(data, wset); err != nil {
		return "", shimErr(err.Error())
	}
	if !strings.HasPrefix(wset.Key, K_CROSSCHAIN_MSG_PREFIX) {
		return "", shimErr("prefix is not found in key: " + wset.Key)
	}
	if wset.IsDelete != 0 {
		return "", shimErr("wrong flag for is_delete. ")
	}

	return wset.Value, shim.Success(nil)
}

// 处理来自trans protocol的消息
// 构造AM package
/*
 *  报文格式:
 *  version          (4 byte)
 *  identity         (32byte)
 *  protocol type    (4 byte)
 *  message          (variable) 32 + N
 */
func buildAuthMessage(author [32]byte, message []byte) []byte {

	// 计算总报文长度
	totallen := sizeOfUint32() + sizeOfidentity() + sizeOfUint32() + sizeOfBytes(message)

	// 分配报文空间和初始偏移
	pkg := make([]byte, totallen)
	offset := totallen - 1

	// step 1: 填充AM消息版本号
	version := uint32(1)
	uint32ToBytes(offset, version, pkg)
	offset -= sizeOfUint32()

	// step 2: 填充author身份
	identityToBytes(offset, author, pkg)
	offset -= sizeOfidentity()

	// step 3: 填充序列消息协议号
	protocolType := uint32(P2P_MSG_PROTOCOL_TYPE)
	uint32ToBytes(offset, protocolType, pkg)
	offset -= sizeOfUint32()

	// step 4: 填充序列层消息报文内容
	stringToBytes(offset, message, pkg)

	//return []byte(hex.EncodeToString(pkg))
	return pkg
}

func getBytesFromRLP(packet []byte) []byte {
	// 跳过第一个256 bit 偏移
	// 跳过长度字段(256)的高192位(24字节)
	contentlen := bytesToUInt64(packet, 32+24)

	content := make([]byte, contentlen)
	copyBytesWithLen(content, packet, 0, 64, contentlen) // 跳过前面两个256bit 字段
	return content
}

func TestRecvAuthMessage(rlppacket []byte) ([]byte, []byte, pb.Response) {
	return recvAuthMessage(rlppacket)
}

// 解析AM package
/*
 *  报文格式:
 *  version          (4 byte)
 *  发送者的identity         (32byte)
 *  protocol type    (4 byte)
 *  message          (variable) 32 + N
 */
func recvAuthMessage(packet []byte) ([]byte, []byte, pb.Response) {

	// 初始化偏移
	offset := uint32(len(packet) - 1)

	// step 1: 提取AM消息版本号
	version := bytesToUint32(offset, packet)
	offset -= sizeOfUint32()

	// 比较version与版本是否一致
	if version != 1 {
		return nil, nil, shimErr("recvAuthMessage AM message version not match")
	}

	// step 2: 提取报文作者（发送者）的身份
	identity := bytesToIdentity(offset, packet)
	offset -= sizeOfidentity()

	// step 3: 提取上层报文协议号，目前只支持P2P协议
	protocol_type := bytesToUint32(offset, packet)
	offset -= sizeOfUint32()

	if protocol_type != P2P_MSG_PROTOCOL_TYPE {
		return nil, nil, shimErr("recvAuthMessage protocol type not supported")
	}

	// step 4: 提取P2P消息报文
	p2pmsg := bytesToString(offset, packet)

	return identity[:], p2pmsg, shim.Success(nil)
}

// 返回oracle event
func (os *OracleService) sendMessage(stub shim.ChaincodeStubInterface,
	destDomain string,
	receiver [32]byte,
	message []byte,
	msgnounce string,
	collection string,
	msgType string) pb.Response {
	// 找到原始proposal调用的链码，作为发送者身份
	sendercc := os.getSignedProposalChaincode(stub)
	// 构造P2P消息
	p2pmsg, err := os.buildP2PMessage(stub, destDomain, receiver, sendercc, message, msgType)
	if p2pmsg == nil {
		return err
	}

	// 因为发送方与domain绑定，只要一个发送方，发送方的identity置为全0，byte32
	author := sha256.Sum256([]byte(sendercc))
	fmt.Printf("author is %s\n", hex.EncodeToString(author[:]))

	// 构造AM消息
	ammsg := buildAuthMessage(author, p2pmsg)
	if ammsg == nil {
		return shimErr("build AM message failed")
	}

	fmt.Printf("am pkg is **\n%s\nam pkg len is %d\n**\n", hex.EncodeToString(ammsg), len(ammsg))

	// 存储跨链消息到state里
	key := K_CROSSCHAIN_MSG_PREFIX + stub.GetTxID() + "_" + msgnounce
	os.PutState(stub, false, key, ammsg)
	fmt.Printf("save am message in state with key:%s\n", key)

	//var eventmsgjson  []byte
	//eventmsgjson = ammsg
	//// 非collection的AM消息直接写到event中
	//if collection != "" {
	//
	//	// calc hash as key
	//	digest := bytesToHash(ammsg)
	//	fmt.Printf("get digest:%s\n", digest)
	//
	//	// store in private data
	//	{
	//		if err := os.PutPrivateData(stub, true, collection, K_AM_CONTENT_PREFIX + digest, ammsg) ; err != nil {
	//			return shimErr("put private data failed for am message"), nil
	//		}
	//
	//	}
	//
	//	fmt.Printf("get digest:%s\n", digest)
	//
	//	emsg := OracleEvent{}
	//	amsg := AuthMessage{}
	//	amsg.Collection = collection
	//	amsg.Digest = digest
	//	emsg.Message = &amsg
	//
	//	msgjson, err := json.Marshal(emsg)
	//	if err != nil {
	//		return shimErr("marsaling am message for private data failed"), nil
	//	}
	//	eventmsgjson = msgjson
	//}

	return shim.Success(nil)
}

//func (os *OracleService) recvMessage(stub shim.ChaincodeStubInterface, args []string) pb.Response {
//	if len(args) != 3 {
//		return shimErr("AmClient recvMessage: unexpected args length")
//	}
//	var (
//		packet = args[0]
//		srcDomain= args[1]
//		collection = args[2]
//	)
//	return os.recvAMMessage(stub, packet, srcDomain, collection)
//}

func (os *OracleService) recvAMMessage(stub shim.ChaincodeStubInterface,
	packethex string,
	srcDomain string) pb.Response {
	// 从State读期望的domain
	expectedDomain, _ := os.GetState(stub, true, K_EXPECTED_DOMAIN)
	packet, err := hex.DecodeString(packethex)
	if err != nil {
		return shimErr("recvAMMessage hex decode packet failed")
	}

	author, p2ppacket, ret := recvAuthMessage(packet)
	author32 := CopySliceToByte32(author)
	if p2ppacket == nil {
		return ret
	}

	destDomain, content, receiver, seq_no, ret2 := parseP2PMessage(p2ppacket)
	if ret2.Status != shim.OK {
		return ret2
	}
	// 比较目标域名与自身域名是否一致
	fmt.Printf("\ndest domain:%s\n", destDomain)
	if string(destDomain) != string(expectedDomain) {
		return shimErr("dest domain does not match expected")
	}

	var msgType string
	if seq_no == K_UNORDERED_MSG_SEQ {
		msgType = K_MSG_TYPE_UNORDERED
	} else {
		msgType = K_MSG_TYPE_ORDERED
		re := os.checkSeq(stub, srcDomain, author32, string(destDomain), receiver, seq_no)
		if re.Status != shim.OK {
			return re
		}
	}

	fmt.Printf("recv message:%s from %s:%s\n", content, srcDomain, hex.EncodeToString(author))
	msg := RecvAuthMessage{srcDomain, author32, content, receiver, msgType}
	msgstr, _ := json.Marshal(msg)

	return shim.Success([]byte(msgstr))
}

func (os *OracleService) checkSeq(stub shim.ChaincodeStubInterface,
	srcDomain string,
	author32 [32]byte,
	destDomain string,
	receiver [32]byte,
	seq_no uint32,
) pb.Response {

	// 计算消息序列的ID
	seqId := os.calcSeqId(srcDomain, author32, receiver)
	fmt.Printf("recv P2P message from %s:%s -> %s:%s, seqid:%s\n",
		srcDomain,
		hex.EncodeToString(author32[:]),
		destDomain,
		hex.EncodeToString(receiver[:]),
		seqId)

	// 查询该序列的接收消息序号
	var seq chaincodepb.MsgNounce
	seq, err := os.getNounce(stub, K_RECV_SEQ_PREFIX+seqId)
	if err != nil {
		return shimErr("AmClient parse P2P message: get recv seq no failed")
	}

	// 比较接收到的序号与期望接收序号进行比较
	if seq_no != seq.Seqno {
		return shimErr(fmt.Sprintf("AmClient parse P2P message: recv seq no[%d] does not match expected seq no [%d]",
			seq_no, seq.Seqno))
	}

	// 序号自增加一，保存
	seq.Seqno++
	if err := os.putNounce(stub, K_RECV_SEQ_PREFIX+seqId, seq); err != nil {
		return shimErr("AmClient setDomainParser: save seq no failed")
	}
	return shim.Success(nil)

}

func getIdentityForCollection(collection string) [32]byte {
	return sha256.Sum256([]byte(collection))
}

func (os *OracleService) calcSeqId(domain string, from [32]byte, to [32]byte) string {
	fmt.Printf("calcSeqId for %s %s %s\n", domain, hex.EncodeToString(from[:]), hex.EncodeToString(to[:]))
	// 计算序列ID := sha2(dest domain + receiver)
	c := make([]byte, len(domain)+len(from)+len(to))
	copy(c, []byte(domain))
	copy(c[len(domain):], from[:])
	copy(c[len(domain)+len(from):], to[:])

	hash := sha256.Sum256(c)
	seq_id := hex.EncodeToString(hash[:])
	return seq_id
}

type queryP2PMsgSeqResp struct {
	Result uint32 `json:"result"`
}

func (os *OracleService) queryRecvP2PMsgSeq(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 3 {
		return shimErr(fmt.Sprintf(" Unexpected args length %d", len(args)))
	}
	var (
		srcDomain      = args[0]
		author, err1   = hex.DecodeString(args[1])
		receiver, err2 = hex.DecodeString(args[2])
	)

	if err1 != nil {
		return shimErr(fmt.Sprintf("[queryRecvP2PMsgSeq] author format error: %s ", args[1]))
	}
	if err2 != nil {
		return shimErr(fmt.Sprintf("[queryRecvP2PMsgSeq] receiver format error: %s ", args[2]))
	}

	// 计算消息序列的ID
	seq_id := os.calcSeqId(srcDomain,
		CopySliceToByte32(author),
		CopySliceToByte32(receiver))

	// 查询该序列的发送消息序号
	var seq chaincodepb.MsgNounce
	seq, err := os.getNounce(stub, K_RECV_SEQ_PREFIX+seq_id)
	if err != nil {
		return shimErr("AmClient sendMessage: get send seq no failed")
	}

	ret := queryP2PMsgSeqResp{}
	ret.Result = seq.Seqno
	resp, _ := json.Marshal(ret)
	return shim.Success(resp)
}

func (os *OracleService) querySendP2PMsgSeq(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 3 {
		return shimErr(fmt.Sprintf(" Unexpected args length %d", len(args)))
	}
	var (
		destDomain     = args[0]
		author, err1   = hex.DecodeString(args[1])
		receiver, err2 = hex.DecodeString(args[2])
	)
	if err1 != nil {
		return shimErr(fmt.Sprintf("[querySendP2PMsgSeq] author format error: %s ", args[1]))
	}
	if err2 != nil {
		return shimErr(fmt.Sprintf("[querySendP2PMsgSeq] receiver format error: %s ", args[2]))
	}

	// 计算消息序列的ID
	seq_id := os.calcSeqId(destDomain,
		CopySliceToByte32(author),
		CopySliceToByte32(receiver))

	// 查询该序列的发送消息序号
	var seq chaincodepb.MsgNounce
	seq, err := os.getNounce(stub, K_SEND_SEQ_PREFIX+seq_id)
	if err != nil {
		return shimErr("AmClient sendMessage: get send seq no failed")
	}

	ret := queryP2PMsgSeqResp{}
	ret.Result = seq.Seqno
	resp, _ := json.Marshal(ret)
	return shim.Success(resp)
}

func (os *OracleService) setRecvP2PMsgSeq(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 4 {
		return shimErr(fmt.Sprintf(" Unexpected args length %d", len(args)))
	}
	var (
		srcDomain      = args[0]
		author, err1   = hex.DecodeString(args[1])
		receiver, err2 = hex.DecodeString(args[2])
		seqno, err3    = strconv.Atoi(args[3])
	)

	if err1 != nil {
		return shimErr(fmt.Sprintf("[setRecvP2PMsgSeq] author format error: %s ", args[1]))
	}
	if err2 != nil {
		return shimErr(fmt.Sprintf("[setRecvP2PMsgSeq] receiver format error: %s ", args[2]))
	}
	if err3 != nil {
		return shimErr(fmt.Sprintf("[setRecvP2PMsgSeq] seqno format error: %s ", args[3]))
	}

	// 计算消息序列的ID
	seq_id := os.calcSeqId(srcDomain,
		CopySliceToByte32(author),
		CopySliceToByte32(receiver))

	// 查询该序列的发送消息序号
	var seq chaincodepb.MsgNounce
	seq.Seqno = uint32(seqno)
	err := os.putNounce(stub, K_RECV_SEQ_PREFIX+seq_id, seq)
	if err != nil {
		return shimErr("AmClient sendMessage: get send seq no failed")
	}

	ret := queryP2PMsgSeqResp{}
	ret.Result = seq.Seqno
	resp, _ := json.Marshal(ret)
	return shim.Success(resp)
}

func (os *OracleService) setSendP2PMsgSeq(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 4 {
		return shimErr(fmt.Sprintf(" Unexpected args length %d", len(args)))
	}
	var (
		destDomain     = args[0]
		author, err1   = hex.DecodeString(args[1])
		receiver, err2 = hex.DecodeString(args[2])
		seqno, err3    = strconv.Atoi(args[3])
	)
	if err1 != nil {
		return shimErr(fmt.Sprintf("[setRecvP2PMsgSeq] author format error: %s ", args[1]))
	}
	if err2 != nil {
		return shimErr(fmt.Sprintf("[setRecvP2PMsgSeq] receiver format error: %s ", args[2]))
	}
	if err3 != nil {
		return shimErr(fmt.Sprintf("[setRecvP2PMsgSeq] seqno format error: %s ", args[3]))
	}

	// 计算消息序列的ID
	seq_id := os.calcSeqId(destDomain,
		CopySliceToByte32(author),
		CopySliceToByte32(receiver))

	// 设置该序列的发送消息序号
	var seq chaincodepb.MsgNounce
	seq.Seqno = uint32(seqno)
	err := os.putNounce(stub, K_SEND_SEQ_PREFIX+seq_id, seq)
	if err != nil {
		return shimErr("AmClient sendMessage: get send seq no failed")
	}

	ret := queryP2PMsgSeqResp{}
	ret.Result = seq.Seqno
	resp, _ := json.Marshal(ret)
	return shim.Success(resp)
}

func (os *OracleService) getSignedProposalChaincode(stub shim.ChaincodeStubInterface) string {
	signedProposal, err := stub.GetSignedProposal()
	var pp pb.Proposal

	if err = proto.Unmarshal(signedProposal.GetProposalBytes(), &pp); err != nil {
		return ""
	}

	var header comm.Header
	if err = proto.Unmarshal(pp.GetHeader(), &header); err != nil {
		return ""
	}

	var chheader comm.ChannelHeader
	if err = proto.Unmarshal(header.ChannelHeader, &chheader); err != nil {
		return ""
	}

	var cc_hr_ext pb.ChaincodeHeaderExtension
	if err = proto.Unmarshal(chheader.Extension, &cc_hr_ext); err != nil {
		return ""
	}

	return cc_hr_ext.ChaincodeId.Name
}

func (os *OracleService) buildP2PMessage(stub shim.ChaincodeStubInterface,
	destDomain string,
	receiver [32]byte,
	sendercc string,
	message []byte,
	msgType string) ([]byte, pb.Response) {

	// 发送P2P消息给AM合约
	/*
	 * 消息格式：
	 * destination domain           (32 + N bytes)
	 * destination identity         (32 bytes)
	 * uint32 sequence              (4  bytes)
	 * bytes  message               (32 + N)
	 */
	// 计算报文总长度
	totalLen := sizeOfBytes([]byte(message)) + 4 + 32 + sizeOfBytes([]byte(destDomain))
	// 分配报文长度
	pkg := make([]byte, totalLen)
	// 初始偏移，从右往左开始填充
	offset := totalLen - 1

	// step 1: 填充接收端域名
	stringToBytes(offset, []byte(destDomain), pkg)
	offset -= sizeOfBytes([]byte(destDomain))

	// step 2: 填充接收端identity
	identityToBytes(offset, receiver, pkg)
	offset -= sizeOfidentity()

	var sender [32]byte
	if sendercc == "" {
		return nil, shimErr("AmClient buildP2PMessage: sender chaincode not found")
	}

	{
		var selfCCName string
		ccinfo := sysos.Getenv("CORE_CHAINCODE_ID_NAME")
		if ccinfo != "" {
			cc := strings.Split(ccinfo, ":")
			fmt.Println("Name:", cc[0])
			fmt.Println("Version:", strings.Join(cc[1:], ":"))
			selfCCName = cc[0]
		}

		//selfCCName, err := os.GetState(stub, false, K_CROSSCHAIN_CCNAME)
		//if err != nil {
		//	return nil, shimErr("AmClient buildP2PMessage: get self chaincode failed ")
		//}

		if sendercc == string(selfCCName) {
			return nil, shimErr("AmClient buildP2PMessage: sender chaincode is crosschain cc itself")
		} else {
			sender = sha256.Sum256([]byte(sendercc))
		}
	}

	var msgSeq uint32
	if msgType == K_MSG_TYPE_ORDERED {

		// 计算消息序列的ID
		seqId := os.calcSeqId(destDomain, sender, receiver)

		// 查询该序列的发送消息序号
		var seq chaincodepb.MsgNounce
		seq, err := os.getNounce(stub, K_SEND_SEQ_PREFIX+seqId)
		if err != nil {
			return nil, shimErr("AmClient buildP2PMessage: get send seq no failed")
		}
		msgSeq = seq.Seqno

		// 序号自增加一，保存
		seq.Seqno++
		if err := os.putNounce(stub, K_SEND_SEQ_PREFIX+seqId, seq); err != nil {
			return nil, shimErr("AmClient buildP2PMessage: save seq no failed")
		}
	} else if msgType == K_MSG_TYPE_UNORDERED {
		msgSeq = K_UNORDERED_MSG_SEQ
	} else {
		return nil, shimErr(fmt.Sprintf("AmClient buildP2PMessage: unsupported message type:%s", msgType))
	}

	fmt.Printf("AmClient buildP2PMessage: send P2P message from %s:%s -> %s:%s, seq no:%x\n",
		"",
		hex.EncodeToString(sender[:]),
		destDomain,
		hex.EncodeToString(receiver[:]),
		msgSeq)

	// step 3: 填充序列发送消息序列号
	uint32ToBytes(offset, msgSeq, pkg)
	offset -= sizeOfUint32()

	// step 4: 填充发送消息
	stringToBytes(offset, []byte(message), pkg)

	// 返回P2P消息
	return pkg, shim.Success(nil)
}

func TestParseP2PMessage(
	packet []byte,
) ([]byte, []byte, [32]byte, uint32, pb.Response) {
	return parseP2PMessage(packet)
}

func parseP2PMessage(
	packet []byte,
) ([]byte, []byte, [32]byte, uint32, pb.Response) {

	// 从AM接收的消息提取的P2P消息
	/*
	 * 消息格式：
	 * dest domain           (32 + N bytes)
	 * dest identity         (32 bytes)
	 * uint32 sequence              (4  bytes)
	 * bytes  message               (32 + N)
	 */

	// 初始偏移
	var offset uint32
	offset = uint32(len(packet) - 1)

	// step 1: 提取目标域名
	destDomain := string(bytesToString(offset, packet))
	offset -= getStringSize(offset, packet)

	// step 2: 提取目标接收者身份
	identity := bytesToIdentity(offset, packet)
	offset -= sizeOfidentity()

	// step 3: 提取序列的消息号
	seq_no := bytesToUint32(offset, packet)
	offset -= sizeOfUint32()

	// step 4: 提取消息内容
	message := bytesToString(offset, packet)

	// 返回P2P提取后的内容
	return []byte(destDomain), message, identity, seq_no, shim.Success(nil)
}

func (os *OracleService) rejectP2PMessage(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) != 4 {
		return shimErr(fmt.Sprintf(" Unexpected args length %d", len(args)))
	}
	var (
		srcDomain      = args[0]
		author, err1   = hex.DecodeString(args[1])
		receiver, err2 = hex.DecodeString(args[2])
		msgSeq, err3   = strconv.Atoi(args[3])
	)
	if err1 != nil {
		return shimErr(fmt.Sprintf("[rejectP2PMessage] author format error: %s ", args[1]))
	}
	if err2 != nil {
		return shimErr(fmt.Sprintf("[rejectP2PMessage] receiver format error: %s ", args[2]))
	}
	if err3 != nil {
		return shimErr(fmt.Sprintf("[rejectP2PMessage] msgSeq format error: %s ", args[3]))
	}

	// 计算消息序列的ID
	seqId := os.calcSeqId(srcDomain, CopySliceToByte32(author), CopySliceToByte32(receiver))
	fmt.Printf("recv reject P2P message from %s:%s -> %s, seqid:%s\n",
		srcDomain,
		hex.EncodeToString(author[:]),
		hex.EncodeToString(receiver[:]),
		seqId)

	// 查询该序列的接收消息序号
	var seq chaincodepb.MsgNounce
	seq, err := os.getNounce(stub, K_RECV_SEQ_PREFIX+seqId)
	if err != nil {
		return shimErr("AmClient parse P2P message: get recv seq no failed")
	}

	// 比较接收到的序号与期望接收序号进行比较
	if (uint32)(msgSeq) != seq.Seqno {
		return shimErr(fmt.Sprintf("AmClient parse P2P message: recv seq no[%d] does not match expected seq no [%d]",
			msgSeq, seq.Seqno))
	}

	// 序号自增加一，保存
	seq.Seqno++
	if err := os.putNounce(stub, K_RECV_SEQ_PREFIX+seqId, seq); err != nil {
		return shimErr("AmClient setDomainParser: save seq no failed")
	}

	// 返回成功
	return shim.Success(nil)
}

func (os *OracleService) setDomainServiceId(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 2 {
		return shimErr(fmt.Sprintf("Unexpected args length %d", len(args)))
	}
	var (
		domain     = args[0]
		service_id = args[1]
	)

	if err := os.PutState(stub, true, K_DOMAIN_SERVICE_IDS+domain, []byte(service_id)); err != nil {
		return shimErr("setDomainServiceId: service_id save failed")
	}

	return shim.Success([]byte("success"))
}

func (os *OracleService) getDomainServiceId(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 1 {
		return shimErr(fmt.Sprintf("Unexpected args length %d", len(args)))
	}
	var (
		domain = args[0]
	)

	service_id, err := os.GetState(stub, true, K_DOMAIN_SERVICE_IDS+domain)
	if err != nil {
		return shimErr("getDomainServiceId: service_id get state failed")
	}

	return shim.Success(service_id)
}

/*
func (os *OracleService) ParseAMPackage(stub shim.ChaincodeStubInterface, args []string) pb.Response {

}

func (os *OracleService) recvFromRelayer(stub shim.ChaincodeStubInterface, args []string) pb.Response {

}

*/

func (os *OracleService) putNounce(
	stub shim.ChaincodeStubInterface, key string, nounce chaincodepb.MsgNounce) error {

	bs, err := pbDeterministicMarshal(&nounce)
	if err != nil {
		fmt.Printf("putNounce: marshal failed %s\n")
		return err
	}
	err = os.PutState(stub, false, key, bs)
	if err != nil {
		fmt.Printf("putNounce failed for key %s:\n", key)
		return err
	}
	return nil
}

func (os *OracleService) getNounce(
	stub shim.ChaincodeStubInterface, key string) (chaincodepb.MsgNounce, error) {
	var trans chaincodepb.MsgNounce
	bs, err := os.GetState(stub, false, key)
	if err != nil {
		fmt.Printf("getStateProtobuf: get state fatal error for key %s: %s\n",
			key, err)
		return trans, err
	}

	err = proto.Unmarshal(bs, &trans)
	if err != nil {
		fmt.Printf("getStateTransProtocol: unmarshal failed %s\n", err)
		return trans, err
	}
	return trans, nil
}

// -------------------------- Util -----------------------------------------------
func bytesToHash(bs []byte) string {
	h := sha256.Sum256(bs)
	return hex.EncodeToString(h[:])
}

func bytesToUInt64(bs []byte, offset int) uint64 {
	num := uint64(0)
	for i := 0; i < 8; i++ {
		num <<= 8
		num += uint64(bs[offset+i])
	}
	return num
}

func shimErr(err string) pb.Response {
	return shim.Error(err)
}

func uint32ToBytes(offset uint32, i uint32, buf []byte) {
	buf[offset] = byte(i)
	buf[offset-1] = byte(i >> 8)
	buf[offset-2] = byte(i >> 16)
	buf[offset-3] = byte(i >> 24)
}

func bytesToUint32(offset uint32, buf []byte) uint32 {
	var i uint32
	i = 0
	i += uint32(buf[offset-3])
	i <<= 8
	i += uint32(buf[offset-2])
	i <<= 8
	i += uint32(buf[offset-1])
	i <<= 8
	i += uint32(buf[offset])
	return i
}

func identityToBytes(offset uint32, id [32]byte, buf []byte) {
	for i := uint32(0); i < 32; i++ {
		buf[offset-i] = id[31-i]
	}
}

func bytesToIdentity(offset uint32, buf []byte) [32]byte {

	var out [32]byte
	for i := uint32(0); i < 32; i++ {
		out[31-i] = buf[offset-i]
	}
	return out
}

func lenUintToBytes(len uint32) [32]byte {
	var str [32]byte
	uint32ToBytes(31, len, str[:])
	return str
}

func lenBytesToUint(offset uint32, str []byte) uint32 {
	return bytesToUint32(offset, str)
}

func stringToBytes(offset uint32, str []byte, buf []byte) {
	var length uint32
	length = uint32(len(str))

	// len u256
	lenstr := lenUintToBytes(length)

	// copy length
	for i := uint32(0); i < 32; i++ {
		buf[offset-i] = lenstr[31-i]
	}

	offset -= 32

	src_offset := uint32(0)

	// copy content, every 32 byte chunk
	for length > 0 {
		for i := uint32(0); i < length && i < 32; i++ {
			buf[offset-31+i] = str[src_offset+i]
		}
		if length > 32 {
			length -= 32
			src_offset += 32
			offset -= 32
		} else {
			break
		}
	}
}

func bytesToString(offset uint32, buf []byte) []byte {

	bLen := lenBytesToUint(offset, buf)

	offset -= 32

	srcOffset := uint32(0)
	out := make([]byte, bLen)
	for bLen > 0 {
		for i := uint32(0); i < bLen && i < 32; i++ {
			out[srcOffset+i] = buf[offset-31+i]
		}
		if bLen > 32 {
			bLen -= 32
			srcOffset += 32
			offset -= 32
		} else {
			break
		}
	}
	return out
}

// 返回字符串占用空间
func getStringSize(offset uint32, buf []byte) uint32 {

	totalLen := uint32(0)
	totalLen += 32
	u := lenBytesToUint(offset, buf)
	totalLen += 32 * (u / 32)
	if u%32 > 0 {
		totalLen += 32
	}
	return totalLen
}

func sizeOfBytes(message []byte) uint32 {
	reallen := uint32(len(message))
	u := reallen / 32 * 32
	if reallen%32 != 0 {
		u += 32
	}
	u += 32
	return u
}

func sizeOfUint32() uint32 {
	return 4
}

func sizeOfidentity() uint32 {
	return 32
}

func CopySliceToByte32(src []byte) [32]byte {
	var dst [32]byte
	for i := uint32(0); i < 32; i++ {
		dst[i] = src[i]
	}
	return dst
}

func copyBytesWithLen(dst []byte, src []byte, dstoffset uint64, srcoffset uint64, len uint64) {
	for i := uint64(0); i < len; i++ {
		dst[dstoffset+i] = src[srcoffset+i]
	}
}

func copyBytes(dst []byte, src []byte, offset uint64) {
	for i := uint64(0); i < uint64(len(src)); i++ {
		dst[offset+i] = src[i]
	}
}

func IsSameByte32(a [32]byte, b [32]byte) bool {
	for i := uint32(0); i < 32; i++ {
		if a[i] != b[i] {
			return false
		}
	}
	return true
}

func (os *OracleService) GetState(stub shim.ChaincodeStubInterface, stablized bool, key string) ([]byte, error) {

	vstate, err := stub.GetState(key)
	if err != nil {
		return nil, err
	}

	if vstate != nil && stablized {
		vstate = vstate[0 : len(vstate)-len(K_STATE_STABLIZED_SUFFIX)]
	}

	return vstate, nil
}

func (os *OracleService) PutState(stub shim.ChaincodeStubInterface, stablized bool, key string, value []byte) error {

	if stablized {
		value = append(value, []byte(K_STATE_STABLIZED_SUFFIX)...)
	}

	err := stub.PutState(key, value)

	return err
}

func (os *OracleService) GetPrivateData(stub shim.ChaincodeStubInterface, stablized bool, collection string, key string) ([]byte, error) {

	vstate, err := stub.GetPrivateData(collection, key)
	if err != nil {
		return nil, err
	}

	if stablized {
		vstate = vstate[0 : len(vstate)-len(K_STATE_STABLIZED_SUFFIX)]
	}

	return vstate, nil
}

func (os *OracleService) PutPrivateData(stub shim.ChaincodeStubInterface, stablized bool, collection string, key string, value []byte) error {

	if stablized {
		value = append(value, []byte(K_STATE_STABLIZED_SUFFIX)...)
	}

	err := stub.PutPrivateData(collection, key, value)

	return err
}
