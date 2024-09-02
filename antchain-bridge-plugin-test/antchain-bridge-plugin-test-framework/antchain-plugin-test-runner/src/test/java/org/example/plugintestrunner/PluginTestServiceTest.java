package org.example.plugintestrunner;

import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import org.example.plugintestrunner.chainmanager.EthChainManager;
import org.example.plugintestrunner.exception.PluginManagerException;
import org.example.plugintestrunner.exception.TestCaseLoaderException;
import org.example.plugintestrunner.service.PluginManagerService;
import org.example.plugintestrunner.service.PluginTestService;
import org.example.plugintestrunner.util.PTRLogger;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
import com.alipay.antchain.bridge.plugins.ethereum.EthereumConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.CipherException;
import org.web3j.protocol.exceptions.TransactionException;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class PluginTestServiceTest {

    private PluginTestService pluginTestService;


    private PluginManagerService pluginManagerService;

    // 插件相关配置
    private final String PLUGIN_DIRECTORY = "src/main/resources/plugins";
    private final String JAR_PATH = "simple-ethereum-bbc-0.2.0-plugin.jar";
    private final String PLUGIN_PRODUCT = "simple-ethereum";
    private final String DOMAIN_NAME = "domain1";

    // 测试链配置
    private final String URL = "http://localhost:8545";
    private final String PRIVATE_KEY = "f6e7eeb17472eed03b28ddbfe132428d0147e482cffcda6f43e553321d08a4f8";
    private final String PASSWORD = "123456";
    private final String WALLET_DIR = "/tmp/ethereum/keystore";
    private Long GAS_PRICE;
    private Long GAS_LIMIT;


    // 创建 PluginTestService，加载测试用例
    @BeforeEach
    public void init() throws TestCaseLoaderException, PluginManagerException {
        PTRLogger logger = new PTRLogger();
        // 创建 PluginManagerService，加载启动插件
        pluginManagerService = new PluginManagerService(logger, PLUGIN_DIRECTORY);
        pluginManagerService.testLoadPlugin(JAR_PATH);
        pluginManagerService.testStartPlugin(JAR_PATH);
        pluginManagerService.testCreateBBCService(PLUGIN_PRODUCT, DOMAIN_NAME);
        // 创建 PluginTestService
//        pluginTestService = new PluginTestService(logger);
//        pluginTestService.setPluginManagerService(pluginManagerService);
    }

    private AbstractBBCContext mockValidCtx() throws IOException, InvalidAlgorithmParameterException, CipherException, NoSuchAlgorithmException, NoSuchProviderException, TransactionException, InterruptedException {
        // 配置测试链(创建私钥，设置 gasPrice 和 gasLimit)
        EthChainManager ethChainManager = new EthChainManager(URL);
        EthereumConfig mockConf = new EthereumConfig();
        mockConf.setUrl(URL);
        mockConf.setPrivateKey(PRIVATE_KEY);
        mockConf.setGasPrice(2300000000L);
        mockConf.setGasLimit(3000000);
//        mockConf.setGasPrice(ethChainManager.getGasPriceLong());
//        mockConf.setGasLimit(ethChainManager.getGasLimitLong());
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        System.out.println("url: " + mockConf.getUrl());
        System.out.println("privateKey: " + mockConf.getPrivateKey());
        System.out.println("gasPrice: " + mockConf.getGasPrice());
        System.out.println("gasLimit: " + mockConf.getGasLimit());
        return mockCtx;
    }


    @Test
    public void testRunSpecificPlugin() throws InvalidAlgorithmParameterException, CipherException, IOException, NoSuchAlgorithmException, NoSuchProviderException, TransactionException, InterruptedException {
        AbstractBBCContext context = mockValidCtx();
        IBBCService bbcService = pluginManagerService.getBBCService(PLUGIN_PRODUCT, DOMAIN_NAME);
        bbcService.startup(context);
//        bbcService.setupSDPMessageContract();
        bbcService.setupAuthMessageContract();
        System.out.println(bbcService.getContext().getAuthMessageContract().getContractAddress());
    }
}
