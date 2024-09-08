#!/bin/bash

# 定义一个日志函数，增加时间戳和日志级别
log() {
    local level=$1
    local message=$2
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] [$level] $message"
}
