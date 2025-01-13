package main

import (
	"fabric_nft_crosschain/assets/token"
	"github.com/hyperledger/fabric-chaincode-go/shim"
)

func main() {
	err := shim.Start(new(token.ERC1155TokenImpl))
	if err != nil {
		panic(err)
	}
}
