#!/bin/bash
# https://fisco-bcos-doc.readthedocs.io/zh-cn/latest/docs/quick_start/air_installation.html

# 设置脚本目录和配置文件路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="fiscobcos"

# 引入工具脚本
source "$SCRIPT_DIR"/../utils.sh

# 读取配置文件的函数
read_config() {
    log "INFO" "Reading configuration from $CONFIG_FILE for chain type $CHAIN_TYPE"
    get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir
    if [ -z "$data_dir" ]; then
        log "ERROR" "Configuration 'data_dir' is missing. Exiting."
        exit 1
    fi
    log "INFO" "Configuration - data_dir: $data_dir"
}

# 导航到数据目录的函数
navigate_to_data_dir() {
    log "INFO" "Navigating to data directory: $data_dir"
    if cd "$data_dir"; then
        log "INFO" "Successfully navigated to $data_dir"
    else
        log "ERROR" "Failed to navigate to $data_dir. Exiting."
        exit 1
    fi
}

# 运行停止所有节点脚本的函数
stop_all_nodes() {
    local stop_script="nodes/127.0.0.1/stop_all.sh"
    log "INFO" "Running script to stop all nodes: $stop_script"
    if [ -x "$stop_script" ]; then
        bash "$stop_script" || {
            log "ERROR" "Failed to execute $stop_script. Exiting."
            exit 1
        }
        log "INFO" "Successfully executed $stop_script"
    else
        log "ERROR" "Stop script $stop_script is not executable or does not exist. Exiting."
        exit 1
    fi
}

# 删除数据目录的函数
delete_data_dir() {
    log "INFO" "Deleting data directory: $data_dir"
    if rm -rf "$data_dir"; then
        log "INFO" "Successfully deleted data directory: $data_dir"
    else
        log "ERROR" "Failed to delete data directory: $data_dir. Exiting."
        exit 1
    fi
}

# 检查节点进程是否仍在运行的函数
check_node_processes() {
    log "INFO" "Checking if FISCO BCOS processes are still running."
    if ps aux | grep -v grep | grep fisco-bcos > /dev/null; then
        log "WARNING" "FISCO BCOS processes are still running."
    else
        log "INFO" "No FISCO BCOS processes found."
    fi
}

# 执行清理操作的主函数
cleanup_fiscobcos() {
    read_config
    navigate_to_data_dir
    stop_all_nodes
    delete_data_dir
    check_node_processes
    log "INFO" "FISCO BCOS cleanup completed successfully."
}

# 主函数，按顺序调用各个模块
main() {
    cleanup_fiscobcos
}

# 执行主函数
main
