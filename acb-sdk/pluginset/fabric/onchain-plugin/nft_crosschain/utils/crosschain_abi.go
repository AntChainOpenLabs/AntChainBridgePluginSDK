package utils

import (
	"fmt"
	"github.com/ethereum/go-ethereum/accounts/abi"
	"math/big"
	"strings"
)

type TransferMsg struct {
	DstDomain []byte
	Holder    [32]byte // hex string
}

type CrossChainMsg struct {
	Ids              []*big.Int
	Amounts          []*big.Int
	SrcAssetContract [32]byte // string or hex string
	DstAssetContract [32]byte // string or hex string
	Holder           [32]byte // hex string
	Status           uint8
}

type CrossChainStatus uint8

const (
	CROSSCHAIN_START CrossChainStatus = iota
	CROSSCHAIN_SUCCESS
	CROSSCHAIN_ERROR
)

const CrosschainMsgABI = `[
	{ "name" : "transferMsgPack", "type": "function", "inputs": [
		{"name":"DstDomain","type":"bytes"},
		{"name":"Holder","type":"bytes32"}]},
	{ "name" : "transferMsgUnpack", "type": "function", "outputs": [
		{"name":"DstDomain","type":"bytes"},
		{"name":"Holder","type":"bytes32"}]},
	{ "name" : "CrossChainMsgPack", "type": "function", "inputs": [
		{"name":"Ids","type":"uint256[]"},
		{"name":"Amounts","type":"uint256[]"},
		{"name":"SrcAssetContract","type":"bytes32"},
		{"name":"DstAssetContract","type":"bytes32"},
		{"name":"Holder","type":"bytes32"},
		{"name":"Status","type":"uint8"}]},
	{ "name" : "CrossChainMsgUnpack", "type": "function", "outputs": [
		{"name":"Ids","type":"uint256[]"},
		{"name":"Amounts","type":"uint256[]"},
		{"name":"SrcAssetContract","type":"bytes32"},
		{"name":"DstAssetContract","type":"bytes32"},
		{"name":"Holder","type":"bytes32"},
		{"name":"Status","type":"uint8"}]}
]`

// holder是hex字符串
func PackTransferMsg(dstDomain []byte, holder [32]byte) ([]byte, error) {
	crosschainMsgABI, err := abi.JSON(strings.NewReader(CrosschainMsgABI))
	if err != nil {
		return nil, fmt.Errorf("failed to create abi: %v", err)
	}

	packedData, err := crosschainMsgABI.Pack("transferMsgPack", dstDomain, holder)
	if err != nil {
		return nil, fmt.Errorf("failed to pack transfer msg: %v", err)
	}
	// 前4个字节是函数签名
	return packedData[4:], nil
}

func UnpackTransferMsg(transferPackedData []byte) (*TransferMsg, error) {
	crosschainMsgABI, err := abi.JSON(strings.NewReader(CrosschainMsgABI))
	if err != nil {
		return nil, fmt.Errorf("failed to create abi: %v", err)
	}

	var transferMsg TransferMsg
	if err := crosschainMsgABI.Unpack(&transferMsg, "transferMsgUnpack", transferPackedData); err != nil {
		return nil, fmt.Errorf("failed to unpack transfer msg: %v", err)
	}

	return &transferMsg, nil
}

func PackCrossChainMsg(
	ids, amounts []*big.Int,
	srcAssetContract, dstAsseetContract, holder [32]byte,
	status uint8) ([]byte, error) {
	crosschainMsgABI, err := abi.JSON(strings.NewReader(CrosschainMsgABI))
	if err != nil {
		return nil, fmt.Errorf("failed to create abi: %v", err)
	}

	packedData, err := crosschainMsgABI.Pack("CrossChainMsgPack", ids, amounts, srcAssetContract, dstAsseetContract, holder, status)
	if err != nil {
		return nil, fmt.Errorf("failed to pack crosschain msg: %v", err)
	}
	return packedData[4:], nil
}

func UnpackCrossChainMsg(crosschainPackedData []byte) (*CrossChainMsg, error) {
	crosschainMsgABI, err := abi.JSON(strings.NewReader(CrosschainMsgABI))
	if err != nil {
		return nil, fmt.Errorf("failed to create abi: %v", err)
	}

	var crosschainMsg CrossChainMsg
	if err := crosschainMsgABI.Unpack(&crosschainMsg, "CrossChainMsgUnpack", crosschainPackedData); err != nil {
		return nil, fmt.Errorf("failed to unpack crosschain msg: %v", err)
	}

	return &crosschainMsg, nil
}
