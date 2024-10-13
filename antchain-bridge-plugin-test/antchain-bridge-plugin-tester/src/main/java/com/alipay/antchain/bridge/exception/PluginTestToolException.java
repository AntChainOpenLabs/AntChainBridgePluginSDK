package com.alipay.antchain.bridge.exception;

public class PluginTestToolException extends Exception{
    public PluginTestToolException(String message) {
        super(message);
    }

    public PluginTestToolException(String message, Throwable cause) {
        super(message, cause);
    }


    public static class ServiceNullException extends PluginTestToolException{

        public ServiceNullException(String message) {
            super(message);
        }

        public ServiceNullException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ContextNullException extends PluginTestToolException{

        public ContextNullException(String message) {
            super(message);
        }

        public ContextNullException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class AuthMessageContractNullException extends PluginTestToolException{

        public AuthMessageContractNullException(String message) {
            super(message);
        }

        public AuthMessageContractNullException(String message, Throwable cause) {
            super(message, cause);
        }
    }


    public static class AuthMessageContractNotNullException extends SetupAuthMessageContractTestException {
        public AuthMessageContractNotNullException(String message) {
            super(message);
        }

        public AuthMessageContractNotNullException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class AuthMessageContractAddressNullException extends PluginTestToolException{

        public AuthMessageContractAddressNullException(String message) {
            super(message);
        }

        public AuthMessageContractAddressNullException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class AuthMessageContractStatusException extends PluginTestToolException{

        public AuthMessageContractStatusException(String message) {
            super(message);
        }

        public AuthMessageContractStatusException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class SDPContractNullException extends PluginTestToolException{

        public SDPContractNullException(String message) {
            super(message);
        }

        public SDPContractNullException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class SDPContractNotNullException extends PluginTestToolException{

        public SDPContractNotNullException(String message) {
            super(message);
        }

        public SDPContractNotNullException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class SDPContractStatusException extends PluginTestToolException{

        public SDPContractStatusException(String message) {
            super(message);
        }

        public SDPContractStatusException(String message, Throwable cause) {
            super(message, cause);
        }
    }



    public static class StartUpTestException extends PluginTestToolException {
        public StartUpTestException(String message) {
            super(message);
        }

        public StartUpTestException(String message, Throwable cause) {
            super(message, cause);
        }
    }


    public static class ShutDownTestException extends PluginTestToolException {
        public ShutDownTestException(String message) {
            super(message);
        }

        public ShutDownTestException(String message, Throwable cause) {
            super(message, cause);
        }
    }


    public static class SetupAuthMessageContractTestException extends PluginTestToolException{
        public SetupAuthMessageContractTestException(String message) {
            super(message);
        }

        public SetupAuthMessageContractTestException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class GetContextTestException extends PluginTestToolException{

        public GetContextTestException(String message) {
            super(message);
        }

        public GetContextTestException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public static class SetupSDPMessageContractTestException extends PluginTestToolException{
        public SetupSDPMessageContractTestException(String message) {
            super(message);
        }

        public SetupSDPMessageContractTestException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class QuerySDPMessageSeqTestException extends PluginTestToolException{
        public QuerySDPMessageSeqTestException(String message) {
            super(message);
        }

        public QuerySDPMessageSeqTestException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class SetProtocolTestException extends PluginTestToolException{
        public SetProtocolTestException(String message) {
            super(message);
        }

        public SetProtocolTestException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class SetAMContractAndLocalDomainTestException extends PluginTestToolException{
        public SetAMContractAndLocalDomainTestException(String message) {
            super(message);
        }

        public SetAMContractAndLocalDomainTestException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class GetAuthMessageContractAddressException extends PluginTestToolException{
        public GetAuthMessageContractAddressException(String message) {
            super(message);
        }

        public GetAuthMessageContractAddressException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class GetLocalDomainException extends PluginTestToolException{
        public GetLocalDomainException(String message) {
            super(message);
        }

        public GetLocalDomainException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class CrossChainMessageReceiptNullException extends PluginTestToolException{
        public CrossChainMessageReceiptNullException(String message) {
            super(message);
        }

        public CrossChainMessageReceiptNullException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class CrossChainMessageReceiptFailedException extends PluginTestToolException{
        public CrossChainMessageReceiptFailedException(String message) {
            super(message);
        }

        public CrossChainMessageReceiptFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class CrossChainMessageReceiptNotConfirmedException extends PluginTestToolException{
        public CrossChainMessageReceiptNotConfirmedException(String message) {
            super(message);
        }

        public CrossChainMessageReceiptNotConfirmedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class CrossChainMessageReceiptSuccessStatusMismatchException extends PluginTestToolException{
        public CrossChainMessageReceiptSuccessStatusMismatchException(String message) {
            super(message);
        }

        public CrossChainMessageReceiptSuccessStatusMismatchException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ReadCrossChainMessageReceiptTestException extends PluginTestToolException{
        public ReadCrossChainMessageReceiptTestException(String message) {
            super(message);
        }

        public ReadCrossChainMessageReceiptTestException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ReadCrossChainMessageByHeightTestException extends PluginTestToolException{
        public ReadCrossChainMessageByHeightTestException(String message) {
            super(message);
        }

        public ReadCrossChainMessageByHeightTestException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
