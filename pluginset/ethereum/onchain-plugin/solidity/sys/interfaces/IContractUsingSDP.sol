// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

interface IContractUsingSDP {

    /**
     * @dev SDP contract would call this function to deliver the message from sender contract
     * on sender blockchain. This message sent with no order and in parallel.
     *
     * @param senderDomain the domain name of the sending blockchain.
     * @param author the id of the sender.
     * @param message the raw message from sender contract.
     */
    function recvUnorderedMessage(string memory senderDomain, bytes32 author, bytes memory message) external;

    /**
     * @dev SDP contract would call this function to deliver the ack response from receiver 
     * contract. The original message being acked now is processed successfully on receiving 
     * blockchain. And the original sdp packet is sent as unordered message.
     *
     * @param receiverDomain the domain name of the receiving blockchain for original message and where the ack response from.
     * @param receiver the id of the receiver for original message.
     * @param nonce unique number of original message from sender to receiver.
     * @param message the message sent to receiver.
     */
    function ackOnUnorderedMessageSuccess(string memory receiverDomain, bytes32 receiver, uint64 nonce, bytes memory message) external;

    /**
     * @dev SDP contract would call this function to deliver the ack response from receiver 
     * contract. The original message being acked now is processed with errors on receiving 
     * blockchain. And the original sdp packet is sent as unordered message.
     *
     * @param receiverDomain the domain name of the receiving blockchain for original message and where the ack response from.
     * @param receiver the id of the receiver for original message.
     * @param nonce unique number of original message from sender to receiver.
     * @param message the message sent to receiver.
     * @param errorMsg the error msg from receiver.
     */
    function ackOnUnorderedMessageError(string memory receiverDomain, bytes32 receiver, uint64 nonce, bytes memory message, string memory errorMsg) external;

    /**
     * @dev SDP contract would call this function to deliver the message from sender contract
     * on sender blockchain. This message sent with order.
     *
     * @param senderDomain the domain name of the sending blockchain.
     * @param author the id of the sender.
     * @param message the raw message from sender contract.
     */
    function recvMessage(string memory senderDomain, bytes32 author, bytes memory message) external;

    /**
     * @dev SDP contract would call this function to deliver the ack response from receiver 
     * contract. The original message being acked now is processed successfully on receiving 
     * blockchain. And the original sdp packet is sent as ordered message.
     *
     * @param receiverDomain the domain name of the receiving blockchain for original message and where the ack response from.
     * @param receiver the id of the receiver for original message.
     * @param sequence sequence number of original message from sender to receiver.
     * @param message the message sent to receiver.
     */
    function ackOnSuccess(string memory receiverDomain, bytes32 receiver, uint32 sequence, bytes memory message) external;

    /**
     * @dev SDP contract would call this function to deliver the ack response from receiver 
     * contract. The original message being acked now is processed with errors on receiving 
     * blockchain. And the original sdp packet is sent as ordered message.
     *
     * @param receiverDomain the domain name of the receiving blockchain for original message and where the ack response from.
     * @param receiver the id of the receiver for original message.
     * @param sequence sequence number of original message from sender to receiver.
     * @param message the message sent to receiver.
     * @param errorMsg the error msg from receiver.
     */
    function ackOnError(string memory receiverDomain, bytes32 receiver, uint32 sequence, bytes memory message, string memory errorMsg) external;
}
