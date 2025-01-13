#!/bin/bash

#
# Copyright 2023 Ant Group
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

CURR_DIR="$(
  cd $(dirname $0)
  pwd
)"
source ${CURR_DIR}/print.sh

print_title

if [ ! -d ${CURR_DIR}/../certs ]; then
  mkdir -p ${CURR_DIR}/../certs
fi

# rsa 或 sm2，默认为 rsa
CRYPTO_SUITE=$1
if [ -z $CRYPTO_SUITE ]; then
  CRYPTO_SUITE="rsa"
fi
log_info "插件服务密钥算法：${CRYPTO_SUITE}"

if [ ${CRYPTO_SUITE} == "rsa" ]; then
  openssl genrsa -out ${CURR_DIR}/../certs/server.key 2048 >/dev/null 2>&1
  if [ $? -ne 0 ]; then
    log_error "failed to generate server.key"
    exit 1
  fi
elif [ ${CRYPTO_SUITE} == "sm2" ]; then
  # 获取OpenSSL版本
  version=$(openssl version | cut -d' ' -f2)

  # 定义一个函数用于比较版本号
  version_gt() { test "$(printf '%s\n' "$@" | sort -V | head -n 1)" != "$1"; }

  # 检查版本是否高于1.1.1
  if version_gt $version "1.1.1"; then
    log_info "OpenSSL version is greater than 1.1.1. You version: $version"
  else
    log_error "OpenSSL version is not greater than 1.1.1. Your version: $version"
    exit -1
  fi

  # 生成国密密钥
  openssl ecparam -genkey -name SM2 -out ${CURR_DIR}/../certs/server.key >/dev/null 2>&1
  if [ $? -ne 0 ]; then
    log_error "failed to generate server.key"
    exit 1
  fi
else
  log_error "key algorithm ${CRYPTO_SUITE} is not supported"
fi

openssl pkcs8 -topk8 -inform pem -in ${CURR_DIR}/../certs/server.key -nocrypt -out ${CURR_DIR}/../certs/server_pkcs8.key
if [ $? -ne 0 ]; then
  log_error "failed to generate pkcs8 server.key"
  exit 1
fi
mv ${CURR_DIR}/../certs/server_pkcs8.key ${CURR_DIR}/../certs/server.key
log_info "generate server.key successfully"

openssl req -new -x509 -days 36500 -key ${CURR_DIR}/../certs/server.key -out ${CURR_DIR}/../certs/server.crt -subj "/C=CN/ST=mykey/L=mykey/O=mykey/OU=mykey/CN=pluginserver"
if [ $? -ne 0 ]; then
  log_error "failed to generate server.crt"
  exit 1
fi
log_info "generate server.crt successfully"

if [ ! -f "trust.crt" ]; then
  cp ${CURR_DIR}/../certs/server.crt ${CURR_DIR}/../certs/trust.crt
  log_info "generate trust.crt successfully"
fi

openssl pkcs12 -export -in ${CURR_DIR}/../certs/server.crt -inkey ${CURR_DIR}/../certs/server.key -out ${CURR_DIR}/../certs/cert.pfx -passout pass: