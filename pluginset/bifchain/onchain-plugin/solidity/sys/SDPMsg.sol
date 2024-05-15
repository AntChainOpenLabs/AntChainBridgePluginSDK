pragma solidity ^0.8.0;

import "./interfaces/ISDPMessage.sol";
import "./interfaces/IAuthMessage.sol";
import "./interfaces/IContractUsingSDP.sol";
import "./lib/sdp/SDPLib.sol";
import "./lib/utils/Ownable.sol";

contract SDPMsg is ISDPMessage, Ownable {

    uint32 constant UNORDERED_SEQUENCE = 0xffffffff;

    address public amAddress;

    bytes32 public localDomainHash;

    mapping(bytes32 => uint32) sendSeq;
    mapping(bytes32 => uint32) recvSeq;

    modifier onlyAM() {
        require(
            amAddress == msg.sender,
            "SDPMsg: not valid am contract"
        );
        _;
    }

    function setAmContract(address newAmContract) override external onlyOwner {
        require(newAmContract != address(0), "SDPMsg: invalid am contract");
        amAddress = newAmContract;
    }

    function getAmAddress() external view returns (address) {
        return amAddress;
    }

    function setLocalDomain(string memory domain) external override onlyOwner {
        localDomainHash = keccak256(abi.encodePacked(domain));
    }

    function getLocalDomain() external view returns (bytes32) {
        return localDomainHash;
    }

    function sendMessage(string calldata receiverDomain, bytes32 receiverID, bytes calldata message) override external {
        _beforeSend(receiverDomain, receiverID, message);

        SDPMessage memory sdpMessage = SDPMessage(
            {
                receiveDomain: receiverDomain,
                receiver: receiverID,
                message: message,
                sequence: _getAndUpdateSendSeq(receiverDomain, msg.sender, receiverID)
            }
        );

        bytes memory rawMsg = SDPLib.encodeSDPMessage(sdpMessage);

        IAuthMessage(amAddress).recvFromProtocol(msg.sender, rawMsg);

        _afterSend();
    }

    function sendUnorderedMessage(string calldata receiverDomain, bytes32 receiverID, bytes calldata message) override external {
        _beforeSendUnordered(receiverDomain, receiverID, message);

        SDPMessage memory sdpMessage = SDPMessage(
            {
                receiveDomain: receiverDomain,
                receiver: receiverID,
                message: message,
                sequence: UNORDERED_SEQUENCE
            }
        );

        bytes memory rawMsg = SDPLib.encodeSDPMessage(sdpMessage);

        IAuthMessage(amAddress).recvFromProtocol(msg.sender, rawMsg);

        _afterSendUnordered();
    }

    function recvMessage(string calldata senderDomain, bytes32 senderID, bytes calldata pkg) override external onlyAM {
        _beforeRecv(senderDomain, senderID, pkg);

        SDPMessage memory sdpMessage = SDPLib.decodeSDPMessage(pkg);

        require(
            keccak256(abi.encodePacked(sdpMessage.receiveDomain)) == localDomainHash,
            "SDPMsg: wrong receiving domain"
        );

        if (sdpMessage.sequence == UNORDERED_SEQUENCE) {
            _routeUnorderedMessage(senderDomain, senderID, sdpMessage);
        } else {
            _routeOrderedMessage(senderDomain, senderID, sdpMessage);
        }

        _afterRecv();
    }

    function querySDPMessageSeq(string calldata senderDomain, bytes32 senderID, string calldata receiverDomain, bytes32 receiverID) override external view returns (uint32) {
        require(
            keccak256(abi.encodePacked(receiverDomain)) == localDomainHash,
            "SDPMsg: wrong receiving domain"
        );
        bytes32 seqKey = SDPLib.getReceivingSeqID(senderDomain, senderID, receiverID);
        uint32 seq = recvSeq[seqKey];
        return seq;
    }

    function _routeOrderedMessage(string calldata senderDomain, bytes32 senderID, SDPMessage memory sdpMessage) internal {
        uint32 seqExpected = _getAndUpdateRecvSeq(senderDomain, senderID, sdpMessage.receiver);
        require(
            sdpMessage.sequence == seqExpected,
            "SDPMsg: sequence not equal"
        );

        IContractUsingSDP(SDPLib.encodeCrossChainIDIntoAddress(sdpMessage.receiver))
                .recvMessage(senderDomain, senderID, sdpMessage.message);
    }

    function _routeUnorderedMessage(string calldata senderDomain, bytes32 senderID, SDPMessage memory sdpMessage) internal {
        IContractUsingSDP(SDPLib.encodeCrossChainIDIntoAddress(sdpMessage.receiver))
                .recvUnorderedMessage(senderDomain, senderID, sdpMessage.message);
    }

    function _getAndUpdateSendSeq(string memory receiveDomain, address sender, bytes32 receiver) internal returns (uint32) {
        bytes32 seqKey = SDPLib.getSendingSeqID(receiveDomain, sender, receiver);
        uint32 seq = sendSeq[seqKey];
        sendSeq[seqKey]++;
        return seq;
    }

    function _getAndUpdateRecvSeq(string memory sendDomain, bytes32 sender, bytes32 receiver) internal returns (uint32) {
        bytes32 seqKey = SDPLib.getReceivingSeqID(sendDomain, sender, receiver);
        uint32 seq = recvSeq[seqKey];
        recvSeq[seqKey]++;
        return seq;
    }

    function _beforeSend(string calldata receiverDomain, bytes32 receiverID, bytes calldata message) internal {}

    function _afterSend() internal {}

    function _beforeSendUnordered(string calldata receiverDomain, bytes32 receiverID, bytes calldata message) internal {}

    function _afterSendUnordered() internal {}

    function _beforeRecv(string calldata senderDomain, bytes32 senderID, bytes calldata pkg) internal {}

    function _afterRecv() internal {}

    /**
     * @dev This empty reserved space is put in place to allow future versions to add new
     * variables without shifting down storage in the inheritance chain.
     * See https://docs.openzeppelin.com/contracts/4.x/upgradeable#storage_gaps
     */
    uint256[50] private __gap;
}
