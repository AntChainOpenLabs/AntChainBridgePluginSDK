pragma solidity ^0.4.22;

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