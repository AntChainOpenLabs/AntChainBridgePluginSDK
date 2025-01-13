#!/bin/bash

bin=`dirname "${BASH_SOURCE-$0}"`
SUPERVISOR_CLI_HOME=`cd "$bin"; pwd`

Help=$(
  cat <<-"HELP"

 start.sh - Start the Committee-ptc Supervisor Command Line Interface Tool

 Usage:
   start.sh <params>

 Examples:
  1. start with the default supervisor config file `supervisor-cli/conf/config.json`：
   start.sh
  2. start with specific supervisor config file:
   start.sh -c ${Path}/supervisor-cli-config.json}

 Options:
   -c         config path of Supervisor.
   -h         print help information.

HELP
)

CURR_DIR="$(
  cd $(dirname $0)
  pwd
)"

while getopts "hH:c:" opt; do
  case "$opt" in
  "h")
    echo "$Help"
    exit 0
    ;;
  "c")
    CONFIG_FILE=$OPTARG
    ;;
  "?")
    echo "invalid arguments. "
    exit 1
    ;;
  *)
    echo "Unknown error while processing options"
    exit 1
    ;;
  esac
done


which java > /dev/null
if [ $? -eq 1 ]; then
    echo "no java installed. "
    exit 1
fi

java -jar ${SUPERVISOR_CLI_HOME}/../lib/supervisor-cli.jar --conf=${CONFIG_FILE:-"supervisor-cli/conf/config.json"}