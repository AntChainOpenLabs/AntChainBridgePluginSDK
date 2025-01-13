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

package com.alipay.antchain.bridge.commons.utils.crypto;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.ECKeyUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.PemUtil;
import cn.hutool.crypto.SignUtil;
import cn.hutool.crypto.asymmetric.SignAlgorithm;
import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVCreator;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVMapping;
import com.alipay.antchain.bridge.commons.utils.crypto.secp256k1.ECKeyPair;
import com.alipay.antchain.bridge.commons.utils.crypto.secp256k1.Sign;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.signers.SM2Signer;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

@AllArgsConstructor
@Getter
@TLVMapping(fieldName = "name")
public enum SignAlgoEnum {

    SHA256_WITH_RSA(
            "SHA256withRSA",
            new ISigner() {
                @Override
                public byte[] sign(PrivateKey privateKey, byte[] data) {
                    return SignUtil.sign(SignAlgorithm.SHA256withRSA, privateKey.getEncoded(), null).sign(data);
                }

                @Override
                public boolean verify(PublicKey publicKey, byte[] data, byte[] sig) {
                    return SignUtil.sign(SignAlgorithm.SHA256withRSA, null, publicKey.getEncoded()).verify(data, sig);
                }

                @Override
                @SneakyThrows
                public KeyPair generateKeyPair() {
                    return KeyPairGenerator.getInstance("RSA").generateKeyPair();
                }

                @Override
                public PrivateKey readPemPrivateKey(byte[] pem) {
                    return PemUtil.readPemPrivateKey(new ByteArrayInputStream(pem));
                }
            }
    ),

    SHA256_WITH_ECDSA(
            "SHA256withECDSA",
            new ISigner() {
                @Override
                public byte[] sign(PrivateKey privateKey, byte[] data) {
                    return SignUtil.sign(SignAlgorithm.SHA256withECDSA, privateKey.getEncoded(), null).sign(data);
                }

                @Override
                public boolean verify(PublicKey publicKey, byte[] data, byte[] sig) {
                    return SignUtil.sign(SignAlgorithm.SHA256withECDSA, null, publicKey.getEncoded()).verify(data, sig);
                }

                @Override
                @SneakyThrows
                public KeyPair generateKeyPair() {
                    return KeyPairGenerator.getInstance("EC").generateKeyPair();
                }

                @Override
                @SneakyThrows
                public PrivateKey readPemPrivateKey(byte[] pem) {
                    AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC", "SunEC");
                    parameters.init(new ECGenParameterSpec("secp256r1"));
                    ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);
                    ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(
                            new BigInteger(1, PemUtil.readPem(new ByteArrayInputStream(pem))),
                            ecParameters
                    );
                    KeyFactory kf = KeyFactory.getInstance("EC");
                    return kf.generatePrivate(privateKeySpec);
                }
            }
    ),

    SM3_WITH_SM2(
            "SM3withSM2",
            new ISigner() {
                @Override
                @SneakyThrows
                public byte[] sign(PrivateKey privateKey, byte[] data) {
                    if (!(privateKey instanceof BCECPrivateKey)) {
                        privateKey = KeyUtil.generatePrivateKey("SM2", privateKey.getEncoded());
                    }
                    SM2Signer signer = new SM2Signer();
                    signer.init(true, ECKeyUtil.toPrivateParams(privateKey));
                    signer.update(data, 0, data.length);
                    return signer.generateSignature();
                }

                @Override
                @SneakyThrows
                public boolean verify(PublicKey publicKey, byte[] data, byte[] sig) {
                    if (!(publicKey instanceof BCECPublicKey)) {
                        publicKey = KeyUtil.generatePublicKey("SM2", publicKey.getEncoded());
                    }
                    SM2Signer signer = new SM2Signer();
                    signer.init(false, ECKeyUtil.toPublicParams(publicKey));
                    signer.update(data, 0, data.length);
                    return signer.verifySignature(sig);
                }

                @Override
                @SneakyThrows
                public KeyPair generateKeyPair() {
                    return KeyUtil.generateKeyPair("SM2");
                }

                @Override
                public PrivateKey readPemPrivateKey(byte[] pem) {
                    return PemUtil.readPemPrivateKey(new ByteArrayInputStream(pem));
                }
            }
    ),

    ED25519(
            "Ed25519",
            new ISigner() {
                @Override
                @SneakyThrows
                public byte[] sign(PrivateKey privateKey, byte[] data) {
                    Signature signature = Signature.getInstance("Ed25519");
                    signature.initSign(privateKey);
                    signature.update(data);
                    return signature.sign();
                }

                @Override
                @SneakyThrows
                public boolean verify(PublicKey publicKey, byte[] data, byte[] sig) {
                    Signature signature = Signature.getInstance("Ed25519");
                    signature.initVerify(publicKey);
                    signature.update(data);
                    return signature.verify(sig);
                }

                @Override
                @SneakyThrows
                public KeyPair generateKeyPair() {
                    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Ed25519");
                    keyPairGenerator.initialize(256);
                    return keyPairGenerator.generateKeyPair();
                }

                @Override
                @SneakyThrows
                public PrivateKey readPemPrivateKey(byte[] pem) {
                    KeyFactory keyFactory = KeyFactory.getInstance(
                            PrivateKeyInfo.getInstance(PemUtil.readPem(new ByteArrayInputStream(pem)))
                                    .getPrivateKeyAlgorithm().getAlgorithm().getId()
                    );
                    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(
                            PemUtil.readPem(new ByteArrayInputStream(pem))
                    );
                    return keyFactory.generatePrivate(keySpec);
                }
            }
    ),

    /**
     * Do signature work same as ethereum
     */
    KECCAK256_WITH_SECP256K1(
            "Keccak256WithSecp256k1",
            new ISigner() {
                @Override
                public byte[] sign(PrivateKey privateKey, byte[] data) {
                    if (!(privateKey instanceof ECPrivateKey)) {
                        throw new RuntimeException("Unsupported private key type for Secp256k1: " + privateKey.getClass().getName());
                    }
                    return Sign.signMessage(data, ECKeyPair.create(((ECPrivateKey)privateKey).getS())).toEthereumFormat();
                }

                @Override
                @SneakyThrows
                public boolean verify(PublicKey publicKey, byte[] data, byte[] sig) {
                    if (sig.length != 65) {
                        throw new RuntimeException("Invalid Secp256k1 signature length: " + sig.length);
                    }
                    if (!(publicKey instanceof ECPublicKey)) {
                        throw new RuntimeException("Unsupported public key type for Secp256k1: " + publicKey.getClass().getName());
                    }

                    BigInteger x = ((ECPublicKey) publicKey).getW().getAffineX();
                    BigInteger y = ((ECPublicKey) publicKey).getW().getAffineY();
                    BigInteger pubkey = x.multiply(BigInteger.valueOf(16).pow(64)).add(y);
                    return pubkey.equals(
                            Sign.signedMessageHashToKey(
                                    HashAlgoEnum.KECCAK_256.hash(data),
                                    new Sign.SignatureData((byte) (sig[64] + 27), ArrayUtil.sub(sig, 0, 32), ArrayUtil.sub(sig, 32, 64))
                            )
                    );
                }

                @Override
                @SneakyThrows
                public KeyPair generateKeyPair() {
                    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
                    keyPairGenerator.initialize(new ECGenParameterSpec("secp256k1"));
                    return keyPairGenerator.generateKeyPair();
                }

                @Override
                public PrivateKey readPemPrivateKey(byte[] pem) {
                    return PemUtil.readPemPrivateKey(new ByteArrayInputStream(pem));
                }
            }
    );

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final String name;

    private final ISigner signer;

    @JSONField
    public String getName() {
        return this.name;
    }

    @JSONCreator
    @TLVCreator
    public static SignAlgoEnum getByName(String name) {
        for (SignAlgoEnum signAlgoEnum : SignAlgoEnum.values()) {
            if (signAlgoEnum.getName().equals(name)) {
                return signAlgoEnum;
            }
        }
        throw new AntChainBridgeCommonsException(
                CommonsErrorCodeEnum.UNSUPPORTED_HASH_TYPE_ERROR,
                "Unsupported sign algo type: " + name
        );
    }

    public static SignAlgoEnum valueOf(Byte b) {
        if (SHA256_WITH_RSA.ordinal() == b) {
            return SHA256_WITH_RSA;
        } else if (SHA256_WITH_ECDSA.ordinal() == b) {
            return SHA256_WITH_ECDSA;
        } else if (SM3_WITH_SM2.ordinal() == b) {
            return SM3_WITH_SM2;
        } else if (ED25519.ordinal() == b) {
            return ED25519;
        } else if (KECCAK256_WITH_SECP256K1.ordinal() == b) {
            return KECCAK256_WITH_SECP256K1;
        }
        throw new IllegalArgumentException("Invalid byte value for SignAlgoEnum: " + b);
    }

    public static SignAlgoEnum getSignAlgoByKeySuffix(String keyAlgo) {
        if (StrUtil.endWithIgnoreCase(SHA256_WITH_RSA.getName(), keyAlgo)) {
            return SHA256_WITH_RSA;
        } else if (StrUtil.endWithIgnoreCase(SHA256_WITH_ECDSA.getName(), keyAlgo) || StrUtil.equalsIgnoreCase("secp256r1", keyAlgo)) {
            return SHA256_WITH_ECDSA;
        } else if (StrUtil.endWithIgnoreCase(SM3_WITH_SM2.getName(), keyAlgo)) {
            return SM3_WITH_SM2;
        } else if (StrUtil.endWithIgnoreCase(ED25519.getName(), keyAlgo)) {
            return ED25519;
        } else if (StrUtil.endWithIgnoreCase(KECCAK256_WITH_SECP256K1.getName(), keyAlgo)) {
            return KECCAK256_WITH_SECP256K1;
        }
        throw new IllegalArgumentException("Invalid key algo: " + keyAlgo);
    }
}
