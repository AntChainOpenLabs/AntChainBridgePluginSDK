package com.alipay.antchain.bridge.commons;

import java.security.KeyPair;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.KeyUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import com.alipay.antchain.bridge.commons.core.bta.BlockchainTrustAnchorV1;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class BlockchainTrustAnchorV1Test {

    private static final KeyPair kPair = KeyUtil.generateKeyPair("EC");

    private static final BlockchainTrustAnchorV1 bta = new BlockchainTrustAnchorV1();

    @BeforeClass
    public static void setUp() throws Exception {
        bta.setSubjectProduct("mychain_0.10");
        bta.setDomain(new CrossChainDomain("test.org"));
        bta.setExtension(HexUtil.decodeHex("0001"));
        bta.setBcOwnerSigAlgo(SignAlgoEnum.SHA256_WITH_ECDSA);
        bta.setSubjectIdentity(HexUtil.decodeHex("0001"));
        bta.setSubjectVersion(1);
        bta.setBcOwnerPublicKey(kPair.getPublic().getEncoded());
    }

    @Test
    public void testSign() throws Exception {
        bta.sign(kPair.getPrivate());
        Assert.assertTrue(bta.validate());
    }
}
