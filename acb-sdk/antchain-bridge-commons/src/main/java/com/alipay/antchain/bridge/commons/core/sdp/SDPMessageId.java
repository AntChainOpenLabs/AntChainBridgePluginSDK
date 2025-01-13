/*
 * Copyright 2024 Ant Group
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

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import lombok.NonNull;
import lombok.Setter;

@Setter
public class SDPMessageId {

    public static final SDPMessageId ZERO_MESSAGE_ID = new SDPMessageId(new byte[32]);

    public SDPMessageId(@NonNull String messageIdHex) {
        this(HexUtil.decodeHex(messageIdHex));
    }

    public SDPMessageId(byte[] messageId) {
        if (ObjectUtil.isNull(messageId)) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.SDP_MESSAGE_DECODE_ERROR,
                    "message id is null"
            );
        }
        if (messageId.length != 32) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.SDP_MESSAGE_DECODE_ERROR,
                    StrUtil.format("expected 32 bytes but get {}", messageId.length)
            );
        }
        this.messageId = messageId;
    }

    private byte[] messageId;

    public String toHexStr() {
        return HexUtil.encodeHexStr(this.messageId);
    }

    public byte[] toByte32() {
        return messageId;
    }
}
