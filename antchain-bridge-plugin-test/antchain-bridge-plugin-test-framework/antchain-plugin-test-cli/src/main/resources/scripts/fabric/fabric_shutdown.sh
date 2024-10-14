#!/bin/bash

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="fabric"

# 引入工具函数
source "$SCRIPT_DIR"/../utils.sh

# 读取配置文件函数
read_config() {
    log "INFO" "Reading configuration file: $CONFIG_FILE"
    if [[ ! -f "$CONFIG_FILE" ]]; then
        log "ERROR" "Configuration file $CONFIG_FILE does not exist."
        exit 1
    fi
    get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir channel wallet_dir
    log "INFO" "Configuration successfully read: data_dir=$data_dir, channel=$channel, wallet_dir=$wallet_dir"
}

# 关闭网络函数
shutdown_network() {
    log "INFO" "Shutting down the network..."
    NETWORK_DIR="${data_dir}/fabric-samples/first-network"

    if [[ ! -d "$NETWORK_DIR" ]]; then
        log "ERROR" "Network directory $NETWORK_DIR does not exist."
        exit 1
    fi

    cd "$NETWORK_DIR" || exit

    if yes "" | ./byfn.sh down; then
        log "INFO" "Network shut down successfully."
    else
        log "ERROR" "Failed to shut down the network. Please check the network status."
        exit 1
    fi
}

# 清理函数
cleanup() {
    log "INFO" "Cleanup complete."
}

# 主函数
main() {
    log "INFO" "Starting network shutdown script."

    read_config
    shutdown_network
    cleanup

    log "INFO" "Network shutdown script completed successfully."
}

# 执行主函数
main
