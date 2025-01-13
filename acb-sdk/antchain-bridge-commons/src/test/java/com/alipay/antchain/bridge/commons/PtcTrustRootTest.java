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

import cn.hutool.core.map.MapUtil;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.commons.core.ptc.PTCVerifyAnchor;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.PrivateKey;

public class PtcTrustRootTest {

    private static final String PTC_CERT = "-----BEGIN PROOF TRANSFORMATION COMPONENT CERTIFICATE-----\n" +
            "AAD4AQAAAAABAAAAMQEADAAAAGFudGNoYWluLXB0YwIAAQAAAAIDAGsAAAAAAGUA\n" +
            "AAAAAAEAAAAAAQBYAAAAMFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEC4Wuvhr7FFHJ\n" +
            "4Fqa3HoxeuP0rzMJr3PBFI/ng5gxWxhbJcU5rwfdg4mcuJzlpjWYe6Oi4oifOpb7\n" +
            "8usUKQk/wwQACAAAAL33wmYAAAAABQAIAAAAPSukaAAAAAAGAKAAAAAAAJoAAAAA\n" +
            "AAMAAAAxLjABAA0AAABjb21taXR0ZWUtcHRjAgABAAAAAQMAawAAAAAAZQAAAAAA\n" +
            "AQAAAAABAFgAAAAwVjAQBgcqhkjOPQIBBgUrgQQACgNCAAQLha6+GvsUUcngWprc\n" +
            "ejF64/SvMwmvc8EUj+eDmDFbGFslxTmvB92DiZy4nOWmNZh7o6LiiJ86lvvy6xQp\n" +
            "CT/DBAAAAAAABwCfAAAAAACZAAAAAAAKAAAAS0VDQ0FLLTI1NgEAIAAAAFsd3DdS\n" +
            "GQUHCKafwbD5hJ70Y7IdNtrjnH10OVZoQvxzAgAWAAAAS2VjY2FrMjU2V2l0aFNl\n" +
            "Y3AyNTZrMQMAQQAAAPi7je8dWPyFAtNduzBIwjYKHpspsxzZIcvjAwPnirHQVsdu\n" +
            "X2H1nxiTZ7LU5u0WUAZskpd3tDQoTzLUC6ol47UB\n" +
            "-----END PROOF TRANSFORMATION COMPONENT CERTIFICATE-----\n";

    private static final String ptcPrivateKey = "-----BEGIN EC PRIVATE KEY-----\n" +
            "MHQCAQEEIC0cCFjdnZIsj2U3BuCLsuXnE6+FN0K+VwUD74rwY5WsoAcGBSuBBAAK\n" +
            "oUQDQgAEC4Wuvhr7FFHJ4Fqa3HoxeuP0rzMJr3PBFI/ng5gxWxhbJcU5rwfdg4mc\n" +
            "uJzlpjWYe6Oi4oifOpb78usUKQk/ww==\n" +
            "-----END EC PRIVATE KEY-----\n";

    @Test
    public void testNewPtcTrustRoot() {
        AbstractCrossChainCertificate ptcCertObj = CrossChainCertificateUtil.readCrossChainCertificateFromPem(PTC_CERT.getBytes());
        PrivateKey ptcPrivateKeyObj = SignAlgoEnum.KECCAK256_WITH_SECP256K1.getSigner().readPemPrivateKey(ptcPrivateKey.getBytes());
        String bcdnsDomainSpace = ".com";

        // build it first
        PTCTrustRoot ptcTrustRoot = PTCTrustRoot.builder()
                .ptcCrossChainCert(ptcCertObj)
                .networkInfo("{}".getBytes())
                .issuerBcdnsDomainSpace(new CrossChainDomain(bcdnsDomainSpace))
                .sigAlgo(SignAlgoEnum.KECCAK256_WITH_SECP256K1)
                .verifyAnchorMap(MapUtil.builder(
                        BigInteger.ZERO,
                        new PTCVerifyAnchor(
                                BigInteger.ZERO,
                                "{}".getBytes()
                        )
                ).build())
                .build();

        // sign it with ptc private key which applied PTC certificate
        ptcTrustRoot.sign(ptcPrivateKeyObj);

        Assert.assertNotNull(ptcTrustRoot.getSig());
    }
}
