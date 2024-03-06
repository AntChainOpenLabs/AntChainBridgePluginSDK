package com.alipay.antchain.bridge.commons.core.sdp;

import cn.hutool.core.util.ByteUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import lombok.Getter;

/**
 * Enum {@code AtomicFlagEnum} gives all types for SDP packet.
 */
@Getter
public enum AtomicFlagEnum {

    /**
     * {@code NONE_ATOMIC} means that this request has no atomic feature
     * and no ack response expected. Sender contract has no need to
     * implement ack processing interfaces for this type requests.
     */
    NONE_ATOMIC((byte) 0),

    /**
     * {@code ATOMIC_REQUEST} means this sdp packet is cross-chain sending to
     * another blockchain which expected to receive the message contained in
     * packet payload. This request expect an ack response from receiving
     * blockchain to notify the sender contract that cross-chain calling success
     * or not.
     */
    ATOMIC_REQUEST((byte) 1),

    /**
     * {@code ACK_SUCCESS} means this sdp packet has been processed
     * successfully on receiving blockchain and send an ack response back.
     */
    ACK_SUCCESS((byte) 2),

    /**
     * {@code ACK_ERROR} means this sdp packet has been processed
     * with errors when calling receiver contract on receiving blockchain.
     * But there is no transaction revert on receiving blockchain and all
     * state changes about cross-chain system contracts saved, for example
     * sequence added. And then an ack response sent back.
     */
    ACK_ERROR((byte) 3),

    /**
     * <p>
     *     {@code ACK_RECEIVE_TX_FAILED} means that sdp packet has been processed
     *     with errors and receiving transaction has been reverted. This is usually
     *     caused by bugs of cross-chain system contract implementation or receiving
     *     blockchain account.
     * </p>
     * <p>
     *     This type ack response wouldn't protect by ledger verification executed and signed by
     *     {@code PTC} because that no state update on receiving blockchain with transaction revert.
     *     Relayer would commit this ack response by a special {@code BBCService} method.
     *     So sender contract's developer must know this ack response is unsafe.
     * </p>
     */
    ACK_RECEIVE_TX_FAILED((byte) 4),

    /**
     * <p>
     *     {@code ACK_UNKNOWN_EXCEPTION} means that some unexpected situations happened
     *     like retry out of times or some timeout stuff.
     * </p>
     * <p>
     *     This type ack response wouldn't protect by ledger verification executed and signed by
     *     {@code PTC} because that no state update on receiving blockchain.
     *     Relayer would commit this ack response by a special {@code BBCService} method.
     *     So sender contract's developer must know this ack response is unsafe.
     * </p>
     */
    ACK_UNKNOWN_EXCEPTION((byte) 5);

    public static AtomicFlagEnum parseFrom(byte value) {
        if (value == NONE_ATOMIC.value) {
            return NONE_ATOMIC;
        } else if (value == ATOMIC_REQUEST.value) {
            return ATOMIC_REQUEST;
        } else if (value == ACK_SUCCESS.value) {
            return ACK_SUCCESS;
        } else if (value == ACK_ERROR.value) {
            return ACK_ERROR;
        } else if (value == ACK_RECEIVE_TX_FAILED.value) {
            return ACK_RECEIVE_TX_FAILED;
        } else if (value == ACK_UNKNOWN_EXCEPTION.value) {
            return ACK_UNKNOWN_EXCEPTION;
        }
        throw new AntChainBridgeCommonsException(
                CommonsErrorCodeEnum.SDP_MESSAGE_DECODE_ERROR,
                "no sdp atomic flag found for " + ByteUtil.byteToUnsignedInt(value)
        );
    }

    public static boolean withErrorMsg(AtomicFlagEnum atomicFlag) {
        return ObjectUtil.isNotNull(atomicFlag) && atomicFlag.value > ACK_SUCCESS.value;
    }

    AtomicFlagEnum(byte value) {
        this.value = value;
    }

    public boolean isAtomic() {
        return this == ATOMIC_REQUEST;
    }

    private final byte value;
}
