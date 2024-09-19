#!/bin/bash
# https://docs.hyperchain.cn/document/detail?type=1&id=58
# https://docs.hyperchain.cn/docs/flato-solo/3.2-flato-tutorial
# https://github.com/hyperchain/hyperchain/releases

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="hyperchain"
source "$SCRIPT_DIR"/../utils.sh


get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" version

# step1. 运行 docker
image="hyperchaincn/solo:v$version"

# 检查是否已经有指定版本的镜像
if docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "$image"; then
  log "INFO" "Image $image already exists, skipping pull."
else
  log "INFO" "Image $image not found, pulling the image..."
  docker pull "$image"
fi

log "INFO" "Starting hyperchaincn/solo:v$version..."
docker run -d -p 8081:8081 hyperchaincn/solo:v"$version"