package com.alipay.antchain.bridge.ptc.committee;

import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

import cn.hutool.core.util.ByteUtil;
import cn.hutool.core.util.HexUtil;
import com.alipay.antchain.bridge.commons.utils.crypto.HashAlgoEnum;
import com.alipay.antchain.bridge.commons.utils.crypto.secp256k1.ECDSASignature;
import com.alipay.antchain.bridge.commons.utils.crypto.secp256k1.ECKeyPair;
import com.alipay.antchain.bridge.commons.utils.crypto.secp256k1.Sign;
import lombok.SneakyThrows;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.junit.Assert;
import org.junit.Test;

public class TestSecp256k1Keys {

    @Test
    @SneakyThrows
    public void test() {
        Security.addProvider(new BouncyCastleProvider());

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
        ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("secp256k1");

        keyPairGenerator.initialize(ecGenParameterSpec);

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        StringWriter stringWriter = new StringWriter(256);
        JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
        jcaPEMWriter.writeObject(keyPair.getPublic());
        jcaPEMWriter.close();
        String pubkeyPem = stringWriter.toString();
        System.out.println(pubkeyPem);

        stringWriter = new StringWriter(256);
        jcaPEMWriter = new JcaPEMWriter(stringWriter);
        jcaPEMWriter.writeObject(keyPair.getPrivate());
        jcaPEMWriter.close();
        String privateKeyPem = stringWriter.toString();
        System.out.println(privateKeyPem);
//
//        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
//
//        subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(PemUtil.readPem(new ByteArrayInputStream(pubkeyPem.getBytes())));
//
//        KeyUtil.generatePublicKey(
//                AlgorithmId.get(subjectPublicKeyInfo.getAlgorithm().getAlgorithm().getId()).getName(),
//                subjectPublicKeyInfo.getEncoded()
//        );

        System.out.println(((BCECPrivateKey) keyPair.getPrivate()).getS().toString(16));

        byte[] raw = HashAlgoEnum.KECCAK_256.hash("test".getBytes());
        System.out.println(HexUtil.encodeHexStr(raw));
        // 30450220755fc1c7a4e41f9e732950f0c020ba4a314495eff79a45df275d999e4d6680a10221008478ddaaff54d11dcc3b13f08a24d4a50935dfa44ca3a3cb38041b9c4a59b572
        // 3045022100860f467508e2cd8a4cc5845eafd4156e2313863b99ee39fd9c48b3d0d42dba0e02201a6a357f4556fb0045bfbebdeaffc175605c28c77e8ce92e759856adc9b3ee5f
        // 304402200fc0e5aecf7149776dd65edbdd438d93422c8870d1dc57128adb1b940d691d41022071fa45e0956e56e202f46401497afed18415c63248bda4ecbc8b340b9b9ae93f

        ECKeyPair keyPair1 = ECKeyPair.create(((BCECPrivateKey) keyPair.getPrivate()).getS());
        Sign.SignatureData signatureData = Sign.signMessage(raw, keyPair1, false);

        byte[] rawSig = new byte[65];
        rawSig[64] = ByteUtil.intToByte(ByteUtil.byteToUnsignedInt(signatureData.getV()[0]) - 27);
        System.arraycopy(signatureData.getR(), 0, rawSig, 0, 32);
        System.arraycopy(signatureData.getS(), 0, rawSig, 32, 32);

        System.out.println("0x" + Sign.getAddress(keyPair1.getPublicKey()));
        System.out.println(HexUtil.encodeHexStr(rawSig));

//        Sign.SignatureData signatureData = Sign.signMessage("test".getBytes(), ECKeyPair.create(((BCECPrivateKey) keyPair.getPrivate()).getD()));
//        byte[] derFormat = CryptoUtils.toDerFormat(signature);
//        System.out.println(HexUtil.encodeHexStr(derFormat));
//        System.out.println(Base64.encode(derFormat));

//        Sign.SignatureData signatureData = Sign.createSignatureData(signature, keyPair1.getPublicKey(), Hash.sha3("test".getBytes()));

        byte[] signatureBytes = rawSig;
        byte v = signatureBytes[64];
        if (v < 27) {
            v += 27;
        }

        Sign.SignatureData sd =
                new Sign.SignatureData(
                        v,
                        (byte[]) Arrays.copyOfRange(signatureBytes, 0, 32),
                        (byte[]) Arrays.copyOfRange(signatureBytes, 32, 64));

        String addressRecovered = null;
        boolean match = false;

        // Iterate for each possible key to recover
        for (int i = 0; i < 4; i++) {
            BigInteger publicKey =
                    Sign.recoverFromSignature(
                            (byte) i,
                            new ECDSASignature(
                                    new BigInteger(1, sd.getR()), new BigInteger(1, sd.getS())),
                            raw);

            if (publicKey != null) {
                addressRecovered = "0x" + Sign.getAddress(publicKey);

                if (addressRecovered.equals("0x" + Sign.getAddress(keyPair1.getPublicKey()))) {
                    match = true;
                    break;
                }
            }
        }

        Assert.assertTrue(match);
        Assert.assertEquals(addressRecovered, ("0x" + Sign.getAddress(keyPair1.getPublicKey())));

//        String message = "v0G9u7huK4mJb2K1";
//
//        String prefix = "\u0019Ethereum Signed Message:\n" + message.length();
//        byte[] msgHash = Hash.sha3((prefix + message).getBytes());
//        System.out.println(HexUtil.encodeHexStr(msgHash));
    }
}
