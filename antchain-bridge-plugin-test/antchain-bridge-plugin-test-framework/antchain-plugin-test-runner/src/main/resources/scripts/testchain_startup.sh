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

  # 验证读取的变量值
  echo "DATADIR=${DATADIR}"
  echo "HTTP_ADDR=${HTTP_ADDR}"
  echo "HTTP_PORT=${HTTP_PORT}"
  echo "HTTP_API=${HTTP_API}"
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

echo "Starting Geth in developer mode..."
rm -rf "$DATADIR" && mkdir -p "$DATADIR"
nohup geth --dev --datadir "$DATADIR" --http --http.addr "$HTTP_ADDR" --http.port "$HTTP_PORT" --http.api "$HTTP_API" &

# 保存 Geth 进程ID
echo $! > "$DATADIR/geth.pid"
echo "Geth started with PID $(cat $DATADIR/geth.pid)"