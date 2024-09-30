#!/bin/bash

# 确保脚本以 root 用户运行（可选，根据需要保留）
#if [[ "$EUID" -ne 0 ]]; then
#    echo "ERROR: 这个脚本必须以 root 用户运行。" >&2
#    exit 1
#fi

# 设置脚本目录和配置文件路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="ethereum"

# 引入工具脚本
source "$SCRIPT_DIR"/../utils.sh

# 全局变量（可选，根据需要使用）
# data_dir 和其他配置项将通过 read_config 函数设置

# 读取配置文件的函数
read_config() {
    log "INFO" "Reading configuration from $CONFIG_FILE for chain type $CHAIN_TYPE"
    get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir
}

# 检查 PID 文件是否存在的函数
check_pid_file() {
    pid_file="$data_dir/geth.pid"
    log "INFO" "Checking if PID file exists at $pid_file..."
    if [ -f "$pid_file" ]; then
        GETH_PID=$(cat "$pid_file")
        log "INFO" "Found PID file with PID $GETH_PID."
        return 0
    else
        log "ERROR" "PID file not found at $pid_file. Is Geth running?"
        return 1
    fi
}

# 检查 Geth 进程是否在运行的函数
is_geth_running() {
    local pid=$1
    if ps -p "$pid" > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# 停止 Geth 进程的函数
stop_geth_process() {
    local pid=$1
    log "INFO" "Stopping Geth process with PID $pid..."

    kill -15 "$pid" || {
        log "WARNING" "Failed to send SIGTERM to Geth process with PID $pid."
    }

    sleep 5

    if is_geth_running "$pid"; then
        log "WARNING" "Geth process with PID $pid did not stop, sending SIGKILL..."
        kill -9 "$pid" || {
            log "ERROR" "Failed to send SIGKILL to Geth process with PID $pid."
            return 1
        }
    else
        log "INFO" "Geth process with PID $pid stopped successfully."
    fi

    return 0
}

# 移除 PID 文件的函数
remove_pid_file() {
    local pid_file=$1
    log "INFO" "Removing PID file at $pid_file..."
    rm -f "$pid_file" || {
        log "WARNING" "Failed to remove PID file at $pid_file."
    }
}

# 移除数据目录的函数
remove_data_dir() {
    log "INFO" "Removing data directory at $data_dir..."
    rm -rf "$data_dir" || {
        log "ERROR" "Failed to remove data directory at $data_dir."
        exit 1
    }
    log "INFO" "Data directory removed."
}

# 执行清理操作的主函数
cleanup() {
    if check_pid_file; then
        if is_geth_running "$GETH_PID"; then
            if stop_geth_process "$GETH_PID"; then
                remove_pid_file "$pid_file"
            else
                log "ERROR" "Failed to stop Geth process with PID $GETH_PID."
                exit 1
            fi
        else
            log "WARNING" "No Geth process found with PID $GETH_PID. Removing stale PID file."
            remove_pid_file "$pid_file"
        fi
    fi

    remove_data_dir

    log "INFO" "Cleanup complete."
}

# 主函数，按顺序调用各个模块
main() {
    read_config
    cleanup
}

# 执行主函数
main
