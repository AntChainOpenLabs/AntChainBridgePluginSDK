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

package com.alipay.antchain.bridge.commons.bcdns.utils;

import java.security.KeyFactory;
import java.security.PublicKey;

import cn.ac.caict.bid.model.BIDDocumentOperation;
import cn.ac.caict.bid.model.BIDpublicKeyOperation;
import cn.bif.common.JsonUtils;
import cn.bif.exception.EncException;
import cn.bif.module.encryption.model.KeyType;
import cn.bif.utils.base.Base58;
import cn.bif.utils.hash.HashUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.BCUtil;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import org.bouncycastle.jcajce.spec.RawEncodedKeySpec;

public class BIDHelper {

    public static BIDDocumentOperation getBIDDocumentFromRawSubject(byte[] rawSubject) {
        return JsonUtils.toJavaObject(new String(rawSubject), BIDDocumentOperation.class);
    }

    public static String encAddress(KeyType type, byte[] rawPkey) {
        byte[] buff = new byte[22];
        byte[] hashPkey = HashUtil.CalHash(type, rawPkey);
        System.arraycopy(hashPkey, 10, buff, 0, 22);
        String encAddress = Base58.encode(buff);
        switch (type) {
            case ED25519:
                return "did:bid:ef" + encAddress;
            case SM2:
                return "did:bid:zf" + encAddress;
            default:
                throw new EncException("type does not exist");
        }
    }

    public static PublicKey getPublicKeyFromBIDDocument(BIDDocumentOperation document) {
        try {
            BIDpublicKeyOperation biDpublicKeyOperation = document.getPublicKey()[0];
            byte[] rawPubkeyWithSignals = HexUtil.decodeHex(biDpublicKeyOperation.getPublicKeyHex());
            byte[] rawPubkey = new byte[rawPubkeyWithSignals.length - 3];
            System.arraycopy(rawPubkeyWithSignals, 3, rawPubkey, 0, rawPubkey.length);
            if (biDpublicKeyOperation.getType() == KeyType.ED25519) {
                return KeyFactory.getInstance("Ed25519").generatePublic(new RawEncodedKeySpec(rawPubkey));
            } else if (biDpublicKeyOperation.getType() == KeyType.SM2) {
                return BCUtil.decodeECPoint(rawPubkey, "prime256v1");
            }
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.BCDNS_BID_PUBLIC_KEY_ALGO_NOT_SUPPORT,
                    StrUtil.format("the key type of BID is not expected")
            );
        } catch (AntChainBridgeCommonsException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.BCDNS_OID_BID_INFO_ERROR,
                    "failed to get public key from subject bid info",
                    e
            );
        }
    }

    public static byte[] getRawPublicKeyFromBIDDocument(BIDDocumentOperation document) {
        BIDpublicKeyOperation biDpublicKeyOperation = document.getPublicKey()[0];
        byte[] rawPubkeyWithSignals = HexUtil.decodeHex(biDpublicKeyOperation.getPublicKeyHex());
        byte[] rawPubkey = new byte[rawPubkeyWithSignals.length - 3];
        System.arraycopy(rawPubkeyWithSignals, 3, rawPubkey, 0, rawPubkey.length);
        return rawPubkey;
    }

    public static KeyType getKeyTypeFromPublicKey(PublicKey publicKey) {
        if (StrUtil.equalsAnyIgnoreCase(publicKey.getAlgorithm(), "Ed25519", "1.3.6.1.4.1.11591.15.1")) {
            return KeyType.ED25519;
        }
        if (StrUtil.equalsAnyIgnoreCase(publicKey.getAlgorithm(), "SM2", "EC", "1.0.14888.3.14")) {
            return KeyType.SM2;
        }
        return null;
    }
}
