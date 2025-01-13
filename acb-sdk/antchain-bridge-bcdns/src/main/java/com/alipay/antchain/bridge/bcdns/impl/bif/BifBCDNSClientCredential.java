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

package com.alipay.antchain.bridge.bcdns.impl.bif;

import cn.bif.module.encryption.model.KeyType;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.ECKeyUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.PemUtil;
import cn.hutool.jwt.signers.AlgorithmUtil;
import com.alipay.antchain.bridge.bcdns.types.exception.AntChainBridgeBCDNSException;
import com.alipay.antchain.bridge.bcdns.types.exception.BCDNSErrorCodeEnum;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.ICredentialSubject;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPublicKey;

import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;

@Getter
public class BifBCDNSClientCredential {

    private AbstractCrossChainCertificate clientCert;

    private ICredentialSubject clientCredentialSubject;

    private PrivateKey clientKey;

    private PrivateKey authorizedKey;

    private PublicKey authorizedPublicKey;

    private SignAlgoEnum sigAlgo;

    private SignAlgoEnum authorizedSigAlgo;

    public BifBCDNSClientCredential(
            String clientCertPem,
            String privateKeyPem,
            String sigAlgo,
            String authorizedKeyPem,
            String authorizedPublicKeyPem,
            String authorizedSigAlgo
    ) {
        this(clientCertPem, privateKeyPem, SignAlgoEnum.getByName(sigAlgo), authorizedKeyPem, authorizedPublicKeyPem, SignAlgoEnum.getByName(authorizedSigAlgo));
    }

    public BifBCDNSClientCredential(
            String clientCertPem,
            String privateKeyPem,
            SignAlgoEnum sigAlgo,
            String authorizedKeyPem,
            String authorizedPublicKeyPem,
            SignAlgoEnum authorizedSigAlgo
    ) {
        this.clientCert = CrossChainCertificateUtil.readCrossChainCertificateFromPem(
                clientCertPem.getBytes()
        );
        this.clientCredentialSubject = clientCert.getCredentialSubjectInstance();
        this.clientKey = readPrivateKeyFromPem(privateKeyPem);
        this.authorizedKey = readPrivateKeyFromPem(authorizedKeyPem);
        this.authorizedPublicKey = readPublicKeyFromPem(authorizedPublicKeyPem);
        this.sigAlgo = sigAlgo;
        this.authorizedSigAlgo = authorizedSigAlgo;
    }

    public byte[] signAuthorizedRequest(byte[] rawRequest) {
        try {
            return authorizedSigAlgo.getSigner().sign(authorizedKey, rawRequest);
        } catch (Exception e) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_SIGN_REQUEST_FAILED,
                    "failed to sign for request using the authorized key: ",
                    e
            );
        }
    }

    public byte[] signRequest(byte[] rawRequest) {
        try {
            return sigAlgo.getSigner().sign(clientKey, rawRequest);
        } catch (Exception e) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_SIGN_REQUEST_FAILED,
                    "failed to sign for request: ",
                    e
            );
        }
    }

    public String getBifFormatAuthorizedPublicKey() {
        return getBifFormatPublicKey(authorizedPublicKey);
    }

    public String getBifFormatClientPublicKey() {
        return getBifFormatPublicKey(CrossChainCertificateUtil.getPublicKeyFromCrossChainCertificate(clientCert));
    }

    private String getBifFormatPublicKey(PublicKey publicKey) {
        byte[] rawPublicKey;
        if (StrUtil.equalsIgnoreCase(publicKey.getAlgorithm(), "Ed25519")) {
            if (publicKey instanceof BCEdDSAPublicKey) {
                rawPublicKey = ((BCEdDSAPublicKey) publicKey).getPointEncoding();
            } else {
                throw new RuntimeException("your Ed25519 public key class not support: " + publicKey.getClass().getName());
            }
        } else if (StrUtil.equalsAnyIgnoreCase(publicKey.getAlgorithm(), "SM2", "EC")) {
            if (publicKey instanceof ECPublicKey) {
                rawPublicKey = ECKeyUtil.toPublicParams(publicKey).getQ().getEncoded(false);
            } else {
                throw new RuntimeException("your SM2/EC public key class not support: " + publicKey.getClass().getName());
            }
        } else {
            throw new RuntimeException(publicKey.getAlgorithm() + " not support");
        }

        byte[] rawPublicKeyWithSignals = new byte[rawPublicKey.length + 3];
        System.arraycopy(rawPublicKey, 0, rawPublicKeyWithSignals, 3, rawPublicKey.length);
        rawPublicKeyWithSignals[0] = -80;
        rawPublicKeyWithSignals[1] = StrUtil.equalsIgnoreCase(publicKey.getAlgorithm(), "Ed25519") ? KeyType.ED25519_VALUE : KeyType.SM2_VALUE;
        rawPublicKeyWithSignals[2] = 102;

        return HexUtil.encodeHexStr(rawPublicKeyWithSignals);
    }

    @SneakyThrows
    private PrivateKey readPrivateKeyFromPem(String privateKeyPem) {
        try {
            return PemUtil.readPemPrivateKey(new ByteArrayInputStream(privateKeyPem.getBytes()));
        } catch (Exception e) {
            byte[] rawPemOb = PemUtil.readPem(new ByteArrayInputStream(privateKeyPem.getBytes()));
            KeyFactory keyFactory = KeyFactory.getInstance(
                    PrivateKeyInfo.getInstance(rawPemOb).getPrivateKeyAlgorithm().getAlgorithm().getId()
            );
            return keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(
                            rawPemOb
                    )
            );
        }
    }

    @SneakyThrows
    private PublicKey readPublicKeyFromPem(String publicKeyPem) {
        SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(PemUtil.readPem(new ByteArrayInputStream(publicKeyPem.getBytes())));
        return KeyUtil.generatePublicKey(
                AlgorithmUtil.getAlgorithm(keyInfo.getAlgorithm().getAlgorithm().getId()),
                keyInfo.getEncoded()
        );
    }
}
