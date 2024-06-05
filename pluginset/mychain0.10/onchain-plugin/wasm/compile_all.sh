#!/bin/sh
CUR_DIR=`pwd`
for i in `ls schema/*.fbs`; do myflatc.sh ./schema/ $i; done
for i in `ls *.cpp`; do my++ $i -o ${i%.*}.wasm; done
# 通过宏，编译出针对tee的合约
my++ auth_message.cpp -DMYCHAIN_TEE -o auth_message_tee.wasm
mv *.wasc ${CUR_DIR}/../../offchain-plugin/src/main/resources/contract/1.5.0/wasm/
rm *.wasm *.abi
