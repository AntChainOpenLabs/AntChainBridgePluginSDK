#!/bin/bash

CURR_DIR="$(cd `dirname $0`; pwd)"
source ${CURR_DIR}/print.sh

print_title

cd ${CURR_DIR}/../pluginset/ethereum/onchain-plugin

tar -zcf ${CURR_DIR}/sys-contracts-sol_0.1.4-SNAPSHOT.tar.gz --exclude='solidity/scenarios' --exclude='solidity/sys/artifacts' --exclude='solidity/sys/interfaces/artifacts' --exclude='solidity/sys/lib/am/artifacts' --exclude='solidity/sys/lib/sdp/artifacts' --exclude='solidity/sys/lib/utils/artifacts' solidity 
if [ $? -ne 0 ]; then
    log_error "failed to tar the contracts"
    exit 1
fi
log_info "success"