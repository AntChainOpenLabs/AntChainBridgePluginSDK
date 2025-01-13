package com.alipay.antchain.bridge.ptc.committee.types.tpbta;

import java.io.IOException;
import java.lang.reflect.Type;

import cn.hutool.core.codec.Base64;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.ptc.exception.AntChainBridgePtcException;
import com.alipay.antchain.bridge.ptc.exception.PtcErrorCodeEnum;

public class CrossChainLaneSerializer implements ObjectSerializer {

    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        if (!(object instanceof CrossChainLane)) {
            throw new AntChainBridgePtcException(
                    PtcErrorCodeEnum.COMMITTEE_ERROR,
                    "only support CrossChainLane, but got " + object.getClass().getName()
            );
        }
        serializer.write(Base64.encode(((CrossChainLane) object).encode()));
    }
}
