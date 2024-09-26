#!/bin/bash

# https://www.cnblogs.com/biaogejiushibiao/p/12290728.html


SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="fabric"
source "$SCRIPT_DIR"/../utils.sh


# 读取配置文件
get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir channel wallet_dir version


log "INFO" "Creating root directory at ${data_dir} "
mkdir -p "${data_dir}" && cd "${data_dir}" || exit

# 下载 insntall-fabric.sh 脚本
log "INFO" "Downloading Fabric binaries..."
if [ ! -f "$SCRIPT_DIR/install-fabric.sh" ]; then
    log "INFO" "install-fabric.sh not found in $SCRIPT_DIR. Downloading..."
    curl -sSLO https://raw.githubusercontent.com/hyperledger/fabric/main/scripts/install-fabric.sh && chmod +x install-fabric.sh
else
    log "INFO" "install-fabric.sh already exists in $SCRIPT_DIR. Skipping download."
    cp "$SCRIPT_DIR/install-fabric.sh" ./
fi

# 运行 install-fabric.sh 脚本，下载 samples
log "INFO" "Running the install-fabric.sh script..."
if [ -d "$data_dir/fabric-samples" ]; then
    log "INFO" "The fabric-samples directory already exists. Skipping installation."
else
    log "INFO" "The fabric-samples directory does not exist. Starting installation..."
    ./install-fabric.sh -f "$version" samples
fi


# 进入 fabric-samples 的 scripts 目录
log "INFO" "Starting the Fabric network..."
cd fabric-samples || exit
# 检查文件是否已经存在
if [ -f "hyperledger-fabric-linux-amd64-$version.tar.gz" ]; then
    log "INFO" "Fabric archive already exists, extracting and overwriting..."
else
    log "INFO" "Fabric archive not found, downloading..."
    curl -L -O -x http://49.52.27.67:7890 https://github.com/hyperledger/fabric/releases/download/v"$version"/hyperledger-fabric-linux-amd64-"$version".tar.gz
fi

tar --overwrite -xvf hyperledger-fabric-linux-amd64-"$version".tar.gz

# Check if the fabric-ca archive exists
if [ -f "hyperledger-fabric-ca-linux-amd64-$version.tar.gz" ]; then
    log "INFO" "Fabric-CA archive already exists, extracting and overwriting..."
else
    log "INFO" "Fabric-CA archive not found, downloading..."
    curl -L -O -x http://49.52.27.67:7890 https://github.com/hyperledger/fabric-ca/releases/download/v"$version"/hyperledger-fabric-ca-linux-amd64-"$version".tar.gz
fi

tar --overwrite -xvf hyperledger-fabric-ca-linux-amd64-"$version".tar.gz


# 运行测试链
cd ./first-network || exit
yes "" | ./byfn.sh up -c mychannel


# 配置文件修改
CRYPTO_CONFIG_FILE="$data_dir/fabric-samples/first-network/crypto-config"
CONF_FILE="$SCRIPT_DIR/conf.json"
cp "$CONF_FILE" "$data_dir/"
python3 "$SCRIPT_DIR"/fill_args.py "$data_dir/conf.json" "$CRYPTO_CONFIG_FILE"

# CONF_FILE="$SCRIPT_DIR/conf.json"
# MSP_DIR="$data_dir/fabric-samples/first-network/crypto-config/peerOrganizations/org2.example.com/users/Admin@org2.example.com/msp"
# # 替换 conf.json 中的 key
# KEY_FILE=$(ls "$MSP_DIR"/keystore/*_sk)
# if [ -z "$KEY_FILE" ]; then
#   log "ERROR" "Error: No key file found"
#   exit 1
# fi
# KEY_CONTENT=$(cat "$KEY_FILE")
# jq --arg key "$KEY_CONTENT" '.user.key = $key' "$CONF_FILE" > tmp_conf.json && mv tmp_conf.json "$CONF_FILE"

# # 替换 conf.json 中的 cert
# CERT_FILE=$(ls "$MSP_DIR"/signcerts/*.pem)
# if [ -z "$CERT_FILE" ]; then
#   log "ERROR" "Error: No cert file found"
#   exit 1
# fi
# CERT_CONTENT=$(cat "$CERT_FILE")
# jq --arg cert "$CERT_CONTENT" '.user.cert = $cert' "$CONF_FILE" > tmp_conf.json && mv tmp_conf.json "$CONF_FILE"


