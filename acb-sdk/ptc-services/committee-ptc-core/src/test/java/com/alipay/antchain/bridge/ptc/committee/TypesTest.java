package com.alipay.antchain.bridge.ptc.committee;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;

import cn.hutool.core.map.MapUtil;
import cn.hutool.crypto.PemUtil;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.X509PubkeyInfoObjectIdentity;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.commons.core.ptc.PTCVerifyAnchor;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import com.alipay.antchain.bridge.ptc.committee.types.network.CommitteeNetworkInfo;
import com.alipay.antchain.bridge.ptc.committee.types.trustroot.CommitteeVerifyAnchor;
import org.junit.Assert;
import org.junit.Test;

public class TypesTest {

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

    private static final String committeeNodePublicKey = "-----BEGIN PUBLIC KEY-----\n" +
            "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEC4Wuvhr7FFHJ4Fqa3HoxeuP0rzMJr3PB\n" +
            "FI/ng5gxWxhbJcU5rwfdg4mcuJzlpjWYe6Oi4oifOpb78usUKQk/ww==\n" +
            "-----END PUBLIC KEY-----\n";

    private static final String committeeNodeTLSCert = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDnDCCAoSgAwIBAgIJANoR+ubebhQbMA0GCSqGSIb3DQEBCwUAMHwxETAPBgNV\n" +
            "BAoMCGFudGNoYWluMQ4wDAYDVQQLDAVvZGF0czElMCMGA1UEAwwcYW50Y2hhaW4u\n" +
            "b2RhdHNfdGxzLnNpdC5vZGF0czERMA8GA1UEBAwIdGxzLnJvb3QxHTAbBgNVBAkM\n" +
            "FENOLlpoZWppYW5nLkhhbmd6aG91MB4XDTIzMDYwNTE0Mzc0NloXDTMzMDYwMjE0\n" +
            "Mzc0NlowcTELMAkGA1UEBhMCQ04xETAPBgNVBAgMCFpoZWppYW5nMREwDwYDVQQH\n" +
            "DAhIYW5nemhvdTERMA8GA1UECgwIYW50Y2hhaW4xDjAMBgNVBAsMBW9kYXRzMRkw\n" +
            "FwYDVQQDDBBkcy50bHMuc2l0Lm9kYXRzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A\n" +
            "MIIBCgKCAQEAz4nUv+ykC+xZuWgupkKQg3VIoTN4yz0o9lriqpufg+Qteh2wniAR\n" +
            "dWjdrkHpNYBJtMMz1bEL84w8yC+kqM5IOFpDsnPHLRqQb2yktcjBssyVe71BvuGU\n" +
            "7wT+N7DQUbIzTg7F+yzCbNij7NjUacV70EKdCkRBqIYSLEKbzMBB4r/huZYuPZad\n" +
            "rhIXuoh6CdotVOE4iZGwUx0pqJCjQHN9DZ5lZX302dK6cqXvtr79BkRUfwUcqmNU\n" +
            "pl18jMUKi+V/S54py92YnJFdpd//QmdaAY7WaDImtnpuafw+tRNZ0FR0ivejic/y\n" +
            "jfqW2HTi7GOtyVlvJa2KHq3oKtGiA0jbjwIDAQABoywwKjAJBgNVHRMEAjAAMB0G\n" +
            "A1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjANBgkqhkiG9w0BAQsFAAOCAQEA\n" +
            "lDmUT3expsbDPiDB0L1R4JVjRck+0KMG0kUKt5GkApwvQOaTXLWpS9XoXxN2j7Hf\n" +
            "UGVHW18KmG3zMn3ZwT5koyPcHohnq8SoDCNGf0XCT9WHaDpSnmZmrwY1zdTNcCkM\n" +
            "kphnHdNERM8xAH1dXX+MW7oqzIxVkQU9NI8NRm+u0aRZUs+kMAoz/NNHUgR+pPQw\n" +
            "GUAzwoASp+LiTYsXM6XBW8OpB3PM6nOEYzpmbzE2LYdHxvS4mkUl74Cyz31L0PSq\n" +
            "Q45YA8S2qdqNCWgo+vIFIJqhZf8ymw9VRHGFpgqufZRbkgAxMWkast2AXGaOjUvB\n" +
            "N92eu9p3hyI/j1XOLD9CRA==\n" +
            "-----END CERTIFICATE-----";

    @Test
    public void testNewPtcTrustRoot() {
        AbstractCrossChainCertificate ptcCertObj = CrossChainCertificateUtil.readCrossChainCertificateFromPem(PTC_CERT.getBytes());
        PrivateKey ptcPrivateKeyObj = SignAlgoEnum.KECCAK256_WITH_SECP256K1.getSigner().readPemPrivateKey(ptcPrivateKey.getBytes());
        String bcdnsDomainSpace = ".com";

        // prepare the public keys
        PublicKey committeeNodePubkey = new X509PubkeyInfoObjectIdentity(
                PemUtil.readPem(new ByteArrayInputStream(committeeNodePublicKey.getBytes()))
        ).getPublicKey();

        CommitteeVerifyAnchor verifyAnchor = new CommitteeVerifyAnchor("committee");
        verifyAnchor.addNode("node1", "key1", committeeNodePubkey);

        // prepare the network stuff
        CommitteeNetworkInfo committeeNetworkInfo = new CommitteeNetworkInfo("committee");
        committeeNetworkInfo.addEndpoint("node1", "grpcs://0.0.0.0:8080", committeeNodeTLSCert);

        // build it first
        PTCTrustRoot ptcTrustRoot = PTCTrustRoot.builder()
                .ptcCrossChainCert(ptcCertObj)
                .networkInfo(committeeNetworkInfo.encode())
                .issuerBcdnsDomainSpace(new CrossChainDomain(bcdnsDomainSpace))
                .sigAlgo(SignAlgoEnum.KECCAK256_WITH_SECP256K1)
                .verifyAnchorMap(MapUtil.builder(
                        BigInteger.ZERO,
                        new PTCVerifyAnchor(
                                BigInteger.ZERO,
                                verifyAnchor.encode()
                        )
                ).build())
                .build();

        // sign it with ptc private key which applied PTC certificate
        ptcTrustRoot.sign(ptcPrivateKeyObj);

        Assert.assertNotNull(ptcTrustRoot.getSig());
    }
}