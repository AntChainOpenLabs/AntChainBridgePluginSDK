#!/bin/bash

CURR_DIR="$(cd `dirname $0`; pwd)"
source ${CURR_DIR}/print.sh

print_title

cd ${CURR_DIR}/../antchain-bridge-commons
mvn clean install -Dmaven.test.skip=true > /dev/null 2>&1
if [ $? -ne 0 ]; then
    log_error "failed to build antchain-bridge-commons"
    exit 1
fi
log_info "build antchain-bridge-commons successfully"
cd - > /dev/null 2>&1

cd ${CURR_DIR}/../antchain-bridge-spi
mvn clean install -Dmaven.test.skip=true > /dev/null 2>&1
if [ $? -ne 0 ]; then
    log_error "failed to build antchain-bridge-spi"
    exit 1
fi
log_info "build antchain-bridge-spi successfully"
cd - > /dev/null 2>&1

cd ${CURR_DIR}/../antchain-bridge-plugin-lib
mvn clean install -Dmaven.test.skip=true > /dev/null 2>&1
if [ $? -ne 0 ]; then
    log_error "failed to build antchain-bridge-plugin-lib"
    exit 1
fi
log_info "build antchain-bridge-plugin-lib successfully"
cd - > /dev/null 2>&1

cd ${CURR_DIR}/../antchain-bridge-plugin-manager
mvn clean install -Dmaven.test.skip=true > /dev/null 2>&1
if [ $? -ne 0 ]; then
    log_error "failed to build antchain-bridge-plugin-manager"
    exit 1
fi
log_info "build antchain-bridge-plugin-manager successfully"
cd - > /dev/null 2>&1

if [ -d "${CURR_DIR}/antchain-bridge-sdk" ]; then
    rm -rf ${CURR_DIR}/antchain-bridge-sdk
fi

mkdir -p ${CURR_DIR}/antchain-bridge-sdk/libs

cp ${CURR_DIR}/../antchain-bridge-commons/target/antchain-bridge-commons*.jar ${CURR_DIR}/antchain-bridge-sdk/libs
if [ $? -ne 0 ]; then
    log_error "failed to copy antchain-bridge-commons"
    exit 1
fi
cp ${CURR_DIR}/../antchain-bridge-spi/target/antchain-bridge-spi*.jar ${CURR_DIR}/antchain-bridge-sdk/libs
if [ $? -ne 0 ]; then
    log_error "failed to copy antchain-bridge-spi"
    exit 1
fi
cp ${CURR_DIR}/../antchain-bridge-plugin-lib/target/antchain-bridge-plugin-lib*.jar ${CURR_DIR}/antchain-bridge-sdk/libs
if [ $? -ne 0 ]; then
    log_error "failed to copy antchain-bridge-plugin-lib"
    exit 1
fi
cp ${CURR_DIR}/../antchain-bridge-plugin-manager/target/antchain-bridge-plugin-manager*.jar ${CURR_DIR}/antchain-bridge-sdk/libs
if [ $? -ne 0 ]; then
    log_error "failed to copy antchain-bridge-plugin-manager"
    exit 1
fi

cp ${CURR_DIR}/install_sdk.sh ${CURR_DIR}/antchain-bridge-sdk/
cp ${CURR_DIR}/print.sh ${CURR_DIR}/antchain-bridge-sdk/libs/
cd ${CURR_DIR}/
tar -zcf antchain-bridge-sdk.tar.gz antchain-bridge-sdk
if [ $? -ne 0 ]; then
    log_error "failed to tar the sdk"
    exit 1
fi
log_info "success"
rm -rf antchain-bridge-sdk