package utils

import (
	_ "bytes"
	"encoding/hex"
	"fmt"
	"github.com/stretchr/testify/assert"
	"math/big"

	"testing"
)

func TestPackTransferMsg_UnpackTransferMsg(t *testing.T) {
	dstDomain := "dstDomain"
	var holder [32]byte
	rawHolder, _ := hex.DecodeString("a0db994b1520f62c27f57bba911f0080677f0f8ff169f8e66fd4743812df4f3a")
	holder = CopySliceToByte32(rawHolder)
	fmt.Println(len(rawHolder))

	packedData, err := PackTransferMsg([]byte(dstDomain), holder)
	assert.Nil(t, err)

	transferMsg, err := UnpackTransferMsg(packedData)
	assert.Nil(t, err)
	assert.Equal(t, dstDomain, string(transferMsg.DstDomain))
	assert.Equal(t, holder, transferMsg.Holder)
}

func TestPackCrossChainMsg_UnpackCrossChainMsg(t *testing.T) {
	crossChainMsg := CrossChainMsg{
		Ids:              []*big.Int{big.NewInt(1), big.NewInt(2)},
		Amounts:          []*big.Int{big.NewInt(10), big.NewInt(20)},
		SrcAssetContract: CopySliceToByte32([]byte("a1db994b1520f62c27f57bba911f0080677f0f8ff169f8e66fd4743812df4f3a")),
		DstAssetContract: CopySliceToByte32([]byte("a2db994b1520f62c27f57bba911f0080677f0f8ff169f8e66fd4743812df4f3a")),
		Holder:           CopySliceToByte32([]byte("a3db994b1520f62c27f57bba911f0080677f0f8ff169f8e66fd4743812df4f3a")),
		Status:           uint8(CROSSCHAIN_START),
	}

	packedData, err := PackCrossChainMsg(
		crossChainMsg.Ids,
		crossChainMsg.Amounts,
		crossChainMsg.SrcAssetContract,
		crossChainMsg.DstAssetContract,
		crossChainMsg.Holder,
		crossChainMsg.Status)
	assert.Nil(t, err)

	unpackCrossChainMsg, err := UnpackCrossChainMsg(packedData)
	assert.Nil(t, err)
	assert.Equal(t, &crossChainMsg, unpackCrossChainMsg)
}
