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

import java.util.Collections;
import java.util.List;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.HexUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.bta.AbstractBlockchainTrustAnchor;
import com.alipay.antchain.bridge.commons.core.bta.BlockchainTrustAnchorV0;
import com.alipay.antchain.bridge.commons.exception.base.AntChainBridgeBaseException;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TLVUtilsTest {

    private static BlockchainTrustAnchorV0 bta;

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

    @BeforeClass
    public static void setup() {
        bytes = HexUtil.decodeHex("0102");

        bta = new BlockchainTrustAnchorV0();

        bta.setBcOwnerSig(bytes);
        bta.setDomain(new CrossChainDomain("test"));
        bta.setExtension(bytes);
        bta.setAuthMessageID(bytes);
        bta.setSubjectIdentity(bytes);
        bta.setSubjectProductSVN(10);
        bta.setBcOwnerSigAlgo(AbstractBlockchainTrustAnchor.SignType.SIGN_ALGO_SHA256_WITH_ECC);
        bta.setBcOwnerPublicKey(bytes);
        bta.setSubjectProductID("test");
        bta.setHashToSign(bytes);

        upData = "00005f0000000000040000000000000001000400000074657374020004000000746573740300040000000a0000000a000200000001020400020000000102050002000000010206000200000001020700020000000102080001000000010900020000000102";

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
    }

    @Test
    public void testDecode() {
        AbstractBlockchainTrustAnchor bta = TLVUtils.decode(HexUtil.decodeHex(upData), BlockchainTrustAnchorV0.class);

        Assert.assertEquals("test", bta.getDomain().getDomain());
        Assert.assertArrayEquals(bytes, bta.getExtension());
        Assert.assertEquals(AbstractBlockchainTrustAnchor.SignType.SIGN_ALGO_SHA256_WITH_ECC, bta.getBcOwnerSigAlgo());
        Assert.assertEquals(10, bta.getSubjectProductSVN());
    }

    @Test
    public void testEncode() {
        System.out.println(HexUtil.encodeHexStr(TLVUtils.encode(bta)));
        Assert.assertArrayEquals(HexUtil.decodeHex(upData), TLVUtils.encode(bta));
    }

    @Test
    public void testDecodeAndEncode() {
        AbstractBlockchainTrustAnchor bta = TLVUtils.decode(HexUtil.decodeHex(upData), BlockchainTrustAnchorV0.class);

        Assert.assertEquals("test", bta.getDomain().getDomain());
        Assert.assertArrayEquals(bytes, bta.getExtension());
        Assert.assertEquals(AbstractBlockchainTrustAnchor.SignType.SIGN_ALGO_SHA256_WITH_ECC, bta.getBcOwnerSigAlgo());
        Assert.assertEquals(10, bta.getSubjectProductSVN());

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
}
