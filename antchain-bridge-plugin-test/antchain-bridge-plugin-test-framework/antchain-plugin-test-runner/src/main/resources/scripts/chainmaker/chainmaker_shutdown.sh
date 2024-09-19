#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="chainmaker"
source "$SCRIPT_DIR"/../utils.sh


# 读取配置文件
get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir

# step1. 进入目录
cd $data_dir/chainmaker-go/scripts || exit

# step2. 停止节点集群
./cluster_quick_stop.sh

# step3. 检查节点进程
ps -ef|grep chainmaker | grep -v grep

# step4. 删除 crypto-config 目录
rm -rf "$SCRIPT_DIR"/crypto-config