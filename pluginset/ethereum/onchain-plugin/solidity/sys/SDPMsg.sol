// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

import "./interfaces/ISDPMessage.sol";
import "./interfaces/IAuthMessage.sol";
import "./interfaces/IContractUsingSDP.sol";
import "./lib/sdp/SDPLib.sol";
import "./lib/utils/Ownable.sol";

contract SDPMsg is ISDPMessage, Ownable {

    using SDPLib for SDPMessage;
    using SDPLib for SDPMessageV2;

    address public amAddress;

    bytes32 public localDomainHash;

    mapping(bytes32 => uint32) sendSeq;
    mapping(bytes32 => uint32) recvSeq;

    mapping(bytes32 => uint32) sendNonce;

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

        bytes memory rawMsg = sdpMessage.encodeSDPMessage();

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
                sequence: SDPLib.UNORDERED_SEQUENCE
            }
        );

        IAuthMessage(amAddress).recvFromProtocol(msg.sender, sdpMessage.encodeSDPMessage());

        _afterSendUnordered();
    }

    function sendMessageV2(string calldata receiverDomain, bytes32 receiverID, bool atomic, bytes calldata message) override external returns (uint32) {
         _beforeSend(receiverDomain, receiverID, message);

        SDPMessageV2 memory sdpMessage = SDPMessageV2(
            {
                version: 2,
                receiveDomain: receiverDomain,
                receiver: receiverID,
                atomicFlag: atomic ? SDPLib.SDP_V2_ATOMIC_FLAG_ATOMIC_REQUEST : SDPLib.SDP_V2_ATOMIC_FLAG_NONE_ATOMIC,
                nonce: SDPLib.MAX_NONCE,
                sequence: _getAndUpdateSendSeq(receiverDomain, msg.sender, receiverID),
                message: message
            }
        );

        IAuthMessage(amAddress).recvFromProtocol(msg.sender, sdpMessage.encodeSDPMessage());

        _afterSend();

        return sdpMessage.sequence;
    }

    function sendUnorderedMessageV2(string calldata receiverDomain, bytes32 receiverID, bool atomic, bytes calldata message) external returns (uint64) {
        _beforeSendUnordered(receiverDomain, receiverID, message);

        SDPMessageV2 memory sdpMessage = SDPMessageV2(
            {
                version: 2,
                receiveDomain: receiverDomain,
                receiver: receiverID,
                atomicFlag: atomic ? SDPLib.SDP_V2_ATOMIC_FLAG_ATOMIC_REQUEST : SDPLib.SDP_V2_ATOMIC_FLAG_NONE_ATOMIC,
                nonce: _getAndUpdateSendNonce(receiverDomain, msg.sender, receiverID),
                sequence: SDPLib.UNORDERED_SEQUENCE,
                message: message
            }
        );

        IAuthMessage(amAddress).recvFromProtocol(msg.sender, sdpMessage.encodeSDPMessage());

        _afterSendUnordered();

        return sdpMessage.nonce;
    }

    function recvMessage(string calldata senderDomain, bytes32 senderID, bytes calldata pkg) override external onlyAM {
        _beforeRecv(senderDomain, senderID, pkg);

        SDPMessage memory sdpMessage;
        sdpMessage.decodeSDPMessage(pkg);

        require(
            keccak256(abi.encodePacked(sdpMessage.receiveDomain)) == localDomainHash,
            "SDPMsg: wrong receiving domain"
        );

        if (sdpMessage.sequence == SDPLib.UNORDERED_SEQUENCE) {
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

        bool res = false;
        string memory errMsg;
        address receiver = SDPLib.encodeCrossChainIDIntoAddress(sdpMessage.receiver);
        try
            IContractUsingSDP(receiver).recvMessage(senderDomain, senderID, sdpMessage.message)
        {
            res = true;
        } catch Error(
            string memory reason
        ) {
            errMsg = reason;
        } catch (
            bytes memory /*lowLevelData*/
        ) {}

        emit receiveMessage(senderDomain, senderID, receiver, seqExpected, res, errMsg);
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

    function _getAndUpdateSendNonce(string memory receiveDomain, address sender, bytes32 receiver) internal returns (uint32) {
        bytes32 nonceKey = SDPLib.getSendingSeqID(receiveDomain, sender, receiver);
        uint32 nonce = sendNonce[nonceKey];
        sendNonce[nonceKey]++;
        return nonce;
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
