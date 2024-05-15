pragma solidity ^0.8.0;

import "./interfaces/IContractUsingSDP.sol";
import "./interfaces/ISDPMessage.sol";
import "./lib/utils/Ownable.sol";

contract AppContract is IContractUsingSDP, Ownable {

    mapping(bytes32 => bytes[]) public recvMsg;

    mapping(bytes32 => bytes[]) public sendMsg;

    address public sdpAddress;

    event recvCrosschainMsg(string senderDomain, bytes32 author, bytes message, bool isOrdered);

    event sendCrosschainMsg(string receiverDomain, bytes32 receiver, bytes  message, bool isOrdered);

    modifier onlySdpMsg() {
        require(msg.sender == sdpAddress, "INVALID_PERMISSION");
        _;
    }

    function setProtocol(address protocolAddress) external onlyOwner {
        sdpAddress = protocolAddress;
    }

    function recvUnorderedMessage(string memory senderDomain, bytes32 author, bytes memory message) external override onlySdpMsg {
        recvMsg[author].push(message);
        emit recvCrosschainMsg(senderDomain, author, message, false);
    }

    function recvMessage(string memory senderDomain, bytes32 author, bytes memory message) external override onlySdpMsg {
        recvMsg[author].push(message);
        emit recvCrosschainMsg(senderDomain, author, message, true);
    }

    function sendUnorderedMessage(string memory receiverDomain, bytes32 receiver, bytes memory message) external {
        ISDPMessage(sdpAddress).sendUnorderedMessage(receiverDomain, receiver, message);

        sendMsg[receiver].push(message);
        emit sendCrosschainMsg(receiverDomain, receiver, message, false);
    }

    function sendMessage(string memory receiverDomain, bytes32 receiver, bytes memory message) external{

        ISDPMessage(sdpAddress).sendMessage(receiverDomain, receiver, message);

        sendMsg[receiver].push(message);
        emit sendCrosschainMsg(receiverDomain, receiver, message, true);
    }

    /**
     * @dev This empty reserved space is put in place to allow future versions to add new
     * variables without shifting down storage in the inheritance chain.
     * See https://docs.openzeppelin.com/contracts/4.x/upgradeable#storage_gaps
     */
    uint256[50] private __gap;
}