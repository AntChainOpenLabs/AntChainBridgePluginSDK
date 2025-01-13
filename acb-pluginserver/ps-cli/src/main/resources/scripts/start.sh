#!/bin/bash

#
# Copyright 2023 Ant Group
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

bin=`dirname "${BASH_SOURCE-$0}"`
CLI_HOME=`cd "$bin"; pwd`

which java > /dev/null
if [ $? -eq 1 ]; then
    echo "no java installed. "
    exit 1
fi

Help=$(cat <<-"HELP"

 start.sh — Start the CLI tool to manage your plugin server

 Usage:
   start.sh <params>

 Examples:
  1. print help info:
   start.sh -h
  2. identify the port number to start CLI
   start.sh -p 9091
  3. identify the server IP to start CLI
   start.sh -H 0.0.0.0

 Options:
   -h         print help info
   -p         identify the port number to start CLI, default 9091
   -H         identify the server IP to start CLI, default 127.0.0.1

HELP
)

while getopts "hH:p:" opt
do
  case "$opt" in
    "h")
      echo "$Help"
      exit 0
      ;;
    "H")
      HOST_IP="${OPTARG}"
      ;;
    "p")
      PORT=${OPTARG}
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

JAR_FILE=`ls ${CLI_HOME}/../lib`

java -jar ${CLI_HOME}/../lib/${JAR_FILE} -p ${PORT:-9091} -H ${HOST_IP:-127.0.0.1}