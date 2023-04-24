package com.alipay.antchain.bridge.commons;

import java.security.KeyPair;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.KeyUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.bta.AbstractBlockchainTrustAnchor;
import com.alipay.antchain.bridge.commons.core.bta.BlockchainTrustAnchorV0;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class BlockchainTrustAnchorV0Test {

    private static final KeyPair kPair = KeyUtil.generateKeyPair("EC");

    private static final BlockchainTrustAnchorV0 bta = new BlockchainTrustAnchorV0();

    @BeforeClass
    public static void setUp() throws Exception {
        bta.setSubjectProductID("mychain_0.10");
        bta.setDomain(new CrossChainDomain("test.org"));
        bta.setExtension(HexUtil.decodeHex("0001"));
        bta.setBcOwnerSigAlgo(AbstractBlockchainTrustAnchor.SignType.SIGN_ALGO_SHA256_WITH_ECC);
        bta.setSubjectIdentity(HexUtil.decodeHex("0001"));
        bta.setSubjectProductSVN(1);
        bta.setBcOwnerPublicKey(kPair.getPublic().getEncoded());
        bta.setAuthMessageID(HexUtil.decodeHex("0001"));
    }

    @Test
    public void testSign() throws Exception {
        bta.sign(kPair.getPrivate());
        Assert.assertTrue(bta.validate());
    }
}
