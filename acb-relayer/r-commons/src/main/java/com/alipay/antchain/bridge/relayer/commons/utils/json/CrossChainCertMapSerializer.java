package com.alipay.antchain.bridge.relayer.commons.utils.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;

public class CrossChainCertMapSerializer implements ObjectSerializer {
    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        Map<String, AbstractCrossChainCertificate> map = (Map<String, AbstractCrossChainCertificate>) object;
        JSONObject jsonObject = new JSONObject(
                map.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> CrossChainCertificateUtil.formatCrossChainCertificateToPem(entry.getValue())
                ))
        );
        serializer.write(jsonObject);
    }
}
