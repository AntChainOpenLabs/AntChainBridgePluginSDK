#!/bin/bash
# https://fisco-bcos-doc.readthedocs.io/zh-cn/latest/docs/quick_start/air_installation.html
# https://fisco-bcos-doc.readthedocs.io/zh-cn/latest/docs/flato-solo/3.2-flato-tutorial
# https://github.com/hyperchain/hyperchain/releases

# 设置脚本目录和配置文件路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="fiscobcos"

# 引入工具脚本
source "$SCRIPT_DIR"/../utils.sh

# 读取配置文件的函数
read_config() {
    log "INFO" "Reading configuration from $CONFIG_FILE for chain type $CHAIN_TYPE"
    get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir version conf_dir conf_file
    log "INFO" "Configuration - data_dir: $data_dir, version: $version, conf_dir: $conf_dir, conf_file: $conf_file"
}

# 检查并安装依赖的函数，支持 Ubuntu 和 CentOS
install_dependencies() {
    log "INFO" "Installing required dependencies: curl, openssl, wget"

    # 检测操作系统
    if [ -e /etc/os-release ]; then
        . /etc/os-release
        OS=$ID
        VERSION=$VERSION_ID
    else
        log "ERROR" "Cannot determine the operating system. Exiting."
        exit 1
    fi

    # 定义依赖项
    dependencies=(curl openssl wget)

    # 初始化需要安装的包列表
    packages_to_install=()

    # 检查每个依赖项是否已安装
    for pkg in "${dependencies[@]}"; do
        if ! command -v "$pkg" &> /dev/null; then
            packages_to_install+=("$pkg")
            log "INFO" "Dependency '$pkg' is not installed. It will be installed."
        else
            log "INFO" "Dependency '$pkg' is already installed. Skipping."
        fi
    done

    # 如果有需要安装的包，则根据操作系统使用相应的包管理器安装
    if [ ${#packages_to_install[@]} -ne 0 ]; then
        case "$OS" in
            ubuntu|debian)
                log "INFO" "Detected OS: $OS $VERSION. Using apt-get to install dependencies."
                sudo apt-get update || { log "ERROR" "apt-get update failed. Exiting."; exit 1; }
                sudo apt-get install -y "${packages_to_install[@]}" || { log "ERROR" "Failed to install dependencies using apt-get. Exiting."; exit 1; }
                ;;
            centos|rhel|fedora)
                log "INFO" "Detected OS: $OS $VERSION. Using yum to install dependencies."
                sudo yum install -y "${packages_to_install[@]}" || { log "ERROR" "Failed to install dependencies using yum. Exiting."; exit 1; }
                ;;
            *)
                log "ERROR" "Unsupported operating system: $OS. Exiting."
                exit 1
                ;;
        esac
    else
        log "INFO" "All required dependencies are already installed. No action needed."
    fi

    log "INFO" "Dependency installation completed."
}

# 创建操作目录并下载建链脚本的函数
setup_directories_and_download() {
    log "INFO" "Creating data directory at $data_dir and navigating to it."
    mkdir -p "$data_dir" && cd "$data_dir" || {
        log "ERROR" "Failed to create or navigate to data directory: $data_dir"
        exit 1
    }

    log "INFO" "Downloading build_chain.sh script."
    curl -#LO "https://osp-1257653870.cos.ap-guangzhou.myqcloud.com/FISCO-BCOS/FISCO-BCOS/releases/v$version/build_chain.sh" && chmod u+x build_chain.sh || {
        log "ERROR" "Failed to download or set execute permission on build_chain.sh."
        exit 1
    }
    log "INFO" "build_chain.sh downloaded and made executable."
}

# 搭建4节点非国密联盟链的函数
build_chain() {
    log "INFO" "Building a 4-node non-GM consortium chain."
    bash build_chain.sh -l 127.0.0.1:4 -p 30300,20200 || {
        log "ERROR" "Failed to build the consortium chain using build_chain.sh."
        exit 1
    }
    log "INFO" "Consortium chain built successfully."
}

# 启动FISCO BCOS链的函数
start_chain() {
    log "INFO" "Starting all FISCO BCOS nodes."
    bash nodes/127.0.0.1/start_all.sh || {
        log "ERROR" "Failed to start FISCO BCOS nodes using start_all.sh."
        exit 1
    }
    log "INFO" "FISCO BCOS nodes started successfully."
}

# 检查节点进程的函数
check_processes() {
    log "INFO" "Checking if FISCO BCOS processes are running."
    if ps aux | grep -v grep | grep fisco-bcos > /dev/null; then
        log "INFO" "FISCO BCOS processes are running."
    else
        log "WARNING" "No FISCO BCOS processes found."
    fi
}

# 配置 SDK 证书的函数
configure_sdk_certificates() {
    log "INFO" "Configuring SDK certificates."

    # 创建 conf 目录
    log "INFO" "Creating configuration directory at $conf_dir."
    mkdir -p "$conf_dir" || {
        log "ERROR" "Failed to create configuration directory: $conf_dir"
        exit 1
    }

    # 复制 SDK 证书文件
    log "INFO" "Copying SDK certificates from $data_dir/nodes/127.0.0.1/sdk/ to $conf_dir."
    cp -r "$data_dir"/nodes/127.0.0.1/sdk/* "$conf_dir" || {
        log "ERROR" "Failed to copy SDK certificates."
        exit 1
    }

    # 复制并修改配置文件
    log "INFO" "Copying and modifying config.toml to $conf_file."
    cp "$SCRIPT_DIR"/config.toml "$conf_file" || {
        log "ERROR" "Failed to copy config.toml to $conf_file."
        exit 1
    }

    # 修改 certPath 路径
    log "INFO" "Updating certPath in $conf_file to $conf_dir."
    sed -i 's|certPath = "conf"|certPath = "'"$conf_dir"'"|' "$conf_file" || {
        log "ERROR" "Failed to update certPath in $conf_file."
        exit 1
    }
    log "INFO" "SDK certificates configured successfully."
}

# 执行所有步骤的主函数
deploy_fiscobcos() {
    read_config
    install_dependencies
    setup_directories_and_download
    build_chain
    start_chain
    check_processes
    configure_sdk_certificates
    log "INFO" "FISCO BCOS deployment completed successfully."
}

# 主函数，按顺序调用各个模块
main() {
    deploy_fiscobcos
}

# 执行主函数
main
