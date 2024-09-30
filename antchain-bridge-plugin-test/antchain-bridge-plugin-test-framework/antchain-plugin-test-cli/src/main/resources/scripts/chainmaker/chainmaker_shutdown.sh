#!/bin/bash

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="chainmaker"

# 引入工具函数
source "$SCRIPT_DIR"/../utils.sh

# 读取配置文件
read_config() {
    log "INFO" "Reading configuration file: $CONFIG_FILE"
    if [[ ! -f "$CONFIG_FILE" ]]; then
        log "ERROR" "Configuration file $CONFIG_FILE does not exist."
        exit 1
    fi
    get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir
    log "INFO" "Configuration read successfully: data_dir=$data_dir"
}

# 进入目录
enter_directory() {
    log "INFO" "Entering directory: $data_dir/chainmaker-go/scripts"
    if cd "$data_dir/chainmaker-go/scripts"; then
        log "INFO" "Successfully entered directory."
    else
        log "ERROR" "Failed to enter directory."
        exit 1
    fi
}

# 停止节点集群
stop_cluster() {
    log "INFO" "Stopping chainmaker node cluster..."
    if ./cluster_quick_stop.sh; then
        log "INFO" "Node cluster stopped successfully."
    else
        log "ERROR" "Failed to stop node cluster."
        exit 1
    fi
}

# 查找并终止所有 chainmaker 进程
kill_chainmaker_processes() {
    log "INFO" "Searching for chainmaker processes..."
    local pids
    pids=$(ps aux | grep './chainmaker start -c' | grep -v 'grep' | awk '{print $2}')

    if [ -z "$pids" ]; then
        log "INFO" "No chainmaker processes found."
    else
        log "INFO" "Found chainmaker processes: $pids"
        for pid in $pids; do
            kill -9 "$pid"
        done
    fi
}

# 检查节点进程
check_node_processes() {
    log "INFO" "Checking for remaining chainmaker processes..."
    ps -ef | grep './chainmaker start -c' | grep -v grep
}

# 删除 crypto-config 目录
delete_crypto_config() {
    log "INFO" "Deleting crypto-config directory..."
    local crypto_config_dir="$SCRIPT_DIR/crypto-config"
    if [ -d "$crypto_config_dir" ]; then
        if rm -rf "$crypto_config_dir"; then
            log "INFO" "Successfully deleted crypto-config directory."
        else
            log "ERROR" "Failed to delete crypto-config directory."
            exit 1
        fi
    else
        log "INFO" "crypto-config directory does not exist. No deletion needed."
    fi
}

# 主函数
main() {
    log "INFO" "Starting ChainMaker node shutdown script."

    read_config
    enter_directory
    stop_cluster
    kill_chainmaker_processes
    check_node_processes
    delete_crypto_config

    log "INFO" "ChainMaker node shutdown script completed successfully."
}

# 执行主函数
main