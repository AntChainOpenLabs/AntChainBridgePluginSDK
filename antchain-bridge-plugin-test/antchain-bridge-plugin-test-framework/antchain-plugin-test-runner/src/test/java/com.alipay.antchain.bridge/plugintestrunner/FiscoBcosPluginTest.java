package com.alipay.antchain.bridge.plugintestrunner;

import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import com.alipay.antchain.bridge.plugintestrunner.chainmanager.fiscobcos.FiscoBcosChainManager;
import com.alipay.antchain.bridge.plugintestrunner.exception.ChainManagerException;
import com.alipay.antchain.bridge.plugintestrunner.exception.PluginManagerException;
import com.alipay.antchain.bridge.plugintestrunner.service.ChainManagerService;
import com.alipay.antchain.bridge.plugintestrunner.service.PluginManagerService;
import com.alipay.antchain.bridge.plugintestrunner.util.PTRLogger;
import com.alipay.antchain.bridge.plugintestrunner.util.ShellScriptRunner;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class FiscoBcosPluginTest {
    // 插件服务
    private PluginManagerService pluginManagerService;
    private ChainManagerService chainManagerService;

    private final String PLUGIN_DIRECTORY = "src/test/resources/plugins";
    private final String JAR_PATH = "fiscobcos-bbc-1.0-SNAPSHOT-plugin.jar";
    private final String PLUGIN_PRODUCT = "fiscobcos";
    private final String DOMAIN_NAME = "fiscobcos-domain";
    private final String SCRIPT_DIR = "scripts";
    private final String LOG_DIR = "logs";

    private FiscoBcosChainManager chainManager;
    private IBBCService bbcService;

    @BeforeEach
    public void init() throws IOException, ChainManagerException, InterruptedException, PluginManagerException {
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
        chainManager = (FiscoBcosChainManager) chainManagerService.getChainManager(PLUGIN_PRODUCT);

        // 配置 context、bbcService
        bbcService = pluginManagerService.getBBCService(PLUGIN_PRODUCT, DOMAIN_NAME);
    }

    @Test
    public void testFiscoBcos() throws ContractException {
        // 部署合约
//        AppContract appContract = AppContract.deploy(chainManager.getClient(), chainManager.getClient().getCryptoSuite().getCryptoKeyPair());

        // 调用 bbcService
        bbcService.startup(chainManager.getBBCContext());
        bbcService.setupAuthMessageContract();
        bbcService.setupSDPMessageContract();
        System.out.println("authMessageContractAddress: " + bbcService.getContext().getAuthMessageContract().getContractAddress());
        System.out.println("sdpMessageContractAddress: " + bbcService.getContext().getSdpContract().getContractAddress());
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
