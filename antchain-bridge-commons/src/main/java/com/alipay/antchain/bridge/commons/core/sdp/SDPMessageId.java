package com.alipay.antchain.bridge.commons.core.sdp;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import lombok.NonNull;
import lombok.Setter;

@Setter
public class SDPMessageId {

    public SDPMessageId(@NonNull String messageIdHex) {
        this(HexUtil.decodeHex(messageIdHex));
    }

    public SDPMessageId(byte[] messageId) {
        if (ObjectUtil.isNull(messageId)) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.SDP_MESSAGE_DECODE_ERROR,
                    "message id is null"
            );
        }
        if (messageId.length != 32) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.SDP_MESSAGE_DECODE_ERROR,
                    StrUtil.format("expected 32 bytes but get {}", messageId.length)
            );
        }
        this.messageId = messageId;
    }

    private byte[] messageId;

    public String toHexStr() {
        return HexUtil.encodeHexStr(this.messageId);
    }

    public byte[] toByte32() {
        return messageId;
    }
}
