pragma solidity ^0.4.22;
pragma experimental ABIEncoderV2;

import "./Utils.sol";

library TLVUtils {
    struct TLVItem {
        uint16 tag;
        uint32 len;
        bytes value;
    }

    struct LVItem {
        uint32 len;
        bytes value;
    }

    function parseTLVItem(bytes memory rawData, uint offset) internal pure returns (TLVItem memory, uint) {
        require(
            rawData.length > offset,
            "length of raw data less than offset"
        );
        require(
            offset >= 6,
            "illegal offset"
        );

        TLVItem memory item;
        item.tag = Utils.reverseUint16(Utils.readUint16(rawData, offset));
        offset += 2;
        item.len = Utils.reverseUint32(Utils.readUint32(rawData, offset));
        offset += 4;
        item.value = Utils.substring(rawData, offset, item.len);
        offset += item.len;

        return (item, offset);
    }

    function parseLVItem(bytes memory rawData, uint offset) internal pure returns (LVItem memory, uint) {
        require(
            rawData.length > offset,
            "length of raw data less than offset"
        );
        require(
            offset >= 6,
            "illegal offset"
        );

        LVItem memory item;
        item.len = Utils.reverseUint32(Utils.readUint32(rawData, offset));
        offset += 4;
        item.value = Utils.substring(rawData, offset, item.len);
        offset += item.len;

        return (item, offset);
    }
}