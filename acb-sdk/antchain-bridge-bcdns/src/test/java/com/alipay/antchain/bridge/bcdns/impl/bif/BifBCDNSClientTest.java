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

import cn.ac.caict.bid.model.BIDDocumentOperation;
import cn.bif.common.JsonUtils;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alipay.antchain.bridge.bcdns.impl.BlockChainDomainNameServiceFactory;
import com.alipay.antchain.bridge.bcdns.impl.bif.conf.BifBCNDSConfig;
import com.alipay.antchain.bridge.bcdns.impl.bif.conf.BifCertificationServiceConfig;
import com.alipay.antchain.bridge.bcdns.impl.bif.conf.BifChainConfig;
import com.alipay.antchain.bridge.bcdns.service.BCDNSTypeEnum;
import com.alipay.antchain.bridge.bcdns.service.IBlockChainDomainNameService;
import com.alipay.antchain.bridge.bcdns.types.req.QueryRelayerCertificateRequest;
import com.alipay.antchain.bridge.bcdns.types.req.QueryThirdPartyBlockchainTrustAnchorRequest;
import com.alipay.antchain.bridge.bcdns.types.req.RegisterThirdPartyBlockchainTrustAnchorRequest;
import com.alipay.antchain.bridge.bcdns.types.resp.*;
import com.alipay.antchain.bridge.commons.bcdns.*;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentityType;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.commons.core.ptc.PTCVerifyAnchor;
import com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyBlockchainTrustAnchor;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import lombok.SneakyThrows;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.mockito.MockedStatic;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.PrivateKey;

@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BifBCDNSClientTest {

    private static IBlockChainDomainNameService bifBCDNSClient;

    private static final String KEY_TYPE = "Ed25519";

    private static final String SIG_ALGO = "Ed25519";

    private static final ObjectIdentityType OID_TYPE = ObjectIdentityType.BID;

    private static final String ANTCHAIN_DOMAIN = "antchain";

    private static AbstractCrossChainCertificate ANTCHAIN_CSR;

    private static String receiptId;

    private static MockedStatic mockStaticObj = null;

    private PTCTrustRoot ptcTrustRoot;

    @BeforeClass
    public static void setup() {

        String rawBifConfig = JSON.toJSONString(
                new BifBCNDSConfig(
                        new BifCertificationServiceConfig(
                                "http://0.0.0.0:8112",
                                FileUtil.readString(getCertsPath() + "relayer.crt", Charset.defaultCharset()),
                                FileUtil.readString(getCertsPath() + "private_key.pem", Charset.defaultCharset()),
                                SIG_ALGO,
                                FileUtil.readString(getCertsPath() + "private_key.pem", Charset.defaultCharset()),
                                FileUtil.readString(getCertsPath() + "public_key.pem", Charset.defaultCharset()),
                                SIG_ALGO
                        ),
                        // {"encAddress":"did:bid:efNc8GGh3Yh2GkNJwBwHmKsqWPEayBtM","encPublicKey":"b065666cee70556107436be380dca88a5a5e403fd54a14d59f4eac322d598136264333","encPrivateKey":"priSPKq5XtWDeVp4nCiQZP39xkK9dUaT4mkRxBxTvVjKE7Fib4","rawPrivateKey":"reD3FF6Fr9ADZDAtMz/rYYSeDnW8PqmrD6eHLILsWjs=","rawPublicKey":"bO5wVWEHQ2vjgNyoilpeQD/VShTVn06sMi1ZgTYmQzM="}
                        new BifChainConfig(
                                "http://test.bifcore.bitfactory.cn",
                                null,
                                "priSPKq5XtWDeVp4nCiQZP39xkK9dUaT4mkRxBxTvVjKE7Fib4",
                                "did:bid:efNc8GGh3Yh2GkNJwBwHmKsqWPEayBtM",
                                "null",
                                "did:bid:efVhZTnRoebM1wBeoFYEcGi2MJBKi39E",
                                "did:bid:efkbC2VASBcwTPyhqaEY64Zs1j7RCQ3",
                                "null",
                                "did:bid:efukSNjb93FhMCHMnHyth3DUKdoCmZ7e",
                                "did:bid:efg633YNeBte5hD6VW5RfeqAk22g6BXf"
                        )
                ), SerializerFeature.PrettyFormat
        );
        System.out.println(rawBifConfig);

        bifBCDNSClient = BlockChainDomainNameServiceFactory.create(BCDNSTypeEnum.BIF, rawBifConfig.getBytes());

        ANTCHAIN_CSR = CrossChainCertificateFactory.createDomainNameCertificateSigningRequest(
                DomainNameCredentialSubject.CURRENT_VERSION,
                new CrossChainDomain(CrossChainDomain.ROOT_DOMAIN_SPACE),
                new CrossChainDomain(ANTCHAIN_DOMAIN),
                ((BifBCDNSClient) bifBCDNSClient).getCertificationServiceClient().getClientCredential().getClientCert()
                        .getCredentialSubjectInstance().getApplicant(),
                ((BifBCDNSClient) bifBCDNSClient).getCertificationServiceClient().getClientCredential().getClientCert()
                        .getCredentialSubjectInstance().getSubject()
        );
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (ObjectUtil.isNotNull(mockStaticObj)) {
            mockStaticObj.close();
        }
    }

    private static String getCertsPath() {
        return StrUtil.format(
                "cccerts/{}/{}/",
                OID_TYPE == ObjectIdentityType.X509_PUBLIC_KEY_INFO ? "x509" : OID_TYPE.name(),
                KEY_TYPE
        ).toLowerCase();
    }

    @Test
    public void test0ApplyDomainCert() {
        ApplyDomainNameCertificateResponse response = bifBCDNSClient.applyDomainNameCertificate(ANTCHAIN_CSR);
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getApplyReceipt());
        Assert.assertFalse(response.getApplyReceipt().isEmpty());

        receiptId = response.getApplyReceipt();
    }

    @Test
    @SneakyThrows
    public void test1QueryStatus() {
        Assert.assertFalse(StrUtil.isEmpty(receiptId));
        ApplicationResult result = null;
        while (ObjectUtil.isNull(result) || !result.isFinalResult()) {
            result = bifBCDNSClient.queryDomainNameCertificateApplicationResult(receiptId);
            Assert.assertNotNull(result);
            Thread.sleep(3_000);
        }

        Assert.assertNotNull(result.getCertificate());
        Assert.assertEquals(
                CrossChainCertificateTypeEnum.DOMAIN_NAME_CERTIFICATE,
                result.getCertificate().getType()
        );
        Assert.assertNotNull(result.getCertificate().getIssuer());
        Assert.assertNotNull(result.getCertificate().getProof());
        // TODO: maybe validate the sig by the public key
    }

    @Test
    public void test2QueryTheBCNDSRoot() {
        QueryBCDNSTrustRootCertificateResponse response = bifBCDNSClient.queryBCDNSTrustRootCertificate();
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getBcdnsTrustRootCertificate());
        Assert.assertEquals(
                CrossChainCertificateTypeEnum.BCDNS_TRUST_ROOT_CERTIFICATE,
                response.getBcdnsTrustRootCertificate().getType()
        );
        BIDDocumentOperation bidDocumentOperation = JsonUtils.toJavaObject(
                new String(
                        response.getBcdnsTrustRootCertificate().getCredentialSubjectInstance().getSubject()
                ),
                BIDDocumentOperation.class
        );

        Assert.assertNotNull(bidDocumentOperation);
        Assert.assertNotNull(bidDocumentOperation.getPublicKey());
        Assert.assertEquals(1, bidDocumentOperation.getPublicKey().length);
    }

    // CrossChainCertificateUtil.formatCrossChainCertificateToPem(clientCredential.clientCert)
    private static final String RELAYER_CERT = "-----BEGIN RELAYER CERTIFICATE-----\n" +
            "AAD7AQAAAAABAAAAMQEAEAAAAGFudGNoYWluLXJlbGF5ZXICAAEAAAADAwA7AAAA\n" +
            "AAA1AAAAAAABAAAAAQEAKAAAAGRpZDpiaWQ6ZWZiVGh5NXNiRzdQM21GVXAyRVdO\n" +
            "NW9RR1g2TFVHd2cEAAgAAACiN2RlAAAAAAUACAAAACJrRWcAAAAABgDmAAAAAADg\n" +
            "AAAAAAADAAAAMS4wAQAQAAAAYW50Y2hhaW4tcmVsYXllcgMAOwAAAAAANQAAAAAA\n" +
            "AQAAAAEBACgAAABkaWQ6YmlkOmVmYlRoeTVzYkc3UDNtRlVwMkVXTjVvUUdYNkxV\n" +
            "R3dnBAB6AAAAeyJwdWJsaWNLZXkiOlt7InR5cGUiOiJFRDI1NTE5IiwicHVibGlj\n" +
            "S2V5SGV4IjoiYjA2NTY2YWY2NjVlZTU1MDYzNWU5ODM3NWM3YjM4ODc2YTJjYzMx\n" +
            "ZTNhOWQ4MTg5NTdlNDZhMjRhMGYyNWE3NGE0NGNjZSJ9XX0HAIgAAAAAAIIAAAAA\n" +
            "AAMAAABTTTMBACAAAAAtlIZVqagiDAs3w16jV3kN+8iL46l8oFnFSG24BH6dJgIA\n" +
            "BwAAAEVkMjU1MTkDAEAAAACTFPfALDJx8NQ3zRV55J+t7umT3qPhq805zDOMP7UH\n" +
            "HvCKAeiXthTuajOvODZvRUdN0gVQBCMnWlKq2dOx0ocO\n" +
            "-----END RELAYER CERTIFICATE-----\n";

    // Base64.encode(clientCredential.clientCert.encode())
    private static final String RELAYER_CERT_ENCODED = "AAD7AQAAAAABAAAAMQEAEAAAAGFudGNoYWluLXJlbGF5ZXICAAEAAAADAwA7AAAAAAA1AAAAAAABAAAAAQEAKAAAAGRpZDpiaWQ6ZWZiVGh5NXNiRzdQM21GVXAyRVdONW9RR1g2TFVHd2cEAAgAAACiN2RlAAAAAAUACAAAACJrRWcAAAAABgDmAAAAAADgAAAAAAADAAAAMS4wAQAQAAAAYW50Y2hhaW4tcmVsYXllcgMAOwAAAAAANQAAAAAAAQAAAAEBACgAAABkaWQ6YmlkOmVmYlRoeTVzYkc3UDNtRlVwMkVXTjVvUUdYNkxVR3dnBAB6AAAAeyJwdWJsaWNLZXkiOlt7InR5cGUiOiJFRDI1NTE5IiwicHVibGljS2V5SGV4IjoiYjA2NTY2YWY2NjVlZTU1MDYzNWU5ODM3NWM3YjM4ODc2YTJjYzMxZTNhOWQ4MTg5NTdlNDZhMjRhMGYyNWE3NGE0NGNjZSJ9XX0HAIgAAAAAAIIAAAAAAAMAAABTTTMBACAAAAAtlIZVqagiDAs3w16jV3kN+8iL46l8oFnFSG24BH6dJgIABwAAAEVkMjU1MTkDAEAAAACTFPfALDJx8NQ3zRV55J+t7umT3qPhq805zDOMP7UHHvCKAeiXthTuajOvODZvRUdN0gVQBCMnWlKq2dOx0ocO";

    /*
    public static final String PTC_CERT = "-----BEGIN PROOF TRANSFORMATION COMPONENT CERTIFICATE-----\n" +
            "AAD4AQAAAAABAAAAMQEADAAAAGFudGNoYWluLXB0YwIAAQAAAAIDAGsAAAAAAGUA\n" +
            "AAAAAAEAAAAAAQBYAAAAMFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEULzSDPM4mhRk\n" +
            "GpCiVirHZTWfzuZ/PfO6+/RfVgGpZxs4EUQZhw5SnYWIwK99KxDPNxtYG7OEiWDA\n" +
            "7+EKZUWBIwQACAAAAJTZ/2YAAAAABQAIAAAAFA3haAAAAAAGAKAAAAAAAJoAAAAA\n" +
            "AAMAAAAxLjABAA0AAABjb21taXR0ZWUtcHRjAgABAAAAAQMAawAAAAAAZQAAAAAA\n" +
            "AQAAAAABAFgAAAAwVjAQBgcqhkjOPQIBBgUrgQQACgNCAARQvNIM8ziaFGQakKJW\n" +
            "KsdlNZ/O5n8987r79F9WAalnGzgRRBmHDlKdhYjAr30rEM83G1gbs4SJYMDv4Qpl\n" +
            "RYEjBAAAAAAABwCfAAAAAACZAAAAAAAKAAAAS0VDQ0FLLTI1NgEAIAAAAM87/iLc\n" +
            "e6uD6qD6prxj4z75IoGzydOhd68+3Y8dODHxAgAWAAAAS2VjY2FrMjU2V2l0aFNl\n" +
            "Y3AyNTZrMQMAQQAAAMK+DN7gXmDRv8nfXwWZe3XCZQQu5mO86LNZxXcp7BgMPfJj\n" +
            "y1wKW5yD51nhMEW2K1AfwEG6n8RWk5Z2jFDE8GMA\n" +
            "-----END PROOF TRANSFORMATION COMPONENT CERTIFICATE-----\n";
    */

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

    public static final AbstractCrossChainCertificate NODE_PTC_CERT = CrossChainCertificateUtil.readCrossChainCertificateFromPem(PTC_CERT.getBytes());

    // HexUtil.encodeHexStr(vcTpBtaReqDto.getTpbta())
    private static final String TPBTA = "0000ca0200000000040000000100000001000400000001000000020001000000010300a000000000009a000000000003000000312e3001000d000000636f6d6d69747465652d7074630200010000000103006b000000000065000000000001000000000100580000003056301006072a8648ce3d020106052b8104000a0342000450bcd20cf3389a14641a90a2562ac765359fcee67f3df3bafbf45f5601a9671b38114419870e529d8588c0af7d2b10cf371b581bb3848960c0efe10a6545812304000000000004007200000000006c00000000001a0000000000140000000000040000007465737401000400000074657374010020000000000000000000000000000000000000000000000000000000000000000000000102002000000000000000000000000000000000000000000000000000000000000000000000010500040000000100000006000a0000004b454343414b2d3235360700d80000000000d2000000000009000000636f6d6d697474656501002400000000001e0000000000180000000000120000000000020000003e3d010004000000000000000200930000008f0000000000890000000000050000006e6f6465310100010000000102007100000000006b00000000000700000064656661756c740100580000003056301006072a8648ce3d020106052b8104000a0342000450bcd20cf3389a14641a90a2562ac765359fcee67f3df3bafbf45f5601a9671b38114419870e529d8588c0af7d2b10cf371b581bb3848960c0efe10a65458123ff009300000000008d000000000009000000636f6d6d69747465650100780000007400000000006e0000000000050000006e6f6465310100160000004b656363616b32353657697468536563703235366b3102004100000089127c1aeec72fceb1c0954646d597c0ec563e3e6dca8705922cba2007220c30631f430e1a8fb097ec5aebb7da7506eca4f3ae0dfd5701d3dda39cf4737c165d00";

    @Test
    public void testQueryRelayerCert() {
        AbstractCrossChainCertificate relayerCert = CrossChainCertificateUtil.readCrossChainCertificateFromPem(RELAYER_CERT.getBytes());
        QueryRelayerCertificateRequest request = new QueryRelayerCertificateRequest();
        request.setRelayerCertId("antchain-relayer");
        request.setName("");
        request.setApplicant(relayerCert.getCredentialSubjectInstance().getApplicant());
        QueryRelayerCertificateResponse callResp = bifBCDNSClient.queryRelayerCertificate(request);
        Assert.assertNotNull(callResp.getCertificate());
    }

    @Test
    public void testVcApply() {
        AbstractCrossChainCertificate relayerCert = CrossChainCertificateUtil.readCrossChainCertificateFromPem(RELAYER_CERT.getBytes());
        // CrossChainCertificateUtil.readCrossChainCertificateFromPem(RELAYER_CERT);
        // mock contract operation
        /*OkHttpClient httpClient = mock(OkHttpClient.class);
        try {
            mockStaticObj = mockStatic(OkHttpClient.class);
            when(new OkHttpClient.Builder(any()).build()).thenReturn(httpClient);
        } catch(Exception e) {
            if (ObjectUtil.isNotNull(mockStaticObj)) {
                mockStaticObj.close();
            }
        }*/

        ApplyRelayerCertificateResponse response = bifBCDNSClient.applyRelayerCertificate(relayerCert);
        // TODO: 构造bcdns-server的不同返回
        /*Response httpRequestResponse = new Response()
        when(httpClient.newCall(any())).thenReturn()*/
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getApplyReceipt());
        Assert.assertFalse(response.getApplyReceipt().isEmpty());
        receiptId = response.getApplyReceipt();
    }

    @Test
    public void testAddPTCTrustRoot() {
        AbstractCrossChainCertificate ptcCertObj = CrossChainCertificateUtil.readCrossChainCertificateFromPem(PTC_CERT.getBytes());
        PrivateKey ptcPrivateKeyObj = SignAlgoEnum.KECCAK256_WITH_SECP256K1.getSigner().readPemPrivateKey(ptcPrivateKey.getBytes());
        String bcdnsDomainSpace = ".com";

        // build it first
        ptcTrustRoot = PTCTrustRoot.builder()
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
        bifBCDNSClient.addPTCTrustRoot(ptcTrustRoot);
    }

    @Test
    public void testAddThirdPartyBlockchainTrustAnchor() {
        CrossChainDomain domain = new CrossChainDomain();
        domain.setDomain("bif-test");
        RegisterThirdPartyBlockchainTrustAnchorRequest request = new RegisterThirdPartyBlockchainTrustAnchorRequest();
        request.setPtcId(((PTCCredentialSubject) NODE_PTC_CERT.getCredentialSubjectInstance()).getApplicant());
        request.setDomain(domain);
        request.setTpbta(ThirdPartyBlockchainTrustAnchor.decode(HexUtil.decodeHex(TPBTA)));
        bifBCDNSClient.registerThirdPartyBlockchainTrustAnchor(request);
    }

    @Test
    public void testQueryThirdPartyBlockchainTrustAnchor() {
        QueryThirdPartyBlockchainTrustAnchorRequest request = new QueryThirdPartyBlockchainTrustAnchorRequest();
        CrossChainLane crossChainLane = ThirdPartyBlockchainTrustAnchor.decode(HexUtil.decodeHex(TPBTA)).getCrossChainLane();
        request.setTpBtaCrossChainLane(crossChainLane);
        request.setTpbtaVersion(ThirdPartyBlockchainTrustAnchor.decode(HexUtil.decodeHex(TPBTA)).getTpbtaVersion());
        bifBCDNSClient.queryThirdPartyBlockchainTrustAnchor(request);
    }
}
