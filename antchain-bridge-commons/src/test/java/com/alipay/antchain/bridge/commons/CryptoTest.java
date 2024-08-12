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

package com.alipay.antchain.bridge.commons;

import java.io.StringWriter;
import java.security.KeyPair;

import com.alipay.antchain.bridge.commons.utils.crypto.ISigner;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.junit.Assert;
import org.junit.Test;

@Slf4j
public class CryptoTest {

    private static final String msgToSign = "test";

    @Test
    public void testSignerSHA256withRSA() {
        KeyPair keyPair = SignAlgoEnum.SHA256_WITH_RSA.getSigner().generateKeyPair();
        byte[] sig = SignAlgoEnum.SHA256_WITH_RSA.getSigner().sign(keyPair.getPrivate(), msgToSign.getBytes());
        Assert.assertTrue(
                SignAlgoEnum.SHA256_WITH_RSA.getSigner().verify(keyPair.getPublic(), msgToSign.getBytes(), sig)
        );
    }

    @Test
    public void testSignerSHA256withECDSA() {
        KeyPair keyPair = SignAlgoEnum.SHA256_WITH_ECDSA.getSigner().generateKeyPair();
        byte[] sig = SignAlgoEnum.SHA256_WITH_ECDSA.getSigner().sign(keyPair.getPrivate(), msgToSign.getBytes());
        Assert.assertTrue(
                SignAlgoEnum.SHA256_WITH_ECDSA.getSigner().verify(keyPair.getPublic(), msgToSign.getBytes(), sig)
        );
    }

    @Test
    public void testSignerSM3WithSM2() {
        KeyPair keyPair = SignAlgoEnum.SM3_WITH_SM2.getSigner().generateKeyPair();
        byte[] sig = SignAlgoEnum.SM3_WITH_SM2.getSigner().sign(keyPair.getPrivate(), msgToSign.getBytes());
        Assert.assertTrue(
                SignAlgoEnum.SM3_WITH_SM2.getSigner().verify(keyPair.getPublic(), msgToSign.getBytes(), sig)
        );
    }

    @Test
    public void testSignerEd25519() {
        KeyPair keyPair = SignAlgoEnum.ED25519.getSigner().generateKeyPair();
        byte[] sig = SignAlgoEnum.ED25519.getSigner().sign(keyPair.getPrivate(), msgToSign.getBytes());
        Assert.assertTrue(
                SignAlgoEnum.ED25519.getSigner().verify(keyPair.getPublic(), msgToSign.getBytes(), sig)
        );
    }

    @Test
    public void testSignerKeccak256WithSecp256k1() {
        KeyPair keyPair = SignAlgoEnum.KECCAK256_WITH_SECP256K1.getSigner().generateKeyPair();
        byte[] sig = SignAlgoEnum.KECCAK256_WITH_SECP256K1.getSigner().sign(keyPair.getPrivate(), msgToSign.getBytes());
        Assert.assertTrue(
                SignAlgoEnum.KECCAK256_WITH_SECP256K1.getSigner().verify(keyPair.getPublic(), msgToSign.getBytes(), sig)
        );
    }

    @Test
    @SneakyThrows
    public void testSignerReadPemKey() {
        for (SignAlgoEnum signAlgo : SignAlgoEnum.values()) {
            log.info("testSignerReadPemKey {}", signAlgo.getName());
            System.out.println("testSignerReadPemKey " + signAlgo.getName());
            readPemKeyLogic(signAlgo.getSigner());
        }
    }

    @SneakyThrows
    private void readPemKeyLogic(ISigner signer) {
        KeyPair keyPair = signer.generateKeyPair();
        // dump the private key into pem
        StringWriter stringWriter = new StringWriter(256);
        JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
//        jcaPEMWriter.writeObject(
//                new JcaPKCS8Generator(keyPair.getPrivate(), null).generate()
//        );
        jcaPEMWriter.writeObject(keyPair.getPrivate());
        jcaPEMWriter.close();
        String privatePem = stringWriter.toString();
        System.out.println(privatePem);

        Assert.assertTrue(
                signer.verify(
                        keyPair.getPublic(),
                        "test".getBytes(),
                        signer.sign(
                                signer.readPemPrivateKey(privatePem.getBytes()),
                                "test".getBytes()
                        )
                )
        );
    }
}
