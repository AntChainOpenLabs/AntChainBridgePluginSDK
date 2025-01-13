package com.alipay.antchain.bridge.commons.core.sdp;

import cn.hutool.core.util.ByteUtil;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import lombok.Getter;

@Getter
public enum TimeoutMeasureEnum {
    NO_TIMEOUT((byte) 0),

    SENDER_HEIGHT((byte) 1),

    RECEIVER_HEIGHT((byte) 2),

    SENDER_TIMESTAMP((byte) 3),

    RECEIVER_TIMESTAMP((byte) 4);

    public static TimeoutMeasureEnum parseFrom(byte value) {
        if (value == NO_TIMEOUT.value) {
            return NO_TIMEOUT;
        } else if (value == SENDER_HEIGHT.value) {
            return SENDER_HEIGHT;
        } else if (value == RECEIVER_HEIGHT.value) {
            return RECEIVER_HEIGHT;
        } else if (value == SENDER_TIMESTAMP.value) {
            return SENDER_TIMESTAMP;
        } else if (value == RECEIVER_TIMESTAMP.value) {
            return RECEIVER_TIMESTAMP;
        }
        throw new AntChainBridgeCommonsException(
                CommonsErrorCodeEnum.SDP_MESSAGE_DECODE_ERROR,
                "no sdp timeout measure found for " + ByteUtil.byteToUnsignedInt(value)
        );
    }

    TimeoutMeasureEnum(byte value) {
        this.value = value;
    }

    private final byte value;

}
