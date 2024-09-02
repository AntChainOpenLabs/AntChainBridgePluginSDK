#!/bin/bash

# 脚本所在路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 配置文件路径
CONFIG_FILE="${SCRIPT_DIR}/../config.properties"

CHAIN_TYPE="ethereum"

# 检查配置文件是否存在
if [ -f "$CONFIG_FILE" ]; then
  # 从.properties文件中读取变量
  DATADIR=$(grep "^${CHAIN_TYPE}.data_dir" "$CONFIG_FILE" | cut -d'=' -f2)
  HTTP_ADDR=$(grep "^${CHAIN_TYPE}.http_addr" "$CONFIG_FILE" | cut -d'=' -f2)
  HTTP_PORT=$(grep "^${CHAIN_TYPE}.http_port" "$CONFIG_FILE" | cut -d'=' -f2)
  HTTP_API=$(grep "^${CHAIN_TYPE}.http_api" "$CONFIG_FILE" | cut -d'=' -f2)
  PRIVATE_KEY_FILE=$(grep "^${CHAIN_TYPE}.private_key_file" "$CONFIG_FILE" | cut -d'=' -f2)
  GAS_PRICE=$(grep "^${CHAIN_TYPE}.gas_price" "$CONFIG_FILE" | cut -d'=' -f2)
  GAS_LIMIT=$(grep "^${CHAIN_TYPE}.gas_limit" "$CONFIG_FILE" | cut -d'=' -f2)


  # 验证读取的变量值
  echo "DATADIR=${DATADIR}"
  echo "HTTP_ADDR=${HTTP_ADDR}"
  echo "HTTP_PORT=${HTTP_PORT}"
  echo "HTTP_API=${HTTP_API}"
  echo "PRIVATE_KEY_FILE=${PRIVATE_KEY_FILE}"
  echo "GAS_PRICE=${GAS_PRICE}"
  echo "GAS_LIMIT=${GAS_LIMIT}"
else
  echo "Configuration file not found!"
  exit 1
fi


# 检查是否已经安装了 geth
if command -v geth &> /dev/null
then
    echo "Geth is already installed. Skipping installation."
else
    echo "Adding Ethereum PPA repository..."
    add-apt-repository -y ppa:ethereum/ethereum

    echo "Updating package lists..."
    apt-get update

    echo "Installing Ethereum..."
    apt-get install -y ethereum
fi

rm -rf "$DATADIR" && mkdir -p "$DATADIR"

# 生成私钥和账户
echo "Generating private key..."
PRIVATE_KEY=$(openssl rand -hex 32)
echo "Private Key: $PRIVATE_KEY"
echo "Importing private key into geth..."
mkdir -p "$DATADIR"/keystore
echo "$PRIVATE_KEY" > "$PRIVATE_KEY_FILE"
# 创建空密码文件
touch "$DATADIR"/empty_password.txt
# 根据私钥创建账户
ACCOUNT_ADDRESS=$(geth account import --datadir $DATADIR --password "$DATADIR"/empty_password.txt "$PRIVATE_KEY_FILE" | grep -oP '(?<=Address: \{).*?(?=\})')
echo "Account Address: 0x$ACCOUNT_ADDRESS"
if [ -z "$ACCOUNT_ADDRESS" ]; then
    echo "Failed to generate account address. Exiting."
    exit 1
fi

# 启动测试链
echo "Starting Geth in developer mode..."
nohup geth --dev --datadir "$DATADIR" -mine --miner.gaslimit "$GAS_LIMIT" --miner.gasprice "$GAS_PRICE" --http --http.addr "$HTTP_ADDR" --http.port "$HTTP_PORT" --http.api "$HTTP_API" > "$DATADIR"/log 2>&1 &

# 保存 Geth 进程ID
echo $! > "$DATADIR/geth.pid"
echo "Geth started with PID $(cat $DATADIR/geth.pid)"