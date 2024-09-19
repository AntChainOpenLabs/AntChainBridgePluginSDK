package org.example.plugintestrunner;


import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import org.example.plugintestrunner.chainmanager.EosChainManager;
import org.example.plugintestrunner.chainmanager.FabricChainManager;
import org.example.plugintestrunner.chainmanager.HyperchainChainManager;
import org.example.plugintestrunner.exception.ChainManagerException;
import org.example.plugintestrunner.exception.PluginManagerException;
import org.example.plugintestrunner.service.ChainManagerService;
import org.example.plugintestrunner.service.PluginManagerService;
import org.example.plugintestrunner.util.PTRLogger;
import org.example.plugintestrunner.util.ShellScriptRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class FabricPluginTest {

    // 插件服务
    private PluginManagerService pluginManagerService;
    private ChainManagerService chainManagerService;

    // 插件配置
    private final String PLUGIN_DIRECTORY = "src/main/resources/plugins";
    private final String JAR_PATH = "simple-fabric-bbc-0.1.1-plugin.jar";
    private final String PLUGIN_PRODUCT = "fabric";
    private final String DOMAIN_NAME = "domain1";
    private final String SCRIPT_DIR = "src/main/resources/scripts";
    private final String LOG_DIR = "logs";


    private FabricChainManager chainManager;
    private AbstractBBCContext bbcContext;
    private IBBCService bbcService;


    @BeforeEach
    public void init() throws PluginManagerException, IOException, ChainManagerException, InterruptedException {
        PTRLogger logger = new PTRLogger();
        // 加载启动插件
        pluginManagerService = new PluginManagerService(logger, PLUGIN_DIRECTORY);
        pluginManagerService.testLoadPlugin(JAR_PATH);
        pluginManagerService.testStartPlugin(JAR_PATH);
        pluginManagerService.testCreateBBCService(PLUGIN_PRODUCT, DOMAIN_NAME);
        // 创建测试链
        ShellScriptRunner shellScriptRunner = new ShellScriptRunner(LOG_DIR, SCRIPT_DIR);
        chainManagerService = new ChainManagerService(logger, shellScriptRunner);
        chainManagerService.startup(PLUGIN_PRODUCT);
        chainManager = (FabricChainManager) chainManagerService.getChainManager(PLUGIN_PRODUCT);
        // 配置 context、bbcService
        bbcContext = new DefaultBBCContext();
        bbcContext.setConfForBlockchainClient(chainManager.getConfig().getBytes());
        bbcService = pluginManagerService.getBBCService(PLUGIN_PRODUCT, DOMAIN_NAME);
    }

    @Test
    public void testEos() {
        System.out.println(chainManager.getConfig());
        bbcService.startup(bbcContext);
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
