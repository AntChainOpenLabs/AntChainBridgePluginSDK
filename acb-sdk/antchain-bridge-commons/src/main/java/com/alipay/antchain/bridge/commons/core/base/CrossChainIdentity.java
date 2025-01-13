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

package com.alipay.antchain.bridge.commons.core.base;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.HexUtil;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVMapping;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@TLVMapping(fieldName = "rawID")
public class CrossChainIdentity {

    public static final CrossChainIdentity ZERO_ID = CrossChainIdentity.fromHexStr("0000000000000000000000000000000000000000000000000000000000000000");

    public static final int IDENTITY_LENGTH = 32;

    public static CrossChainIdentity fromHexStr(String hex) {
        CrossChainIdentity id = new CrossChainIdentity();
        id.setRawID(HexUtil.decodeHex(hex));
        if (id.getRawID().length != IDENTITY_LENGTH) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.CROSS_CHAIN_IDENTITY_DECODE_ERROR,
                    String.format("expected id with length %d but got %d", IDENTITY_LENGTH, id.getRawID().length)
            );
        }
        return id;
    }

    public CrossChainIdentity(byte[] rawID) {
        this.rawID = rawID;
        if (getRawID().length != IDENTITY_LENGTH) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.CROSS_CHAIN_IDENTITY_DECODE_ERROR,
                    String.format("expected id with length %d but got %d", IDENTITY_LENGTH, getRawID().length)
            );
        }
    }

    private byte[] rawID;

    public void setRawID(byte[] rawID) {
        if (rawID.length != IDENTITY_LENGTH) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.CROSS_CHAIN_IDENTITY_DECODE_ERROR,
                    String.format("expected id with length %d but got %d", IDENTITY_LENGTH, rawID.length)
            );
        }
        this.rawID = rawID;
    }

    public String toHex() {
        return HexUtil.encodeHexStr(rawID);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CrossChainIdentity)) {
            return false;
        }
        return ArrayUtil.equals(rawID, ((CrossChainIdentity) obj).getRawID());
    }
}
