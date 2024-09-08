#!/bin/bash
# https://docs.chainmaker.org.cn/v3.0.0/html/quickstart/%E9%80%9A%E8%BF%87%E5%91%BD%E4%BB%A4%E8%A1%8C%E4%BD%93%E9%AA%8C%E9%93%BE.html

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="chainmaker"
source "$SCRIPT_DIR"/../utils.sh


# 读取配置文件
get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir version

# step1. 创建并打开目录
mkdir -p $data_dir && cd $data_dir || exit

# step2. 源码下载
# 下载源码
REPO_1_URL="https://git.chainmaker.org.cn/chainmaker/chainmaker-go.git"
REPO_1_BRANCH="v$version"
REPO_1_DIR="chainmaker-go"

REPO_2_URL="https://git.chainmaker.org.cn/chainmaker/chainmaker-cryptogen.git"
REPO_2_BRANCH="v$version"
REPO_2_DIR="chainmaker-cryptogen"

# 判断第一个目录是否存在
if [ -d "$REPO_1_DIR" ]; then
    log "INFO" "Directory $REPO_1_DIR already exists. Skipping clone for $REPO_1_DIR."
else
    log "INFO" "Cloning $REPO_1_URL into $REPO_1_DIR..."
    git clone -b "$REPO_1_BRANCH" --depth=1 "$REPO_1_URL"
fi

# 判断第二个目录是否存在
if [ -d "$REPO_2_DIR" ]; then
    log "INFO" "Directory $REPO_2_DIR already exists. Skipping clone for $REPO_2_DIR."
else
    log "INFO" "Cloning $REPO_2_URL into $REPO_2_DIR..."
    git clone -b "$REPO_2_BRANCH" --depth=1 "$REPO_2_URL"
fi

# step3. 源码编译
cd chainmaker-cryptogen || exit
make

# step4. 配置文件生成
# 将编译好的chainmaker-cryptogen，软连接到chainmaker-go/tools目录
cd ../chainmaker-go/tools || exit
ln -s ../../chainmaker-cryptogen/ .

# 采用原始的身份模式，即证书模式（PermissionedWithCert）
# 进入脚本目录
cd ../scripts || exit

# 生成单链4节点集群的证书和配置
# ./prepare.sh 4 1
yes "" | head -n 4 | ./prepare.sh 4 1


# step4. 编译及安装包制作
./build_release.sh

# step5. 启动节点集群
# 执行cluster_quick_start.sh脚本，会解压各个安装包，调用bin目录中的start.sh脚本，启动chainmaker节点
./cluster_quick_start.sh normal

# 启动成功后，将*.tar.gz备份，以免下次启动再次解压缩时文件被覆盖
mkdir -p ../build/bak
mv ../build/release/*.tar.gz ../build/bak

# step6. 查看节点启动使用正常
# 查看进程是否存在
ps -ef|grep chainmaker | grep -v grep
