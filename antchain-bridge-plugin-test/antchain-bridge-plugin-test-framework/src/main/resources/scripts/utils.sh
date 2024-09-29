#!/bin/bash

# 定义函数，传入配置文件、链类型和所需的属性
get_config_values() {
  local config_file=$1
  local chain_type=$2
  shift 2
  local properties=("$@")

  # 检查配置文件是否存在
  if [ -f "$config_file" ]; then
    # 遍历需要获取的属性并从配置文件中提取值
    for property in "${properties[@]}"; do
      value=$(grep "^${chain_type}.${property}" "$config_file" | cut -d'=' -f2)

      # 验证是否成功获取到值
      if [ -z "$value" ]; then
        log "INFO" "${property} not found in the configuration file!"
      else
        # 动态地将属性值赋值给变量，并且不使用 local，使变量可以在全局作用域中使用
        eval "${property}='${value}'"
        # 输出调试信息，确保变量被正确赋值
        log "INFO" "${property}=${value}"
      fi
    done
  else
    log "INFO" "Configuration file not found!"
    exit 1
  fi
}

check_installation() {
    for package in "$@"; do
        dpkg -l | grep -q "$package"
        if [ $? -eq 0 ]; then
            log "INFO" "$package is already installed."
        else
            log "INFO" "$package is not installed. Installing..."
            apt install -y "$package"
        fi
    done
}

log() {
    local level=$1
    local message=$2
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] [$level] $message"
}
