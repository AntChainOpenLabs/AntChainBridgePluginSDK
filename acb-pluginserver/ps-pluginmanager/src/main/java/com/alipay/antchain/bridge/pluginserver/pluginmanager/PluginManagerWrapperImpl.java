/*
 * Copyright 2023 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.bridge.pluginserver.pluginmanager;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.plugins.manager.AntChainBridgePluginManagerFactory;
import com.alipay.antchain.bridge.plugins.manager.core.IAntChainBridgePlugin;
import com.alipay.antchain.bridge.plugins.manager.core.IAntChainBridgePluginManager;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.ClassLoadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PluginManagerWrapperImpl implements IPluginManagerWrapper {

    private final IAntChainBridgePluginManager manager;

    private final String bbcLoggerDir;

    @Value("${pluginserver.plugin.log.bbc.max_history:3}")
    private int maxBBCLogHistory;

    @Value("${pluginserver.plugin.log.bbc.max_file_size:30mb}")
    private String maxBBCLogFileSize;

    @Value("${pluginserver.plugin.log.bbc.level:info}")
    private String bbcLogLevel;

    @Value("${pluginserver.plugin.log.bbc.on:true}")
    private boolean isBBCLogOn;

    private final Map<String, Logger> bbcLoggerMap = new HashMap<>();

    @Autowired
    public PluginManagerWrapperImpl(
            @Value("${pluginserver.plugin.repo}") String path,
            @Value("${logging.file.path}") String appLogDir,
            @Value("${pluginserver.plugin.policy.classloader.resource.ban-with-prefix.APPLICATION:}") String[] resourceBannedPrefixOnAppLevel
    ) {
        log.info("plugins path: {}", Paths.get(path).toAbsolutePath());

        this.bbcLoggerDir = Paths.get(appLogDir, "bbc").toAbsolutePath().toString();
        log.info("bbc logger base dir: {}", Paths.get(bbcLoggerDir).toAbsolutePath());

        this.manager = AntChainBridgePluginManagerFactory.createPluginManager(
                path,
                ObjectUtil.defaultIfNull(convertPathPrefixBannedMap(resourceBannedPrefixOnAppLevel), new HashMap<>())
        );
        loadPlugins();
        startPlugins();
    }

    private Map<ClassLoadingStrategy.Source, Set<String>> convertPathPrefixBannedMap(
            String[] resourceBannedPrefixOnAppLevel
    ) {
        Map<ClassLoadingStrategy.Source, Set<String>> result = new HashMap<>();

        Set<String> appSet = new HashSet<>(ListUtil.of(resourceBannedPrefixOnAppLevel));
        result.put(ClassLoadingStrategy.Source.APPLICATION, appSet);

        return result;
    }

    @Override
    public void loadPlugins() {
        manager.loadPlugins();
    }

    @Override
    public void startPlugins() {
        manager.startPlugins();
    }

    @Override
    public void loadPlugin(String path) {
        manager.loadPlugin(Paths.get(path));
    }

    @Override
    public void startPlugin(String path) {
        manager.startPlugin(Paths.get(path));
    }

    @Override
    public void stopPlugin(String product) {
        manager.stopPlugin(product);
    }

    @Override
    public void startPluginFromStop(String product) {
        manager.startPluginFromStop(product);
    }

    @Override
    public void reloadPlugin(String product) {
        manager.reloadPlugin(product);
    }

    @Override
    public void reloadPlugin(String product, String path) {
        manager.reloadPlugin(product, Paths.get(path));
    }

    @Override
    public IAntChainBridgePlugin getPlugin(String product) {
        return manager.getPlugin(product);
    }

    @Override
    public boolean hasPlugin(String product) {
        return manager.hasPlugin(product);
    }

    @Override
    public List<String> allSupportProducts() {
        return manager.allSupportProducts();
    }

    @Override
    public IBBCService createBBCService(String product, String domain) {
        return manager.createBBCService(product, new CrossChainDomain(domain), createBBCServiceLogger(product, domain));
    }

    @Override
    public IBBCService getBBCService(String product, String domain) {
        return manager.getBBCService(product, new CrossChainDomain(domain));
    }

    @Override
    public boolean hasDomain(String domain) {
        return manager.hasDomain(new CrossChainDomain(domain));
    }

    @Override
    public List<String> allRunningDomains() {
        return manager.allRunningDomains().stream().map(CrossChainDomain::toString).collect(Collectors.toList());
    }

    private Logger createBBCServiceLogger(String product, String domain) {
        if (!isBBCLogOn) {
            return null;
        }
        String loggerName = getLoggerName(product, domain);
        if (bbcLoggerMap.containsKey(loggerName) && ObjectUtil.isNotNull(bbcLoggerMap.get(loggerName))) {
            return bbcLoggerMap.get(loggerName);
        }
        Path logFile = Paths.get(bbcLoggerDir, product, domain + ".log");
        Logger logger = LoggerFactory.getLogger(loggerName);
        if (logger instanceof ch.qos.logback.classic.Logger) {
            log.debug("using logback for bbc logger");

            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(context);
            encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n");
            encoder.setCharset(StandardCharsets.UTF_8);
            encoder.start();

            SizeAndTimeBasedRollingPolicy rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
            rollingPolicy.setContext(context);
            rollingPolicy.setFileNamePattern(logFile + ".%d{yyyy-MM-dd}.%i");
            rollingPolicy.setMaxHistory(maxBBCLogHistory);
            rollingPolicy.setMaxFileSize(FileSize.valueOf(maxBBCLogFileSize));

            RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
            appender.setContext(context);
            appender.setEncoder(encoder);
            appender.setFile(logFile.toString());
            appender.setRollingPolicy(rollingPolicy);

            rollingPolicy.setParent(appender);
            rollingPolicy.start();
            appender.start();

            AsyncAppender asyncAppender = new AsyncAppender();
            asyncAppender.setContext(context);
            asyncAppender.setName(loggerName);
            asyncAppender.addAppender(appender);
            asyncAppender.start();

            ch.qos.logback.classic.Logger loggerLogback = (ch.qos.logback.classic.Logger) logger;
            loggerLogback.setLevel(Level.toLevel(bbcLogLevel));
            loggerLogback.setAdditive(false);
            loggerLogback.addAppender(asyncAppender);

            bbcLoggerMap.put(loggerName, loggerLogback);
            log.info("bbc logger {} created", loggerName);

            return loggerLogback;
        }

        log.debug("logger library not support for now");
        return null;
    }

    private String getLoggerName(String product, String domain) {
        return StrUtil.format("{}::{}", product, domain);
    }
}
