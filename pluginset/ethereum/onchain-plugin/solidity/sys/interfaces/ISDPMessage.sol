// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

import "./ISubProtocol.sol";

/**
 * @dev The SDP contract realizes the cross-chain message transfer protocol between the smart contracts,
 * also known as the SDP protocol.
 *
 * A smart contract on a blockchain can send any customized message to a smart contract on another
 * blockchain through the SDP protocol. In the AntChainBridge cross-chain system, all blockchains are given
 * a domain name. As long as you specify the domain name of the blockchain receiving the message
 * and the address of the receiving contract, you can send a specific message to the receiving
 * contract by sending a transaction on the sending blockchain.
 *
 */
interface ISDPMessage is ISubProtocol {

    /**
     * @dev When SDP contract receive msg in SDPv1 from AuthMessage contract, SDP contract would call the receiver 
     * contract decoded from sdp msg. And then, the result of calling receiver contract would emit this event
     * `receiveMessage` to show detail about this calling.
     *
     * @param senderDomain the domain name of the sending blockchain.
     * @param senderID the id of the sender.
     * @param receiverID the address of the receiver.
     * @param sequence the sequence number for this sdp msg
     * @param result result of receiver contract calling
     * @param errMsg the error msg if calling failed, default null
     */
    event receiveMessage(string senderDomain, bytes32 senderID, address receiverID, uint32 sequence, bool result, string errMsg);

    /**
     * @dev When SDP contract receive msg in SDPv2 from AuthMessage contract, SDP contract would call the receiver 
     * contract decoded from sdp msg. And then, the result of calling receiver contract would emit this event
     * `receiveMessage` to show detail about this calling.
     *
     * @param senderDomain the domain name of the sending blockchain.
     * @param senderID the id of the sender.
     * @param receiverID the address of the receiver.
     * @param sequence the sequence number for this sdp msg
     * @param nonce the unique nonce number for unordered packet.
     * @param atomic_flag the number identifies the type for this SDP packet
     * @param result result of receiver contract calling
     * @param errMsg the error msg if calling failed, default null
     */
    event receiveMessageV2(string senderDomain, bytes32 senderID, address receiverID, uint32 sequence, uint64 nonce, uint8 atomic_flag, bool result, string errMsg);

    /**
     * @dev Smart contracts need to call this method to send orderly cross-chain messages in SDPv2.
     *
     * The domain name of the sending blockchain, the address of the sender, the domain name of the
     * receiving blockchain and the address of the receiver uniquely determine a cross-chain channel.
     * Each channel maintains a sequence number which increases from zero to ensure that the cross-chain
     * messages of the channel are submitted on blockchain in an orderly manner.
     *
     * @param receiverDomain the domain name of the receiving blockchain.
     * @param receiverID the address of the receiver.
     * @param message the raw message from DApp contracts
     *
     * @return the sequence of SDP packet sent.
     */
    function sendMessageV2(string calldata receiverDomain, bytes32 receiverID, bool atomic, bytes calldata message) external returns (bytes32);

    /**
     * @dev Smart contracts need to call this method to send orderly cross-chain messages in SDPv1.
     *
     * The domain name of the sending blockchain, the address of the sender, the domain name of the
     * receiving blockchain and the address of the receiver uniquely determine a cross-chain channel.
     * Each channel maintains a sequence number which increases from zero to ensure that the cross-chain
     * messages of the channel are submitted on blockchain in an orderly manner.
     *
     * @param receiverDomain the domain name of the receiving blockchain.
     * @param receiverID the address of the receiver.
     * @param message the raw message from DApp contracts
     */
    function sendMessage(string calldata receiverDomain, bytes32 receiverID, bytes calldata message) external;

    /**
     * @dev Smart contracts call this method to send cross-chain messages out of order in SDPv2.
     *
     * The sequence number for unordered message is `0xffffffff` means that this message is out of order.
     *
     * @param receiverDomain the domain name of the receiving blockchain.
     * @param receiverID the address of the receiver.
     * @param message the raw message from DApp contracts
     * 
     * @return the nonce of SDP packet sent.
     */
    function sendUnorderedMessageV2(string calldata receiverDomain, bytes32 receiverID, bool atomic, bytes calldata message) external returns (bytes32);

    /**
     * @dev Smart contracts call this method to send cross-chain messages out of order in SDPv1.
     *
     * The sequence number for unordered message is `0xffffffff` means that this message is out of order.
     *
     * @param receiverDomain the domain name of the receiving blockchain.
     * @param receiverID the address of the receiver.
     * @param message the raw message from DApp contracts
     */
    function sendUnorderedMessage(string calldata receiverDomain, bytes32 receiverID, bytes calldata message) external;

    /**
     * @dev Query the current sdp message sequence for the channel identited by `senderDomain`, 
     * `senderID`, `receiverDomain` and `receiverID`.
     *
     * @param senderDomain the domain name of the sending blockchain.
     * @param senderID the id of the sender.
     * @param receiverDomain the domain name of the receiving blockchain.
     * @param receiverID the address of the receiver.
     */
    function querySDPMessageSeq(string calldata senderDomain, bytes32 senderID, string calldata receiverDomain, bytes32 receiverID) external returns (uint32);

    /**
     * @dev Set the domain of local chain to `SDP` contract.
     *
     * @param domain the domain name of local chain.
     */
    function setLocalDomain(string memory domain) external;
}
