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

>The solidity contract used by the current demo is exactly the same as
> the solidity contract source file of the Ethereum demo,
> but the java code generation method is different.
> The contract generation method of FISCO-BCOS is as follows.

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

```shell
mvn clean package -Dmaven.test.skip=true
```

# Run Demo

## Generate configuration file

Change directory to blockchain certificate path like  `~/fisco/127.0.0.1/sdk`

Run generate.sh

```sh
#!/bin/bash

# Read the contents of certificate files and store them as variables
CA_CERT=$(awk '{printf "%s\\n", $0}' ca.crt)
SSL_CERT=$(awk '{printf "%s\\n", $0}' sdk.crt)
SDK_KEY=$(awk '{printf "%s\\n", $0}' sdk.key)

# Create fiscobcos.json
cat > fiscobcos.json << EOF
{
  "caCert": "$CA_CERT",
  "sslCert": "$SSL_CERT",
  "sslKey": "$SDK_KEY",
  "connectPeer": "your_IP:your_port",
  "groupID": "your_group"
}
EOF

```

Copy fiscobcos.json to relayer server

## Prepare contracts

### Create file for contracts code

```sh
cd ~/fisco/console
touch contracts/solidity/SenderContract.sol
touch contracts/solidity/ReceiverContract.sol
```

```sol
pragma solidity ^0.8.0;

interface ProtocolInterface {
    function sendMessage(
        string calldata _destination_domain,
        bytes32 _receiver,
        bytes calldata _message
    ) external;

    function sendUnorderedMessage(
        string calldata _destination_domain,
        bytes32 _receiver,
        bytes calldata _message
    ) external;
}

contract SenderContract {
    address sdp_address;

    function setSdpMSGAddress(address _sdp_address) public {
        sdp_address = _sdp_address;
    }

    function send(
        bytes32 receiver,
        string memory domain,
        bytes memory _msg
    ) public {
        ProtocolInterface sdp = ProtocolInterface(sdp_address);
        sdp.sendMessage(domain, receiver, _msg);
    }

    function sendUnordered(
        bytes32 receiver,
        string memory domain,
        bytes memory _msg
    ) public {
        ProtocolInterface sdp = ProtocolInterface(sdp_address);
        sdp.sendUnorderedMessage(domain, receiver, _msg);
    }
}
```

```sh
pragma solidity ^0.8.0;

contract ReceiverContract {
    bytes last_msg;
    bytes last_uo_msg;

    event amNotify(string key, bytes32 value, string enterprise);

    function recvMessage(
        string memory domain_name,
        bytes32 author,
        bytes memory message
    ) public {
        require(message.length != 32, "32B");
        last_msg = message;
        emit amNotify(domain_name, author, string(message));
    }

    function getLastMsg() public view returns (bytes memory) {
        return last_msg;
    }

    function recvUnorderedMessage(
        string memory domain_name,
        bytes32 author,
        bytes memory message
    ) public {
        require(message.length != 32, "32B");
        last_uo_msg = message;
        emit amNotify(domain_name, author, string(message));
    }

    function getLastUnorderedMsg() public view returns (bytes memory) {
        return last_uo_msg;
    }
}
```

### Start  console

```sh
./start.sh
```

### Deploy contracts

```sh
deploy SenderContract
deploy ReceiverContract
```

### Set SDP address

```sh
call SenderContract {SenderContractAddress} setSdpMSGAddress "{SDPContractAddress}"
```



## Configure  authorization on relayer

```sh
relayer:> add-cross-chain-msg-acl --grantDomain {domain1} --grantIdentity {SenderContractAddress} --ownerDomain {domain2} --ownerIdentity {ReceiverContractAddress}
```

## Send and receive msg

### Send

```sh
call SenderContract {SenderContractAddress} sendUnordered "0x000000000000000000000000{ReceiverContractAddress}" "{domain2}" "{Msg}"
```

### Receive

```sh
call ReceiverContract {ReceiverContractAddress} getLastUnorderedMsg
```

