package com.alipay.antchain.bridge.ptc.committee.types.basic;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.security.PublicKey;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alipay.antchain.bridge.ptc.exception.AntChainBridgePtcException;
import com.alipay.antchain.bridge.ptc.exception.PtcErrorCodeEnum;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

public class X509PubkeySerializer implements ObjectSerializer {

    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        if (!(object instanceof PublicKey)) {
            throw new AntChainBridgePtcException(
                    PtcErrorCodeEnum.COMMITTEE_ERROR,
                    "X509PubkeySerializer only support PublicKey, but got " + object.getClass().getName()
            );
        }

        StringWriter stringWriter = new StringWriter(256);
        JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
        jcaPEMWriter.writeObject(object);
        jcaPEMWriter.close();
        serializer.write(stringWriter.toString());
    }
}
