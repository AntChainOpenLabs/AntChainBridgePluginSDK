#!/bin/bash

DATADIR="/tmp/ethereum"
PID_FILE="$DATADIR/geth.pid"

echo "Checking if PID file exists..."
if [ -f "$PID_FILE" ]; then
  GETH_PID=$(cat "$PID_FILE")
  echo "Found PID file with PID $GETH_PID."

  echo "Checking if Geth process is running..."
  if ps -p "$GETH_PID" > /dev/null; then
    echo "Stopping Geth process with PID $GETH_PID..."

    kill -15 "$GETH_PID"
    sleep 5

    if ps -p "$GETH_PID" > /dev/null; then
      echo "Geth process did not stop, sending SIGKILL..."
      kill -9 "$GETH_PID"
    else
      echo "Geth process stopped successfully."
    fi

    echo "Removing PID file..."
    rm -f "$PID_FILE"
  else
    echo "No Geth process found with PID $GETH_PID. Removing stale PID file."
    rm -f "$PID_FILE"
  fi
else
  echo "PID file not found. Is Geth running?"
fi

echo "Removing data directory at $DATADIR..."
rm -rf "$DATADIR"
echo "Data directory removed."

echo "Cleanup complete."