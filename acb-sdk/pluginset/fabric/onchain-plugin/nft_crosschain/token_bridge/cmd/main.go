package main

import (
	"fabric_nft_crosschain/token_bridge"
	"github.com/hyperledger/fabric-chaincode-go/shim"
)

func main() {
	err := shim.Start(new(token_bridge.TokenBridge))
	if err != nil {
		panic(err)
	}
}
