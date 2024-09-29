#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="fiscobcos"
source "$SCRIPT_DIR"/../utils.sh

get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir

# step1. 打开目录
cd "$data_dir"

# step2. 运行脚本，停止所有节点
bash nodes/127.0.0.1/stop_all.sh

# step3. 删除数据目录
rm -rf "$data_dir"

# step4. 检查节点进程
ps aux |grep -v grep |grep fisco-bcos

