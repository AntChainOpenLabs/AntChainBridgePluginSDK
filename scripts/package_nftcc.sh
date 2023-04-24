#!/bin/bash

CURR_DIR="$(cd `dirname $0`; pwd)"
source ${CURR_DIR}/print.sh

print_title

cd ${CURR_DIR}/../pluginset/ethereum/onchain-plugin/solidity/scenarios/

rm -rf **/artifacts
tar -zcf ${CURR_DIR}/nft-crosschain-contracts-sol_0.1.4-SNAPSHOT.tar.gz nft_crosschain 
if [ $? -ne 0 ]; then
    log_error "failed to tar the contracts"
    exit 1
fi
log_info "success"