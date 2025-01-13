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

import java.math.BigInteger;
import java.nio.ByteOrder;

import cn.hutool.core.util.ByteUtil;
import com.alipay.antchain.bridge.commons.core.base.BlockState;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainIdentity;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SDPMessageV2 extends AbstractSDPMessage {

    public static final int MY_VERSION = 2;

    @Getter
    public static class SDPPayloadV2 implements ISDPPayload {

        private final byte[] payload;

        public SDPPayloadV2(byte[] payload) {
            this.payload = payload;
        }
    }

    private AtomicFlagEnum atomicFlag;

    private long nonce;

    private SDPMessageId messageId;

    private String errorMsg;

    @Override
    public void decode(byte[] rawMessage) {
        if (rawMessage.length < 89) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.SDP_MESSAGE_DECODE_ERROR,
                    String.format("length of message v2 supposes to be 89 bytes at least but get %d . ", rawMessage.length)
            );
        }

        int offset = rawMessage.length - 4;

        offset = extractMessageId(rawMessage, offset);
        offset = extractTargetDomain(rawMessage, offset);
        offset = extractTargetIdentity(rawMessage, offset);
        offset = extractAtomic(rawMessage, offset);
        offset = extractNonce(rawMessage, offset);
        offset = extractSequence(rawMessage, offset);
        offset = extractPayload(rawMessage, offset);
        if (AtomicFlagEnum.withErrorMsg(getAtomicFlag())) {
            extractErrorMsg(rawMessage, offset);
        }
    }

    @Override
    public byte[] encode() {
        byte[] rawMessage = new byte[calcMessageLength()];

        int offset = putVersion(rawMessage, rawMessage.length);
        offset = putMessageId(rawMessage, offset);
        offset = putTargetDomain(rawMessage, offset);
        offset = putTargetIdentity(rawMessage, offset);
        offset = putAtomic(rawMessage, offset);
        offset = putNonce(rawMessage, offset);
        offset = putSequence(rawMessage, offset);
        offset = putPayload(rawMessage, offset);
        if (AtomicFlagEnum.withErrorMsg(getAtomicFlag())) {
            putErrorMsg(rawMessage, offset);
        }

        return rawMessage;
    }

    @Override
    public void setPayload(byte[] payload) {
        this.setSdpPayload(new SDPPayloadV2(payload));
    }

    @Override
    public int getVersion() {
        return MY_VERSION;
    }

    @Override
    public boolean getAtomic() {
        return atomicFlag.isAtomic();
    }

    public SDPPayloadV2 getSDPPayloadV2() {
        return (SDPPayloadV2) getSdpPayload();
    }

    @Override
    public TimeoutMeasureEnum getTimeoutMeasure() {
        return TimeoutMeasureEnum.NO_TIMEOUT;
    }

    @Override
    public BigInteger getTimeout() {
        return BigInteger.ZERO;
    }

    @Override
    public boolean isTimeout(BlockState blockState) {
        return false;
    }

    private int extractMessageId(byte[] rawMessage, int offset) {
        offset -= 32;
        byte[] msgId = new byte[32];
        System.arraycopy(rawMessage, offset, msgId, 0, 32);
        this.setMessageId(new SDPMessageId(msgId));

        return offset;
    }

    private int extractTargetDomain(byte[] rawMessage, int offset) {
        offset -= 4;
        byte[] rawPayloadLen = new byte[4];
        System.arraycopy(rawMessage, offset, rawPayloadLen, 0, 4);

        byte[] rawDomain = new byte[ByteUtil.bytesToInt(rawPayloadLen, ByteOrder.BIG_ENDIAN)];
        offset -= rawDomain.length;
        if (offset < 0) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.SDP_MESSAGE_DECODE_ERROR,
                    "wrong domain or length of payload v2"
            );
        }
        System.arraycopy(rawMessage, offset, rawDomain, 0, rawDomain.length);
        this.setTargetDomain(new CrossChainDomain(new String(rawDomain)));

        return offset;
    }

    private int extractAtomic(byte[] rawMessage, int offset) {
        this.setAtomicFlag(AtomicFlagEnum.parseFrom(rawMessage[--offset]));
        return offset;
    }

    private int extractNonce(byte[] rawMessage, int offset) {
        offset -= 8;
        byte[] rawSeq = new byte[8];
        System.arraycopy(rawMessage, offset, rawSeq, 0, 8);
        this.setNonce(ByteUtil.bytesToLong(rawSeq, ByteOrder.BIG_ENDIAN));

        return offset;
    }

    private int extractTargetIdentity(byte[] rawMessage, int offset) {
        offset -= 32;
        byte[] crossChainID = new byte[32];
        System.arraycopy(rawMessage, offset, crossChainID, 0, 32);
        this.setTargetIdentity(new CrossChainIdentity(crossChainID));

        return offset;
    }

    private int extractSequence(byte[] rawMessage, int offset) {
        offset -= 4;
        byte[] rawSeq = new byte[4];
        System.arraycopy(rawMessage, offset, rawSeq, 0, 4);
        this.setSequence(ByteUtil.bytesToInt(rawSeq, ByteOrder.BIG_ENDIAN));

        return offset;
    }

    private int extractPayload(byte[] rawMessage, int offset) {
        offset -= 4;
        byte[] rawPayloadLen = new byte[4];
        System.arraycopy(rawMessage, offset, rawPayloadLen, 0, 4);

        byte[] payload = new byte[ByteUtil.bytesToInt(rawPayloadLen, ByteOrder.BIG_ENDIAN)];
        offset -= payload.length;
        if (offset < 0) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.SDP_MESSAGE_DECODE_ERROR,
                    "wrong payload or length of payload v2"
            );
        }
        System.arraycopy(rawMessage, offset, payload, 0, payload.length);
        this.setPayload(payload);

        return offset;
    }

    private int extractErrorMsg(byte[] rawMessage, int offset) {
        offset -= 4;
        byte[] rawErrorMsgLen = new byte[4];
        System.arraycopy(rawMessage, offset, rawErrorMsgLen, 0, 4);

        byte[] errorMsg = new byte[ByteUtil.bytesToInt(rawErrorMsgLen, ByteOrder.BIG_ENDIAN)];
        offset -= errorMsg.length;
        if (offset < 0) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.SDP_MESSAGE_DECODE_ERROR,
                    "length of error message in SDPv2 is incorrect"
            );
        }
        System.arraycopy(rawMessage, offset, errorMsg, 0, errorMsg.length);
        this.setErrorMsg(new String(errorMsg));

        return offset;
    }

    private int putVersion(byte[] rawMessage, int offset) {
        offset -= 4;
        System.arraycopy(ByteUtil.intToBytes(this.getVersion(), ByteOrder.BIG_ENDIAN), 0, rawMessage, offset, 4);
        rawMessage[offset] = ByteUtil.intToByte(0xFF);
        return offset;
    }

    private int putMessageId(byte[] rawMessage, int offset) {
        offset -= 32;
        System.arraycopy(this.getMessageId().toByte32(), 0, rawMessage, offset, 32);

        return offset;
    }

    private int putTargetDomain(byte[] rawMessage, int offset) {
        offset -= 4;
        byte[] rawTargetDomain = this.getTargetDomain().toBytes();
        System.arraycopy(ByteUtil.intToBytes(rawTargetDomain.length, ByteOrder.BIG_ENDIAN), 0, rawMessage, offset, 4);

        offset -= rawTargetDomain.length;
        System.arraycopy(rawTargetDomain, 0, rawMessage, offset, rawTargetDomain.length);

        return offset;
    }

    private int putTargetIdentity(byte[] rawMessage, int offset) {
        offset -= 32;
        System.arraycopy(this.getTargetIdentity().getRawID(), 0, rawMessage, offset, 32);

        return offset;
    }

    private int putAtomic(byte[] rawMessage, int offset) {
        rawMessage[--offset] = this.getAtomicFlag().getValue();
        return offset;
    }

    private int putNonce(byte[] rawMessage, int offset) {
        offset -= 8;
        System.arraycopy(ByteUtil.longToBytes(this.getNonce(), ByteOrder.BIG_ENDIAN), 0, rawMessage, offset, 8);

        return offset;
    }

    private int putSequence(byte[] rawMessage, int offset) {
        offset -= 4;
        System.arraycopy(ByteUtil.intToBytes(this.getSequence(), ByteOrder.BIG_ENDIAN), 0, rawMessage, offset, 4);

        return offset;
    }

    private int putPayload(byte[] rawMessage, int offset) {
        offset -= 4;
        System.arraycopy(ByteUtil.intToBytes(this.getPayload().length, ByteOrder.BIG_ENDIAN), 0, rawMessage, offset, 4);

        offset -= this.getPayload().length;
        System.arraycopy(this.getPayload(), 0, rawMessage, offset, this.getPayload().length);

        return offset;
    }

    private int putErrorMsg(byte[] rawMessage, int offset) {
        offset -= 4;
        System.arraycopy(ByteUtil.intToBytes(this.getErrorMsg().getBytes().length, ByteOrder.BIG_ENDIAN), 0, rawMessage, offset, 4);

        offset -= this.getErrorMsg().length();
        System.arraycopy(this.getErrorMsg().getBytes(), 0, rawMessage, offset, this.getErrorMsg().length());

        return offset;
    }

    private int calcMessageLength() {
        return AtomicFlagEnum.withErrorMsg(getAtomicFlag()) ?
                89 + this.getTargetDomain().toBytes().length + this.getPayload().length + 4 + this.getErrorMsg().length() :
                89 + this.getTargetDomain().toBytes().length + this.getPayload().length;
    }
}
