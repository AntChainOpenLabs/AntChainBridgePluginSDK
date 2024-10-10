#!/bin/bash

# 设置脚本目录和配置文件路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="ethereum"

# 引入工具脚本
source "$SCRIPT_DIR"/../utils.sh

# 读取配置文件的函数
read_config() {
    log "INFO" "Reading configuration from $CONFIG_FILE for chain type $CHAIN_TYPE"
    get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir http_addr http_port http_api private_key_file gas_price gas_limit ganache_cmd_path
}

# 创建数据目录的函数
create_data_directory() {
    log "INFO" "Creating data directory: $data_dir"
    if ! mkdir -p "$data_dir"; then
        log "ERROR" "Failed to create data directory: $data_dir"
        exit 1
    fi
}

# 启动Ganache的函数
start_ganache() {
    log "INFO" "Starting Ganache..."
    $(eval echo "$ganache_cmd_path") --db "$data_dir" --host "$http_addr" --port "$http_port" --gasPrice "$gas_price" --gasLimit "$gas_limit" > "$data_dir/ganache_log.txt" 2>&1 &
}

# 检查Ganache是否成功启动的函数
check_ganache_started() {
    for i in {1..10}; do
        if ss -tuln | grep -q ":$http_port"; then
            log "INFO" "Ganache started successfully on port $http_port"
            return
        fi
        sleep 1
    done
    log "ERROR" "Ganache failed to start"
    exit 1
}

# 检查日志文件是否存在的函数
check_log_file() {
    if [ ! -f "$data_dir/ganache_log.txt" ]; then
        log "ERROR" "ganache_log.txt does not exist"
        exit 1
    fi
}

# 提取私钥的函数
extract_private_key() {
    log "INFO" "Extracting private key from ganache log..."
    PRIVATE_KEY=$(grep -A 10 "Private Keys" "$data_dir/ganache_log.txt" | grep -oP '0x[0-9a-f]{64}' | head -n 1)

    if [ -z "$PRIVATE_KEY" ]; then
        log "ERROR" "Failed to extract private key from ganache log"
        exit 1
    fi

    echo "$PRIVATE_KEY" > "$private_key_file"
    log "INFO" "Private key saved to $private_key_file"
}

# 主函数
main() {
    log "INFO" "Starting Ethereum setup script"

    read_config
    create_data_directory
    start_ganache
    check_ganache_started
    check_log_file
    extract_private_key

    log "INFO" "Ethereum setup script completed successfully"
}

# 执行主函数
main