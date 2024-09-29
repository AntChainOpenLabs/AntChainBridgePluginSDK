#!/bin/bash
# https://fisco-bcos-doc.readthedocs.io/zh-cn/latest/docs/quick_start/air_installation.html


SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="fiscobcos"
source "$SCRIPT_DIR"/../utils.sh


get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir version conf_dir conf_file

# step1. 安装ubuntu依赖
check_installation curl openssl wget

# step2. 创建操作目录，下载安装脚本
# 创建操作目录
mkdir -p "$data_dir" && cd "$data_dir" || exit

# 下载建链脚本
curl -#LO https://osp-1257653870.cos.ap-guangzhou.myqcloud.com/FISCO-BCOS/FISCO-BCOS/releases/v"$version"/build_chain.sh && chmod u+x build_chain.sh

# step3. 搭建4节点非国密联盟链
bash build_chain.sh -l 127.0.0.1:4 -p 30300,20200

# step4. 启动FISCO BCOS链
# 启动所有节点
bash nodes/127.0.0.1/start_all.sh

# step5. 检查节点进程
ps aux |grep -v grep |grep fisco-bcos

# step6. 配置 SDK 证书
# 在脚本执行目录下创建 conf 目录
mkdir -p "$conf_dir" && cp -r "$data_dir"/nodes/127.0.0.1/sdk/* "$conf_dir"
cp "$SCRIPT_DIR"/config.toml "$conf_file"
# 修改 toml 中的 cert 路径
sed -i 's|certPath = "conf"|certPath = "'"$conf_dir"'"|' "$conf_file"


