package com.alipay.antchain.bridge.relayer.core.types.pluginserver.exception;

public class PluginServerConnectionFailException extends PluginServerManagerException {
    public PluginServerConnectionFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginServerConnectionFailException(String message) {
        super(message);
    }
}
