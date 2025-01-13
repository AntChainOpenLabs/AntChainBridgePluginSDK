package com.alipay.antchain.bridge.ptc.exception;

import com.alipay.antchain.bridge.commons.exception.base.AntChainBridgeBaseException;

public class AntChainBridgePtcException extends AntChainBridgeBaseException {

    public AntChainBridgePtcException(PtcErrorCodeEnum codeEnum, String longMsg) {
        super(codeEnum.getErrorCode(), codeEnum.getShortMsg(), longMsg);
    }

    public AntChainBridgePtcException(PtcErrorCodeEnum codeEnum, String longMsg, Throwable throwable) {
        super(codeEnum.getErrorCode(), codeEnum.getShortMsg(), longMsg, throwable);
    }
}
