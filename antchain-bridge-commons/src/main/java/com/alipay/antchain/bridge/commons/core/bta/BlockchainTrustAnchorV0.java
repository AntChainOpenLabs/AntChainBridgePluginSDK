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


import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.X509EncodedKeySpec;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.SneakyThrows;

public class BlockchainTrustAnchorV0 extends AbstractBlockchainTrustAnchor {

    public static final int MY_VERSION = 0;

    @TLVField(tag = TLV_TYPE_VERSION, type = TLVTypeEnum.UINT32, order = 0)
    private int version = MY_VERSION;

    private byte[] rawEncodedToSign;

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public void sign(PrivateKey privateKey) {
        switch (this.getBcOwnerSigAlgo()) {
            case SIGN_ALGO_SHA256_WITH_RSA:
                if (!(privateKey instanceof RSAPrivateKey)) {
                    throw new AntChainBridgeCommonsException(
                            CommonsErrorCodeEnum.INCORRECT_BTA,
                            "expect RSAPrivateKey instance but not"
                    );
                }
                signWithRSA((RSAPrivateKey) privateKey);
                break;
            case SIGN_ALGO_SHA256_WITH_ECC:
                if (!(privateKey instanceof ECPrivateKey)) {
                    throw new AntChainBridgeCommonsException(
                            CommonsErrorCodeEnum.INCORRECT_BTA,
                            "expect ECPrivateKey instance but not"
                    );
                }
                signWithECC((ECPrivateKey) privateKey);
                break;
            default:
                throw new AntChainBridgeCommonsException(
                        CommonsErrorCodeEnum.INCORRECT_BTA,
                        String.format("signature algorithm not support: %s", this.getBcOwnerSigAlgo())
                );
        }
    }

    private void signWithRSA(RSAPrivateKey rsaPrivateKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(rsaPrivateKey);
            signature.update(this.getEncodedToSign());

            setBcOwnerSig(signature.sign());
        } catch (Exception e) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.INCORRECT_BTA,
                    "sign raw encode by sha256 with rsa failed"
            );
        }
    }

    private void signWithECC(ECPrivateKey ecPrivateKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(ecPrivateKey);
            signature.update(this.getEncodedToSign());

            setBcOwnerSig(signature.sign());
        } catch (Exception e) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.INCORRECT_BTA,
                    "sign raw encode by sha256 with rsa failed"
            );
        }
    }

    @Override
    public boolean validate() {

        try {
            KeyFactory keyFactory = KeyFactory.getInstance(getKeyAlgo());
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(this.getBcOwnerPublicKey());
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            Signature signature = Signature.getInstance(getSigAlgo());

            signature.initVerify(publicKey);
            signature.update(this.getEncodedToSign());

            return signature.verify(this.getBcOwnerSig());

        } catch (Exception e) {
            return false;
        }
    }

    private String getKeyAlgo() {
        switch (this.getBcOwnerSigAlgo()) {
            case SIGN_ALGO_SHA256_WITH_RSA:
                return "RSA";
            case SIGN_ALGO_SHA256_WITH_ECC:
                return "EC";
            default:
                throw new AntChainBridgeCommonsException(
                        CommonsErrorCodeEnum.INCORRECT_BTA,
                        String.format("pubkey algorithm not support: %s", this.getBcOwnerSigAlgo())
                );
        }
    }

    private String getSigAlgo() {
        switch (this.getBcOwnerSigAlgo()) {
            case SIGN_ALGO_SHA256_WITH_RSA:
                return "SHA256withRSA";
            case SIGN_ALGO_SHA256_WITH_ECC:
                return "SHA256withECDSA";
            default:
                throw new AntChainBridgeCommonsException(
                        CommonsErrorCodeEnum.INCORRECT_BTA,
                        String.format("signature algorithm not support: %s", this.getBcOwnerSigAlgo())
                );
        }
    }

    @Override
    public void decode(byte[] rawMessage) {
        BeanUtil.copyProperties(TLVUtils.decode(rawMessage, BlockchainTrustAnchorV0.class), this);
    }

    @SneakyThrows(Exception.class)
    @Override
    public byte[] encode() {
        calcHashToSign();
        return TLVUtils.encode(this);
    }

    private byte[] getEncodedToSign() {
        if (ObjectUtil.isEmpty(this.rawEncodedToSign)) {
            byte[] packetData = TLVUtils.encode(this, 7);
            if (packetData.length < 6) {
                throw new AntChainBridgeCommonsException(
                        CommonsErrorCodeEnum.INCORRECT_BTA,
                        "packet data too short"
                );
            }
            this.rawEncodedToSign= new byte[packetData.length - 6];
            ArrayUtil.copy(packetData, 6, this.rawEncodedToSign, 0, this.rawEncodedToSign.length);
        }
        return this.rawEncodedToSign;
    }

    public byte[] calcHashToSign() throws Exception {
        if (ObjectUtil.isEmpty(super.getHashToSign())) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(this.getEncodedToSign());
            setHashToSign(md.digest());
        }
        return super.getHashToSign();
    }
}
