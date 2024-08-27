package org.example.plugintestrunner.exception;

public class PluginManagerException extends Exception {

    // 插件加载异常
    public static class PluginLoadException extends PluginManagerException {

        public PluginLoadException(String message) {
            super(message);
        }
        public PluginLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // 插件启动异常
    public static class PluginStartException extends PluginManagerException {
        public PluginStartException(String message) {
            super(message);
        }
        public PluginStartException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // 创建 BBC 服务异常
    public static class BBCServiceCreateException extends PluginManagerException {
        public BBCServiceCreateException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // PluginProduct不存在异常
    public static class PluginProductNotFoundException extends PluginManagerException {
        public PluginProductNotFoundException(String message) {
            super(message);
        }
    }   

    // 插件停止异常
    public static class PluginStopException extends PluginManagerException {
        public PluginStopException(String message) {
            super(message);
        }
        public PluginStopException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // 插件未找到异常
    public static class PluginNotFoundException extends PluginManagerException {
        public PluginNotFoundException(String message) {
            super(message);
        }
    }

    // 基础异常类的构造函数
    public PluginManagerException(String message) {
        super(message);
    }

    public PluginManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}