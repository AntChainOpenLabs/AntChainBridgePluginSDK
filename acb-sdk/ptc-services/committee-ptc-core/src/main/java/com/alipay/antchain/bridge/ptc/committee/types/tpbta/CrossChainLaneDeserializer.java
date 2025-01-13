package com.alipay.antchain.bridge.ptc.committee.types.tpbta;

import java.lang.reflect.Type;

import cn.hutool.core.codec.Base64;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.ptc.exception.AntChainBridgePtcException;
import com.alipay.antchain.bridge.ptc.exception.PtcErrorCodeEnum;
import lombok.SneakyThrows;

public class CrossChainLaneDeserializer implements ObjectDeserializer {
    @Override
    @SneakyThrows
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        if (!CrossChainLane.class.isAssignableFrom((Class<?>) type)) {
            throw new AntChainBridgePtcException(
                    PtcErrorCodeEnum.COMMITTEE_ERROR,
                    "only support CrossChainLane type"
            );
        }

        String value = parser.parseObject(String.class);

        return (T) CrossChainLane.decode(Base64.decode(value));
    }

    @Override
    public int getFastMatchToken() {
        return 0;
    }
}
