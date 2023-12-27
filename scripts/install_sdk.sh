#!/bin/bash

CURR_DIR="$(cd `dirname $0`; pwd)"
source ${CURR_DIR}/print.sh

print_title

mvn install:install-file -Dfile=${CURR_DIR}/../libs/antchain-bridge-commons-${SDK_VERSION}.jar -DgroupId=com.alipay.antchain.bridge -DartifactId=antchain-bridge-commons -Dversion=${SDK_VERSION} -Dpackaging=jar > /dev/null 2>&1
if [ $? -ne 0 ]; then
    log_error "failed to install antchain-bridge-commons-${SDK_VERSION}.jar"
    exit 1
fi
log_info "successful to install antchain-bridge-commons-${SDK_VERSION}.jar"

mvn install:install-file -Dfile=${CURR_DIR}/../libs/antchain-bridge-spi-${SDK_VERSION}.jar -DgroupId=com.alipay.antchain.bridge -DartifactId=antchain-bridge-spi -Dversion=${SDK_VERSION} -Dpackaging=jar > /dev/null 2>&1
if [ $? -ne 0 ]; then
    log_error "failed to install antchain-bridge-spi-${SDK_VERSION}.jar"
    exit 1
fi
log_info "successful to install antchain-bridge-spi-${SDK_VERSION}.jar"

mvn install:install-file -Dfile=${CURR_DIR}/../libs/antchain-bridge-plugin-lib-${SDK_VERSION}.jar -DgroupId=com.alipay.antchain.bridge -DartifactId=antchain-bridge-plugin-lib -Dversion=${SDK_VERSION} -Dpackaging=jar > /dev/null 2>&1
if [ $? -ne 0 ]; then
    log_error "failed to install antchain-bridge-plugin-lib-${SDK_VERSION}.jar"
    exit 1
fi
log_info "successful to install antchain-bridge-plugin-lib-${SDK_VERSION}.jar"

mvn install:install-file -Dfile=${CURR_DIR}/../libs/antchain-bridge-plugin-manager-${SDK_VERSION}.jar -DgroupId=com.alipay.antchain.bridge -DartifactId=antchain-bridge-plugin-manager -Dversion=${SDK_VERSION} -Dpackaging=jar > /dev/null 2>&1
if [ $? -ne 0 ]; then
    log_error "failed to install antchain-bridge-plugin-manager-${SDK_VERSION}.jar"
    exit 1
fi
log_info "successful to install antchain-bridge-plugin-manager-${SDK_VERSION}.jar"

mvn install:install-file -Dfile=${CURR_DIR}/../libs/antchain-bridge-bcdns-${SDK_VERSION}.jar -DgroupId=com.alipay.antchain.bridge -DartifactId=antchain-bridge-bcdns -Dversion=${SDK_VERSION} -Dpackaging=jar > /dev/null 2>&1
if [ $? -ne 0 ]; then
    log_error "failed to copy antchain-bridge-bcdns-${SDK_VERSION}.jar"
    exit 1
fi
log_info "successful to install antchain-bridge-bcdns-${SDK_VERSION}.jar"

log_info "success"