package com.alipay.antchain.bridge.plugintestrunner.exception;

// 定义自定义异常类
public class ChainManagerException extends Exception {

    // 链类型不支持异常
    public static class InvalidProductException extends ChainManagerException {
        public InvalidProductException(String message) {
            super(message);
        }
    }

    // 链管理器未初始化异常
    public static class ChainManagerNotInitializedException extends ChainManagerException {
        public ChainManagerNotInitializedException(String message) {
            super(message);
        }
    }

    // 脚本未找到异常
    public static class ScriptNotFoundException extends ChainManagerException {
        public ScriptNotFoundException(String message) {
            super(message);
        }
    }

    // 脚本执行异常
    public static class ScriptExecutionException extends ChainManagerException {
        public ScriptExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // 链管理器构造异常
    public static class ChainManagerConstructionException extends ChainManagerException {
        public ChainManagerConstructionException(String message, Throwable cause) {
            super(message, cause);
        }
        public ChainManagerConstructionException(String message) {
            super(message);
        }
    }

    // 链配置构造异常
    public static class ChainConfigConstructionException extends ChainManagerException {
        public ChainConfigConstructionException(String message, Throwable cause) {
            super(message, cause);
        }
        public ChainConfigConstructionException(String message) {
            super(message);
        }
    }   


    // 链不支持异常
    public static class ChainNotSupportedException extends ChainManagerException {
        public ChainNotSupportedException(String message) {
            super(message);
        }
    }

    public static class ChainManagerNotFoundExpcetion extends  ChainManagerException {
        public ChainManagerNotFoundExpcetion(String message) {super(message);}
    }

    // 基础异常类的构造函数
    public ChainManagerException(String message) {
        super(message);
    }

    public ChainManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}