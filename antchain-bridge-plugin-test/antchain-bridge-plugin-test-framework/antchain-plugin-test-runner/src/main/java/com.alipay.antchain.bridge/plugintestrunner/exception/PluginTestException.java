package com.alipay.antchain.bridge.plugintestrunner.exception;


public class PluginTestException extends Exception{
    public PluginTestException(String message) { super(message); }

    public PluginTestException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class StartupException extends PluginTestException{
        public StartupException(String message) {
            super(message);
        }

        public StartupException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ShutdownException extends PluginTestException{
        public ShutdownException(String message) {
            super(message);
        }

        public ShutdownException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class GetContextException extends PluginTestException{
        public GetContextException(String message) {
            super(message);
        }

        public GetContextException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class QueryLatestHeightException extends PluginTestException{
        public QueryLatestHeightException(String message) {
            super(message);
        }

        public QueryLatestHeightException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class SetupAuthMessageContractException extends PluginTestException{
        public SetupAuthMessageContractException(String message) {
            super(message);
        }

        public SetupAuthMessageContractException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class SetupSDPMessageContractException extends PluginTestException{
        public SetupSDPMessageContractException(String message) {
            super(message);
        }

        public SetupSDPMessageContractException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class SetLocalDomainException extends PluginTestException{
        public SetLocalDomainException(String message) {
            super(message);
        }

        public SetLocalDomainException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class QuerySDPMessageSeqException extends PluginTestException{
        public QuerySDPMessageSeqException(String message) {
            super(message);
        }

        public QuerySDPMessageSeqException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class SetProtocolException extends PluginTestException{
        public SetProtocolException(String message) {
            super(message);
        }

        public SetProtocolException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class SetAmContractException extends PluginTestException{
        public SetAmContractException(String message) {
            super(message);
        }

        public SetAmContractException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ReadCrossChainMessagesByHeightException extends PluginTestException{
        public ReadCrossChainMessagesByHeightException(String message) {
            super(message);
        }

        public ReadCrossChainMessagesByHeightException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class RelayAuthMessageException extends PluginTestException{
        public RelayAuthMessageException(String message) {
            super(message);
        }

        public RelayAuthMessageException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ReadCrossChainMessageReceiptException extends PluginTestException{
        public ReadCrossChainMessageReceiptException(String message) {
            super(message);
        }

        public ReadCrossChainMessageReceiptException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DependencyException extends PluginTestException{
        public DependencyException(String message) {
            super(message);
        }

        public DependencyException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class PluginTestToolNotSupportException extends PluginTestException{
        public PluginTestToolNotSupportException(String message) {
            super(message);
        }

        public PluginTestToolNotSupportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
