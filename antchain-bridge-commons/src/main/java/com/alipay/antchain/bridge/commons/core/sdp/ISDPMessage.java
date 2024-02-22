/*
 * Copyright 2023 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.bridge.commons.core.sdp;

import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainIdentity;
import com.alipay.antchain.bridge.commons.core.base.IMessage;

/**
 * Interface {@code ISDPMessage} defines all methods for the SDP
 * message object.
 *
 * <p>
 *      Smart-contract Datagram Protocol, SDP in short, provides the
 *      crosschain communication ability between contracts on different
 *      blockchains. The SDP contract is supposed to pre-deployed on each
 *      blockchain involved in crosschain network. During the communication
 *      processing, SDP contracts pack the message from sender contract into
 *      SDP packet and sink the raw packet bytes into AM packet. After
 *      ACB network relaying the cross-chain message onto receiving blockchain,
 *      the SDP contract on receiving blockchain would unpack the SDP packet
 *      and deliver the message from sender contract to the identified
 *      receiver contract.
 * </p>
 * <p>
 *     SDP now provides two versions of implementations, version one and two.
 *     Version one provides ability to sending ordered or unordered messages
 *     to another contract. And version two offer the atomic type message sending
 *     to another contract expecting multiple type acknowledge responses
 *     in different situations. And version two is good at preventing replay attacks.
 * </p>
 */
public interface ISDPMessage extends IMessage {

    /**
     * Get the receiving blockchain domain name.
     *
     * @return receiving blockchain domain name
     */
    CrossChainDomain getTargetDomain();

    /**
     * Get the receiving contract identity.
     *
     * <p>
     *     All crosschain identity in AntChain Bridge crosschain network is 32 bytes
     *     fixed. Usually crosschain identity is hash value of 256 bit length or less
     *     bytes value with padding zeros as prefix.
     * </p>
     * @return crosschain identity
     */
    CrossChainIdentity getTargetIdentity();

    /**
     * Return the sequence value in SDP packet.
     * <p>
     *     If sequence greater than or equal to zero, it means ordered SDP packet.
     *     Ordered SDP packet would be committed to receiving blockchain by order.
     *     If the SDP packet with last sequence number is not committed on receiving
     *     blockchain, the next SDP packet can't be delivered successfully to receiving
     *     blockchain.
     * </p>
     * <p>
     *     If sequence is {@code 0xFFFFFFFF}, it means unordered SDP packet.
     *     Unordered SDP packet is parallel sent to receiving blockchain.
     * </p>
     *
     * @return the sequence value in SDP packet
     */
    int getSequence();

    /**
     * The message bytes from sender contract.
     *
     * @return raw message
     */
    byte[] getPayload();

    /**
     * Version number of this SDP packet.
     *
     * @return version
     */
    int getVersion();

    /**
     * The nonce value for unordered packet.
     *
     * <p>
     *      In crosschain channel between sender and receiver, the nonce of each SDP
     *      packet is unique. The receiving SDP contract would check if the SDP packet
     *      has been processed by recording its nonce value.
     * </p>
     *
     * <p>
     *     Only version two has this work.
     * </p>
     *
     * @return nonce value
     */
    long getNonce();

    /**
     * Shows that is this SDP packet atomic.
     *
     * @return atomic or not
     */
    boolean getAtomic();

    /**
     * Method {@code getAtomicFlag} return the flag value shows that
     * which atomic type of this sdp packet, details please check
     * {@link AtomicFlagEnum}
     * @return flag value to show the type
     */
    AtomicFlagEnum getAtomicFlag();
}
