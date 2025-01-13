package com.alipay.antchain.bridge.relayer.core.types.pluginserver.exception;

public class PluginServerRegistrationFailException extends PluginServerManagerException {
    public PluginServerRegistrationFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginServerRegistrationFailException(String message) {
        super(message);
    }
}
