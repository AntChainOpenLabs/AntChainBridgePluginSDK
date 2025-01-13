package com.alipay.antchain.bridge.ptc.committee.types.network;

import java.lang.reflect.Type;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;

public class EndpointAddressDeserializer implements ObjectDeserializer {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        return (T) new EndpointAddress(parser.parseObject(String.class));
    }

    @Override
    public int getFastMatchToken() {
        return 0;
    }
}
