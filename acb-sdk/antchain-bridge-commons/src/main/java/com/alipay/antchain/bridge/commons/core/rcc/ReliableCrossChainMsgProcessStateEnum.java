package com.alipay.antchain.bridge.commons.core.rcc;

import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVCreator;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVMapping;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@TLVMapping(
        fieldName = "code"
)
public enum ReliableCrossChainMsgProcessStateEnum {
    SUCCESS("success"),

    PENDING("pending"),

    FAILED("failed");

    private final String code;

    @TLVCreator
    public static ReliableCrossChainMsgProcessStateEnum getByCode(String code) {
        for (ReliableCrossChainMsgProcessStateEnum state : values()) {
            if (state.getCode().equalsIgnoreCase(code)) {
                return state;
            }
        }
        return null;
    }
}
