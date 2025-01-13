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
import com.alipay.antchain.bridge.commons.core.base.CrossChainIdentity;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractAuthMessage implements IAuthMessage {

    public static int decodeVersionFromBytes(byte[] rawMessage) {
        if (rawMessage.length < 4) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.AUTH_MESSAGE_DECODE_ERROR,
                    String.format("message length must greater than 4 bytes but received %d bytes", rawMessage.length)
            );
        }
        byte[] rawVersion = new byte[4];
        System.arraycopy(rawMessage, rawMessage.length - 4, rawVersion, 0, 4);
        return ByteUtil.bytesToInt(rawVersion, ByteOrder.BIG_ENDIAN);
    }

    private CrossChainIdentity identity;

    private int upperProtocol;

    private byte[] payload;

    public int checkVersion(byte[] rawMessage) {
        int offset = rawMessage.length - 4;
        byte[] rawVersion = new byte[4];
        System.arraycopy(rawMessage, offset, rawVersion, 0, 4);

        int version = ByteUtil.bytesToInt(rawVersion, ByteOrder.BIG_ENDIAN);
        if (version != this.getVersion()) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.AUTH_MESSAGE_DECODE_ERROR,
                    String.format("expect version %d but got version %d", this.getVersion(), version)
            );
        }

        return offset;
    }

    public int extractCrossChainID(byte[] rawMessage, int offset) {
        offset -= 32;
        byte[] crossChainID = new byte[32];
        System.arraycopy(rawMessage, offset, crossChainID, 0, 32);
        this.setIdentity(new CrossChainIdentity(crossChainID));

        return offset;
    }

    public int extractUpperProtocol(byte[] rawMessage, int offset) {
        offset -= 4;
        byte[] rawUpperProtocolNum = new byte[4];
        System.arraycopy(rawMessage, offset, rawUpperProtocolNum, 0, 4);
        this.setUpperProtocol(ByteUtil.bytesToInt(rawUpperProtocolNum, ByteOrder.BIG_ENDIAN));

        return offset;
    }

    public int putVersion(byte[] rawMessage, int offset) {
        offset -= 4;
        System.arraycopy(ByteUtil.intToBytes(this.getVersion(), ByteOrder.BIG_ENDIAN), 0, rawMessage, offset, 4);

        return offset;
    }

    public int putCrossChainID(byte[] rawMessage, int offset) {
        offset -= 32;
        System.arraycopy(this.getIdentity().getRawID(), 0, rawMessage, offset, 32);

        return offset;
    }

    public int putUpperProtocol(byte[] rawMessage, int offset) {
        offset -= 4;
        System.arraycopy(ByteUtil.intToBytes(this.getUpperProtocol(), ByteOrder.BIG_ENDIAN), 0, rawMessage, offset, 4);

        return offset;
    }
}
