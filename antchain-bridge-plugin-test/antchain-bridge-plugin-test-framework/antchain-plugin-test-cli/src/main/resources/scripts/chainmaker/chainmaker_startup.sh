#!/bin/bash

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="chainmaker"

# 引入工具函数
source "$SCRIPT_DIR"/../utils.sh

# 读取配置文件
read_config() {
    log "INFO" "Reading configuration file: $CONFIG_FILE"
    if [[ ! -f "$CONFIG_FILE" ]]; then
        log "ERROR" "Configuration file $CONFIG_FILE does not exist."
        exit 1
    fi
    get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir version conf_file
    log "INFO" "Configuration read successfully: data_dir=$data_dir, version=$version, conf_file=$conf_file"
}

# 创建并进入目录
create_and_enter_directory() {
    log "INFO" "Creating and entering directory: $data_dir"
    if mkdir -p "$data_dir" && cd "$data_dir"; then
        log "INFO" "Directory created and entered: $data_dir"
    else
        log "ERROR" "Failed to create or enter directory: $data_dir"
        exit 1
    fi
}

# 克隆源码库
clone_repo_if_not_exists() {
    local repo_url=$1
    local repo_branch=$2
    local repo_dir=$3

    log "INFO" "Checking if directory $repo_dir exists..."
    if [ -d "$repo_dir" ]; then
        log "INFO" "Directory $repo_dir already exists. Skipping clone."
    else
        log "INFO" "Cloning repository from $repo_url (branch: $repo_branch) into $repo_dir..."
        if git clone -b "$repo_branch" --depth=1 "$repo_url"; then
            log "INFO" "Repository cloned successfully."
        else
            log "ERROR" "Failed to clone repository."
            exit 1
        fi
    fi
}

# 编译源码
compile_source() {
    local compile_dir=$1

    log "INFO" "Compiling source in directory: $compile_dir"
    if cd "$compile_dir" && make; then
        log "INFO" "Source compiled successfully."
    else
        log "ERROR" "Failed to compile source."
        exit 1
    fi
}

# 创建软连接
create_symlink() {
    local target=$1
    local link_name=$2

    log "INFO" "Creating symbolic link from $target to $link_name (force overwrite if exists)"
    if ln -sf "$target" "$link_name"; then
        log "INFO" "Symbolic link created successfully."
    else
        log "ERROR" "Failed to create symbolic link."
        exit 1
    fi
}

# 生成证书和配置文件
generate_config_and_certificates() {
    local script_dir=$1
    log "INFO" "Generating certificates and configuration using prepare.sh script in directory: $script_dir"
    if cd "$script_dir" && yes "" | head -n 4 | ./prepare.sh 4 1; then
        log "INFO" "Certificates and configuration generated successfully."
    else
        log "ERROR" "Failed to generate certificates and configuration."
        exit 1
    fi
}

# 编译并制作安装包
build_release() {
    log "INFO" "Building release package..."
    if ./build_release.sh; then
        log "INFO" "Release package built successfully."
    else
        log "ERROR" "Failed to build release package."
        exit 1
    fi
}

# 启动节点集群
start_cluster() {
    log "INFO" "Starting node cluster..."
    if ./cluster_quick_start.sh normal; then
        log "INFO" "Node cluster started successfully."
    else
        log "ERROR" "Failed to start node cluster."
        exit 1
    fi
}

# 备份安装包
backup_release() {
    log "INFO" "Backing up release packages..."
    if mkdir -p ../build/bak && mv ../build/release/*.tar.gz ../build/bak; then
        log "INFO" "Release packages backed up successfully."
    else
        log "ERROR" "Failed to backup release packages."
        exit 1
    fi
}

# 检查节点是否启动成功
check_node_status() {
    log "INFO" "Checking if chainmaker nodes are running..."
    if ps -ef | grep chainmaker | grep -v grep > /dev/null; then
        log "INFO" "Chainmaker nodes are running."
    else
        log "ERROR" "No chainmaker nodes are running."
        exit 1
    fi
}

# 主函数
main() {
    log "INFO" "Starting ChainMaker setup script."

    read_config
    create_and_enter_directory

    clone_repo_if_not_exists "https://git.chainmaker.org.cn/chainmaker/chainmaker-go.git" "v$version" "chainmaker-go"
    clone_repo_if_not_exists "https://git.chainmaker.org.cn/chainmaker/chainmaker-cryptogen.git" "v$version" "chainmaker-cryptogen"

    compile_source "chainmaker-cryptogen"
    create_symlink "$data_dir/chainmaker-cryptogen/" "$data_dir/chainmaker-go/tools/"

    generate_config_and_certificates "$data_dir/chainmaker-go/scripts"
    build_release
    start_cluster
    backup_release
    check_node_status

    log "INFO" "ChainMaker setup completed successfully."
}

# 执行主函数
main