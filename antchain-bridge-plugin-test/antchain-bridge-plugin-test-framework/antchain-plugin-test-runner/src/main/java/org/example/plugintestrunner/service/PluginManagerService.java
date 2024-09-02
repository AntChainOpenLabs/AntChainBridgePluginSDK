package org.example.plugintestrunner.service;

import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.plugins.manager.exception.AntChainBridgePluginManagerException;

import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import lombok.Getter;
import org.example.plugintestrunner.exception.PluginManagerException;
import org.example.plugintestrunner.exception.PluginManagerException.*;
import org.example.plugintestrunner.testcase.TestCase;
import org.example.plugintestrunner.util.LogLevel;
import org.example.plugintestrunner.util.PTRLogger;
import com.alipay.antchain.bridge.plugins.manager.pf4j.Pf4jAntChainBridgePluginManager;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PluginManagerService extends AbstractService{

    @Getter
    private String pluginDirectory;
    private final Pf4jAntChainBridgePluginManager manager;

    public PluginManagerService(PTRLogger logger, String pluginDirectory) {
        super(logger);
        this.pluginDirectory = pluginDirectory;
        manager = new Pf4jAntChainBridgePluginManager(pluginDirectory);
    }

    @Override
    public void run(TestCase testCase) {
        // 设置 MDC
        logger.putMDC(testCase.getName());

        try {
            runTest(testCase);
            logger.rlog(LogLevel.INFO, "Plugin loading and starting test for " + testCase.getName() + ": PASSED");
            // 运行成功
            testCase.setPluginLoadAndStartTestSuccess(true);
        } catch (PluginManagerException e) {
            logger.rlog(LogLevel.ERROR, "Plugin loading and starting test for " + testCase.getName() + ": FAILED");
            logger.rlog(LogLevel.ERROR, e.getMessage());
            if (e.getCause() != null) {
                logger.rlog(LogLevel.ERROR, e.getCause().getMessage());
            }
        }

        // 清除 MDC
        logger.clearMDC();
    }

    private void runTest(TestCase testCase) throws PluginManagerException{
        // 获取测试用例的参数
        boolean isLoadPlugin = testCase.isLoadPlugin();
        boolean isStartPlugin = testCase.isStartPlugin();
        boolean isStartPluginFromStop = testCase.isStartPluginFromStop();
        boolean isStopPlugin = testCase.isStopPlugin();
        boolean isCreateBBCService = testCase.isCreateBBCService();
        String productName = testCase.getProduct();
        String jarPath = Paths.get(testCase.getJarPath()).toString();
        String domain = testCase.getDomain();

        // loadPlugin -> startPlugin -> stopPlugin -> startPluginFromStop
        // loadPlugin -> startPlugin -> createBBCService
        if (isLoadPlugin) {
            testLoadPlugin(jarPath);
        }

        if (isStartPlugin) {
            if (!isLoadPlugin) {
                testLoadPlugin(jarPath);
            }
            testStartPlugin(jarPath);
        }

        if (isStopPlugin) {
            if (!isLoadPlugin) {
                testLoadPlugin(jarPath);
            }
            if (!isStartPlugin) {
                testStartPlugin(jarPath);
            }
            testStopPlugin(productName);
        }

        if (isStartPluginFromStop) {
            if (!isLoadPlugin) {
                testLoadPlugin(jarPath);
            }
            if (!isStartPlugin) {
                testStartPlugin(jarPath);
            }
            if (!isStopPlugin) {
                testStopPlugin(productName);
            }
            testStartPluginFromStop(productName);
        }

        if (isCreateBBCService) {
            if (isStartPluginFromStop) {
                testCreateBBCService(productName, domain);
            } else {
                if (isStopPlugin) {
                    testStartPlugin(jarPath);
                    testCreateBBCService(productName, domain);
                } else {
                    if (!isLoadPlugin) {
                        testLoadPlugin(jarPath);
                    }
                    if (!isStartPlugin) {
                        testStartPlugin(jarPath);
                    }
                    testCreateBBCService(productName, domain);
                }
            }
        }
    }

    @Override
    public void close() {

    }

    // 加载特定路径下的插件
    public void testLoadPlugin(String jarPath) throws PluginManagerException {
        Path path = resolveJarPath(jarPath);
        logger.plog(LogLevel.INFO, "Loading plugin from " + path);
        try {
            manager.loadPlugin(path);
        } catch (AntChainBridgePluginManagerException e){
            throw new PluginLoadException("Failed to load plugin " + path, e);
        }
        logger.plog(LogLevel.INFO, "Plugin " + path + " has been successfully loaded");
    }

    // 启动特定路径下的插件
    public void testStartPlugin(String jarPath) throws PluginManagerException {
        Path path = resolveJarPath(jarPath);
        logger.plog(LogLevel.INFO, "Starting plugin from " + path);
        try {
            manager.startPlugin(path);
            logger.plog(LogLevel.INFO, "Plugin " + path + " has been successfully started");
        } catch (AntChainBridgePluginManagerException e) {
            throw new PluginStartException("Failed to start plugin: " + path, e);
        }
    }
    
    // 关闭特定路径下的插件
    public void testStopPlugin(String pluginProduct) throws PluginManagerException {
        try {
            manager.stopPlugin(pluginProduct);
            logger.plog(LogLevel.INFO, "Plugin " + pluginProduct + " has been successfully stopped");
        } catch (Exception e) {
            throw new PluginStopException("Failed to stop plugin " + pluginProduct, e);
        }
    }

    // 启动已经关闭的插件
    public void testStartPluginFromStop(String pluginProduct) throws PluginManagerException {
        try {
            manager.startPluginFromStop(pluginProduct);
            logger.plog(LogLevel.INFO, "Plugin " + pluginProduct + " has been successfully started from stop");
        } catch (Exception e) {
            throw new PluginStartException("Failed to start plugin " + pluginProduct + " from stop", e);
        }
    }

    // 重新加载插件
    public void testReloadPlugin(String jarPath, String pluginProduct) throws PluginManagerException {
        Path path = resolveJarPath(jarPath);
        try {
            manager.reloadPlugin(pluginProduct, path);
        } catch (Exception e) {
            throw new PluginLoadException("Failed to reload plugin " + path, e);
        }
        logger.plog(LogLevel.INFO, "Plugin " + path + " has been successfully reloaded");
    }

    // 检查插件是否存在
    public boolean hasPlugin(String pluginProduct){
        return manager.hasPlugin(pluginProduct);
    }

    // 创建 BBC 服务
    public void testCreateBBCService(String pluginProduct, String domainName) throws PluginManagerException {
        try {
            CrossChainDomain domain = new CrossChainDomain(domainName);
            manager.createBBCService(pluginProduct, domain);
            logger.plog(LogLevel.INFO, "BBC service for " + pluginProduct + " has been successfully created");
        } catch (Exception e) {
            throw new BBCServiceCreateException("Failed to create BBC service", e);
        }
    }

    public boolean hasDomain(String domainName) {
        return manager.hasDomain(new CrossChainDomain(domainName));
    }

    public boolean hasBBCService(String pluginProduct, String domainName) {
        return manager.getBBCService(pluginProduct, new CrossChainDomain(domainName)) != null;
    }

    public IBBCService getBBCService(String pluginProduct, String domainName) {
        return manager.getBBCService(pluginProduct, new CrossChainDomain(domainName));
    }

    private Path resolveJarPath(String jarPath) {
        Path path = Paths.get(jarPath);
        if (path.getNameCount() == 1) {
            path = Paths.get(pluginDirectory, jarPath).toAbsolutePath();
        } else {
            path = path.toAbsolutePath();
        }
        return path;
    }

}