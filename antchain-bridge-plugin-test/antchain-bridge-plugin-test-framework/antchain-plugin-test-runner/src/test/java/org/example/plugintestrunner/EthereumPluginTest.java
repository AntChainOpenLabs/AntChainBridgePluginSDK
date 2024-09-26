package org.example.plugintestrunner;

import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
import com.alipay.antchain.bridge.plugins.ethereum.abi.AppContract;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import org.example.plugintestrunner.chainmanager.EthChainManager;
import org.example.plugintestrunner.exception.ChainManagerException;
import org.example.plugintestrunner.exception.PluginManagerException;
import org.example.plugintestrunner.service.ChainManagerService;
import org.example.plugintestrunner.service.PluginManagerService;
import org.example.plugintestrunner.util.PTRLogger;
import org.example.plugintestrunner.util.ShellScriptRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import java.io.IOException;

public class EthereumPluginTest {

    // 插件服务
    private PluginManagerService pluginManagerService;
    private ChainManagerService chainManagerService;

    // 插件配置
    private final String PLUGIN_DIRECTORY = "src/main/resources/plugins";
    private final String JAR_PATH = "simple-ethereum-bbc-0.2.0-plugin.jar";
    private final String PLUGIN_PRODUCT = "simple-ethereum";
    private final String DOMAIN_NAME = "domain1";
    private final String SCRIPT_DIR = "src/main/resources/scripts";
    private final String LOG_DIR = "logs";


    private EthChainManager chainManager;
    private AppContract appContract;
    private AbstractBBCContext bbcContext;
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
        chainManager = (EthChainManager)chainManagerService.getChainManager(PLUGIN_PRODUCT);
        // 配置 context、bbcService
        bbcContext = new DefaultBBCContext();
        bbcContext.setConfForBlockchainClient(chainManager.getConfig().getBytes());
        bbcService = pluginManagerService.getBBCService(PLUGIN_PRODUCT, DOMAIN_NAME);
    }

    @Test
    public void testEth() throws Exception {
        // 部署合约
        Credentials credentials = Credentials.create(chainManager.getPrivateKey());
        RawTransactionManager rawTransactionManager = new RawTransactionManager(
                chainManager.getWeb3j(), credentials, chainManager.getWeb3j().ethChainId().send().getChainId().longValue());
        appContract = AppContract.deploy(
                chainManager.getWeb3j(),
                rawTransactionManager,
                new DefaultGasProvider()
        ).send();
        System.out.println("contract address: " + appContract.getContractAddress());
        // 调用 bbcService
        bbcService.startup(bbcContext);
        bbcService.setupAuthMessageContract();
        bbcService.setupSDPMessageContract();
        System.out.println("authMessageContract address: " + bbcService.getContext().getAuthMessageContract().getContractAddress());
        System.out.println("sdpMessageContract address: " + bbcService.getContext().getSdpContract().getContractAddress());
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
