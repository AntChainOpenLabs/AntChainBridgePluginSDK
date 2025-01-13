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

package com.alipay.antchain.bridge.commons.core.bta;


import java.security.PrivateKey;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.SneakyThrows;

public class BlockchainTrustAnchorV1 extends AbstractBlockchainTrustAnchor {

    public static final int MY_VERSION = 1;

    @TLVField(tag = TLV_TYPE_VERSION, type = TLVTypeEnum.UINT32, order = 0)
    private int version = MY_VERSION;

    private byte[] rawEncodedToSign;

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public void sign(PrivateKey privateKey) {
        setBcOwnerSig(getBcOwnerSigAlgo().getSigner().sign(privateKey, getEncodedToSign()));
    }

    @Override
    public boolean validate() {
        return getBcOwnerSigAlgo().getSigner().verify(
                getBcOwnerPublicKeyObj(),
                getEncodedToSign(),
                getBcOwnerSig()
        );
    }

    @Override
    public void decode(byte[] rawMessage) {
        BeanUtil.copyProperties(TLVUtils.decode(rawMessage, BlockchainTrustAnchorV1.class), this);
    }

    @SneakyThrows(Exception.class)
    @Override
    public byte[] encode() {
        return TLVUtils.encode(this);
    }

    private byte[] getEncodedToSign() {
        if (ObjectUtil.isEmpty(this.rawEncodedToSign)) {
            byte[] packetData = TLVUtils.encode(this, 0xfe);
            if (packetData.length < 6) {
                throw new AntChainBridgeCommonsException(
                        CommonsErrorCodeEnum.INCORRECT_BTA,
                        "packet data too short"
                );
            }
            this.rawEncodedToSign = new byte[packetData.length - 6];
            ArrayUtil.copy(packetData, 6, this.rawEncodedToSign, 0, this.rawEncodedToSign.length);
        }
        return this.rawEncodedToSign;
    }
}
