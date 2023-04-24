#!/bin/bash

CURR_DIR="$(cd `dirname $0`; pwd)"
source ${CURR_DIR}/libs/print.sh

print_title

mvn install:install-file -Dfile=${CURR_DIR}/libs/antchain-bridge-commons-0.1-SNAPSHOT.jar -DgroupId=com.alipay.antchain.bridge -DartifactId=antchain-bridge-commons -Dversion=0.1-SNAPSHOT -Dpackaging=jar > /dev/null 2>&1
if [ $? -ne 0 ]; then
    log_error "failed to copy antchain-bridge-commons-0.1-SNAPSHOT.jar"
    exit 1
fi
log_info "successful to install antchain-bridge-commons-0.1-SNAPSHOT.jar"

mvn install:install-file -Dfile=${CURR_DIR}/libs/antchain-bridge-spi-0.1-SNAPSHOT.jar -DgroupId=com.alipay.antchain.bridge -DartifactId=antchain-bridge-spi -Dversion=0.1-SNAPSHOT -Dpackaging=jar > /dev/null 2>&1
if [ $? -ne 0 ]; then
    log_error "failed to copy antchain-bridge-spi-0.1-SNAPSHOT.jar"
    exit 1
fi
log_info "successful to install antchain-bridge-spi-0.1-SNAPSHOT.jar"

mvn install:install-file -Dfile=${CURR_DIR}/libs/antchain-bridge-plugin-lib-0.1-SNAPSHOT.jar -DgroupId=com.alipay.antchain.bridge -DartifactId=antchain-bridge-plugin-lib -Dversion=0.1-SNAPSHOT -Dpackaging=jar > /dev/null 2>&1
if [ $? -ne 0 ]; then
    log_error "failed to copy antchain-bridge-plugin-lib-0.1-SNAPSHOT.jar"
    exit 1
fi
log_info "successful to install antchain-bridge-plugin-lib-0.1-SNAPSHOT.jar"

mvn install:install-file -Dfile=${CURR_DIR}/libs/antchain-bridge-plugin-manager-0.1-SNAPSHOT.jar -DgroupId=com.alipay.antchain.bridge -DartifactId=antchain-bridge-plugin-manager -Dversion=0.1-SNAPSHOT -Dpackaging=jar > /dev/null 2>&1
if [ $? -ne 0 ]; then
    log_error "failed to copy antchain-bridge-plugin-manager-0.1-SNAPSHOT.jar"
    exit 1
fi
log_info "successful to install antchain-bridge-plugin-manager-0.1-SNAPSHOT.jar"

log_info "success"