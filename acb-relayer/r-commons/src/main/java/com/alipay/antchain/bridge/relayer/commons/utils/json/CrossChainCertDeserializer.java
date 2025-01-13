package com.alipay.antchain.bridge.relayer.commons.utils.json;

import java.lang.reflect.Type;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;

public class CrossChainCertDeserializer implements ObjectDeserializer {
    @Override
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        String value = parser.parseObject(String.class);
        return (T) CrossChainCertificateUtil.readCrossChainCertificateFromPem(value.getBytes());
    }

    @Override
    public int getFastMatchToken() {
        return 0;
    }
}
