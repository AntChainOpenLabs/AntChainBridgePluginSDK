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

import cn.hutool.core.util.ByteUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainIdentity;
import com.alipay.antchain.bridge.commons.core.rules.SDPPayloadV1Rule;
import com.alipay.antchain.bridge.commons.core.rules.aspect.AntChainBridgeRules;
import com.alipay.antchain.bridge.commons.utils.codec.CoderResult;
import com.alipay.antchain.bridge.commons.utils.codec.EvmCoderUtil;
import lombok.Getter;

public class SDPMessageV1 extends AbstractSDPMessage {

    public static final int MY_VERSION = 1;

    @Getter
    public static class SDPPayloadV1 implements ISDPPayload {

        private byte[] payload;

        @AntChainBridgeRules({SDPPayloadV1Rule.class})
        public SDPPayloadV1(byte[] payload) {
            this.payload = payload;
        }
    }

    private int extractTargetDomain(byte[] rawMessage, int offset) {
        CoderResult<byte[]> result = EvmCoderUtil.parseVarBytes(rawMessage, offset);
        this.setTargetDomain(new CrossChainDomain(new String(result.getResult())));
        return result.getOffset();
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
        CoderResult<byte[]> result = EvmCoderUtil.parseVarBytes(rawMessage, offset);
        this.setPayload(result.getResult());
        return result.getOffset();
    }

    @Override
    public void decode(byte[] rawMessage) {
        int offset = rawMessage.length;

        offset = extractTargetDomain(rawMessage, offset);
        offset = extractTargetIdentity(rawMessage, offset);
        offset = extractSequence(rawMessage, offset);
        extractPayload(rawMessage, offset);
    }

    private int putTargetDomain(byte[] rawMessage, int offset) {
        return EvmCoderUtil.sinkVarBytes(this.getTargetDomain().toBytes(), rawMessage, offset);
    }

    private int putTargetIdentity(byte[] rawMessage, int offset) {
        offset -= 32;
        System.arraycopy(this.getTargetIdentity().getRawID(), 0, rawMessage, offset, 32);

        return offset;
    }

    private int putSequence(byte[] rawMessage, int offset) {
        offset -= 4;
        System.arraycopy(ByteUtil.intToBytes(this.getSequence(), ByteOrder.BIG_ENDIAN), 0, rawMessage, offset, 4);

        return offset;
    }

    private int putPayload(byte[] rawMessage, int offset) {
        return EvmCoderUtil.sinkVarBytes(this.getPayload(), rawMessage, offset);
    }

    @Override
    public byte[] encode() {
        byte[] rawMessage = new byte[100
                + EvmCoderUtil.calcBytesInEvmWord(this.getTargetDomain().toBytes().length)
                + EvmCoderUtil.calcBytesInEvmWord(this.getPayload().length)];

        int offset = putTargetDomain(rawMessage, rawMessage.length);
        offset = putTargetIdentity(rawMessage, offset);
        offset = putSequence(rawMessage, offset);
        putPayload(rawMessage, offset);

        return rawMessage;
    }

    @Override
    public int getVersion() {
        return MY_VERSION;
    }

    @Override
    public void setPayload(byte[] payload) {
        setSdpPayload(new SDPPayloadV1(payload));
    }

    @Override
    public boolean getAtomic() {
        return false;
    }
}
