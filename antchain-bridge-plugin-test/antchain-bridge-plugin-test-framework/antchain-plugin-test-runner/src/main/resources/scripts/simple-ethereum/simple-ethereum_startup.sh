#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="ethereum"
source "$SCRIPT_DIR"/../utils.sh

# 读取配置文件
get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir http_addr http_port http_api private_key_file gas_price gas_limit

# 检查是否已经安装了 geth
if command -v geth &> /dev/null
then
    log "INFO" "Geth is already installed. Skipping installation."
else
    log "INFO" "Adding Ethereum PPA repository..."
    add-apt-repository -y ppa:ethereum/ethereum

    log "INFO" "Updating package lists..."
    apt-get update

    log "INFO" "Installing Ethereum..."
    apt-get install -y ethereum
fi

rm -rf "$data_dir" && mkdir -p "$data_dir"

# 生成私钥和账户
log "INFO" "Generating private key..."
PRIVATE_KEY=$(openssl rand -hex 32)
log "INFO" "Private Key: $PRIVATE_KEY"
log "INFO" "Importing private key into geth..."
mkdir -p "$data_dir"/keystore
echo "$PRIVATE_KEY" > "$private_key_file"
# 创建空密码文件
touch "$data_dir"/empty_password.txt
# 根据私钥创建账户
ACCOUNT_ADDRESS=$(geth account import --datadir $data_dir --password "$data_dir"/empty_password.txt "$private_key_file" | grep -oP '(?<=Address: \{).*?(?=\})')
log "INFO" "Account Address: 0x$ACCOUNT_ADDRESS"
if [ -z "$ACCOUNT_ADDRESS" ]; then
    log "ERROR" "Failed to generate account address. Exiting."
    exit 1
fi

# 启动测试链
log "INFO" "Starting Geth in developer mode..."
nohup geth --dev --datadir "$data_dir" -mine --miner.gaslimit "$gas_price" --miner.gasprice "$gas_limit" --http --http.addr "$http_addr" --http.port "$http_port" --http.api "$http_api" > "$data_dir"/log 2>&1 &

# 保存 Geth 进程ID
echo $! > "$data_dir/geth.pid"
log "INFO" "Geth started with PID $(cat $data_dir/geth.pid)"