package com.alipay.antchain.bridge.plugintestrunner;


import com.alipay.antchain.bridge.EthPluginTestTool;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import com.alipay.antchain.bridge.plugintestrunner.chainmanager.eth.EthChainManager;
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

public class EthereumPluginTest {

    // 插件服务
    private PluginManagerService pluginManagerService;
    private ChainManagerService chainManagerService;

    // 插件配置
    private final String PLUGIN_DIRECTORY = "src/test/resources/plugins";
    private final String JAR_PATH = "simple-ethereum-bbc-0.2.0-plugin.jar";
    private final String PLUGIN_PRODUCT = "simple-ethereum";
    private final String DOMAIN_NAME = "simple-ethereum-domain";
    private final String SCRIPT_DIR = "scripts";
    private final String LOG_DIR = "logs";


    private EthChainManager chainManager;
    private IBBCService bbcService;
    EthPluginTestTool ethPluginTestTool;



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
        chainManager = (EthChainManager)chainManagerService.getChainManager(PLUGIN_PRODUCT);
        // 如果使用本地测试环境，将创建测试链的代码注释，同时打开下面的代码，修改相应参数
        // chainManager = new EthChainManager(ChainConfig.EthChainConfig.getHttpUrl(), ChainConfig.EthChainConfig.privateKeyFile, ChainConfig.EthChainConfig.gasPrice, ChainConfig.EthChainConfig.gasLimit);
        // 配置 context、bbcService
        bbcService = pluginManagerService.getBBCService(PLUGIN_PRODUCT, DOMAIN_NAME);
        ethPluginTestTool = new EthPluginTestTool(chainManager.getBBCContext(), (AbstractBBCService) bbcService);
    }

    @Test
    public void testEthBBCServiceInterface() {
        bbcService.startup(chainManager.getBBCContext());

        bbcService.setupAuthMessageContract();
        System.out.println("authMessageContract address: " + bbcService.getContext().getAuthMessageContract().getContractAddress());

        bbcService.setupSDPMessageContract();
        System.out.println("sdpMessageContract address: " + bbcService.getContext().getSdpContract().getContractAddress());

        bbcService.setProtocol(bbcService.getContext().getSdpContract().getContractAddress(),"0");

        bbcService.setAmContract(bbcService.getContext().getAuthMessageContract().getContractAddress());

        bbcService.setLocalDomain("receiverDomain");

        bbcService.shutdown();
    }

    @Test
    public void testTestToolStartup() {
        ethPluginTestTool.startupTest();
    }

    @Test
    public void testTestToolShutdown() {
        ethPluginTestTool.shutdownTest();
    }

    @Test
    public void tetTestToolGetContext() {
        ethPluginTestTool.getContextTest();
    }

    @Test
    public void testTestToolSetupAmContractTest() {
        ethPluginTestTool.setupAmContractTest();
    }

    @Test
    public void testTestToolSetupSdpContractTest() {
        ethPluginTestTool.setupSdpContractTest();
    }

    @Test
    public void testTestToolSetProtocolTest() throws Exception {
        ethPluginTestTool.setProtocolTest();
    }

    @Test
    public void testTestToolQuerySdpMessageSeqTest() {
        ethPluginTestTool.querySdpMessageSeqTest();
    }

    @Test
    public void testTestToolSetAmContractAndLocalDomainTest() {
        ethPluginTestTool.setAmContractAndLocalDomainTest();
    }

    @Test
    public void testTestToolReadCrossChainMessageReceiptTest() {
        ethPluginTestTool.readCrossChainMessageReceiptTest();
    }

    @Test
    public void testTestToolReadCrossChainMessageByHeightTest() {
        ethPluginTestTool.readCrossChainMessageByHeightTest();
    }

    @Test
    public void testTestToolRelayAuthMessageTest() {
        ethPluginTestTool.relayAuthMessageTest();
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
