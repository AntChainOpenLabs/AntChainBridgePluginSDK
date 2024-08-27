package org.example.plugintestrunner.exception;

public class TestCaseLoaderException extends Exception{
    public TestCaseLoaderException(String message) {
        super(message);
    }
    public TestCaseLoaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class InValidFieldException extends TestCaseLoaderException {
        public InValidFieldException(String message) {
            super(message);
        }

        public InValidFieldException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
