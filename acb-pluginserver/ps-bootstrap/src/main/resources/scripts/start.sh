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

Help=$(
  cat <<-"HELP"

 start.sh — Start the plugin server

 Usage:
   start.sh <params>

 Examples:
  1. start in system service mode：
   start.sh -s
  2. start in application mode:
   start.sh

 Options:
   -s         run in system service mode.
   -d         add env variable for java process.
   -h         print help information.

HELP
)

CURR_DIR="$(
  cd $(dirname $0)
  pwd
)"

while getopts "hsd:" opt; do
  case "$opt" in
  "h")
    echo "$Help"
    exit 0
    ;;
  "s")
    IF_SYS_MODE="on"
    ;;
  "d")
    JAVA_EXTRA_ENV=$OPTARG
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

source ${CURR_DIR}/print.sh

print_title

JAR_FILE=$(ls ${CURR_DIR}/../lib/ | grep '.jar')

if [ "$IF_SYS_MODE" == "on" ]; then
  if [[ "$OSTYPE" == "darwin"* ]]; then
    log_error "${OSTYPE} not support running in system service mode"
    exit 1
  fi

  touch /usr/lib/systemd/system/test123 >/dev/null && rm -f /usr/lib/systemd/system/test123
  if [ $? -ne 0 ]; then
    log_error "Your account on this OS must have authority to access /usr/lib/systemd/system/"
    exit 1
  fi

  log_info "running in system service mode"

  JAVA_BIN=$(which java)
  if [ -z "$JAVA_BIN" ]; then
    log_error "install jdk before start"
    exit 1
  fi
  START_CMD="${JAVA_BIN} -Dio.netty.native.detectNativeLibraryDuplicates=false ${JAVA_EXTRA_ENV} -jar -Dlogging.file.path=${CURR_DIR}/../log ${CURR_DIR}/../lib/${JAR_FILE} --spring.config.location=file:${CURR_DIR}/../config/application.yml"
  WORK_DIR="$(
    cd ${CURR_DIR}/..
    pwd
  )"

  sed -i -e "s#@@START_CMD@@#${START_CMD}#g" ${CURR_DIR}/plugin-server.service
  sed -i -e "s#@@WORKING_DIR@@#${WORK_DIR}#g" ${CURR_DIR}/plugin-server.service

  cp -f ${CURR_DIR}/plugin-server.service /usr/lib/systemd/system/
  if [ $? -ne 0 ]; then
    log_error "failed to cp plugin-server.service to /usr/lib/systemd/system/"
    exit 1
  fi

  systemctl daemon-reload && systemctl enable plugin-server.service
  if [ $? -ne 0 ]; then
    log_error "failed to enable plugin-server.service"
    exit 1
  fi

  systemctl start plugin-server
  if [ $? -ne 0 ]; then
    log_error "failed to start plugin-server.service"
    exit 1
  fi

else
  log_info "running in app mode"
  log_info "start plugin-server now..."

  cd ${CURR_DIR}/..
  java -Dio.netty.native.detectNativeLibraryDuplicates=false ${JAVA_EXTRA_ENV} -jar -Dlogging.file.path=${CURR_DIR}/../log ${CURR_DIR}/../lib/${JAR_FILE} --spring.config.location=file:${CURR_DIR}/../config/application.yml >/dev/null 2>&1 &
  if [ $? -ne 0 ]; then
    log_error "failed to start plugin-server"
    exit 1
  fi
fi

log_info "plugin-server started successfully"
