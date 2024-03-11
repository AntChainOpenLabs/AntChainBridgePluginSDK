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
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractSDPMessage implements ISDPMessage {

    public static int UNORDERED_SEQUENCE = 0xFFFFFFFF;

    public static int MAGIC_NUMBER = 0xFF000000;

    public static int decodeVersionFromBytes(byte[] rawMessage) {
        if (rawMessage.length < 4) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.SDP_MESSAGE_DECODE_ERROR,
                    String.format("message length must greater than 4 bytes but received %d bytes", rawMessage.length)
            );
        }
        byte[] rawVersion = new byte[4];
        System.arraycopy(rawMessage, rawMessage.length - 4, rawVersion, 0, 4);

        if (ByteUtil.intToByte(MAGIC_NUMBER >> 24) == rawVersion[0]) {
            rawVersion[0] = ByteUtil.intToByte(0);
            return ByteUtil.bytesToInt(rawVersion, ByteOrder.BIG_ENDIAN);
        }

        return SDPMessageV1.MY_VERSION;
    }

    private CrossChainDomain targetDomain;

    private CrossChainIdentity targetIdentity;

    private int sequence;

    private ISDPPayload sdpPayload;

    @Override
    public byte[] getPayload() {
        return sdpPayload.getPayload();
    }

    abstract void setPayload(byte[] payload);

    public abstract void setNonce(long nonce);

    public abstract void setAtomicFlag(AtomicFlagEnum atomicFlag);

    public abstract void setMessageId(SDPMessageId messageId);
}
