package com.alipay.antchain.bridge.plugintestrunner.exception;

public class TestCaseException extends Exception{
    public TestCaseException(String message) {
        super(message);
    }
    public TestCaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class InValidFieldException extends TestCaseException {
        public InValidFieldException(String message) {
            super(message);
        }

        public InValidFieldException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class TestCaseChainConfToChainManagerException extends TestCaseException {
        public TestCaseChainConfToChainManagerException(String message) {
            super(message);
        }

        public TestCaseChainConfToChainManagerException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class TestCaseChainConfToClassException extends TestCaseException {
        public TestCaseChainConfToClassException(String message) {
            super(message);
        }

        public TestCaseChainConfToClassException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
