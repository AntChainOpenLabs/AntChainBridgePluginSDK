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
    get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir http_port
}

# 停止 Geth 进程的函数
stop_geth_process() {
    # 查找占用 http_port 且 command 为 node 的进程
    PIDS=$(lsof -t -i:"$http_port" | xargs -I {} sh -c 'ps -p {} -o comm= | grep -q "^node$" && echo {}')

    # 检查是否找到占用端口且为 node 的进程
    if [ -n "$PIDS" ]; then
        for PID in $PIDS; do
            log "INFO" "Trying to kill process $PID (command: node)..."

            # 尝试优雅地终止进程
            kill "$PID"

            # 检查进程是否仍然存在
            sleep 2
            if kill -0 "$PID" &> /dev/null; then
                log "WARNING" "Process $PID did not stop, forcing termination..."

                # 强制终止
                kill -9 "$PID"
                if [ $? -eq 0 ]; then
                    log "INFO" "Process $PID (command: node) was killed."
                else
                    log "ERROR" "Failed to kill process $PID (command: node)."
                fi
            else
                log "INFO" "Process $PID (command: node) stopped gracefully."
            fi
        done
    else
        log "INFO" "No node process found on port $http_port."
    fi
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
    stop_geth_process

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
