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

package com.alipay.antchain.bridge.relayer.core.types.network.ws;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.*;

import cn.hutool.core.net.SSLUtil;
import cn.hutool.crypto.PemUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class WsSslFactory {

    @Value("${relayer.network.node.tls.private_key_path}")
    private Resource privateKeyPath;

    @Value("${relayer.network.node.tls.trust_ca_path}")
    private Resource trustCaPath;

//    private PrivateKey randomTLSPrivateKey;
//
//    private Certificate randomTLSCert;

    public SSLContext getSslContext() throws Exception {
        PrivateKey privateKey = PemUtil.readPemPrivateKey(privateKeyPath.getInputStream());
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        List<Certificate> trustCertificates = (List<Certificate>) cf.generateCertificates(trustCaPath.getInputStream());
//        if (ObjectUtil.isNull(randomTLSPrivateKey)) {
//            KeyPair keyPair = KeyUtil.generateKeyPair("RSA", 2048);
//            randomTLSPrivateKey = keyPair.getPrivate();
//            X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
//            X500Principal dnName = new X500Principal("CN=Example");
//            certGen.setSubjectDN(dnName);
//            certGen.setIssuerDN(dnName);
//            certGen.setPublicKey(keyPair.getPublic());
//            certGen.setNotBefore(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30));
//            certGen.setNotAfter(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365 * 10));
//            certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
//            randomTLSCert = certGen.generate(keyPair.getPrivate(), "BC");
//        }
//
//        Certificate[] trustCertificates = new Certificate[]{randomTLSCert};

        char[] keyStorePassword = new char[0];
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        int count = 0;
        for (Certificate cert : trustCertificates) {
            keyStore.setCertificateEntry("cert" + count, cert);
            count++;
        }
        keyStore.setKeyEntry("key", privateKey, keyStorePassword, trustCertificates.toArray(new Certificate[0]));

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
        );
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm()
        );

        trustManagerFactory.init(keyStore);
        keyManagerFactory.init(keyStore, keyStorePassword);

        return SSLUtil.createSSLContext(
                "TLSv1.2",
                keyManagerFactory.getKeyManagers(),
                new TrustManager[] {
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }

                            public void checkClientTrusted(X509Certificate[] certs, String authType) {}

                            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                        }
                }
        );
    }
}
