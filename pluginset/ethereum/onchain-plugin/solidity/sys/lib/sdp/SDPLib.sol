// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

import "../utils/TypesToBytes.sol";
import "../utils/BytesToTypes.sol";
import "../utils/SizeOf.sol";

struct SDPMessage {
    string receiveDomain;
    bytes32 receiver;
    bytes message;
    uint32 sequence;
}

struct SDPMessageV2 {
    uint32 version;
    string receiveDomain;
    bytes32 receiver;
    uint8 atomicFlag;
    uint64 nonce;
    uint32 sequence;
    bytes message;
}

library SDPLib {

    uint8 constant SDP_V2_ATOMIC_FLAG_NONE_ATOMIC = 0;

    uint8 constant SDP_V2_ATOMIC_FLAG_ATOMIC_REQUEST = 1;

    uint8 constant SDP_V2_ATOMIC_FLAG_ACK_SUCCESS = 2;

    uint8 constant SDP_V2_ATOMIC_FLAG_ACK_ERROR = 3;

    uint8 constant SDP_V2_ATOMIC_FLAG_ACK_RECEIVE_TX_FAILED = 4;

    uint8 constant SDP_V2_ATOMIC_FLAG_ACK_UNKNOWN_EXCEPTION = 5;

    uint32 constant UNORDERED_SEQUENCE = 0xffffffff;

    // @notice only for orderred msg
    uint64 constant MAX_NONCE = 0xffffffffffffffff;

    function encodeSDPMessage(SDPMessage memory sdpMessage) pure internal returns (bytes memory) {

        uint256 len = SizeOf.sizeOfBytes(sdpMessage.message) + 4 + 32 + SizeOf.sizeOfString(sdpMessage.receiveDomain);

        bytes memory pkg = new bytes(len);
        uint offset = len;

        // 填充接受者的domain
        TypesToBytes.stringToBytes(offset, bytes(sdpMessage.receiveDomain), pkg);
        offset -= SizeOf.sizeOfString(sdpMessage.receiveDomain);

        // 填充接受者identity
        TypesToBytes.bytes32ToBytes(offset, sdpMessage.receiver, pkg);
        offset -= SizeOf.sizeOfBytes32();

        // 填充sequence
        TypesToBytes.uintToBytes(offset, sdpMessage.sequence, pkg);
        offset -= SizeOf.sizeOfUint(32);

        // 填充消息
        TypesToBytes.stringToBytes(offset, sdpMessage.message, pkg);
        offset -= SizeOf.sizeOfBytes(sdpMessage.message);

        return pkg;
    }

    function decodeSDPMessage(SDPMessage memory sdpMessage, bytes memory rawMessage) pure internal {
        uint256 offset = rawMessage.length;

        uint32 dest_domain_len = BytesToTypes.bytesToUint32(offset, rawMessage) + 32;
        bytes memory dest_domain = new bytes(dest_domain_len);
        BytesToTypes.bytesToString(offset, rawMessage, dest_domain);
        offset -= SizeOf.sizeOfBytes(dest_domain);

        bytes32 receiver = BytesToTypes.bytesToBytes32(offset, rawMessage);
        offset -= SizeOf.sizeOfBytes32();

        uint32 sequence = BytesToTypes.bytesToUint32(offset, rawMessage);
        offset -= SizeOf.sizeOfInt(32);

        uint32 message_len = BytesToTypes.bytesToUint32(offset, rawMessage) + 32;
        bytes memory message = new bytes(message_len);
        BytesToTypes.bytesToString(offset, rawMessage, message);
        offset -= SizeOf.sizeOfBytes(message);

        sdpMessage = SDPMessage(
            {
                receiveDomain: string(dest_domain),
                receiver: receiver,
                message: message,
                sequence: sequence
            }
        );
    }

    function encodeSDPMessage(SDPMessageV2 memory sdpMessage) pure internal returns (bytes memory) {
        require(
            sdpMessage.version == 2, 
            "encodeSDPMessage: wrong version"
        );
        
        require(
            sdpMessage.message.length <= 0xFFFFFFFF,
            "encodeSDPMessage: body length overlimit"
        );

        uint total_size = 57 + bytes(sdpMessage.receiveDomain).length + sdpMessage.message.length;
        bytes memory pkg = new bytes(total_size);
        uint offset = total_size;

        TypesToBytes.uintToBytes(offset, sdpMessage.version, pkg);
        offset -= SizeOf.sizeOfInt(32);

        TypesToBytes.varBytesToBytes(offset, bytes(sdpMessage.receiveDomain), pkg);
        
        TypesToBytes.bytes32ToBytes(offset, sdpMessage.receiver, pkg);
        offset -= SizeOf.sizeOfBytes32();

        TypesToBytes.byteToBytes(offset, sdpMessage.atomicFlag, pkg);
        offset -= 1;

        TypesToBytes.uint64ToBytes(offset, sdpMessage.atomicFlag, pkg);
        offset -= SizeOf.sizeOfInt(64);

        TypesToBytes.uint32ToBytes(offset, sdpMessage.sequence, pkg);
        offset -= SizeOf.sizeOfInt(32);

        TypesToBytes.varBytesToBytes(offset, sdpMessage.message, pkg);

        return pkg;
    }

    function decodeSDPMessage(SDPMessageV2 memory sdpMessage, bytes memory rawMessage) internal pure {
        uint256 offset = rawMessage.length;

        sdpMessage.version = BytesToTypes.bytesToUint32(offset, rawMessage);
        offset -= SizeOf.sizeOfUint(32);

        sdpMessage.receiveDomain = string(BytesToTypes.bytesToVarBytes(offset, rawMessage));

        sdpMessage.receiver = BytesToTypes.bytesToBytes32(offset, rawMessage);
        offset -= SizeOf.sizeOfBytes32();

        sdpMessage.atomicFlag = BytesToTypes.bytesToUint8(offset, rawMessage);
        offset -= 1;

        sdpMessage.nonce = BytesToTypes.bytesToUint64(offset, rawMessage);
        offset -= 8;

        sdpMessage.sequence = BytesToTypes.bytesToUint32(offset, rawMessage);
        offset -= 4;

        sdpMessage.message = BytesToTypes.bytesToVarBytes(offset, rawMessage);
    }

    function getSendingSeqID(string memory receiveDomain, address sender, bytes32 receiver) pure internal returns (bytes32) {
        bytes32 sender32 = TypesToBytes.addressToBytes32(sender);
        return keccak256(abi.encodePacked(sender32, keccak256(abi.encodePacked(receiveDomain)), receiver));
    }

    function getReceivingSeqID(string memory sendDomain, bytes32 sender, bytes32 receiver) pure internal returns (bytes32) {
        return keccak256(abi.encodePacked(keccak256(abi.encodePacked(sendDomain)), sender, receiver));
    }

    function encodeCrossChainIDIntoAddress(bytes32 id) pure internal returns (address) {
        bytes memory rawId = new bytes(32);
        TypesToBytes.bytes32ToBytes(32, id, rawId);
        return BytesToTypes.bytesToAddress(32, rawId);
    }
}
