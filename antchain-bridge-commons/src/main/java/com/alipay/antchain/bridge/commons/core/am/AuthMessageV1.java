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

import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.utils.codec.CoderResult;
import com.alipay.antchain.bridge.commons.utils.codec.EvmCoderUtil;

public class AuthMessageV1 extends AbstractAuthMessage {

    public static final int MY_VERSION = 1;

    @Override
    public void decode(byte[] rawMessage) {
        if (rawMessage.length < 72) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.AUTH_MESSAGE_DECODE_ERROR,
                    String.format("length of message v1 supposes to be 72 bytes at least but get %d . ", rawMessage.length)
            );
        }

        int offset = checkVersion(rawMessage);
        offset = extractCrossChainID(rawMessage, offset);
        offset = extractUpperProtocol(rawMessage, offset);

        decodeMyPayload(rawMessage, offset);
    }

    private int decodeMyPayload(byte[] rawMessage, int offset) {
        CoderResult<byte[]> result = EvmCoderUtil.parseVarBytes(rawMessage, offset);
        this.setPayload(result.getResult());

        return result.getOffset();
    }

    @Override
    public int getVersion() {
        return MY_VERSION;
    }

    @Override
    public byte[] encode() {
        byte[] rawMessage = new byte[72 + EvmCoderUtil.calcBytesInEvmWord(this.getPayload().length)];

        int offset = putVersion(rawMessage, rawMessage.length);
        offset = putCrossChainID(rawMessage, offset);
        offset = putUpperProtocol(rawMessage, offset);

        encodeMyPayload(rawMessage, offset);

        return rawMessage;
    }

    private int encodeMyPayload(byte[] rawMessage, int offset) {
        return EvmCoderUtil.sinkVarBytes(this.getPayload(), rawMessage, offset);
    }
}
