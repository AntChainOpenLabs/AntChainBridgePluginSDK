#! /bin/bash

rm *.pb.go
protoc -I=. -I=$GOPATH/src -I=$GOPATH/src/github.com/gogo/protobuf/protobuf --gofast_out=. chaincode.proto
