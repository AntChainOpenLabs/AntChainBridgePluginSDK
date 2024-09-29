#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="eos"
source "$SCRIPT_DIR"/../utils.sh

get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir

# pid=$(lsof -t -i:8888)

# if [ -n "$pid" ]; then
#     echo "Stopping process on port 8888 with PID: $pid"
#     kill -9 $pid
#     echo "Process $pid has been stopped."
# else
#     echo "No process is using port 8888."
# fi
#!/bin/bash

# 查找所有由 nodeos 命令启动的进程
nodeos_pids=$(pgrep -f nodeos)

# 检查是否找到相关进程
if [ -z "$nodeos_pids" ]; then
    log "INFO" "No nodeos processes found."
else
    log "INFO" "Found nodeos processes: $nodeos_pids"
    # 关闭所有 nodeos 进程
    for pid in $nodeos_pids; do
        log "INFO" "Killing nodeos process with PID: $pid"
        kill -9 $pid
    done
    log "INFO" "All nodeos processes have been stopped."
fi

rm -rf "$data_dir"
rm -rf ~/eosio-wallet/./default.wallet
