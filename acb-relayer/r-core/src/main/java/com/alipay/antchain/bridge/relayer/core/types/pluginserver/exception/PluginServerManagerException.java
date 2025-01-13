package com.alipay.antchain.bridge.relayer.core.types.pluginserver.exception;

import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;

public class PluginServerManagerException extends AntChainBridgeRelayerException {
    public PluginServerManagerException(String message, Throwable cause) {
        super(RelayerErrorCodeEnum.CORE_PLUGIN_SERVER_ERROR, message, cause);
    }

    public PluginServerManagerException(String message) {
        super(RelayerErrorCodeEnum.CORE_PLUGIN_SERVER_ERROR, message);
    }
}
