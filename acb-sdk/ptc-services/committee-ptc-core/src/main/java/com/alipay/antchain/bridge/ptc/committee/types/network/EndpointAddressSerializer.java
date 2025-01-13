package com.alipay.antchain.bridge.ptc.committee.types.network;

import java.io.IOException;
import java.lang.reflect.Type;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;

public class EndpointAddressSerializer implements ObjectSerializer {
    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        EndpointAddress address = (EndpointAddress) object;
        serializer.write(address.getUrl());
    }
}
