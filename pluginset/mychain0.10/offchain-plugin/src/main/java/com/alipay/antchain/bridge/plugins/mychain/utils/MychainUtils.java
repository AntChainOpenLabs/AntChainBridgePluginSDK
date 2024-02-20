package com.alipay.antchain.bridge.plugins.mychain.utils;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.plugins.mychain.crypto.CryptoSuiteEnum;
import com.alipay.mychain.sdk.common.Parameters;
import com.alipay.mychain.sdk.common.VMTypeEnum;
import com.alipay.mychain.sdk.vm.EVMParameter;
import com.alipay.mychain.sdk.vm.WASMParameter;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.sec.ECPrivateKey;
import org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
import org.bouncycastle.jcajce.util.JcaJceHelper;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

public class MychainUtils {

    public static String contractAddrFormat(String evmContractName, String wasmContractName) {
        return StrUtil.format("{\"evm\":\"{}\", \"wasm\":\"{}\"}", evmContractName, wasmContractName);
    }

    public static VMTypeEnum getSupportedParameterType(Parameters parameters) {
        if (parameters instanceof EVMParameter) {
            return VMTypeEnum.EVM;
        } else if (parameters instanceof WASMParameter) {
            return VMTypeEnum.WASM;
        } else {
            throw new RuntimeException(
                    StrUtil.format("Unsupported parameter type: {}",
                            parameters.getClass().getTypeName()));
        }
    }

    public static void checkKeyPair(String priKey, String pubKey, CryptoSuiteEnum cryptoSuiteEnum) {

        try {
            priKey = extract(priKey);
            JcaJceHelper jcaJceHelper = new DefaultJcaJceHelper();
            KeyFactory keyFactory;
            if (cryptoSuiteEnum == CryptoSuiteEnum.CRYPTO_SUITE_SM) {
                Security.addProvider(new BouncyCastleProvider());
                keyFactory = KeyFactory.getInstance("EC", "BC");
            } else {
                keyFactory = jcaJceHelper.createKeyFactory("ECDSA");
            }

            PrivateKey privateKey = keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(priKey)));
            PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(privateKey.getEncoded());
            ECPrivateKey ecPrivateKey = ECPrivateKey.getInstance(privateKeyInfo.parsePrivateKey());
            byte[] originalBytes = ecPrivateKey.getPublicKey().getBytes();
            byte[] transform = new byte[originalBytes.length - 1];
            System.arraycopy(originalBytes, 1, transform, 0, transform.length);

            if (!StrUtil.equalsIgnoreCase(Hex.toHexString(transform), pubKey)) {
                throw new RuntimeException("pubkey in hex not equal");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String extract(String original) {
        int start = 0;
        int end = 0;
        boolean strip = false;
        StringBuilder sb = new StringBuilder();
        while (start < original.length()) {
            end = start;
            if (original.charAt(end) == '-') {
                strip = true;
            }
            while (end < original.length() && original.charAt(end) != '\n') {
                end++;
            }
            if (!strip) {
                if (end > 0 && original.charAt(end - 1) == '\r') {
                    sb.append(original, start, end - 1);
                } else {
                    sb.append(original, start, end);
                }
            }
            start = end + 1;
            strip = false;
        }
        return sb.toString();
    }
}
