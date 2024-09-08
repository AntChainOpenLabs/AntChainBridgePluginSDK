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
log "INFO" "Pulling hyperchaincn/solo:v$version..."
docker pull hyperchaincn/solo:v"$version"

log "INFO" "Starting hyperchaincn/solo:v$version..."
docker run -d -p 8081:8081 hyperchaincn/solo:v"$version"