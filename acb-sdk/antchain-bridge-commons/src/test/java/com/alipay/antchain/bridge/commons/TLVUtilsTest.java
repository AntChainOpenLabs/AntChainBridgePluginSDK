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

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alipay.antchain.bridge.commons.bcdns.PTCCredentialSubject;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.X509PubkeyInfoObjectIdentity;
import com.alipay.antchain.bridge.commons.core.bta.AbstractBlockchainTrustAnchor;
import com.alipay.antchain.bridge.commons.core.bta.BlockchainTrustAnchorV1;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTypeEnum;
import com.alipay.antchain.bridge.commons.core.ptc.PTCVerifyAnchor;
import com.alipay.antchain.bridge.commons.exception.base.AntChainBridgeBaseException;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVCreator;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVMapping;
import com.alipay.antchain.bridge.commons.utils.crypto.HashAlgoEnum;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import lombok.*;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TLVUtilsTest {

    private static BlockchainTrustAnchorV1 bta;

    private static PTCTrustRoot trustRoot;

    private static String rawTrustRoot;

    private static byte[] bytes;

    private static String upData;

    private static TestBytesArray testBytesArray;

    private static String rawTestBytesArray;

    private static TestStringArray testStringArray;

    private static String rawTestStringArray;

    private static TestRecursiveOuter recursiveOuter;

    private static String rawRecursiveOuter;

    private static String rawRecursiveOuterWithOrderList;

    private static TestListOfReferenceClazz listOfReferenceClazz;

    private static String rawListOfReferenceClazz;

    private static TestMap testMap;

    private static String rawTestMap;

    private final static String rawEnumSerializedUsingTLVMapping = "00001700000001000a0000004b454343414b2d32353602000100000000";

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

    @BeforeClass
    @SneakyThrows
    public static void setup() {
        bytes = HexUtil.decodeHex("0102");

        PTCCredentialSubject ptcCredentialSubject = new PTCCredentialSubject();

        ptcCredentialSubject.setName("test");
        ptcCredentialSubject.setType(PTCTypeEnum.COMMITTEE);
        ptcCredentialSubject.setApplicant(new X509PubkeyInfoObjectIdentity(bytes));
        ptcCredentialSubject.setVersion("test");
        ptcCredentialSubject.setSubjectInfo(bytes);

        trustRoot = new PTCTrustRoot();
        trustRoot.setVerifyAnchorMap(
                MapUtil.builder(BigInteger.ONE, new PTCVerifyAnchor(BigInteger.ONE, bytes))
                        .build()
        );
        trustRoot.setPtcCrossChainCert(CrossChainCertificateUtil.readCrossChainCertificateFromPem(PTC_CERT.getBytes()));
        trustRoot.setNetworkInfo("test".getBytes());
        trustRoot.setIssuerBcdnsDomainSpace(new CrossChainDomain("test"));
        trustRoot.setSig(bytes);
        trustRoot.setSigAlgo(SignAlgoEnum.ED25519);

        rawTrustRoot = "000051020000000004000000746573740100fe0100000000f80100000000010000003101000c000000616e74636861696e2d7074630200010000000203006b000000000065000000000001000000000100580000003056301006072a8648ce3d020106052b8104000a034200040b85aebe1afb1451c9e05a9adc7a317ae3f4af3309af73c1148fe78398315b185b25c539af07dd83899cb89ce5a635987ba3a2e2889f3a96fbf2eb1429093fc3040008000000bdf7c266000000000500080000003d2ba468000000000600a000000000009a000000000003000000312e3001000d000000636f6d6d69747465652d7074630200010000000103006b000000000065000000000001000000000100580000003056301006072a8648ce3d020106052b8104000a034200040b85aebe1afb1451c9e05a9adc7a317ae3f4af3309af73c1148fe78398315b185b25c539af07dd83899cb89ce5a635987ba3a2e2889f3a96fbf2eb1429093fc304000000000007009f00000000009900000000000a0000004b454343414b2d3235360100200000005b1ddc375219050708a69fc1b0f9849ef463b21d36dae39c7d7439566842fc730200160000004b656363616b32353657697468536563703235366b31030041000000f8bb8def1d58fc8502d35dbb3048c2360a1e9b29b31cd921cbe30303e78ab1d056c76e5f61f59f189367b2d4e6ed1650066c929777b434284f32d40baa25e3b5010200040000007465737403001e00000001000000011500000000000f000000000001000000010100020000000102040007000000456432353531390500020000000102";

        bta = new BlockchainTrustAnchorV1();

        bta.setBcOwnerSig(bytes);
        bta.setDomain(new CrossChainDomain("test"));
        bta.setExtension(bytes);
        bta.setSubjectIdentity(bytes);
        bta.setInitBlockHash(bytes);
        bta.setInitHeight(BigInteger.ONE);
        bta.setSubjectVersion(10);
        bta.setBcOwnerSigAlgo(SignAlgoEnum.SHA256_WITH_ECDSA);
        bta.setBcOwnerPublicKey(bytes);
        bta.setSubjectProduct("test");
        bta.setAmId(DigestUtil.sha256("am".getBytes()));

        upData = "0000840000000000040000000100000001000400000074657374020004000000746573740300040000000a00000004000200000001020b0001000000010c000200000001020a0020000000ab6db599234d2636659cba1aa191bd014c3867d5cfade98ff694785c20c28fc605000200000001020600020000000102070001000000010800020000000102";

        testBytesArray = new TestBytesArray();
        testBytesArray.setValue(
                ListUtil.toList(HexUtil.decodeHex("010101"), HexUtil.decodeHex("020202"))
        );

        rawTestBytesArray = "00001400000001000e0000000300000001010103000000020202";

        testStringArray = new TestStringArray();
        testStringArray.setValue(
                ListUtil.toList("test1", "test2")
        );

        rawTestStringArray = "000018000000010012000000050000007465737431050000007465737432";

        TestRecursiveInner recursiveInner = new TestRecursiveInner();
        recursiveInner.setBytes(HexUtil.decodeHex("010101"));
        recursiveInner.setString("test");
        recursiveOuter = new TestRecursiveOuter();
        recursiveOuter.setDomain(new CrossChainDomain("mydomain"));
        recursiveOuter.setInner(recursiveInner);

        rawRecursiveOuter = "00002d0000000100080000006d79646f6d61696e02001900000000001300000001000300000001010102000400000074657374";

        rawRecursiveOuterWithOrderList = "00002d000000020019000000000013000000010003000000010101020004000000746573740100080000006d79646f6d61696e";

        listOfReferenceClazz = new TestListOfReferenceClazz();
        listOfReferenceClazz.setListOfReferences(ListUtil.toList(recursiveOuter, recursiveOuter));

        rawListOfReferenceClazz = "00007400000001006e0000003300000000002d0000000100080000006d79646f6d61696e020019000000000013000000010003000000010101020004000000746573743300000000002d0000000100080000006d79646f6d61696e02001900000000001300000001000300000001010102000400000074657374";

        testMap = new TestMap(MapUtil.builder(new CrossChainDomain("test"), listOfReferenceClazz).build());

        rawTestMap = "00008c00000001008600000004000000746573747a00000000007400000001006e0000003300000000002d0000000100080000006d79646f6d61696e020019000000000013000000010003000000010101020004000000746573743300000000002d0000000100080000006d79646f6d61696e02001900000000001300000001000300000001010102000400000074657374";
    }

    @Test
    public void testDecode() {
        AbstractBlockchainTrustAnchor bta = TLVUtils.decode(HexUtil.decodeHex(upData), BlockchainTrustAnchorV1.class);

        Assert.assertEquals("test", bta.getDomain().getDomain());
        Assert.assertArrayEquals(bytes, bta.getExtension());
        Assert.assertEquals(SignAlgoEnum.SHA256_WITH_ECDSA, bta.getBcOwnerSigAlgo());
        Assert.assertEquals(10, bta.getSubjectVersion());
        Assert.assertArrayEquals(DigestUtil.sha256("am"), bta.getAmId());
    }

    @Test
    public void testEncode() {
        System.out.println(HexUtil.encodeHexStr(TLVUtils.encode(bta)));
        Assert.assertArrayEquals(HexUtil.decodeHex(upData), TLVUtils.encode(bta));
    }

    @Test
    public void testDecodeAndEncode() {
        AbstractBlockchainTrustAnchor bta = TLVUtils.decode(HexUtil.decodeHex(upData), BlockchainTrustAnchorV1.class);

        Assert.assertEquals("test", bta.getDomain().getDomain());
        Assert.assertArrayEquals(bytes, bta.getExtension());
        Assert.assertEquals(SignAlgoEnum.SHA256_WITH_ECDSA, bta.getBcOwnerSigAlgo());
        Assert.assertEquals(10, bta.getSubjectVersion());

        Assert.assertArrayEquals(HexUtil.decodeHex(upData), TLVUtils.encode(bta));
    }

    @Test
    public void testBytesArray() {
        byte[] rawBA = TLVUtils.encode(testBytesArray);
        Assert.assertEquals(rawTestBytesArray, HexUtil.encodeHexStr(rawBA));

        TestBytesArray testBytesArrayDecoded = TLVUtils.decode(HexUtil.decodeHex(rawTestBytesArray), TestBytesArray.class);
        Assert.assertArrayEquals(testBytesArray.getValue().toArray(), testBytesArrayDecoded.getValue().toArray());
    }

    @Test
    public void testStringArray() {
        byte[] rawBA = TLVUtils.encode(testStringArray);
        Assert.assertEquals(rawTestStringArray, HexUtil.encodeHexStr(rawBA));

        TestStringArray testStringArrayDecoded = TLVUtils.decode(HexUtil.decodeHex(rawTestStringArray), TestStringArray.class);
        Assert.assertArrayEquals(testStringArray.getValue().toArray(), testStringArrayDecoded.getValue().toArray());
    }

    @Test
    public void testRecursive() {
        Assert.assertEquals(rawRecursiveOuter, HexUtil.encodeHexStr(TLVUtils.encode(recursiveOuter)));

        TestRecursiveOuter outer = TLVUtils.decode(HexUtil.decodeHex(rawRecursiveOuter), TestRecursiveOuter.class);
        Assert.assertEquals(recursiveOuter.domain.getDomain(), outer.getDomain().getDomain());
        Assert.assertEquals(recursiveOuter.inner.string, outer.getInner().getString());
        Assert.assertArrayEquals(recursiveOuter.inner.bytes, outer.getInner().getBytes());
    }

    @Test
    public void testListOfReferences() {
        String hexRes = HexUtil.encodeHexStr(TLVUtils.encode(listOfReferenceClazz));
        Assert.assertEquals(rawListOfReferenceClazz, hexRes);

        TestListOfReferenceClazz testListOfReferenceClazz = TLVUtils.decode(HexUtil.decodeHex(rawListOfReferenceClazz), TestListOfReferenceClazz.class);
        Assert.assertEquals(
                listOfReferenceClazz.getListOfReferences().size(),
                testListOfReferenceClazz.getListOfReferences().size()
        );
        Assert.assertEquals(
                listOfReferenceClazz.getListOfReferences().get(0).getDomain().getDomain(),
                testListOfReferenceClazz.getListOfReferences().get(0).getDomain().getDomain()
        );
        Assert.assertEquals(
                listOfReferenceClazz.getListOfReferences().get(0).getInner().getString(),
                testListOfReferenceClazz.getListOfReferences().get(0).getInner().getString()
        );
        Assert.assertArrayEquals(
                listOfReferenceClazz.getListOfReferences().get(0).getInner().getBytes(),
                testListOfReferenceClazz.getListOfReferences().get(0).getInner().getBytes()
        );
    }

    @Test
    public void testEncodeWithRequiredOrderList() {
        Assert.assertEquals(
                rawRecursiveOuterWithOrderList,
                HexUtil.encodeHexStr(TLVUtils.encode(recursiveOuter, ListUtil.toList(1, 0)))
        );

        Assert.assertThrows(
                AntChainBridgeBaseException.class,
                () -> TLVUtils.encode(recursiveOuter, ListUtil.toList(1, 1))
        );

        Assert.assertThrows(
                AntChainBridgeBaseException.class,
                () -> TLVUtils.encode(recursiveOuter, Collections.EMPTY_LIST)
        );
    }

    @Test
    public void testMapFieldEncodeAndDecode() {
        Assert.assertEquals(
                rawTrustRoot,
                HexUtil.encodeHexStr(trustRoot.encode())
        );

        PTCTrustRoot ptcTrustRoot = PTCTrustRoot.decode(HexUtil.decodeHex(rawTrustRoot));
        Assert.assertEquals(
                trustRoot.getVerifyAnchorMap().size(),
                ptcTrustRoot.getVerifyAnchorMap().size()
        );
        Assert.assertEquals(
                HexUtil.encodeHexStr(trustRoot.getVerifyAnchorMap().get(BigInteger.valueOf(1L)).getAnchor()),
                HexUtil.encodeHexStr(ptcTrustRoot.getVerifyAnchorMap().get(BigInteger.valueOf(1L)).getAnchor())
        );
    }

    @Test
    public void testMapAnnotatedWithTLVMapping() {
        Assert.assertEquals(
                rawTestMap,
                HexUtil.encodeHexStr(TLVUtils.encode(testMap))
        );
        TestMap temp = TLVUtils.decode(HexUtil.decodeHex(rawTestMap), TestMap.class);
        Assert.assertEquals(
                1,
                temp.map.size()
        );
        Assert.assertEquals(
                new CrossChainDomain("test"),
                temp.map.keySet().toArray()[0]
        );
        Assert.assertEquals(
                listOfReferenceClazz.listOfReferences.get(0).getDomain(),
                temp.map.get(new CrossChainDomain("test")).listOfReferences.get(0).getDomain()
        );
        Assert.assertEquals(
                listOfReferenceClazz.listOfReferences.get(0).getInner().getString(),
                temp.map.get(new CrossChainDomain("test")).listOfReferences.get(0).getInner().getString()
        );
    }

    @Test
    public void testEnumSerializedUsingTLVMapping() {
        TestEnumNotSerializedAsUint8 obj = new TestEnumNotSerializedAsUint8();
        obj.hashAlgo = HashAlgoEnum.KECCAK_256;
        obj.testEnumWithoutTLVCreator = TestEnumWithoutTLVCreator.TEST_ENUM;

        Assert.assertEquals(
                rawEnumSerializedUsingTLVMapping,
                TLVUtils.encodeToHex(obj)
        );

        TestEnumNotSerializedAsUint8 temp = TLVUtils.decodeFromHex(rawEnumSerializedUsingTLVMapping, TestEnumNotSerializedAsUint8.class);
        Assert.assertEquals(
                HashAlgoEnum.KECCAK_256,
                temp.hashAlgo
        );
        Assert.assertEquals(
                TestEnumWithoutTLVCreator.TEST_ENUM,
                temp.testEnumWithoutTLVCreator
        );
    }

    @Test
    public void testVarInt() {
        TestVarInt varInt = new TestVarInt();
        System.out.println(TLVUtils.encodeToHex(varInt));
        Assert.assertEquals(
                "0000230000000000010000006401000100000064020001000000640300010000006404000100000064",
                TLVUtils.encodeToHex(varInt)
        );

        TestVarInt varInt1 = TLVUtils.decode(HexUtil.decodeHex("0000230000000000010000006401000100000064020001000000640300010000006404000100000064"), TestVarInt.class);
        Assert.assertEquals(
                varInt.var1,
                varInt1.var1
        );
        Assert.assertEquals(
                varInt.var2,
                varInt1.var2
        );
        Assert.assertEquals(
                varInt.var3,
                varInt1.var3
        );
        Assert.assertEquals(
                varInt.var4,
                varInt1.var4
        );
        Assert.assertEquals(
                varInt.var5,
                varInt1.var5
        );
    }

    public static class TestBytesArray {

        @TLVField(tag = 1, type = TLVTypeEnum.BYTES_ARRAY, order = 0)
        List<byte[]> value;

        public List<byte[]> getValue() {
            return value;
        }

        public void setValue(List<byte[]> value) {
            this.value = value;
        }
    }

    public static class TestStringArray {

        @TLVField(tag = 1, type = TLVTypeEnum.STRING_ARRAY, order = 0)
        List<String> value;

        public List<String> getValue() {
            return value;
        }

        public void setValue(List<String> value) {
            this.value = value;
        }
    }

    public static class TestRecursiveOuter {
        @TLVField(tag = 1, type = TLVTypeEnum.STRING, order = 0)
        private CrossChainDomain domain;

        @TLVField(tag = 2, type = TLVTypeEnum.BYTES, order = 1)
        private TestRecursiveInner inner;

        public CrossChainDomain getDomain() {
            return domain;
        }

        public void setDomain(CrossChainDomain domain) {
            this.domain = domain;
        }

        public TestRecursiveInner getInner() {
            return inner;
        }

        public void setInner(TestRecursiveInner inner) {
            this.inner = inner;
        }
    }

    public static class TestRecursiveInner {
        @TLVField(tag = 1, type = TLVTypeEnum.BYTES, order = 0)
        private byte[] bytes;

        @TLVField(tag = 2, type = TLVTypeEnum.STRING, order = 1)
        private String string;

        public byte[] getBytes() {
            return bytes;
        }

        public void setBytes(byte[] bytes) {
            this.bytes = bytes;
        }

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }
    }

    public static class TestListOfReferenceClazz {

        @TLVField(tag = 1, type = TLVTypeEnum.BYTES_ARRAY, order = 0)
        private List<TestRecursiveOuter> listOfReferences;

        public List<TestRecursiveOuter> getListOfReferences() {
            return listOfReferences;
        }

        public void setListOfReferences(List<TestRecursiveOuter> listOfReferences) {
            this.listOfReferences = listOfReferences;
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    public static class TestMap {
        @TLVField(tag = 1, type = TLVTypeEnum.MAP)
        private Map<CrossChainDomain, TestListOfReferenceClazz> map;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class TestEnumNotSerializedAsUint8 {
        @TLVField(tag = 1, type = TLVTypeEnum.STRING)
        private HashAlgoEnum hashAlgo;

        @TLVField(tag = 2, type = TLVTypeEnum.UINT8)
        private TestEnumWithoutTLVCreator testEnumWithoutTLVCreator;
    }

    @AllArgsConstructor
    @Getter
    @TLVMapping(fieldName = "name")
    public enum TestEnumWithoutTLVCreator {
        TEST_ENUM("test");

        private final String name;

        public static TestEnumWithoutTLVCreator valueOf(Byte value) {
            if (TEST_ENUM.ordinal() == value) {
                return TEST_ENUM;
            }
            throw new RuntimeException();
        }

        @TLVCreator
        public static TestEnumWithoutTLVCreator getByName(String name) {
            for (TestEnumWithoutTLVCreator enumWithoutTLVCreator : TestEnumWithoutTLVCreator.values()) {
                if (StrUtil.equalsIgnoreCase(enumWithoutTLVCreator.getName(), name)) {
                    return enumWithoutTLVCreator;
                }
            }
            return null;
        }
    }

    @Getter
    @Setter
    public static class TestVarInt {
        @TLVField(tag = 0, type = TLVTypeEnum.VAR_INT)
        private long var1 = 100;

        @TLVField(tag = 1, type = TLVTypeEnum.VAR_INT)
        private BigInteger var2 = BigInteger.valueOf(100);

        @TLVField(tag = 2, type = TLVTypeEnum.VAR_INT)
        private Long var3 = 100L;

        @TLVField(tag = 3, type = TLVTypeEnum.VAR_INT)
        private int var4 = 100;

        @TLVField(tag = 4, type = TLVTypeEnum.VAR_INT)
        private Integer var5 = 100;

    }
}
