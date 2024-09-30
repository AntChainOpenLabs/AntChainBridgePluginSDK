#!/bin/bash

# https://www.cnblogs.com/biaogejiushibiao/p/12290728.html

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="eos"

# 引入工具函数
source "$SCRIPT_DIR"/../utils.sh

# 读取配置文件函数
read_config() {
    log "INFO" "Reading configuration file: $CONFIG_FILE"
    if [[ ! -f "$CONFIG_FILE" ]]; then
        log "ERROR" "Configuration file $CONFIG_FILE does not exist."
        exit 1
    fi
    get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir
    log "INFO" "Configuration successfully read: data_dir=$data_dir"
}

# 查找并终止 nodeos 进程函数
find_and_kill_nodeos() {
    log "INFO" "Searching for nodeos processes..."
    nodeos_pids=$(pgrep -f nodeos)

    if [ -z "$nodeos_pids" ]; then
        log "INFO" "No nodeos processes found."
    else
        log "INFO" "Found nodeos processes: $nodeos_pids"
        # 关闭所有 nodeos 进程
        for pid in $nodeos_pids; do
            log "INFO" "Killing nodeos process with PID: $pid"
            kill -9 "$pid"
        done
        log "INFO" "All nodeos processes have been stopped."
    fi
}

# 清理目录和文件函数
cleanup_directories() {
    log "INFO" "Cleaning up directories and files..."
    if rm -rf "$data_dir"; then
        log "INFO" "Deleted data directory: $data_dir"
    else
        log "ERROR" "Failed to delete data directory: $data_dir"
        exit 1
    fi

    if rm -rf ~/eosio-wallet/./default.wallet; then
        log "INFO" "Deleted default wallet file: ~/eosio-wallet/./default.wallet"
    else
        log "ERROR" "Failed to delete default wallet file: ~/eosio-wallet/./default.wallet"
        exit 1
    fi
}

# 主函数
main() {
    log "INFO" "Starting EOSIO shutdown script."

    read_config
    find_and_kill_nodeos
    cleanup_directories

    log "INFO" "EOSIO shutdown script completed successfully."
}

# 执行主函数
main
