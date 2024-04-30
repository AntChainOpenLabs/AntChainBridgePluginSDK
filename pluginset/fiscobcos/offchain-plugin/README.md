<div align="center">
  <img alt="am logo" src="https://gw.alipayobjects.com/zos/bmw-prod/3ee4adc7-1960-4dbf-982e-522ac135a0c0.svg" width="250" >
  <h1 align="center">FISCO-BCOS Plugin</h1>
  <p align="center">
    <a href="http://makeapullrequest.com">
      <img alt="pull requests welcome badge" src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat">
    </a>
  </p>
</div>


# Build

1. Run the following command to generate contracts abi code:
- Get console tool for FISCO-BCOS
```shell
mkdir -p ~/fisco && cd ~/fisco
# Fetch console
curl -#LO https://github.com/FISCO-BCOS/console/releases/download/v3.6.0/download_console.sh

# If you are unable to execute the above commands for a long time due to network problems, please try the following commands:
curl -#LO https://gitee.com/FISCO-BCOS/console/raw/master/tools/download_console.sh

bash download_console.sh
```
- Place the contract in the contract directory of the console
```shell
cp -r /onchain-plugin/solidity/* ~/fisco/console/contracts/solidity
```
- Generate Java code, abi file and bin file 
```shell
cd ~/fisco/console
bash contract2java.sh solidity -p com.alipay.antchain.bridge.plugins.fiscobcos -s ./contracts/solidity/sys-contract/AuthMsg.sol
bash contract2java.sh solidity -p com.alipay.antchain.bridge.plugins.fiscobcos -s ./contracts/solidity/sys-contract/SDPMsg.sol
bash contract2java.sh solidity -p com.alipay.antchain.bridge.plugins.fiscobcos -s ./contracts/solidity/sys-contract/AppContract.sol
```
-  Copy result to offchain directory

```shell
cp -r ~/fisco/console/contracts/sdk/java/com/alipay/antchain/bridge/plugins/fiscobcos/*  /offchain-plugin/src/main/java/com/alipay/antchain/bridge/plugins/fiscobcos/abi
```

2. Then execute the compile command in the plugin project directory 
   to get the jar for use as a plugin

```agsl
mvn clean package -Dmaven.test.skip=true
```