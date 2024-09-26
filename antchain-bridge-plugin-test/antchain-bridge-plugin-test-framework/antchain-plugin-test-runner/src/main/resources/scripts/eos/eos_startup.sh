#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="eos"
source "$SCRIPT_DIR"/../utils.sh


get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir version http_server_address account_name private_key_file

mkdir $data_dir && cd $data_dir || exit

# Check if nodeos command exists
if ! command -v nodeos &> /dev/null
then
    echo "nodeos not found, starting download and installation of eosio..."

    # Download eosio_$version-1-ubuntu-20.04_amd64.deb

    filename="eosio_${version}-1-ubuntu-20.04_amd64.deb"
    if [ -f "$filename" ]; then
    log "INFO" "EOSIO package already downloaded, skipping download."
    else
        curl -L -O https://github.com/EOSIO/eos/releases/download/v$version/$filename
    fi

    # Install the deb package using dpkg
    dpkg -i eosio_"$version"-1-ubuntu-20.04_amd64.deb

    # Check for any missing dependencies and install them
    if [ $? -ne 0 ]; then
        echo "Dependency issues detected, attempting to fix..."
        apt-get install -f -y
        dpkg -i eosio_"$version"-1-ubuntu-20.04_amd64.deb
    fi

    # Clean up the downloaded deb file
    rm eosio_"$version"-1-ubuntu-20.04_amd64.deb

    echo "eosio successfully installed."
else
    echo "nodeos is already installed, skipping installation."
fi

echo "
plugin = eosio::producer_plugin
plugin = eosio::producer_api_plugin
plugin = eosio::chain_api_plugin
plugin = eosio::history_api_plugin
plugin = eosio::http_plugin

http-server-address = 0.0.0.0:8888

enable-stale-production = true

producer-name = eosio

access-control-allow-origin = *
access-control-allow-headers = Content-Type
access-control-allow-credentials = true
http-validate-host = false
" > $data_dir/config.ini

nohup nodeos --config-dir "$data_dir" --data-dir "$data_dir"/data --contracts-console --verbose-http-errors --filter-on "*" > "$data_dir"/nodeos.log 2>&1 &

## 初始化私钥
#mkdir -p "$data_dir"/keosd
## 运行 keosd
##keosd --data-dir /tmp/eosio-wallet/keosd/data --config-dir /tmp/eosio-wallet/keosd --http-server-address=127.0.0.1:8900 &
#keosd --data-dir "$data_dir"/keosd/data --config-dir "$data_dir"/keosd --http-server-address=127.0.0.1:"$http_server_address" &
## 创建钱包
##cleos --wallet-url http://127.0.0.1:8900 wallet create --to-console
#cleos --wallet-url http://127.0.0.1:"$http_server_address" wallet create --to-console
## 解锁钱包
## cleos --wallet-url http://127.0.0.1:8900 wallet unlock --password PW5JATqKRknDMXHVjzQjrawWDQhWwZioCSTPe39x1xpTLcUjLzoAU
#cleos --wallet-url http://127.0.0.1:"$http_server_address" wallet unlock --password PW5JATqKRknDMXHVjzQjrawWDQhWwZioCSTPe39x1xpTLcUjLzoAU
## 导入超级钱包
## cleos --wallet-url http://127.0.0.1:8900 wallet import --private-key 5KQwrPbwdL6PhXujxW37FSSQZ1JiwsST4cqQzDeyXtP79zkvFD3
#cleos --wallet-url http://127.0.0.1:"$http_server_address" wallet import --private-key 5KQwrPbwdL6PhXujxW37FSSQZ1JiwsST4cqQzDeyXtP79zkvFD3
## 创建一对公私钥
#cleos create key --to-console
## 通过超级钱包创建账户
## cleos --wallet-url http://127.0.0.1:8900 create account eosio alice EOS6jncWRTRHYhFfacVdThHsL9hidbof4eyZpQPUsFmbTDAYNkV9f
#cleos --wallet-url http://127.0.0.1:"$http_server_address" create account eosio "account_name" EOS8RnptfAdZbsjssEkmgAWdstPtUm8hLLuRdZRjoRQdqD3mp3ZMT


# 普通钱包
# cleos create key --to-console
# cleos --wallet-url http://127.0.0.1:8900 wallet import --private-key 5K5EyEUcvgK4bC4dJK8DQTKHFFd7FWP46UZXGJBi2hfFzu1k44q
# 使用钱包创建账户
# cleos --wallet-url http://127.0.0.1:8900 create account eosio mynewaccount EOS6jncWRTRHYhFfacVdThHsL9hidbof4eyZpQPUsFmbTDAYNkV9f

# sleep 2 s
sleep 2
cleos wallet create --to-console
cleos wallet import --private-key 5KQwrPbwdL6PhXujxW37FSSQZ1JiwsST4cqQzDeyXtP79zkvFD3
# 创建 test 账户
output=$(cleos create key --to-console 2>&1)
private_key=$(echo "$output" | grep "Private key" | awk '{print $3}')
public_key=$(echo "$output" | grep "Public key" | awk '{print $3}')
cleos wallet import --private-key "$private_key"
cleos create account eosio test "$public_key" "$public_key"
echo "$private_key" > "$private_key_file"
# 创建 sys.am 账户
#output=$(cleos create key --to-console 2>&1)
#private_key=$(echo "$output" | grep "Private key" | awk '{print $3}')
#public_key=$(echo "$output" | grep "Public key" | awk '{print $3}')
#cleos wallet import --private-key "$private_key"
#cleos create account eosio sys.am "$public_key" "$public_key"
#echo "$private_key" > "$data_dir"/sys_am_private_key.txt
