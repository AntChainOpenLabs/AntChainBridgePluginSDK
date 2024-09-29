#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="ethereum"
source "$SCRIPT_DIR"/../utils.sh


# 读取配置文件
get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" data_dir

data_dir="/tmp/ethereum"
pid_file="$data_dir/geth.pid"

log "INFO" "Checking if PID file exists..."
if [ -f "$pid_file" ]; then
  GETH_PID=$(cat "$pid_file")
  log "INFO" "Found PID file with PID $GETH_PID."

  log "INFO" "Checking if Geth process is running..."
  if ps -p "$GETH_PID" > /dev/null; then
    log "INFO" "Stopping Geth process with PID $GETH_PID..."

    kill -15 "$GETH_PID"
    sleep 5

    if ps -p "$GETH_PID" > /dev/null; then
      log "WARNING" "Geth process did not stop, sending SIGKILL..."
      kill -9 "$GETH_PID"
    else
      log "INFO" "Geth process stopped successfully."
    fi

    log "INFO" "Removing PID file..."
    rm -f "$pid_file"
  else
    log "WARNING" "No Geth process found with PID $GETH_PID. Removing stale PID file."
    rm -f "$pid_file"
  fi
else
  log "ERROR" "PID file not found. Is Geth running?"
fi

echo "Removing data directory at $data_dir..."
rm -rf "$data_dir"
echo "Data directory removed."

echo "Cleanup complete."