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

import java.math.BigInteger;
import java.security.PublicKey;

import cn.hutool.crypto.KeyUtil;
import cn.hutool.jwt.signers.AlgorithmUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVPacket;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

@Getter
@Setter
public abstract class AbstractBlockchainTrustAnchor implements IBlockchainTrustAnchor {

    public static final short TLV_TYPE_VERSION = 0x0000;
    public static final short TLV_TYPE_DOMAIN_NAME = 0x0001;
    public static final short TLV_TYPE_SUBJECT_PRODUCT = 0x0002;
    public static final short TLV_TYPE_SUBJECT_VERSION = 0x0003;
    public static final short TLV_TYPE_SUBJECT_IDENTITY = 0x0004;
    public static final short TLV_TYPE_EXTENSION = 0x0005;
    public static final short TLV_TYPE_DOMAIN_CERT_PUB_KEY = 0x0006;
    public static final short TLV_TYPE_DOMAIN_CERT_SIGN_ALGO = 0x0007;
    public static final short TLV_TYPE_DOMAIN_CERT_SIGNATURE = 0x0008;
    public static final short TLV_TYPE_PTC_ID = 0x0009;
    public static final short TLV_TYPE_AM_ID = 0x0000a;
    public static final short TLV_TYPE_INIT_HEIGHT = 0x0000b;
    public static final short TLV_TYPE_INIT_BLOCKHASH = 0x0000c;

    public static int decodeVersionFromBytes(byte[] raw) {
        return TLVPacket.decode(raw).getItemForTag(TLV_TYPE_VERSION).getUint32Value();
    }

    @TLVField(tag = TLV_TYPE_DOMAIN_NAME, type = TLVTypeEnum.STRING, order = 1)
    private CrossChainDomain domain;

    @TLVField(tag = TLV_TYPE_SUBJECT_PRODUCT, type = TLVTypeEnum.STRING, order = 2)
    private String subjectProduct;

    @TLVField(tag = TLV_TYPE_SUBJECT_VERSION, type = TLVTypeEnum.UINT32, order = 3)
    private int subjectVersion;

    @TLVField(tag = TLV_TYPE_SUBJECT_IDENTITY, type = TLVTypeEnum.BYTES, order = 4)
    private byte[] subjectIdentity;

    @TLVField(tag = TLV_TYPE_INIT_HEIGHT, type = TLVTypeEnum.VAR_INT, order = 5)
    private BigInteger initHeight;

    @TLVField(tag = TLV_TYPE_INIT_BLOCKHASH, type = TLVTypeEnum.BYTES, order = 6)
    private byte[] initBlockHash;

    @TLVField(tag = TLV_TYPE_AM_ID, type = TLVTypeEnum.BYTES, order = 7)
    private byte[] amId;

    @TLVField(tag = TLV_TYPE_EXTENSION, type = TLVTypeEnum.BYTES, order = 8)
    private byte[] extension;

    @TLVField(tag = TLV_TYPE_DOMAIN_CERT_PUB_KEY, type = TLVTypeEnum.BYTES, order = 9)
    private byte[] bcOwnerPublicKey;

    @TLVField(tag = TLV_TYPE_PTC_ID, type = TLVTypeEnum.BYTES, order = 10)
    private ObjectIdentity ptcOid;

    @TLVField(tag = TLV_TYPE_DOMAIN_CERT_SIGN_ALGO, type = TLVTypeEnum.UINT8, order = 11)
    private SignAlgoEnum bcOwnerSigAlgo;

    @TLVField(tag = TLV_TYPE_DOMAIN_CERT_SIGNATURE, type = TLVTypeEnum.BYTES, order = 0xff)
    private byte[] bcOwnerSig;

    @Override
    @SneakyThrows
    public PublicKey getBcOwnerPublicKeyObj() {
        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(bcOwnerPublicKey);
        return KeyUtil.generatePublicKey(
                AlgorithmUtil.getAlgorithm(publicKeyInfo.getAlgorithm().getAlgorithm().getId()),
                publicKeyInfo.getEncoded()
        );
    }
}
