package com.alipay.antchain.bridge.relayer.commons.utils.json;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;

public class CrossChainCertMapDeserializer implements ObjectDeserializer {

    @Override
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        JSONObject value = parser.parseObject(JSONObject.class);
        if (value.isEmpty()) {
            return (T) new HashMap<String, AbstractCrossChainCertificate>();
        }
        if (value.getInnerMap().entrySet().iterator().next().getValue() instanceof String) {
            Map<String, String> map = value.toJavaObject(new TypeReference<Map<String, String>>(){});
            return (T) map.entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> CrossChainCertificateUtil.readCrossChainCertificateFromPem(entry.getValue().getBytes())
            ));
        }

        return (T) value.toJavaObject(new TypeReference<Map<String, AbstractCrossChainCertificate>>(){});
    }

    @Override
    public int getFastMatchToken() {
        return 0;
    }
}
