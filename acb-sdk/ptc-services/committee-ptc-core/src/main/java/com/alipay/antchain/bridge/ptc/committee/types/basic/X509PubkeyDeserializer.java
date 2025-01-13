package com.alipay.antchain.bridge.ptc.committee.types.basic;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;
import java.security.PublicKey;

import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.PemUtil;
import cn.hutool.jwt.signers.AlgorithmUtil;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alipay.antchain.bridge.ptc.exception.AntChainBridgePtcException;
import com.alipay.antchain.bridge.ptc.exception.PtcErrorCodeEnum;
import lombok.SneakyThrows;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

public class X509PubkeyDeserializer implements ObjectDeserializer {
    @Override
    @SneakyThrows
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        if (!PublicKey.class.isAssignableFrom((Class<?>) type)) {
            throw new AntChainBridgePtcException(
                    PtcErrorCodeEnum.COMMITTEE_ERROR,
                    "only support PublicKey type"
            );
        }

        String value = parser.parseObject(String.class);

        // only accept x.509 public key in PEM format
        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(
                PemUtil.readPem(new ByteArrayInputStream(value.getBytes()))
        );

        return (T) KeyUtil.generatePublicKey(
                AlgorithmUtil.getAlgorithm(subjectPublicKeyInfo.getAlgorithm().getAlgorithm().getId()),
                subjectPublicKeyInfo.getEncoded()
        );
    }

    @Override
    public int getFastMatchToken() {
        return 0;
    }
}
