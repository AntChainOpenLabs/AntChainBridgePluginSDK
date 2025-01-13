package utils

import (
	"crypto/sha256"
	"fmt"
	"github.com/hyperledger/fabric-chaincode-go/pkg/cid"
	"github.com/hyperledger/fabric-chaincode-go/shim"
	"math/big"
	"strings"
)

func GetMsgSenderAddress(stub shim.ChaincodeStubInterface) ([]byte, error) {
	cert, err := cid.GetX509Certificate(stub)
	if err != nil {
		return nil, fmt.Errorf("failed to parse CA: %v", err)
	}
	return GetAddrFromRaw(cert.RawSubjectPublicKeyInfo), nil
}

func GetAddrFromRaw(raw []byte) []byte {
	hash := sha256.New()
	hash.Write(raw)
	return hash.Sum(nil)
}

func IsZeroBytes(addr []byte) bool {
	return addr == nil || len(addr) == 0
}

func AsSingletonArray(element *big.Int) []*big.Int {
	array := make([]*big.Int, 1)
	array[0] = element
	return array
}

// 将big.Int的slice转换为","隔开的字符串
func ConverBigintListToString(items []*big.Int) string {
	strList := make([]string, 0)
	for _, item := range items {
		strList = append(strList, item.String())
	}
	return strings.Join(strList, ",")
}

func ConverStringToBigintList(str string) []*big.Int {
	strList := strings.Split(str, ",")
	retList := make([]*big.Int, 0)
	for _, s := range strList {
		tmp, _ := big.NewInt(0).SetString(s, 10)
		retList = append(retList, tmp)
	}
	return retList
}

func GetChaincodeNameHash(name []byte) []byte {
	nameHash := sha256.Sum256(name)
	return nameHash[:]
}

func CopySliceToByte32(src []byte) [32]byte {
	var dst [32]byte
	for i := uint32(0); i < 32; i++ {
		dst[i] = src[i]
	}
	return dst
}
