package com.alipay.antchain.bridge.plugintestrunner;


import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import com.alipay.antchain.bridge.plugintestrunner.chainmanager.eos.EosChainManager;
import com.alipay.antchain.bridge.plugintestrunner.exception.ChainManagerException;
import com.alipay.antchain.bridge.plugintestrunner.exception.PluginManagerException;
import com.alipay.antchain.bridge.plugintestrunner.service.ChainManagerService;
import com.alipay.antchain.bridge.plugintestrunner.service.PluginManagerService;
import com.alipay.antchain.bridge.plugintestrunner.util.PTRLogger;
import com.alipay.antchain.bridge.plugintestrunner.util.ShellScriptRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class EosPluginTest {

    // 插件服务
    private PluginManagerService pluginManagerService;
    private ChainManagerService chainManagerService;

    // 插件配置
    private final String PLUGIN_DIRECTORY = "src/test/resources/plugins";
    private final String JAR_PATH = "eos-bbc-0.1.2-plugin.jar";
    private final String PLUGIN_PRODUCT = "eos";
    private final String DOMAIN_NAME = "eos-domain";
    private final String SCRIPT_DIR = "scripts";
    private final String LOG_DIR = "logs";


    private EosChainManager chainManager;
    private IBBCService bbcService;


    @BeforeEach
    public void init() throws PluginManagerException, IOException, ChainManagerException, InterruptedException {
        PTRLogger logger = PTRLogger.getInstance();
        // 加载启动插件
        pluginManagerService = new PluginManagerService(logger, PLUGIN_DIRECTORY);
        pluginManagerService.testLoadPlugin(JAR_PATH);
        pluginManagerService.testStartPlugin(JAR_PATH);
        pluginManagerService.testCreateBBCService(PLUGIN_PRODUCT, DOMAIN_NAME);
        // 创建测试链
        ShellScriptRunner shellScriptRunner = new ShellScriptRunner(LOG_DIR, SCRIPT_DIR);
        chainManagerService = new ChainManagerService(logger, shellScriptRunner);
        chainManagerService.startup(PLUGIN_PRODUCT);
        chainManager = (EosChainManager) chainManagerService.getChainManager(PLUGIN_PRODUCT);
        // 配置 context、bbcService
        bbcService = pluginManagerService.getBBCService(PLUGIN_PRODUCT, DOMAIN_NAME);
    }

    @Test
    public void testEos() {
        System.out.println(chainManager.getConfig());
        bbcService.startup(chainManager.getBBCContext());
    }

    @AfterEach
    public void close() {
        if (chainManagerService != null) {
            chainManagerService.close();
        }
        if (pluginManagerService != null) {
            pluginManagerService.close();
        }
    }
}
