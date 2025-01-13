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

package com.alipay.antchain.bridge.commons.core.am;

import java.nio.ByteOrder;

import cn.hutool.core.util.ByteUtil;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AuthMessageV2 extends AbstractAuthMessage {

    public static final int MY_VERSION = 2;

    private AuthMessageTrustLevelEnum trustLevel;

    @Override
    public void decode(byte[] rawMessage) {
        if (rawMessage.length < 45) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.AUTH_MESSAGE_DECODE_ERROR,
                    String.format("length of message v2 supposes to be 45 bytes at least but get %d . ", rawMessage.length)
            );
        }

        int offset = checkVersion(rawMessage);
        offset = extractCrossChainID(rawMessage, offset);
        offset = extractUpperProtocol(rawMessage, offset);
        offset = extractTrustLevel(rawMessage, offset);

        decodeMyPayload(rawMessage, offset);
    }

    private int decodeMyPayload(byte[] rawMessage, int offset) {
        offset -= 4;
        byte[] rawPayloadLen = new byte[4];
        System.arraycopy(rawMessage, offset, rawPayloadLen, 0, 4);

        byte[] payload = new byte[ByteUtil.bytesToInt(rawPayloadLen, ByteOrder.BIG_ENDIAN)];
        offset -= payload.length;
        if (offset < 0) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.AUTH_MESSAGE_DECODE_ERROR,
                    "wrong payload or length of payload v2"
            );
        }
        System.arraycopy(rawMessage, offset, payload, 0, payload.length);
        this.setPayload(payload);

        return offset;
    }

    private int extractTrustLevel(byte[] rawMessage, int offset) {
        offset -= 1;
        byte rawTrustLevel = rawMessage[offset];
        this.setTrustLevel(
                AuthMessageTrustLevelEnum.parseFromValue(
                        ByteUtil.byteToUnsignedInt(rawTrustLevel)
                )
        );

        return offset;
    }

    @Override
    public byte[] encode() {
        byte[] rawMessage = new byte[45 + this.getPayload().length];

        int offset = putVersion(rawMessage, rawMessage.length);
        offset = putCrossChainID(rawMessage, offset);
        offset = putUpperProtocol(rawMessage, offset);
        offset = putTrustLevel(rawMessage, offset);

        encodeMyPayload(rawMessage, offset);

        return rawMessage;
    }

    private int putTrustLevel(byte[] rawMessage, int offset) {
        offset -= 1;
        rawMessage[offset] = (byte) this.getTrustLevel().ordinal();
        return offset;
    }

    private int encodeMyPayload(byte[] rawMessage, int offset) {
        offset -= 4;
        System.arraycopy(ByteUtil.intToBytes(this.getPayload().length, ByteOrder.BIG_ENDIAN), 0, rawMessage, offset, 4);

        offset -= this.getPayload().length;
        System.arraycopy(this.getPayload(), 0, rawMessage, offset, this.getPayload().length);

        return offset;
    }

    @Override
    public int getVersion() {
        return MY_VERSION;
    }
}
