pragma solidity ^0.8.0;

interface IContractUsingSDP {

    function recvUnorderedMessage(string memory senderDomain, bytes32 author, bytes memory message) external;

    function recvMessage(string memory senderDomain, bytes32 author, bytes memory message) external;
}
