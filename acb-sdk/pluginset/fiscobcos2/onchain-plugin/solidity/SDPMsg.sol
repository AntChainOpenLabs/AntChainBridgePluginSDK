// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.4.22;

import "./interfaces/ISDPMessage.sol";
import "./interfaces/IAuthMessage.sol";
import "./interfaces/IContractUsingSDP.sol";
import "./interfaces/IContractWithAcks.sol";
import "./lib/sdp/SDPLib.sol";
import "./lib/utils/Ownable.sol";

contract SDPMsg is ISDPMessage, Ownable {
    using SDPLib for SDPLib.SDPMessage;
    using SDPLib for SDPLib.SDPMessageV2;

    string constant RECV_UNORDERED_MSG_METHOD_SIGN = "recvUnorderedMessage(string,bytes32,bytes)";
    string constant RECV_ORDERED_MSG_METHOD_SIGN = "recvMessage(string,bytes32,bytes)";


    uint8 constant SDP_V2_ATOMIC_FLAG_NONE_ATOMIC = 0;

    uint8 constant SDP_V2_ATOMIC_FLAG_ATOMIC_REQUEST = 1;

    uint8 constant SDP_V2_ATOMIC_FLAG_ACK_SUCCESS = 2;

    uint8 constant SDP_V2_ATOMIC_FLAG_ACK_ERROR = 3;

    uint8 constant SDP_V2_ATOMIC_FLAG_ACK_RECEIVE_TX_FAILED = 4;

    uint8 constant SDP_V2_ATOMIC_FLAG_ACK_UNKNOWN_EXCEPTION = 5;

    uint32 constant UNORDERED_SEQUENCE = 0xffffffff;

    uint64 constant MAX_NONCE = 0xffffffffffffffff;

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

    function setAmContract(address newAmContract) external onlyOwner {
        require(newAmContract != address(0), "SDPMsg: invalid am contract");
        amAddress = newAmContract;
    }

    function getAmAddress() external view returns (address) {
        return amAddress;
    }

    function setLocalDomain(string domain) external onlyOwner {
        localDomainHash = keccak256(abi.encodePacked(domain));
    }

    function getLocalDomain() external view returns (bytes32) {
        return localDomainHash;
    }

    function sendMessage(string receiverDomain, bytes32 receiverID, bytes message) external {
        _beforeSend(receiverDomain, receiverID, message);

        SDPLib.SDPMessage memory sdpMessage = SDPLib.SDPMessage(
            {
                receiveDomain: receiverDomain,
                receiver: receiverID,
                message: message,
                sequence: _getAndUpdateSendSeq(receiverDomain, msg.sender, receiverID)
            }
        );

        bytes memory rawMsg = SDPLib.encode(sdpMessage);

        IAuthMessage(amAddress).recvFromProtocol(msg.sender, rawMsg);

        _afterSend();
    }

    function sendUnorderedMessage(string receiverDomain, bytes32 receiverID, bytes message) external {
        _beforeSendUnordered(receiverDomain, receiverID, message);

        SDPLib.SDPMessage memory sdpMessage = SDPLib.SDPMessage(
            {
                receiveDomain: receiverDomain,
                receiver: receiverID,
                message: message,
                sequence: UNORDERED_SEQUENCE
            }
        );

        IAuthMessage(amAddress).recvFromProtocol(msg.sender, sdpMessage.encode());

        _afterSendUnordered();
    }

    function sendMessageV2(string receiverDomain, bytes32 receiverID, bool atomic, bytes message) external returns (bytes32) {
        _beforeSend(receiverDomain, receiverID, message);

        SDPLib.SDPMessageV2 memory sdpMessage = SDPLib.SDPMessageV2(
            {
                version: 2,
                messageId: bytes32(0),
                receiveDomain: receiverDomain,
                receiver: receiverID,
                atomicFlag: atomic ? SDP_V2_ATOMIC_FLAG_ATOMIC_REQUEST : SDP_V2_ATOMIC_FLAG_NONE_ATOMIC,
                nonce: MAX_NONCE,
                sequence: _getAndUpdateSendSeq(receiverDomain, msg.sender, receiverID),
                message: message,
                errorMsg: ""
            }
        );
        sdpMessage.calcMessageId(localDomainHash);

        IAuthMessage(amAddress).recvFromProtocol(msg.sender, sdpMessage.encode());

        _afterSend();

        return sdpMessage.messageId;
    }

    function sendUnorderedMessageV2(string receiverDomain, bytes32 receiverID, bool atomic, bytes message) external returns (bytes32) {
        _beforeSendUnordered(receiverDomain, receiverID, message);

        SDPLib.SDPMessageV2 memory sdpMessage = SDPLib.SDPMessageV2(
            {
                version: 2,
                messageId: bytes32(0),
                receiveDomain: receiverDomain,
                receiver: receiverID,
                atomicFlag: atomic ? SDP_V2_ATOMIC_FLAG_ATOMIC_REQUEST : SDP_V2_ATOMIC_FLAG_NONE_ATOMIC,
                nonce: _getAndUpdateSendNonce(receiverDomain, msg.sender, receiverID),
                sequence: UNORDERED_SEQUENCE,
                message: message,
                errorMsg: ""
            }
        );
        sdpMessage.calcMessageId(localDomainHash);

        IAuthMessage(amAddress).recvFromProtocol(msg.sender, sdpMessage.encode());

        _afterSendUnordered();

        return sdpMessage.messageId;
    }

    function recvMessage(string senderDomain, bytes32 senderID, bytes pkg) external onlyAM {
        _beforeRecv(senderDomain, senderID, pkg);

        uint32 version = SDPLib.getSDPVersionFrom(pkg);
        if (version == 1) {
            _processSDPv1(senderDomain, senderID, pkg);
        } else if (version == 2) {
            _processSDPv2(senderDomain, senderID, pkg);
        } else {
            revert("unsupport sdp version");
        }

        _afterRecv();
    }

    function querySDPMessageSeq(string senderDomain, bytes32 senderID, string receiverDomain, bytes32 receiverID) external returns (uint32) {
        require(
            keccak256(abi.encodePacked(receiverDomain)) == localDomainHash,
            "SDPMsg: wrong receiving domain"
        );
        bytes32 seqKey = SDPLib.getReceivingSeqID(senderDomain, senderID, receiverID);
        uint32 seq = recvSeq[seqKey];
        return seq;
    }

    function _processSDPv1(string senderDomain, bytes32 senderID, bytes memory pkg) internal {
        SDPLib.SDPMessage memory sdpMessage;
        sdpMessage.decode(pkg);

        require(
            keccak256(abi.encodePacked(sdpMessage.receiveDomain)) == localDomainHash,
            "SDPMsg: wrong receiving domain"
        );

        if (sdpMessage.sequence == UNORDERED_SEQUENCE) {
            _routeUnorderedMessage(senderDomain, senderID, sdpMessage);
        } else {
            _routeOrderedMessage(senderDomain, senderID, sdpMessage);
        }
    }

    function _routeOrderedMessage(string senderDomain, bytes32 senderID, SDPLib.SDPMessage memory sdpMessage) internal {
        uint32 seqExpected = _getAndUpdateRecvSeq(senderDomain, senderID, sdpMessage.receiver);
        require(
            sdpMessage.sequence == seqExpected,
            "SDPMsg: sequence not equal"
        );

        bool res = false;
        string memory errMsg;
        address receiver = sdpMessage.getReceiverAddress();

        res = receiver.call(abi.encodeWithSignature(
            RECV_ORDERED_MSG_METHOD_SIGN,
            senderDomain, senderID, sdpMessage.message));

        emit receiveMessage(senderDomain, senderID, receiver, seqExpected, res, errMsg);
    }

    function _routeUnorderedMessage(string senderDomain, bytes32 senderID, SDPLib.SDPMessage memory sdpMessage) internal {
        IContractUsingSDP(sdpMessage.getReceiverAddress())
        .recvUnorderedMessage(senderDomain, senderID, sdpMessage.message);
    }

    function _processSDPv2(string senderDomain, bytes32 senderID, bytes memory pkg) internal {
        SDPLib.SDPMessageV2 memory sdpMessage;
        sdpMessage.decode(pkg);

        require(
            keccak256(abi.encodePacked(sdpMessage.receiveDomain)) == localDomainHash,
            "SDPMsg: wrong receiving domain"
        );

        if (
            sdpMessage.atomicFlag == SDP_V2_ATOMIC_FLAG_NONE_ATOMIC
            || sdpMessage.atomicFlag == SDP_V2_ATOMIC_FLAG_ATOMIC_REQUEST
        ) {
            _processSDPv2Request(senderDomain, senderID, sdpMessage);
        } else if (sdpMessage.atomicFlag == SDP_V2_ATOMIC_FLAG_ACK_SUCCESS) {
            _processSDPv2AckSuccess(senderDomain, senderID, sdpMessage);
        } else if (sdpMessage.atomicFlag == SDP_V2_ATOMIC_FLAG_ACK_ERROR) {
            _processSDPv2AckError(senderDomain, senderID, sdpMessage);
        } else {
            revert("unexpected atomic flag");
        }
    }

    function _processSDPv2Request(string senderDomain, bytes32 senderID, SDPLib.SDPMessageV2 memory sdpMessage) internal {
        bool res;
        string memory errMsg;
        if (sdpMessage.sequence == UNORDERED_SEQUENCE) {
            (res, errMsg) = _routeUnorderedMessageV2(senderDomain, senderID, sdpMessage);
            if (sdpMessage.atomicFlag == SDP_V2_ATOMIC_FLAG_NONE_ATOMIC) {
                require(res, errMsg);
            }
        } else {
            (res, errMsg) = _routeOrderedMessageV2(senderDomain, senderID, sdpMessage);
        }

        emit ReceiveMessageV2(
            sdpMessage.messageId,
            senderDomain,
            senderID,
            sdpMessage.receiveDomain,
            sdpMessage.getReceiverAddress(),
            sdpMessage.sequence,
            sdpMessage.nonce,
            sdpMessage.atomicFlag,
            res,
            errMsg
        );

        if (sdpMessage.atomicFlag == SDP_V2_ATOMIC_FLAG_ATOMIC_REQUEST) {
            _ackSDPv2Request(sdpMessage, senderDomain, senderID, res, errMsg);
        }
    }

    function _routeOrderedMessageV2(string senderDomain, bytes32 senderID, SDPLib.SDPMessageV2 memory sdpMessage) internal returns (bool, string memory) {
        uint32 seqExpected = _getAndUpdateRecvSeq(senderDomain, senderID, sdpMessage.receiver);
        require(sdpMessage.sequence == seqExpected, "SDPMsg: sequence not equal");

        bool res = false;
        string memory errMsg;
        address receiver = sdpMessage.getReceiverAddress();

        res = receiver.call(abi.encodeWithSignature(
            RECV_ORDERED_MSG_METHOD_SIGN,
            senderDomain, senderID, sdpMessage.message));

        return (res, errMsg);
    }

    function _routeUnorderedMessageV2(string senderDomain, bytes32 senderID, SDPLib.SDPMessageV2 memory sdpMessage) internal returns (bool, string memory) {
        bool res = false;
        string memory errMsg;

        res = sdpMessage.getReceiverAddress().call(abi.encodeWithSignature(
            RECV_UNORDERED_MSG_METHOD_SIGN,
            senderDomain, senderID, sdpMessage.message));

        return (res, errMsg);
    }

    function _ackSDPv2Request(SDPLib.SDPMessageV2 memory sdpMessage, string senderDomain, bytes32 senderID, bool res, string memory errMsg) internal {
        address receiverAddr = sdpMessage.getReceiverAddress();

        sdpMessage.receiveDomain = senderDomain;
        sdpMessage.receiver = senderID;
        sdpMessage.atomicFlag = res ? SDP_V2_ATOMIC_FLAG_ACK_SUCCESS : SDP_V2_ATOMIC_FLAG_ACK_ERROR;
        sdpMessage.errorMsg = res ? "" : errMsg;

        IAuthMessage(amAddress).recvFromProtocol(receiverAddr, sdpMessage.encode());
    }

    function _processSDPv2AckSuccess(string senderDomain, bytes32 senderID, SDPLib.SDPMessageV2 memory sdpMessage) internal {
        IContractWithAcks(sdpMessage.getReceiverAddress()).ackOnSuccess(
            sdpMessage.messageId,
            senderDomain,
            senderID,
            sdpMessage.sequence,
            sdpMessage.nonce,
            sdpMessage.message
        );
        emit ReceiveMessageV2(
            sdpMessage.messageId,
            senderDomain,
            senderID,
            sdpMessage.receiveDomain,
            sdpMessage.getReceiverAddress(),
            sdpMessage.sequence,
            sdpMessage.nonce,
            sdpMessage.atomicFlag,
            true,
            "success"
        );
    }

    function _processSDPv2AckError(string senderDomain, bytes32 senderID, SDPLib.SDPMessageV2 memory sdpMessage) internal {
        IContractWithAcks(sdpMessage.getReceiverAddress()).ackOnError(
            sdpMessage.messageId,
            senderDomain,
            senderID,
            sdpMessage.sequence,
            sdpMessage.nonce,
            sdpMessage.message,
            sdpMessage.errorMsg
        );
        emit ReceiveMessageV2(
            sdpMessage.messageId,
            senderDomain,
            senderID,
            sdpMessage.receiveDomain,
            sdpMessage.getReceiverAddress(),
            sdpMessage.sequence,
            sdpMessage.nonce,
            sdpMessage.atomicFlag,
            true,
            sdpMessage.errorMsg
        );
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

    function _beforeSend(string receiverDomain, bytes32 receiverID, bytes message) internal {}

    function _afterSend() internal {}

    function _beforeSendUnordered(string receiverDomain, bytes32 receiverID, bytes message) internal {}

    function _afterSendUnordered() internal {}

    function _beforeRecv(string senderDomain, bytes32 senderID, bytes pkg) internal {}

    function _afterRecv() internal {}

    /**
     * @dev This empty reserved space is put in place to allow future versions to add new
     * variables without shifting down storage in the inheritance chain.
     * See https://docs.openzeppelin.com/contracts/4.x/upgradeable#storage_gaps
     */
    uint256[50] private __gap;
}
