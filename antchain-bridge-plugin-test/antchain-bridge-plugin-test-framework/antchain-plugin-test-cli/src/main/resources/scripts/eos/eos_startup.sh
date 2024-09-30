#!/bin/bash

# https://www.cnblogs.com/biaogejiushibiao/p/12290728.html

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="eos"

# 引入工具函数
source "$SCRIPT_DIR"/../utils.sh

# 读取配置文件函数
read_config() {
    log "INFO" "Reading configuration file: $CONFIG_FILE"
    if [[ ! -f "$CONFIG_FILE" ]]; then
        log "ERROR" "Configuration file $CONFIG_FILE does not exist."
        exit 1
    fi
    get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir version http_server_address account_name private_key_file
    log "INFO" "Configuration successfully read: data_dir=$data_dir, version=$version, http_server_address=$http_server_address, account_name=$account_name, private_key_file=$private_key_file"
}

# 创建根目录函数
create_root_directory() {
    log "INFO" "Creating root directory: ${data_dir}"
    if mkdir -p "${data_dir}"; then
        log "INFO" "Root directory created successfully or already exists: ${data_dir}"
    else
        log "ERROR" "Failed to create root directory: ${data_dir}"
        exit 1
    fi

    if cd "${data_dir}"; then
        log "INFO" "Navigated to root directory: ${data_dir}"
    else
        log "ERROR" "Failed to navigate to root directory: ${data_dir}"
        exit 1
    fi
}

# 检查并安装 EOSIO 函数
check_and_install_eosio() {
    log "INFO" "Checking if nodeos is installed..."
    if ! command -v nodeos &> /dev/null
    then
        log "INFO" "nodeos not found, starting download and installation of eosio..."

        # 检查依赖项
        log "INFO" "Checking for required dependencies..."
        local dependencies=(curl dpkg apt-get)
        for cmd in "${dependencies[@]}"; do
            if ! command -v "$cmd" &>/dev/null; then
                log "ERROR" "Missing dependency: $cmd. Please install it and retry."
                exit 1
            else
                log "INFO" "Dependency found: $cmd"
            fi
        done

        # 检查操作系统是否为 Ubuntu
        if [[ -f /etc/os-release ]]; then
            . /etc/os-release
            if [[ "$ID" != "ubuntu" ]]; then
                log "ERROR" "Unsupported OS: $ID. Only Ubuntu is supported."
                exit 1
            fi
        else
            log "ERROR" "Cannot determine the operating system. /etc/os-release not found."
            exit 1
        fi

        # 下载 eosio 包
        filename="eosio_${version}-1-ubuntu-20.04_amd64.deb"
        if [ -f "$filename" ]; then
            log "INFO" "EOSIO package already downloaded, skipping download."
        else
            log "INFO" "Downloading EOSIO package: $filename"
            if curl -L -O "https://github.com/EOSIO/eos/releases/download/v$version/$filename"; then
                log "INFO" "Downloaded $filename successfully."
            else
                log "ERROR" "Failed to download $filename."
                exit 1
            fi
        fi

        # 安装 deb 包
        log "INFO" "Installing EOSIO package: $filename"
        if dpkg -i "$filename"; then
            log "INFO" "EOSIO installed successfully."
        else
            log "WARNING" "Dependency issues detected, attempting to fix..."
            if apt-get install -f -y && dpkg -i "$filename"; then
                log "INFO" "EOSIO installed successfully after fixing dependencies."
            else
                log "ERROR" "Failed to install EOSIO after fixing dependencies."
                exit 1
            fi
        fi

        # 清理下载的 deb 文件
        log "INFO" "Cleaning up downloaded package: $filename"
        rm -f "$filename"
        log "INFO" "EOSIO installation completed."
    else
        log "INFO" "nodeos is already installed, skipping installation."
    fi
}

# 设置配置文件函数
setup_config_ini() {
    log "INFO" "Setting up config.ini..."
    cat <<EOF > "$data_dir/config.ini"
plugin = eosio::producer_plugin
plugin = eosio::producer_api_plugin
plugin = eosio::chain_api_plugin
plugin = eosio::history_api_plugin
plugin = eosio::http_plugin

http-server-address = 0.0.0.0:8888

enable-stale-production = true

producer-name = eosio

access-control-allow-origin = *
access-control-allow-headers = Content-Type
access-control-allow-credentials = true
http-validate-host = false
EOF
    log "INFO" "config.ini has been set up successfully."
}

# 启动 nodeos 函数
start_nodeos() {
    log "INFO" "Starting nodeos..."
    nohup nodeos --config-dir "$data_dir" --data-dir "$data_dir/data" --contracts-console --verbose-http-errors --filter-on "*" > "$data_dir/nodeos.log" 2>&1 &
    log "INFO" "nodeos started successfully."
    sleep 2
}

# 设置钱包和账户函数
setup_wallet_and_account() {
    log "INFO" "Creating and setting up wallet..."
    cleos wallet create --to-console
    cleos wallet import --private-key 5KQwrPbwdL6PhXujxW37FSSQZ1JiwsST4cqQzDeyXtP79zkvFD3

    log "INFO" "Creating test account..."
    output=$(cleos create key --to-console 2>&1)
    private_key=$(echo "$output" | grep "Private key" | awk '{print $3}')
    public_key=$(echo "$output" | grep "Public key" | awk '{print $3}')
    cleos wallet import --private-key "$private_key"
    cleos create account eosio "$account_name" "$public_key" "$public_key"
    echo "$private_key" > "$private_key_file"
    log "INFO" "Test account created and private key saved to $private_key_file."
}

# 主函数
main() {
    log "INFO" "Starting EOSIO setup script."

    read_config
    create_root_directory
    check_and_install_eosio
    setup_config_ini
    start_nodeos
    setup_wallet_and_account

    log "INFO" "EOSIO setup script completed successfully."
}

# 执行主函数
main
