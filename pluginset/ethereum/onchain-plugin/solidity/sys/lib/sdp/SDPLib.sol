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

library SDPLib {

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

    function decodeSDPMessage(bytes memory rawMessage) pure internal returns (SDPMessage memory) {
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

        return SDPMessage(
            {
                receiveDomain: string(dest_domain),
                receiver: receiver,
                message: message,
                sequence: sequence
            }
        );
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
