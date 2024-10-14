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
    log "INFO" "Configured Hyperchain version: $version"
}

# 获取指定版本的 Docker 镜像名称的函数
get_docker_image() {
    image="hyperchaincn/solo:v$version"
    log "INFO" "Docker image set to: $image"
}

# 获取所有使用指定镜像创建的容器 ID 的函数
get_container_ids() {
    container_ids=$(docker ps -a --filter "ancestor=$image" --format "{{.ID}}")
    log "INFO" "Retrieved container IDs: $container_ids"
}

# 检查是否有相关容器存在的函数
check_containers_exist() {
    if [ -z "$container_ids" ]; then
        log "INFO" "No containers found for $image."
        return 1
    else
        log "INFO" "Found containers for $image: $container_ids"
        return 0
    fi
}

# 停止指定容器的函数
stop_containers() {
    log "INFO" "Stopping containers: $container_ids"
    docker stop $container_ids
    if [ $? -eq 0 ]; then
        log "INFO" "Successfully stopped containers: $container_ids"
    else
        log "ERROR" "Failed to stop some containers: $container_ids"
        exit 1
    fi
}

# 移除指定容器的函数
remove_containers() {
    log "INFO" "Removing containers: $container_ids"
    docker rm $container_ids
    if [ $? -eq 0 ]; then
        log "INFO" "Successfully removed containers: $container_ids"
    else
        log "ERROR" "Failed to remove some containers: $container_ids"
        exit 1
    fi
}

# 执行清理操作的主函数
cleanup_containers() {
    get_container_ids
    if check_containers_exist; then
        stop_containers
        remove_containers
        log "INFO" "All related containers have been stopped and removed."
    else
        log "INFO" "No cleanup needed."
    fi
}

# 部署操作的主函数
deploy_hyperchain() {
    read_config
    get_docker_image
    cleanup_containers
}

# 主函数，按顺序调用各个模块
main() {
    deploy_hyperchain
    log "INFO" "Hyperchain deployment cleanup completed successfully."
}

# 执行主函数
main
