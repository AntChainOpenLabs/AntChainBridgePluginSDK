#!/bin/bash
# https://docs.hyperchain.cn/document/detail?type=1&id=58
# https://docs.hyperchain.cn/docs/flato-solo/3.2-flato-tutorial
# https://github.com/hyperchain/hyperchain/releases

# 设置脚本目录和配置文件路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="hyperchain"

# 引入工具脚本
source "$SCRIPT_DIR"/../utils.sh


# 读取配置文件的函数
read_config() {
    log "INFO" "Reading configuration from $CONFIG_FILE for chain type $CHAIN_TYPE"
    get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" version
    # 设置镜像名称
    image="hyperchaincn/solo:v$version"
    log "INFO" "Configured Hyperchain version: $version"
    log "INFO" "Docker image set to: $image"
}

# 检查 Docker 是否安装的函数
check_docker_installed() {
    if ! command -v docker &> /dev/null; then
        log "ERROR" "Docker is not installed. Please install Docker and try again."
        exit 1
    fi
    log "INFO" "Docker is installed."
}

# 检查并拉取 Docker 镜像的函数
pull_docker_image() {
    log "INFO" "Checking if Docker image $image exists..."
    if docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^$image$"; then
        log "INFO" "Image $image already exists, skipping pull."
    else
        log "INFO" "Image $image not found, pulling the image..."
        if docker pull "$image"; then
            log "INFO" "Successfully pulled Docker image $image."
        else
            log "ERROR" "Failed to pull Docker image $image."
            exit 1
        fi
    fi
}

# 启动 Docker 容器的函数
start_docker_container() {
    log "INFO" "Starting Docker container with image $image..."

    # 检查是否已经有运行中的容器使用相同的端口
    if lsof -i:8081 &> /dev/null; then
        log "ERROR" "Port 8081 is already in use. Please free the port and try again."
        exit 1
    fi

    # 启动容器
    CONTAINER_ID=$(docker run -d -p 8081:8081 "$image")

    if [ -n "$CONTAINER_ID" ]; then
        log "INFO" "Docker container started successfully with Container ID: $CONTAINER_ID"
    else
        log "ERROR" "Failed to start Docker container with image $image."
        exit 1
    fi
}

# 执行部署操作的主函数
deploy_hyperchain() {
    read_config
    check_docker_installed
    pull_docker_image
    start_docker_container
}

# 主函数，按顺序调用各个模块
main() {
    deploy_hyperchain
    log "INFO" "Hyperchain deployment completed successfully."
}

# 执行主函数
main
