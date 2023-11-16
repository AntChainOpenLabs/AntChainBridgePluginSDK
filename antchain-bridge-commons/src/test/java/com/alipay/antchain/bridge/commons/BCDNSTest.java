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

package com.alipay.antchain.bridge.commons;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.PemUtil;
import cn.hutool.crypto.digest.SM3;
import com.alipay.antchain.bridge.commons.bcdns.*;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentityType;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class BCDNSTest {

    private static KeyPair keyPair;

    private static PrivateKey privateKey;

    private static final String KEY_ALGO = "SM2";// "Ed25519";

    private static final String SIG_ALGO = "SM3WITHSM2"; // "Ed25519";

    @BeforeClass
    public static void setUp() throws Exception {
        new ObjectIdentity();
//        Security.addProvider(new BouncyCastleProvider());

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGO);
        keyPairGenerator.initialize(256);
        keyPair = keyPairGenerator.generateKeyPair();

        // dump the private key into pem
        StringWriter stringWriter = new StringWriter(256);
        JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
        jcaPEMWriter.writeObject(keyPair.getPrivate());
        jcaPEMWriter.close();
        String privatePem = stringWriter.toString();
        System.out.println(privatePem);
        FileUtil.writeBytes(privatePem.getBytes(), "cc_certs/private_key.pem");

        if (StrUtil.equalsIgnoreCase(KEY_ALGO, "SM2")) {
            privateKey = PemUtil.readPemPrivateKey(new ByteArrayInputStream(privatePem.getBytes()));
        } else {
            KeyFactory keyFactory = KeyFactory.getInstance(
                    PrivateKeyInfo.getInstance(PemUtil.readPem(new ByteArrayInputStream(privatePem.getBytes())))
                            .getPrivateKeyAlgorithm().getAlgorithm().getId()
            );
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(
                    PemUtil.readPem(new ByteArrayInputStream(privatePem.getBytes()))
            );
            privateKey = keyFactory.generatePrivate(keySpec);
        }

        Assert.assertNotNull(privateKey);
    }

    @Test
    public void testCrossChainCertificate() throws Exception {

        // construct a bcdns root cert

        AbstractCrossChainCertificate certificate = CrossChainCertificateFactory.createCrossChainCertificate(
                CrossChainCertificateV1.MY_VERSION,
                "test",
                new ObjectIdentity(ObjectIdentityType.X509_PUBLIC_KEY_INFO, keyPair.getPublic().getEncoded()),
                DateUtil.currentSeconds(),
                DateUtil.offsetDay(new Date(), 365).getTime() / 1000,
                new BCDNSTrustRootCredentialSubject(
                        "bif",
                        new ObjectIdentity(ObjectIdentityType.X509_PUBLIC_KEY_INFO, keyPair.getPublic().getEncoded()),
                        new byte[]{}
                )
        );

        // this is how to sign something with private key
        Signature signer = Signature.getInstance(SIG_ALGO);
        signer.initSign(privateKey);
        signer.update(certificate.getEncodedToSign());
        byte[] signature = signer.sign();

        // this is how to verify the signature
        Signature verifier = Signature.getInstance(SIG_ALGO);
        verifier.initVerify(keyPair.getPublic());
        verifier.update(certificate.getEncodedToSign());
        Assert.assertTrue(verifier.verify(signature));

        certificate.setProof(
                new AbstractCrossChainCertificate.IssueProof(
                        "SM3",
                        SM3.create().digest(certificate.getEncodedToSign()),
                        SIG_ALGO,
                        signature
                )
        );
        System.out.println(CrossChainCertificateUtil.formatCrossChainCertificateToPem(certificate));
        FileUtil.writeBytes(CrossChainCertificateUtil.formatCrossChainCertificateToPem(certificate).getBytes(), "cc_certs/trust_root.crt");

        // construct a domain cert
        AbstractCrossChainCertificate domainCert = CrossChainCertificateFactory.createCrossChainCertificate(
                CrossChainCertificateV1.MY_VERSION,
                "testdomain",
                new ObjectIdentity(ObjectIdentityType.X509_PUBLIC_KEY_INFO, keyPair.getPublic().getEncoded()),
                DateUtil.currentSeconds(),
                DateUtil.offsetDay(new Date(), 365).getTime() / 1000,
                new DomainNameCredentialSubject(
                        DomainNameCredentialSubject.CURRENT_VERSION,
                        DomainNameTypeEnum.DOMAIN_NAME,
                        new CrossChainDomain(".com"),
                        new CrossChainDomain("antchain.com"),
                        new ObjectIdentity(ObjectIdentityType.X509_PUBLIC_KEY_INFO, keyPair.getPublic().getEncoded()),
                        new byte[]{}
                )
        );

        signer = Signature.getInstance(SIG_ALGO);
        signer.initSign(privateKey);
        signer.update(domainCert.getEncodedToSign());
        domainCert.setProof(
                new AbstractCrossChainCertificate.IssueProof(
                        "SM3",
                        SM3.create().digest(domainCert.getEncodedToSign()),
                        SIG_ALGO,
                        signer.sign()
                )
        );
        System.out.println(CrossChainCertificateUtil.formatCrossChainCertificateToPem(domainCert));
        FileUtil.writeBytes(CrossChainCertificateUtil.formatCrossChainCertificateToPem(domainCert).getBytes(), "cc_certs/antchain.com.crt");

        // construct a domain space cert
        AbstractCrossChainCertificate domainSpaceCert = CrossChainCertificateFactory.createCrossChainCertificate(
                CrossChainCertificateV1.MY_VERSION,
                ".com",
                new ObjectIdentity(ObjectIdentityType.X509_PUBLIC_KEY_INFO, keyPair.getPublic().getEncoded()),
                DateUtil.currentSeconds(),
                DateUtil.offsetDay(new Date(), 365).getTime() / 1000,
                new DomainNameCredentialSubject(
                        DomainNameCredentialSubject.CURRENT_VERSION,
                        DomainNameTypeEnum.DOMAIN_NAME_SPACE,
                        new CrossChainDomain(CrossChainDomain.ROOT_DOMAIN_SPACE),
                        new CrossChainDomain(".com"),
                        new ObjectIdentity(ObjectIdentityType.X509_PUBLIC_KEY_INFO, keyPair.getPublic().getEncoded()),
                        new byte[]{}
                )
        );
        signer = Signature.getInstance(SIG_ALGO);
        signer.initSign(privateKey);
        signer.update(domainSpaceCert.getEncodedToSign());
        domainSpaceCert.setProof(
                new AbstractCrossChainCertificate.IssueProof(
                        "SM3",
                        SM3.create().digest(domainSpaceCert.getEncodedToSign()),
                        SIG_ALGO,
                        signer.sign()
                )
        );
        System.out.println(CrossChainCertificateUtil.formatCrossChainCertificateToPem(domainSpaceCert));
        FileUtil.writeBytes(CrossChainCertificateUtil.formatCrossChainCertificateToPem(domainSpaceCert).getBytes(), "cc_certs/x.com.crt");

        // construct a relayer cert
        AbstractCrossChainCertificate relayerCert = CrossChainCertificateFactory.createCrossChainCertificate(
                CrossChainCertificateV1.MY_VERSION,
                "antchain-relayer",
                new ObjectIdentity(ObjectIdentityType.X509_PUBLIC_KEY_INFO, keyPair.getPublic().getEncoded()),
                DateUtil.currentSeconds(),
                DateUtil.offsetDay(new Date(), 365).getTime() / 1000,
                new RelayerCredentialSubject(
                        RelayerCredentialSubject.CURRENT_VERSION,
                        "antchain-relayer",
                        new ObjectIdentity(ObjectIdentityType.X509_PUBLIC_KEY_INFO, keyPair.getPublic().getEncoded()),
                        new byte[]{}
                )
        );
        signer = Signature.getInstance(SIG_ALGO);
        signer.initSign(privateKey);
        signer.update(relayerCert.getEncodedToSign());
        relayerCert.setProof(
                new AbstractCrossChainCertificate.IssueProof(
                        "SM3",
                        SM3.create().digest(relayerCert.getEncodedToSign()),
                        SIG_ALGO,
                        signer.sign()
                )
        );
        System.out.println(CrossChainCertificateUtil.formatCrossChainCertificateToPem(relayerCert));
        FileUtil.writeBytes(CrossChainCertificateUtil.formatCrossChainCertificateToPem(relayerCert).getBytes(), "cc_certs/relayer.crt");

        Assert.assertEquals(KEY_ALGO.equals("Ed25519") ? 32 : 65, relayerCert.getCredentialSubjectInstance().getRawSubjectPublicKey().length);
        Assert.assertTrue(
                StrUtil.endWith(
                        HexUtil.encodeHexStr(keyPair.getPublic().getEncoded()),
                        HexUtil.encodeHexStr(relayerCert.getCredentialSubjectInstance().getRawSubjectPublicKey())
                )
        );
    }

    @Test
    public void testMultipleCerts() throws Exception {

        // construct a domain cert
        AbstractCrossChainCertificate domainCert = CrossChainCertificateFactory.createCrossChainCertificate(
                CrossChainCertificateV1.MY_VERSION,
                "testdomain1",
                new ObjectIdentity(ObjectIdentityType.X509_PUBLIC_KEY_INFO, keyPair.getPublic().getEncoded()),
                DateUtil.currentSeconds(),
                DateUtil.offsetDay(new Date(), 365).getTime() / 1000,
                new DomainNameCredentialSubject(
                        DomainNameCredentialSubject.CURRENT_VERSION,
                        DomainNameTypeEnum.DOMAIN_NAME,
                        new CrossChainDomain(".com"),
                        new CrossChainDomain("catchain.com"),
                        new ObjectIdentity(ObjectIdentityType.X509_PUBLIC_KEY_INFO, keyPair.getPublic().getEncoded()),
                        new byte[]{}
                )
        );

        Signature signer = Signature.getInstance(SIG_ALGO);
        signer.initSign(privateKey);
        signer.update(domainCert.getEncodedToSign());
        domainCert.setProof(
                new AbstractCrossChainCertificate.IssueProof(
                        "SM3",
                        SM3.create().digest(domainCert.getEncodedToSign()),
                        SIG_ALGO,
                        signer.sign()
                )
        );
        System.out.println(CrossChainCertificateUtil.formatCrossChainCertificateToPem(domainCert));
        FileUtil.writeBytes(CrossChainCertificateUtil.formatCrossChainCertificateToPem(domainCert).getBytes(), "cc_certs/catchain.com.crt");

        // construct a domain cert
        domainCert = CrossChainCertificateFactory.createCrossChainCertificate(
                CrossChainCertificateV1.MY_VERSION,
                "testdomain2",
                new ObjectIdentity(ObjectIdentityType.X509_PUBLIC_KEY_INFO, keyPair.getPublic().getEncoded()),
                DateUtil.currentSeconds(),
                DateUtil.offsetDay(new Date(), 365).getTime() / 1000,
                new DomainNameCredentialSubject(
                        DomainNameCredentialSubject.CURRENT_VERSION,
                        DomainNameTypeEnum.DOMAIN_NAME,
                        new CrossChainDomain(".com"),
                        new CrossChainDomain("dogchain.com"),
                        new ObjectIdentity(ObjectIdentityType.X509_PUBLIC_KEY_INFO, keyPair.getPublic().getEncoded()),
                        new byte[]{}
                )
        );

        signer = Signature.getInstance(SIG_ALGO);
        signer.initSign(privateKey);
        signer.update(domainCert.getEncodedToSign());
        domainCert.setProof(
                new AbstractCrossChainCertificate.IssueProof(
                        "SM3",
                        SM3.create().digest(domainCert.getEncodedToSign()),
                        SIG_ALGO,
                        signer.sign()
                )
        );
        System.out.println(CrossChainCertificateUtil.formatCrossChainCertificateToPem(domainCert));
        FileUtil.writeBytes(CrossChainCertificateUtil.formatCrossChainCertificateToPem(domainCert).getBytes(), "cc_certs/dogchain.com.crt");

        // construct a domain cert
        domainCert = CrossChainCertificateFactory.createCrossChainCertificate(
                CrossChainCertificateV1.MY_VERSION,
                "testdomain3",
                new ObjectIdentity(ObjectIdentityType.X509_PUBLIC_KEY_INFO, keyPair.getPublic().getEncoded()),
                DateUtil.currentSeconds(),
                DateUtil.offsetDay(new Date(), 365).getTime() / 1000,
                new DomainNameCredentialSubject(
                        DomainNameCredentialSubject.CURRENT_VERSION,
                        DomainNameTypeEnum.DOMAIN_NAME,
                        new CrossChainDomain(".com"),
                        new CrossChainDomain("birdchain.com"),
                        new ObjectIdentity(ObjectIdentityType.X509_PUBLIC_KEY_INFO, keyPair.getPublic().getEncoded()),
                        new byte[]{}
                )
        );

        signer = Signature.getInstance(SIG_ALGO);
        signer.initSign(privateKey);
        signer.update(domainCert.getEncodedToSign());
        domainCert.setProof(
                new AbstractCrossChainCertificate.IssueProof(
                        "SM3",
                        SM3.create().digest(domainCert.getEncodedToSign()),
                        SIG_ALGO,
                        signer.sign()
                )
        );
        System.out.println(CrossChainCertificateUtil.formatCrossChainCertificateToPem(domainCert));
        FileUtil.writeBytes(CrossChainCertificateUtil.formatCrossChainCertificateToPem(domainCert).getBytes(), "cc_certs/birdchain.com.crt");
    }
}
