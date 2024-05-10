#!/bin/bash

# 读取文件内容并存为变量
CA_CERT=$(awk '{printf "%s\\n", $0}' ca.crt)
SSL_CERT=$(awk '{printf "%s\\n", $0}' sdk.crt)
SDK_KEY=$(awk '{printf "%s\\n", $0}' sdk.key)

# 创建fiscobcos.json文件并写入内容
cat > fiscobcos.json << EOF
{
  "caCert": "$CA_CERT",
  "sslCert": "$SSL_CERT",
  "sslKey": "$SDK_KEY",
  "connectPeer": "172.20.155.69:20200",
  "groupID": "group0"
}
EOF
