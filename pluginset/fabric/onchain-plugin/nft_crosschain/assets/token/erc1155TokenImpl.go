package token

import (
	"encoding/hex"
	"encoding/json"
	"fabric_nft_crosschain/utils"
	"fmt"
	"github.com/hyperledger/fabric-chaincode-go/shim"
	"github.com/hyperledger/fabric-protos-go/peer"
	"math/big"
	"strings"
)

const (
	TokenName        = "TokenName"
	TokenSymbol      = "TokenSymbol"
	TokenURI         = "TokenURI"
	TokenBalance     = "%s-%s-Balance"
	TokenTotalSupply = "%s-TotalSupply"

	EventTranferSingle = "%s-transfeSingle"
	EventList          = "event"
)

type ERC1155TokenImpl struct{}

var _ ERC1155 = (*ERC1155TokenImpl)(nil)

func (token *ERC1155TokenImpl) Init(stub shim.ChaincodeStubInterface) peer.Response {
	// 1 判断是否已经初始化
	rawName, _ := stub.GetState(TokenName)
	if len(rawName) != 0 {
		return shim.Success(nil)
	}

	// 2 参数检查
	args := stub.GetArgs()
	if len(args) != 3 {
		return shim.Error("args number should be 3")
	}
	// 2.1 name
	if args[1] == nil {
		return shim.Error(fmt.Sprintf("token name can't be empty"))
	}
	// 2.2 symbol
	if args[2] == nil {
		return shim.Error(fmt.Sprintf("token symbol can't be empty"))
	}

	// 3 存参数状态
	if err := stub.PutState(TokenName, args[1]); err != nil {
		return shim.Error(fmt.Sprintf("failed To put token name: %v", err))
	}
	if err := stub.PutState(TokenSymbol, args[2]); err != nil {
		return shim.Error(fmt.Sprintf("failed To put token symbol: %v", err))
	}

	return shim.Success(nil)
}

func (token *ERC1155TokenImpl) Invoke(stub shim.ChaincodeStubInterface) peer.Response {
	fn, _ := stub.GetFunctionAndParameters()
	args := stub.GetArgs()
	if len(args) == 0 {
		return shim.Error("no args")
	}
	args = args[1:] // 下标0为函数名称

	switch fn {
	case "init":
		return token.Init(stub)
	case "balanceOf":
		return token.balanceOf(stub, args)
	case "balanceOfBatch":
		return token.balanceOfBatch(stub, args)
	case "setApprovalForAll": // todo
		return token.setApprovalForAll(stub, args)
	case "isApprovedForAll": // todo
		return token.isApprovedForAll(stub, args)
	case "safeTransferFrom":
		return token.safeTransferFrom(stub, args)
	case "safeBatchTransferFrom":
		return token.safeBatchTransferFrom(stub, args)

	case "getURI":
		return token.getURI(stub, args)
	case "setURI":
		return token.setURI(stub, args)
	case "mint":
		return token.mint(stub, args)
	//case "mintBatch":
	//	return token.mintBatch(stub, args)
	//case "burn":
	//	return token.burn(stub, args)
	//case "burnBatch":
	//	return token.burnBatch(stub, args)

	// 锚定资产合约新增接口
	case "mintBatchByTB":
		return token.mintBatchByTB(stub, args)

	// 测试接口
	case "testSafeTransferFrom":
		return token.testSafeTransferFrom(stub, args)
	case "clientAccountID":
		return token.clientAccountID(stub)
	}

	return shim.Error(fmt.Sprintf("no function name %s found", fn))
}

// implementation of erc1155 interface ====================================

func (token *ERC1155TokenImpl) balanceOf(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	// account, assetId
	if len(args) != 2 {
		return shim.Error("args number should be 2")
	}

	account, err := hex.DecodeString(string(args[0]))
	if err != nil {
		account = args[0]
		//return shim.Error(fmt.Sprintf("failed to decode hex holder: %v", err))
	}

	if utils.IsZeroBytes(account) {
		return shim.Error("address zero is not a valid owner")
	}

	id, ok := big.NewInt(0).SetString(string(args[1]), 10)
	if !ok {
		return shim.Error(fmt.Sprintf("failed to decode asset id: %s", args[1]))
	}

	rawBalance, _ := stub.GetState(balanceKey(account, id))
	if len(rawBalance) == 0 {
		return shim.Success([]byte(big.NewInt(0).String()))
	}

	balance := big.NewInt(0).SetBytes(rawBalance)

	return shim.Success([]byte(balance.String()))
}

func (token *ERC1155TokenImpl) balanceOfBatch(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	if len(args) != 2 {
		return shim.Error("args number should be 2")
	}

	strAccs := strings.Split(string(args[0]), ",")
	strIds := strings.Split(string(args[1]), ",")
	if len(strIds) != len(strAccs) {
		return shim.Error(fmt.Sprintf("accounts(%d) and ids(%d) length mismatch", len(strAccs), len(strIds)))
	}

	balances := make([]*big.Int, 0)
	for i, strAcc := range strAccs {
		// get account
		account, err := hex.DecodeString(strAcc)
		if err != nil {
			account = []byte(strAcc) // 合约账户
			//return shim.Error(fmt.Sprintf("failed to decode hex holder: %v", err))
		}

		if utils.IsZeroBytes(account) {
			return shim.Error("address zero is not a valid owner")
		}

		// get id
		id, ok := big.NewInt(0).SetString(strIds[i], 10)
		if !ok {
			return shim.Error(fmt.Sprintf("failed to decode asset id: %s", strIds[i]))
		}

		// get balance
		rawBalance, _ := stub.GetState(balanceKey(account, id))
		if len(rawBalance) == 0 {
			balances = append(balances, big.NewInt(0))
		} else {
			balances = append(balances, big.NewInt(0).SetBytes(rawBalance))
		}
	}

	return shim.Success([]byte(utils.ConverBigintListToString(balances)))
}

func (token *ERC1155TokenImpl) setApprovalForAll(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	//TODO implement me
	panic("implement me")
}

func (token *ERC1155TokenImpl) isApprovedForAll(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	//TODO implement me
	panic("implement me")
}

func (token *ERC1155TokenImpl) _isApprovedForAll(from []byte, sender []byte) bool {
	// todo
	return true
}

func (token *ERC1155TokenImpl) safeTransferFrom(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	// fromAccount, toAccount(TBContract), assetId, amount, data("")
	if len(args) != 5 {
		return shim.Error("number of transferLogic args should be 5")
	}

	sender, err := utils.GetMsgSenderAddress(stub)
	if err != nil {
		return shim.Error(fmt.Sprintf("failed To get tx sender: %v", err))
	}

	from, err := hex.DecodeString(string(args[0]))
	isFromContract := false
	if err != nil {
		from = args[0]
		isFromContract = true
	}

	if string(from) != string(sender) && !token._isApprovedForAll(from, sender) {
		return shim.Error(fmt.Sprintf("from(%s) is not token owner or approved", string(from)))
	}

	to, err := hex.DecodeString(string(args[1]))
	isToContract := false
	if err != nil {
		// 无法hex即为合约地址
		to = args[1]
		isToContract = true
	}

	id, ok := big.NewInt(0).SetString(string(args[2]), 10)
	if !ok {
		return shim.Error(fmt.Sprintf("failed to decode asset id: %s", args[2]))
	}

	amt, ok := big.NewInt(0).SetString(string(args[3]), 10)
	if !ok {
		return shim.Error(fmt.Sprintf("failed to decode amount: %s", args[3]))
	}

	// 外部接口传入参数，需要hex
	data, err := hex.DecodeString(string(args[4]))
	if err != nil {
		return shim.Error(fmt.Sprintf("failed to decode data: %s", args[4]))
	}

	return token._safeTransferFrom(stub, sender, from, to, id, amt, data, isFromContract, isToContract)
}

func (token *ERC1155TokenImpl) _safeTransferFrom(stub shim.ChaincodeStubInterface, operator, from, to []byte, id, amt *big.Int, data []byte, isFromContract, isToContract bool) peer.Response {
	// todo: 判断to地址为空

	// 判断amt为正数
	if amt.Sign() != 1 {
		return shim.Error("amount should be positive")
	}

	ids := utils.AsSingletonArray(id)
	amts := utils.AsSingletonArray(amt)

	if err := token._beforeTokenTransfer(stub, operator, from, to, ids, amts, data); err != nil {
		return shim.Error(fmt.Sprintf("failed to do beforeTokenTransfer: %v", err))
	}

	if err := token._transferLogic(stub, operator, from, to, id, amt); err != nil {
		var fromStr string
		var toStr string
		if isFromContract {
			fromStr = string(from)
		} else {
			fromStr = hex.EncodeToString(from)
		}
		if isToContract {
			toStr = string(to)
		} else {
			toStr = hex.EncodeToString(to)
		}
		return shim.Error(fmt.Sprintf("failed to do transfer: from %s, to %s, asset %s, amount %s: %v", fromStr, toStr, id.String(), amt.String(), err))
	}

	token._afterTokenTransfer(stub, operator, from, to, ids, amts, data)

	if err := token._doSafeTransferAcceptanceCheck(stub, operator, from, to, id, amt, data, isFromContract, isToContract); err != nil {
		return shim.Error(fmt.Sprintf("failed to do SafeTransferAcceptanceCheck: %v", err))
	}

	return shim.Success(nil)
}

func (token *ERC1155TokenImpl) safeBatchTransferFrom(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	// sender(tb), receiver(account), ids, amts, data
	if len(args) != 5 {
		return shim.Error("number of transferLogic args should be 5")
	}

	sender, err := utils.GetMsgSenderAddress(stub)
	if err != nil {
		return shim.Error(fmt.Sprintf("failed To get tx sender: %v", err))
	}

	fmt.Printf("safeBatchTransferFrom, sender:%s, from: %s, to: %s, ids: %s, amts: %s, data: %s\n", sender, args[0], args[1], args[2], args[3], args[4])

	from, err := hex.DecodeString(string(args[0]))
	isFromContract := false
	if err != nil {
		from = args[0]
		isFromContract = true
	}

	if !isFromContract && string(from) != string(sender) && !token._isApprovedForAll(from, sender) {
		return shim.Error(fmt.Sprintf("from(%s) is not token owner or approved", string(from)))
	}

	to, err := hex.DecodeString(string(args[1]))
	isToContract := false
	if err != nil {
		// 无法hex即为合约地址
		to = args[1]
		isToContract = true
	}

	ids := utils.ConverStringToBigintList(string(args[2]))
	amts := utils.ConverStringToBigintList(string(args[3]))

	msgData := args[4]

	return token._safeBatchTransferFrom(stub, sender, from, to, ids, amts, msgData, isFromContract, isToContract)
}

func (token *ERC1155TokenImpl) _safeBatchTransferFrom(stub shim.ChaincodeStubInterface, operator, from, to []byte, ids []*big.Int, amts []*big.Int, data []byte, isFromContract, isToContract bool) peer.Response {
	if utils.IsZeroBytes(to) {
		return shim.Error("transfer to zero addr")
	}

	if err := token._beforeTokenTransfer(stub, operator, from, to, ids, amts, data); err != nil {
		return shim.Error(fmt.Sprintf("failed to do beforeTokenTransfer: %v", err))
	}

	for i, id := range ids {
		amt := amts[i]
		// 判断amt为正数
		if amt.Sign() != 1 {
			return shim.Error("amount should be positive")
		}

		if err := token._transferLogic(stub, operator, from, to, id, amt); err != nil {
			var fromStr string
			var toStr string
			if isFromContract {
				fromStr = string(from)
			} else {
				fromStr = hex.EncodeToString(from)
			}
			if isToContract {
				toStr = string(to)
			} else {
				toStr = hex.EncodeToString(to)
			}
			return shim.Error(fmt.Sprintf("failed to do transfer: from %s, to %s, asset %s, amount %s: %v", fromStr, toStr, id.String(), amt.String(), err))
		}
	}

	token._afterTokenTransfer(stub, operator, from, to, ids, amts, data)

	if err := token._doSafeBatchTransferAcceptanceCheck(stub, operator, from, to, ids, amts, data, isToContract); err != nil {
		return shim.Error(fmt.Sprintf("failed to do SafeBatchTransferAcceptanceCheck: %v", err))
	}

	return shim.Success(nil)
}

func (token *ERC1155TokenImpl) _transferLogic(stub shim.ChaincodeStubInterface, operator, from, to []byte, id, amt *big.Int) error {
	fmt.Printf("_transferLogic, hexOperator:%s, hexFrom: %s, hexTo: %s\n", hex.EncodeToString(operator), hex.EncodeToString(from), hex.EncodeToString(to))

	// 获取来源账户资产余额
	fromKey := balanceKey(from, id)
	rawFromBalance, err := stub.GetState(fromKey)
	if err != nil {
		return fmt.Errorf(fmt.Sprintf("failed To get From balance: %v", err))
	}
	fromBal := big.NewInt(0).SetBytes(rawFromBalance)

	// 来源账户资产减少
	fromBal = fromBal.Sub(fromBal, amt)
	if fromBal.Sign() == -1 {
		sum := fromBal.Add(fromBal, amt)
		return fmt.Errorf(fmt.Sprintf("From balance %s is less than the amount %s", sum.String(), amt.String()))
	}

	// 获取目的账户资产余额
	toKey := balanceKey(to, id)
	rawToBalance, err := stub.GetState(toKey)
	if err != nil {
		return fmt.Errorf(fmt.Sprintf("failed To get receive account balance: %v", err))
	}
	toBal := big.NewInt(0).SetBytes(rawToBalance)

	// 目的账户资产余额增加
	toBal = toBal.Add(toBal, amt)

	// 更新来源账户余额
	if fromBal.Sign() == 0 {
		if err := stub.DelState(fromKey); err != nil {
			return fmt.Errorf(fmt.Sprintf("failed To delete balance for From account: %v", err))
		}
	} else {
		if err := stub.PutState(fromKey, fromBal.Bytes()); err != nil {
			return fmt.Errorf(fmt.Sprintf("failed To put balance for From account: %v", err))
		}
	}

	// 更新目的账户余额
	if err := stub.PutState(toKey, toBal.Bytes()); err != nil {
		return fmt.Errorf(fmt.Sprintf("failed To put balance for receiver account: %v", err))
	}

	// 抛出转账事件
	rawEvent, err := json.Marshal(&TransferSingleEvent{
		Operator: operator,
		From:     from,
		To:       to,
		Id:       id.Bytes(),
		Amount:   amt.Bytes(),
	})
	if err != nil {
		return fmt.Errorf(fmt.Sprintf("failed to json marshal: %v", err))
	}
	if err := stub.SetEvent(fmt.Sprintf(EventTranferSingle, hex.EncodeToString(id.Bytes())), rawEvent); err != nil {
		return fmt.Errorf(fmt.Sprintf("failed to set event: %v", err))
	}

	return nil
}

func (token *ERC1155TokenImpl) _afterTokenTransfer(stub shim.ChaincodeStubInterface, operator []byte, from []byte, to []byte, ids []*big.Int, amts []*big.Int, data []byte) {
}

func (token *ERC1155TokenImpl) _doSafeTransferAcceptanceCheck(stub shim.ChaincodeStubInterface,
	operator []byte, from []byte, to []byte, id *big.Int, amt *big.Int, data []byte, isFromContract, isToContract bool) error {
	if isToContract {
		rawName, err := stub.GetState(TokenName)
		if err != nil {
			return fmt.Errorf("failed to get token name: %v", err)
		}

		var fromStr string
		if isFromContract {
			fromStr = string(from)
		} else {
			fromStr = hex.EncodeToString(from)
		}

		result := stub.InvokeChaincode(
			string(to),
			[][]byte{
				[]byte("onERC1155Received"),
				[]byte(hex.EncodeToString(operator)),
				[]byte(fromStr), // from account(hex string)
				rawName,         // from contract(chaincode name)
				id.Bytes(),
				amt.Bytes(),
				data,
			},
			stub.GetChannelID())
		if result.Status != shim.OK {
			return fmt.Errorf("Failed to invoke chaincode %s: %s", string(to), result.String())
		}
	}
	return nil
}

func (token *ERC1155TokenImpl) _doSafeBatchTransferAcceptanceCheck(stub shim.ChaincodeStubInterface, operator []byte, from []byte, to []byte, ids []*big.Int, amts []*big.Int, data []byte, isToContract bool) error {
	if isToContract {
		strIds := make([]string, 0)
		strAmts := make([]string, 0)
		for i, id := range ids {
			strIds = append(strIds, id.String())
			strAmts = append(strAmts, amts[i].String())
		}
		result := stub.InvokeChaincode(
			string(to),
			[][]byte{
				[]byte("onERC1155BatchReceived"),
				[]byte(hex.EncodeToString(operator)),
				[]byte(hex.EncodeToString(from)),
				[]byte(strings.Join(strIds, ",")),
				[]byte(strings.Join(strAmts, ",")),
				data,
			},
			stub.GetChannelID())
		if result.Status != shim.OK {
			return fmt.Errorf("Failed to invoke chaincode %s: %s", string(to), result.String())
		}
	}
	return nil
}

func (token *ERC1155TokenImpl) getURI(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	if len(args) != 1 {
		return shim.Error("number of args should be 1")
	}

	id, ok := big.NewInt(0).SetString(string(args[0]), 10)
	if !ok {
		return shim.Error(fmt.Sprintf("failed to decode asset id: %s", args[2]))
	}

	uri, err := stub.GetState(TokenURI)
	if err != nil {
		return shim.Error(fmt.Sprintf("failed to get uri: %v", err))
	}

	return shim.Success([]byte(fmt.Sprintf("%s%s", string(uri), id.String())))
}

func (token *ERC1155TokenImpl) setURI(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	if len(args) != 1 {
		return shim.Error("number of args should be 1")
	}

	if err := stub.PutState(TokenURI, args[0]); err != nil {
		return shim.Error(fmt.Sprintf("failed to set uri: %v", err))
	}

	return shim.Success(nil)
}

func (token *ERC1155TokenImpl) mint(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	// account, assetId, amount, data
	if len(args) != 4 {
		return shim.Error("number of args should be 4")
	}

	account, err := hex.DecodeString(string(args[0]))
	isToContract := false
	if err != nil {
		account = args[0]
		isToContract = true
	}
	if utils.IsZeroBytes(account) {
		return shim.Error("mint to the zero address")
	}

	id, ok := big.NewInt(0).SetString(string(args[1]), 10)
	if !ok {
		return shim.Error(fmt.Sprintf("failed to decode asset id: %s", args[1]))
	}

	amt, ok := big.NewInt(0).SetString(string(args[2]), 10)
	if !ok {
		return shim.Error(fmt.Sprintf("failed to decode amount: %s", args[2]))
	}
	if amt.Sign() != 1 {
		return shim.Error("amount should be positive")
	}

	data := args[3]

	operator, err := utils.GetMsgSenderAddress(stub)
	if err != nil {
		return shim.Error(fmt.Sprintf("failed To get tx sender: %v", err))
	}

	return token._mint(stub, operator, account, id, amt, data, isToContract)
}

func (token *ERC1155TokenImpl) _mint(stub shim.ChaincodeStubInterface, operator, account []byte, id, amt *big.Int, data []byte, isToContract bool) peer.Response {
	ids := utils.AsSingletonArray(id)
	amts := utils.AsSingletonArray(amt)

	if err := token._beforeTokenTransfer(stub, operator, []byte("0"), account, ids, amts, data); err != nil {
		return shim.Error(fmt.Sprintf("failed to do beforeTokenTransfer: %v", err))
	}

	if err := token._mintLogic(stub, operator, []byte("0"), account, id, amt); err != nil {
		return shim.Error(fmt.Sprintf("failed to do transfer: from %s, to %s, asset %s, amount %s: %v", string("0"), string(account), id.String(), amt.String(), err))
	}

	token._afterTokenTransfer(stub, operator, []byte("0"), account, ids, amts, data)

	if err := token._doSafeTransferAcceptanceCheck(stub, operator, []byte("0"), account, id, amt, data, true, isToContract); err != nil {
		return shim.Error(err.Error())
	}

	return shim.Success(nil)
}

func (token *ERC1155TokenImpl) _mintLogic(stub shim.ChaincodeStubInterface, operator, from, to []byte, id, amt *big.Int) error {
	// 获取目的账户资产余额
	toKey := balanceKey(to, id)
	rawToBalance, err := stub.GetState(toKey)
	if err != nil {
		return fmt.Errorf(fmt.Sprintf("failed To get receive account balance: %v", err))
	}
	toBal := big.NewInt(0).SetBytes(rawToBalance)

	// 目的账户资产余额增加
	toBal = toBal.Add(toBal, amt)

	// 更新目的账户余额
	if err := stub.PutState(toKey, toBal.Bytes()); err != nil {
		return fmt.Errorf(fmt.Sprintf("failed To put balance for receiver account: %v", err))
	}

	// 抛出转账事件
	rawEvent, err := json.Marshal(&TransferSingleEvent{
		Operator: operator,
		From:     from,
		To:       to,
		Id:       id.Bytes(),
		Amount:   amt.Bytes(),
	})
	if err != nil {
		return fmt.Errorf(fmt.Sprintf("failed to json marshal: %v", err))
	}
	if err := stub.SetEvent(fmt.Sprintf(EventTranferSingle, hex.EncodeToString(id.Bytes())), rawEvent); err != nil {
		return fmt.Errorf(fmt.Sprintf("failed to set event: %v", err))
	}

	return nil
}

/*func (token *ERC1155TokenImpl) mintBatch(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	if len(args) != 4 {
		return shim.Error("number of args should be 4")
	}

	account, err := hex.DecodeString(string(args[0]))
	isToContract := false
	if err != nil {
		account = args[0]
		isToContract = true
	}
	if bytes.Equal(account, utils.ZeroAddress()) {
		return shim.Error("mint to the zero address")
	}

	strIds := strings.Split(string(args[2]), ",")
	strAmts := strings.Split(string(args[3]), ",")
	if len(strIds) != len(strAmts) {
		return shim.Error(fmt.Sprintf("ids(%d) and amounts(%d) length mismatch", len(strIds), len(strAmts)))
	}

	ids := make([]*big.Int, 0)
	for _, strId := range strIds {
		tmpId, ok := big.NewInt(0).SetString(strId, 10)
		if !ok {
			return shim.Error(fmt.Sprintf("failed to decode asset id: %s", strId))
		}
		ids = append(ids, tmpId)
	}

	amts := make([]*big.Int, 0)
	for _, strAmt := range strAmts {
		tmpAmt, ok := big.NewInt(0).SetString(strAmt, 10)
		if !ok {
			return shim.Error(fmt.Sprintf("failed to decode amount: %s", strAmt))
		}
		amts = append(amts, tmpAmt)
	}

	data := args[3]

	operator, err := utils.GetMsgSenderAddress(stub)
	if err != nil {
		return shim.Error(fmt.Sprintf("failed To get tx sender: %v", err))
	}

	return token._mintBatch(stub, operator, account, ids, amts, data, isToContract)
}*/

func (token *ERC1155TokenImpl) _mintBatch(stub shim.ChaincodeStubInterface, operator, account []byte, ids, amts []*big.Int, data []byte, isToContract bool) peer.Response {

	if err := token._beforeTokenTransfer(stub, operator, []byte("0"), account, ids, amts, data); err != nil {
		return shim.Error(fmt.Sprintf("failed to do beforeTokenTransfer: %v", err))
	}

	for i := 0; i < len(ids); i++ {
		if err := token._mintLogic(stub, operator, []byte("0"), account, ids[i], amts[i]); err != nil {
			return shim.Error(fmt.Sprintf("failed to do transfer: from %s, to %s, asset %s, amount %s: %v", string("0"), string(account), ids[i].String(), amts[i].String(), err))
		}
	}

	token._afterTokenTransfer(stub, operator, []byte("0"), account, ids, amts, data)

	if err := token._doSafeBatchTransferAcceptanceCheck(stub, operator, []byte("0"), account, ids, amts, data, isToContract); err != nil {
		return shim.Error(err.Error())
	}

	return shim.Success(nil)
}

/*func (token *ERC1155TokenImpl) burn(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	//TODO implement me
	panic("implement me")
}

func (token *ERC1155TokenImpl) burnBatch(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	//TODO implement me
	panic("implement me")
}*/

// ClientAccountID returns the id of the requesting client's account
// In this implementation, the client account ID is the clientId itself
// Users can use this function to get their own account id, which they can then give to others as the payment address
func (token *ERC1155TokenImpl) clientAccountID(stub shim.ChaincodeStubInterface) peer.Response {
	operator, err := utils.GetMsgSenderAddress(stub)
	if err != nil {
		return shim.Error(fmt.Sprintf("failed To get tx sender: %v", err))
	}

	return shim.Success([]byte(hex.EncodeToString(operator)))
}

// 锚定资产合约增加接口 ==================================
func (token *ERC1155TokenImpl) mintBatchByTB(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	if len(args) != 3 {
		return shim.Error("number of args should be 3")
	}

	fmt.Printf("mintBatchByTB start\n")
	account, err := hex.DecodeString(string(args[0]))
	isToContract := false
	if err != nil {
		account = args[0]
		isToContract = true
	}
	if utils.IsZeroBytes(account) {
		return shim.Error("mint to the zero address")
	}

	strIds := strings.Split(string(args[1]), ",")
	strAmts := strings.Split(string(args[2]), ",")
	if len(strIds) != len(strAmts) {
		return shim.Error(fmt.Sprintf("ids(%d) and amounts(%d) length mismatch", len(strIds), len(strAmts)))
	}

	ids := make([]*big.Int, 0)
	for _, strId := range strIds {
		tmpId, ok := big.NewInt(0).SetString(strId, 10)
		if !ok {
			return shim.Error(fmt.Sprintf("failed to decode asset id: %s", strId))
		}
		ids = append(ids, tmpId)
	}

	amts := make([]*big.Int, 0)
	for _, strAmt := range strAmts {
		tmpAmt, ok := big.NewInt(0).SetString(strAmt, 10)
		if !ok {
			return shim.Error(fmt.Sprintf("failed to decode amount: %s", strAmt))
		}
		amts = append(amts, tmpAmt)
	}

	operator, err := utils.GetMsgSenderAddress(stub)
	if err != nil {
		return shim.Error(fmt.Sprintf("failed To get tx sender: %v", err))
	}

	fmt.Printf("mintBatchByTB, operator: %s, to: %s, isToContract: %v\n", hex.EncodeToString(operator), args[0], isToContract)
	return token._mintBatch(stub, operator, account, ids, amts, nil, isToContract)
}

// ERC1155Supply ======================================
func (token *ERC1155TokenImpl) totalSupply(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	//TODO implement me
	panic("implement me")
}

func (token *ERC1155TokenImpl) exists(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	//TODO implement me
	panic("implement me")
}

func (token *ERC1155TokenImpl) _beforeTokenTransfer(stub shim.ChaincodeStubInterface, operator []byte, from []byte, to []byte, ids []*big.Int, amts []*big.Int, data []byte) error {
	// 铸造资产
	if utils.IsZeroBytes(from) {
		for i := 0; i < len(ids); i++ {
			key := supplyKey(ids[i])

			rawSupply, err := stub.GetState(key)
			if err != nil {
				return fmt.Errorf("failed to get totalsupply: %v", err)
			}

			ts := big.NewInt(0)
			if !utils.IsZeroBytes(rawSupply) {
				ts = ts.SetBytes(rawSupply)
			}

			ts = ts.Add(ts, amts[i])
			if err := stub.PutState(key, ts.Bytes()); err != nil {
				return fmt.Errorf("failed to update totalsupply: %v", err)
			}
		}
	}

	// 销毁资产
	if utils.IsZeroBytes(to) {
		for i := 0; i < len(ids); i++ {
			key := supplyKey(ids[i])

			rawSupply, err := stub.GetState(key)
			if err != nil {
				return fmt.Errorf("failed to get totalsupply: %v", err)
			}

			ts := big.NewInt(0)
			if !utils.IsZeroBytes(rawSupply) {
				ts = ts.SetBytes(rawSupply)
			}

			ts = ts.Sub(ts, amts[i])
			if err := stub.PutState(key, ts.Bytes()); err != nil {
				return fmt.Errorf("failed to update totalsupply: %v", err)
			}
		}
	}
	return nil
}

// ===================================================================
// ===================================================================
// ===================================================================

func balanceKey(account []byte, assetId *big.Int) string {
	return fmt.Sprintf(TokenBalance, hex.EncodeToString(account), assetId.String())
}

func supplyKey(id *big.Int) string {
	return fmt.Sprintf(TokenTotalSupply, id)
}

// test contract ===========================================================
func (token *ERC1155TokenImpl) testSafeTransferFrom(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response {
	// from(fromAccount), to(TBContract), assetId, amount, toDomain, toAccount
	if len(args) != 6 {
		return shim.Error("number of transferLogic args should be 6")
	}

	rawAccount, _ := hex.DecodeString(string(args[5]))
	transferData, err := utils.PackTransferMsg(args[4], utils.CopySliceToByte32(rawAccount))
	if err != nil {
		return shim.Error(fmt.Sprintf("failed to pack transfer msg: %v", err))
	}

	newArgs := [][]byte{args[0], args[1], args[2], args[3], []byte(hex.EncodeToString(transferData))}
	return token.safeTransferFrom(stub, newArgs)
}
