package org.example.plugintestrunner.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class PTRLogger {
    private static Logger processLogger;
    private static Logger resultLogger;

    private final String MDC_KEY = "logFile";

    /**
     * Constructs a PTRLogger with custom logger names.
     * If no names are provided, default names are used.
     *
     * @param processLoggerName the name of the process logger
     * @param resultLoggerName the name of the result logger
     */
    public PTRLogger(String processLoggerName, String resultLoggerName) {
        processLogger = LoggerFactory.getLogger(processLoggerName);
        resultLogger = LoggerFactory.getLogger(resultLoggerName);
    }

    /**
     * Default constructor using default logger names.
     */
    public PTRLogger() {
        this("processLogger", "resultLogger");
    }

    public void plog(LogLevel level, String message, Object... args) {
        log(processLogger, level, message, args);
    }

    public void rlog(LogLevel level, String message, Object... args) {
        log(resultLogger, level, message, args);
    }

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
        }
    }

    public void putMDC(String value) {
        MDC.put(MDC_KEY, value);
    }

    public void clearMDC() {
        MDC.clear();
    }
}

