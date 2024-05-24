#!/bin/bash

CURDIR="$(cd `dirname $0`; pwd)"

cd ${CURDIR}/../../ethereum/offchain-plugin/

mvn web3j:generate-sources

mvn clean install -Dmaven.test.skip=true
