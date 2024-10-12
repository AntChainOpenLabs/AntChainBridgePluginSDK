package com.alipay.antchain.bridge.plugintestrunner.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * 单例模式的 PTRLogger 类，确保应用程序运行过程中只有一个实例。
 */
public class PTRLogger {
    private final Logger processLogger;
    private final Logger resultLogger;

    private final String MDC_KEY = "logFile";

    /**
     * 日志级别枚举。
     */
//    public enum LogLevel {
//        INFO,
//        ERROR,
//        DEBUG,
//        WARN
//    }

    /**
     * 私有构造函数，防止外部实例化。
     *
     * @param processLoggerName the name of the process logger
     * @param resultLoggerName the name of the result logger
     */
    private PTRLogger(String processLoggerName, String resultLoggerName) {
        this.processLogger = LoggerFactory.getLogger(processLoggerName);
        this.resultLogger = LoggerFactory.getLogger(resultLoggerName);
    }

    /**
     * 默认私有构造函数，使用默认的 logger 名称。
     */
    private PTRLogger() {
        this("processLogger", "resultLogger");
    }

    /**
     * 静态内部类 - 静态内部类方式实现单例，线程安全且支持延迟加载。
     */
    private static class PTRLoggerHolder {
        private static final PTRLogger INSTANCE = new PTRLogger();
    }

    /**
     * 获取 PTRLogger 的唯一实例。
     *
     * @return PTRLogger 的唯一实例
     */
    public static PTRLogger getInstance() {
        return PTRLoggerHolder.INSTANCE;
    }

    /**
     * 记录 processLogger 的日志。
     *
     * @param level   日志级别
     * @param message 日志消息
     * @param args    参数
     */
    public void plog(LogLevel level, String message, Object... args) {
        log(processLogger, level, message, args);
    }

    /**
     * 记录 resultLogger 的日志。
     *
     * @param level   日志级别
     * @param message 日志消息
     * @param args    参数
     */
    public void rlog(LogLevel level, String message, Object... args) {
        log(resultLogger, level, message, args);
    }

    /**
     * 通用的日志记录方法。
     *
     * @param logger  日志记录器
     * @param level   日志级别
     * @param message 日志消息
     * @param args    参数
     */
    private void log(Logger logger, LogLevel level, String message, Object... args) {
        switch (level) {
            case INFO:
                logger.info(message, args);
                break;
            case ERROR:
                if (args.length > 0 && args[0] instanceof Throwable) {
                    logger.error(message, (Throwable) args[0]);
                } else {
                    logger.error(message, args);
                }
                break;
            case DEBUG:
                logger.debug(message, args);
                break;
            case WARN:
                logger.warn(message, args);
                break;
            default:
                logger.info(message, args);
                break;
        }
    }

    /**
     * 在 MDC 中设置值。
     *
     * @param value MDC 的值
     */
    public void putMDC(String value) {
        MDC.put(MDC_KEY, value);
    }

    /**
     * 清除 MDC 中的值。
     */
    public void clearMDC() {
        MDC.clear();
    }

    /**
     * 获取 processLogger。
     *
     * @return processLogger
     */
    public Logger getProcessLogger() {
        return processLogger;
    }

    /**
     * 获取 resultLogger。
     *
     * @return resultLogger
     */
    public Logger getResultLogger() {
        return resultLogger;
    }
}