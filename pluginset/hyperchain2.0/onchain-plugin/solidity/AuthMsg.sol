// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.4.22;
pragma experimental ABIEncoderV2;

import "./interfaces/IAuthMessage.sol";
import "./interfaces/ISDPMessage.sol";
import "./lib/am/AMLib.sol";
import "./lib/utils/Ownable.sol";

contract AuthMsg is IAuthMessage, Ownable {

    struct SubProtocol {
        uint32 protocolType;
        bool exist;
    }

    // only relayer can call `recvPkgFromRelayer`
    address public relayer;
    bytes public myData;

    // It is recommended that users read the `exist` field first when accessing this variable.
    // If the `exist` field is false, the `protocolType` field is invalid (pay attention to avoid
    //   the ambiguity of SDPType when type is not set).
    // addtess -> type/exist
    mapping(address => SubProtocol) public subProtocols;

    // type -> address
    mapping(uint32 => address) public protocolRoutes;

    event SubProtocolUpdate(uint32 indexed protocolType, address protocolAddress);

    event recvAuthMessage(string recvDomain, bytes rawMsg);

    modifier onlySubProtocols {
        require(
            subProtocols[msg.sender].exist,
            "AuthMsg: sender not valid sub-protocol"
        );
        _;
    }

    modifier onlyRelayer {
        require(
            relayer == msg.sender,
            "AuthMsg: sender not valid relayer"
        );
        _;
    }

    constructor() {
        // The AM contract is deployed by relayer by default.
        // If the admin of heterogeneous chain deployed the AM contract in advance, the admin
        //   can invoke the interface `setRelayer` to set the real relayer address.
        relayer = msg.sender;
    }

    function setRelayer(address relayerAddress) external onlyOwner {
        relayer = relayerAddress;
    }

    function setProtocol(address protocolAddress, uint32 protocolType) external onlyOwner {

        SubProtocol storage p = subProtocols[protocolAddress];
        require(!p.exist, "AuthMsg: protocol exists");
        p.exist = true;
        p.protocolType = protocolType;

        protocolRoutes[protocolType] = protocolAddress;

        emit SubProtocolUpdate(protocolType, protocolAddress);
    }

    function getProtocol(uint32 protocolType) external view returns(address) {
        return protocolRoutes[protocolType];
    }

    function recvFromProtocol(address senderID, bytes message) external onlySubProtocols {
        _beforeSend(senderID, message);

        // use version 1 for now
        AMLib.AuthMessage memory amV1 = AMLib.AuthMessage(
            {
                version: 1,
                author: AMLib.encodeAddressIntoCrossChainID(senderID),
                protocolType: subProtocols[msg.sender].protocolType,
                body: message
            }
        );

        emit SendAuthMessage(AMLib.encodeAuthMessage(amV1));

        _afterSend(amV1);
    }

    // Security risks may exist here！！！
    //
    // In the current example contract, ordered cross-chain messages can be assured of uniqueness
    //   by sequence number（checked in function `_routeOrderedMessage` of function `recvMessage`
    //   of SDP contract ）, but no uniqueness check is done for unordered cross-chain messages!!!
    // By default, the relayer's replay control mechanism is trusted, that is, we trust the relay
    //   not to replay the message.
    function recvPkgFromRelayer(bytes pkg) external onlyRelayer{
        _beforeReceive(pkg);

        string memory domain;
        bytes memory rawResp;
        (domain, rawResp) = AMLib.decodeMessageFromRelayer(pkg);

        emit recvAuthMessage(domain, rawResp);

        _afterReceive(routeAuthMessage(domain, rawResp));
    }

    function routeAuthMessage(string memory domain, bytes memory rawResp) internal returns (AMLib.AuthMessage memory) {
        AMLib.AuthMessage memory message = AMLib.decodeAuthMessage(rawResp);

        require(
            protocolRoutes[message.protocolType] != address(0x0),
            "AuthMsg: no protocol exist"
        );

        // Messages received from the relayer are checked to see if the received chain is the current chain
        //   in the recvMessage method of the SDP contract (that is, if the domains match).
        ISDPMessage(protocolRoutes[message.protocolType]).recvMessage(domain, message.author, message.body);

        return message;
    }

    function _beforeReceive(bytes memory pkg) internal {}

    function _afterReceive(AMLib.AuthMessage memory message) internal {}

    function _beforeSend(address senderID, bytes memory message) internal {}

    function _afterSend(AMLib.AuthMessage memory message) internal {}

    /**
     * @dev This empty reserved space is put in place to allow future versions to add new
     * variables without shifting down storage in the inheritance chain.
     * See https://docs.openzeppelin.com/contracts/4.x/upgradeable#storage_gaps
     */
    uint256[50] private __gap;
}
