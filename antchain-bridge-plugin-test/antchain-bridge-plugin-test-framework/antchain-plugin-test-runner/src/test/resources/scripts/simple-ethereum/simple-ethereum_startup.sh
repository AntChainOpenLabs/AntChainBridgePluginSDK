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
    get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir http_addr http_port http_api private_key_file gas_price gas_limit
}

# 检查并安装 geth 的函数
# 检查并安装 geth 的函数，仅支持 Ubuntu 和 CentOS
install_geth() {
    if command -v geth &> /dev/null; then
        log "INFO" "Geth is already installed. Skipping installation."
        return
    fi

    # 检测操作系统
    if [ -e /etc/os-release ]; then
        . /etc/os-release
        OS=$ID
        VERSION=$VERSION_ID
    else
        log "ERROR" "Cannot determine the operating system. Exiting."
        exit 1
    fi

    case "$OS" in
        ubuntu)
            log "INFO" "Detected OS: Ubuntu $VERSION. Proceeding with Ubuntu installation."

            log "INFO" "Adding Ethereum PPA repository..."
            sudo add-apt-repository -y ppa:ethereum/ethereum || {
                log "ERROR" "Failed to add Ethereum PPA repository. Exiting."
                exit 1
            }

            log "INFO" "Updating package lists..."
            sudo apt-get update || {
                log "ERROR" "Failed to update package lists. Exiting."
                exit 1
            }

            log "INFO" "Installing Ethereum..."
            sudo apt-get install -y ethereum || {
                log "ERROR" "Failed to install Ethereum. Exiting."
                exit 1
            }
            ;;

        centos)
            log "INFO" "Detected OS: CentOS $VERSION. Proceeding with CentOS installation."

            # 安装依赖
            log "INFO" "Installing dependencies..."
            sudo yum install -y epel-release || {
                log "ERROR" "Failed to install EPEL repository. Exiting."
                exit 1
            }
            sudo yum install -y wget tar || {
                log "ERROR" "Failed to install wget and tar. Exiting."
                exit 1
            }

            # 获取最新的 Geth 版本号
            GETH_VERSION=$(curl -s https://api.github.com/repos/ethereum/go-ethereum/releases/latest | grep '"tag_name":' | sed -E 's/.*"v([^"]+)".*/\1/')
            if [ -z "$GETH_VERSION" ]; then
                log "ERROR" "Failed to fetch the latest Geth version. Exiting."
                exit 1
            fi
            log "INFO" "Latest Geth version: $GETH_VERSION"

            # 下载和安装 Geth
            GETH_ARCH=$(uname -m)
            case "$GETH_ARCH" in
                x86_64)
                    ARCH="amd64"
                    ;;
                aarch64|arm64)
                    ARCH="arm64"
                    ;;
                *)
                    log "ERROR" "Unsupported architecture: $GETH_ARCH. Exiting."
                    exit 1
                    ;;
            esac

            DOWNLOAD_URL="https://gethstore.blob.core.windows.net/builds/geth-linux-${ARCH}-${GETH_VERSION}.tar.gz"
            log "INFO" "Downloading Geth from $DOWNLOAD_URL"
            wget -O /tmp/geth.tar.gz "$DOWNLOAD_URL" || {
                log "ERROR" "Failed to download Geth. Exiting."
                exit 1
            }

            log "INFO" "Extracting Geth..."
            tar -xzf /tmp/geth.tar.gz -C /tmp || {
                log "ERROR" "Failed to extract Geth archive. Exiting."
                exit 1
            }

            log "INFO" "Installing Geth..."
            sudo cp /tmp/geth-linux-${ARCH}-${GETH_VERSION}/geth /usr/local/bin/ || {
                log "ERROR" "Failed to copy Geth binary to /usr/local/bin. Exiting."
                exit 1
            }

            # 清理临时文件
            rm -rf /tmp/geth.tar.gz /tmp/geth-linux-${ARCH}-${GETH_VERSION}
            log "INFO" "Geth installation completed."
            ;;

        *)
            log "ERROR" "Unsupported operating system: $OS. This script only supports Ubuntu and CentOS. Exiting."
            exit 1
            ;;
    esac
}

# 设置数据目录的函数
setup_data_dir() {
    log "INFO" "Setting up data directory at $data_dir"
    rm -rf "$data_dir"
    mkdir -p "$data_dir"/keystore
}

# 生成私钥和账户的函数
generate_private_key_and_account() {
    log "INFO" "Generating private key..."
    PRIVATE_KEY=$(openssl rand -hex 32)
    if [ -z "$PRIVATE_KEY" ]; then
        log "ERROR" "Failed to generate private key. Exiting."
        exit 1
    fi
    log "INFO" "Private Key: $PRIVATE_KEY"

    log "INFO" "Importing private key into geth..."
    echo "$PRIVATE_KEY" > "$private_key_file"

    # 创建空密码文件
    touch "$data_dir"/empty_password.txt

    # 导入私钥并获取账户地址
    ACCOUNT_ADDRESS=$(geth account import --datadir "$data_dir" --password "$data_dir"/empty_password.txt "$private_key_file" 2>/dev/null | grep -oP '(?<=Address: \{).*?(?=\})')

    if [ -z "$ACCOUNT_ADDRESS" ]; then
        log "ERROR" "Failed to generate account address. Exiting."
        exit 1
    fi

    log "INFO" "Account Address: 0x$ACCOUNT_ADDRESS"
}

# 启动 Geth 的函数
start_geth() {
    log "INFO" "Starting Geth in developer mode..."

    nohup geth --dev \
          --datadir "$data_dir" \
          --http \
          --http.addr "$http_addr" \
          --http.port "$http_port" \
          --http.api "$http_api" \
          --miner.gasprice "$gas_price" \
          --miner.gaslimit "$gas_limit" \
          > "$data_dir"/log 2>&1 &

    # 获取并保存 Geth 进程ID
    GETH_PID=$!
    echo "$GETH_PID" > "$data_dir/geth.pid"
    log "INFO" "Geth started with PID $GETH_PID"
}

# 主函数，按顺序调用各个模块
main() {
    read_config
    install_geth
    setup_data_dir
    generate_private_key_and_account
    start_geth
}

# 执行主函数
main