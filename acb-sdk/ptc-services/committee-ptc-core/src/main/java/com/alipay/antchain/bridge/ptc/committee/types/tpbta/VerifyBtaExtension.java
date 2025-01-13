package com.alipay.antchain.bridge.ptc.committee.types.tpbta;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class VerifyBtaExtension {

    public static VerifyBtaExtension decode(String json) {
        return JSON.parseObject(json, VerifyBtaExtension.class);
    }

    public static VerifyBtaExtension decode(byte[] json) {
        return JSON.parseObject(json, VerifyBtaExtension.class);
    }

    @JSONField(name = "endorse_root")
    private CommitteeEndorseRoot committeeEndorseRoot;

    @JSONField(name = "raw_crosschain_lane", deserializeUsing = CrossChainLaneDeserializer.class, serializeUsing = CrossChainLaneSerializer.class)
    private CrossChainLane crossChainLane;

    public byte[] encode() {
        return JSON.toJSONBytes(this);
    }
}
