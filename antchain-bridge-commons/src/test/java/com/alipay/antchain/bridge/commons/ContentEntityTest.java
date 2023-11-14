package com.alipay.antchain.bridge.commons;

import cn.bif.model.crypto.KeyPairEntity;
import cn.bif.utils.hex.HexFormat;
import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.PemUtil;
import cn.hutool.crypto.digest.SM3;
import com.alipay.antchain.bridge.commons.bcdns.*;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentityType;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTypeEnum;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.util.Date;

public class ContentEntityTest {
    private static KeyPairEntity keyPair;

    @BeforeClass
    public static void setUp() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        keyPair = KeyPairEntity.getBidAndKeyPair();
    }

    @Test
    public void testPTCContentEntity() throws Exception {

        PTCContentEntity ptcContentEntity = new PTCContentEntity(
                "test",
                PTCTypeEnum.BLOCKCHAIN,
                new ObjectIdentity(ObjectIdentityType.BID, keyPair.getEncAddress().getBytes()),
                keyPair.getEncPublicKey()
        );

        byte[] en = ptcContentEntity.encode();

        PTCContentEntity p = PTCContentEntity.decode(en);
        System.out.println(p.getName());
        System.out.println(p.getType());
        System.out.println(p.getApplicant());
        System.out.println(p.getPublicKey());

    }
}
