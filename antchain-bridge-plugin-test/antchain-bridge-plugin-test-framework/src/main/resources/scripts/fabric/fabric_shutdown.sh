#!/bin/bash


SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="fabric"
source "$SCRIPT_DIR"/../utils.sh


# 读取配置文件
get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir channel wallet_dir


log "INFO" "Shutting down the network..."
cd ${data_dir}/fabric-samples/first-network && yes "" | ./byfn.sh down

if [ $? -ne 0 ]; then
    log "INFO" "Failed to shut down the network. Please check the network status."
    exit 1
fi

# echo "Removing the directory at ${data_dir}..."
# rm -rf ${data_dir}

# if [ -d "$data_dir" ]; then
#     echo "Failed to remove the directory at ${data_dir}."
#     exit 1
# else
#     echo "Cleanup complete."
# fi

log "INFO" "Cleanup complete."