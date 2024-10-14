#!/bin/bash

# https://www.cnblogs.com/biaogejiushibiao/p/12290728.html

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="fabric"

# 引入工具函数
source "$SCRIPT_DIR"/../utils.sh

# 读取配置文件函数
read_config() {
    log "INFO" "Reading configuration file: $CONFIG_FILE"
    if [[ ! -f "$CONFIG_FILE" ]]; then
        log "ERROR" "Configuration file $CONFIG_FILE does not exist."
        exit 1
    fi
    get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir channel version
    log "INFO" "Configuration successfully read: data_dir=$data_dir, channel=$channel, version=$version"
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

# 下载 install-fabric.sh 脚本函数
download_install_fabric() {
    log "INFO" "Downloading install-fabric.sh script..."
    if [ ! -f "$SCRIPT_DIR/install-fabric.sh" ]; then
        log "INFO" "install-fabric.sh not found in $SCRIPT_DIR. Starting download..."
        if curl -sSLO https://raw.githubusercontent.com/hyperledger/fabric/main/scripts/install-fabric.sh; then
            log "INFO" "install-fabric.sh downloaded successfully."
            chmod +x install-fabric.sh
        else
            log "ERROR" "Failed to download install-fabric.sh."
            exit 1
        fi
    else
        log "INFO" "install-fabric.sh already exists in $SCRIPT_DIR. Skipping download."
        if cp "$SCRIPT_DIR/install-fabric.sh" ./; then
            log "INFO" "Copied install-fabric.sh to the current directory successfully."
        else
            log "ERROR" "Failed to copy install-fabric.sh."
            exit 1
        fi
    fi
}

# 运行 install-fabric.sh 脚本下载样例函数
run_install_fabric() {
    log "INFO" "Running install-fabric.sh script..."
    if [ -d "$data_dir/fabric-samples" ]; then
        log "INFO" "fabric-samples directory already exists. Skipping installation."
    else
        log "INFO" "fabric-samples directory does not exist. Starting installation..."
        if ./install-fabric.sh -f "$version" samples; then
            log "INFO" "install-fabric.sh executed successfully."
        else
            log "ERROR" "install-fabric.sh execution failed."
            exit 1
        fi
    fi
}

# 下载并解压 Fabric 二进制文件函数
setup_fabric_binaries() {
    log "INFO" "Setting up Fabric binaries..."
    cd fabric-samples || { log "ERROR" "Failed to navigate to fabric-samples directory."; exit 1; }

    download_and_extract "hyperledger-fabric-linux-amd64-$version.tar.gz" "https://github.com/hyperledger/fabric/releases/download/v$version/hyperledger-fabric-linux-amd64-$version.tar.gz"
    download_and_extract "hyperledger-fabric-ca-linux-amd64-$version.tar.gz" "https://github.com/hyperledger/fabric-ca/releases/download/v$version/hyperledger-fabric-ca-linux-amd64-$version.tar.gz"
}

# 下载并解压指定文件的辅助函数
download_and_extract() {
    local file_name="$1"
    local url="$2"

    log "INFO" "Processing file: $file_name"
    if [ -f "$file_name" ]; then
        log "INFO" "$file_name already exists. Preparing to extract and overwrite."
    else
        log "INFO" "$file_name does not exist. Starting download..."
        if curl -L -O "$url"; then
            log "INFO" "Downloaded $file_name successfully."
        else
            log "ERROR" "Failed to download $file_name."
            exit 1
        fi
    fi

    log "INFO" "Extracting $file_name..."
    if tar --overwrite -xvf "$file_name"; then
        log "INFO" "Extracted $file_name successfully."
    else
        log "ERROR" "Failed to extract $file_name."
        exit 1
    fi
}

# 启动 Fabric 网络函数
start_fabric_network() {
    log "INFO" "Starting the Fabric network..."
    cd ./first-network || { log "ERROR" "Failed to navigate to first-network directory."; exit 1; }
    if yes "" | ./byfn.sh up -c "$channel"; then
        log "INFO" "Fabric network started successfully."
    else
        log "WARNING" "Failed to start Fabric network. Continuing with modify_config_files."
        log "INFO" "If no error is shown above, ignore this warning."
    fi
}

# 修改配置文件函数
modify_config_files() {
    log "INFO" "Modifying configuration files..."
    CRYPTO_CONFIG_FILE="$data_dir/fabric-samples/first-network/crypto-config"
    CONF_FILE="$SCRIPT_DIR/conf.json"

    if [ ! -f "$CONF_FILE" ]; then
        log "ERROR" "Configuration file $CONF_FILE does not exist."
        exit 1
    fi

    if cp "$CONF_FILE" "$data_dir/"; then
        log "INFO" "Copied $CONF_FILE to $data_dir successfully."
    else
        log "ERROR" "Failed to copy $CONF_FILE."
        exit 1
    fi

    if python3 "$SCRIPT_DIR"/fill_args.py "$data_dir/conf.json" "$CRYPTO_CONFIG_FILE"; then
        log "INFO" "Configuration files modified successfully."
    else
        log "ERROR" "Failed to modify configuration files."
        exit 1
    fi
}

# 检查依赖函数
check_dependencies() {
    log "INFO" "Checking for required dependencies..."
    local dependencies=(curl tar python3 mkdir chmod cp)
    for cmd in "${dependencies[@]}"; do
        if ! command -v "$cmd" &>/dev/null; then
            log "ERROR" "Missing dependency: $cmd. Please install it and retry."
            exit 1
        else
            log "INFO" "Dependency found: $cmd"
        fi
    done
    log "INFO" "All required dependencies are installed."
}

# 主函数
main() {
    log "INFO" "Starting Fabric network deployment script."

    check_dependencies
    read_config
    create_root_directory
    download_install_fabric
    run_install_fabric
    setup_fabric_binaries
    start_fabric_network
    modify_config_files

    log "INFO" "Fabric network deployment completed successfully."
}

# 执行主函数
main