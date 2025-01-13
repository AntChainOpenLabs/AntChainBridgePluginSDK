package com.alipay.antchain.bridge.relayer.core.types.exception;

import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;

public class BbcInterfaceNotSupportException extends AntChainBridgeRelayerException {
    public BbcInterfaceNotSupportException() {
        super(RelayerErrorCodeEnum.CORE_BBC_INTERFACE_NOT_SUPPORT, "bbc interface not support");
    }
}
