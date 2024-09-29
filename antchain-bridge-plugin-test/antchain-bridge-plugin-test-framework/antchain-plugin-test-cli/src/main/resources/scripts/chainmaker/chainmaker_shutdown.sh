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

# 查找所有运行中的 chainmaker 进程
pids=$(ps aux | grep './chainmaker -c' | grep -v 'grep' | awk '{print $2}')

# 检查是否有相关进程
if [ -z "$pids" ]; then
  log "INFO" "no chainmaker process found."
else
  # 逐个关闭找到的进程
  log "INFO" "kill chainmaker process: $pids"
  for pid in $pids; do
    kill -9 $pid
    log "INFO" "kill chainmaker process: $pid"
  done
fi

# step3. 检查节点进程
ps -ef | grep chainmaker | grep -v grep

# step4. 删除 crypto-config 目录
rm -rf "$SCRIPT_DIR"/crypto-config