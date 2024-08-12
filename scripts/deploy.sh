#!/bin/bash

CURR_DIR="$(cd `dirname $0`; pwd)"
source ${CURR_DIR}/print.sh

print_title

cd ${CURR_DIR}/../antchain-bridge-commons
mvn deploy -s $GITHUB_WORKSPACE/settings.xml
if [ $? -ne 0 ]; then
    log_error "failed to deploy antchain-bridge-commons"
    exit 1
fi
log_info "deploy antchain-bridge-commons successfully"
cd - > /dev/null 2>&1

cd ${CURR_DIR}/../antchain-bridge-spi
mvn deploy -s $GITHUB_WORKSPACE/settings.xml
if [ $? -ne 0 ]; then
    log_error "failed to deploy antchain-bridge-spi"
    exit 1
fi
log_info "deploy antchain-bridge-spi successfully"
cd - > /dev/null 2>&1

cd ${CURR_DIR}/../antchain-bridge-plugin-lib
mvn deploy -s $GITHUB_WORKSPACE/settings.xml
if [ $? -ne 0 ]; then
    log_error "failed to deploy antchain-bridge-plugin-lib"
    exit 1
fi
log_info "deploy antchain-bridge-plugin-lib successfully"
cd - > /dev/null 2>&1

cd ${CURR_DIR}/../antchain-bridge-plugin-manager
mvn deploy -s $GITHUB_WORKSPACE/settings.xml
if [ $? -ne 0 ]; then
    log_error "failed to deploy antchain-bridge-plugin-manager"
    exit 1
fi
log_info "deploy antchain-bridge-plugin-manager successfully"
cd - > /dev/null 2>&1

cd ${CURR_DIR}/../antchain-bridge-bcdns
mvn deploy -s $GITHUB_WORKSPACE/settings.xml
if [ $? -ne 0 ]; then
    log_error "failed to deploy antchain-bridge-bcdns"
    exit 1
fi
log_info "deploy antchain-bridge-bcdns successfully"
cd - > /dev/null 2>&1

cd ${CURR_DIR}/../bcdns-services/embedded-bcdns/embedded-bcdns-core
mvn deploy -s $GITHUB_WORKSPACE/settings.xml
if [ $? -ne 0 ]; then
    log_error "failed to deploy embedded-bcdns-core"
    exit 1
fi
log_info "deploy embedded-bcdns-core successfully"
cd - > /dev/null 2>&1

cd ${CURR_DIR}/../antchain-bridge-bcdns-factory
mvn deploy -s $GITHUB_WORKSPACE/settings.xml
if [ $? -ne 0 ]; then
    log_error "failed to deploy antchain-bridge-bcdns-factory"
    exit 1
fi
log_info "deploy antchain-bridge-bcdns-factory successfully"
cd - > /dev/null 2>&1

log_info "success"
rm -rf antchain-bridge-sdk