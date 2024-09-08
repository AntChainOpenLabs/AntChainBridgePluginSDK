#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="eos"
source "$SCRIPT_DIR"/../utils.sh


get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir version

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
    dpkg -i eosio_$version-1-ubuntu-20.04_amd64.deb

    # Check for any missing dependencies and install them
    if [ $? -ne 0 ]; then
        echo "Dependency issues detected, attempting to fix..."
        apt-get install -f -y
        dpkg -i eosio_$version-1-ubuntu-20.04_amd64.deb
    fi

    # Clean up the downloaded deb file
    rm eosio_$version-1-ubuntu-20.04_amd64.deb

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

access-control-allow-origin = *
access-control-allow-headers = Content-Type
access-control-allow-credentials = true
http-validate-host = false
" > $data_dir/config.ini

nohup nodeos --config-dir $data_dir --data-dir $data_dir/data --contracts-console --verbose-http-errors --filter-on "*" > $data_dir/nodeos.log 2>&1 &
