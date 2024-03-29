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

import java.nio.ByteOrder;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ByteUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainIdentity;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import lombok.Getter;

public class SDPMessageV2 extends AbstractSDPMessage {

    public static final int MY_VERSION = 2;

    @Getter
    public static class SDPPayloadV2 implements ISDPPayload {

        private byte[] payload;

        public SDPPayloadV2(byte[] payload) {
            this.payload = payload;
        }
    }

    private boolean atomic;

    @Override
    public void decode(byte[] rawMessage) {
        if (rawMessage.length < 49) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.SDP_MESSAGE_DECODE_ERROR,
                    String.format("length of message v2 supposes to be 49 bytes at least but get %d . ", rawMessage.length)
            );
        }

        int offset = rawMessage.length - 4;

        offset = extractTargetDomain(rawMessage, offset);
        offset = extractTargetIdentity(rawMessage, offset);
        offset = extractAtomic(rawMessage, offset);
        offset = extractSequence(rawMessage, offset);
        extractPayload(rawMessage, offset);
    }

    @Override
    public byte[] encode() {
        byte[] rawMessage = new byte[49
                + this.getTargetDomain().toBytes().length
                + this.getPayload().length];

        int offset = putVersion(rawMessage, rawMessage.length);
        offset = putTargetDomain(rawMessage, offset);
        offset = putTargetIdentity(rawMessage, offset);
        offset = putAtomic(rawMessage, offset);
        offset = putSequence(rawMessage, offset);
        putPayload(rawMessage, offset);

        return rawMessage;
    }

    @Override
    void setPayload(byte[] payload) {
        this.setSdpPayload(new SDPPayloadV2(payload));
    }

    @Override
    public int getVersion() {
        return MY_VERSION;
    }

    public void setAtomic(boolean atomic) {
        this.atomic = atomic;
    }

    public boolean getAtomic() {
        return atomic;
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
        this.setAtomic(ByteUtil.byteToUnsignedInt(rawMessage[--offset]) != 0);
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

    private int putVersion(byte[] rawMessage, int offset) {
        offset -= 4;
        System.arraycopy(ByteUtil.intToBytes(this.getVersion(), ByteOrder.BIG_ENDIAN), 0, rawMessage, offset, 4);
        rawMessage[offset] = ByteUtil.intToByte(0xFF);
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
        rawMessage[--offset] = BooleanUtil.toByte(this.getAtomic());
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
}
