package org.example.plugintestrunner;

//import com.ali.antchain.EthPluginTestTool;
//import com.ali.antchain.EthPluginTestTool;
import com.alipay.antchain.bridge.plugins.ethereum.abi.AppContract;
//import com.alipay.antchain.bridge.plugins.ethereum.EthereumBBCService;
import com.ali.antchain.EthPluginTestTool;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import org.example.plugintestrunner.chainmanager.eth.EthChainManager;
import org.example.plugintestrunner.config.ChainConfig;
import org.example.plugintestrunner.exception.ChainManagerException;
import org.example.plugintestrunner.exception.PluginManagerException;
import org.example.plugintestrunner.service.ChainManagerService;
import org.example.plugintestrunner.service.PluginManagerService;
import org.example.plugintestrunner.util.PTRLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;
import org.web3j.tx.RawTransactionManager;
//import org.web3j.crypto.Credentials;
//import org.web3j.protocol.Web3j;
//import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

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
    private AppContract appContract;
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
//        ShellScriptRunner shellScriptRunner = new ShellScriptRunner(LOG_DIR, SCRIPT_DIR);
//        chainManagerService = new ChainManagerService(logger, shellScriptRunner);
//        chainManagerService.startup(PLUGIN_PRODUCT);
//        chainManager = (EthChainManager)chainManagerService.getChainManager(PLUGIN_PRODUCT);
        chainManager = new EthChainManager(ChainConfig.EthChainConfig.getHttpUrl(), ChainConfig.EthChainConfig.privateKeyFile, ChainConfig.EthChainConfig.gasPrice, ChainConfig.EthChainConfig.gasLimit);;
//        chainManager = new EthChainManager("http://172.31.154.210:8545", "/tmp/ethereum/private_key.txt", "4100000000", "10000000");
        // 配置 context、bbcService
        bbcService = pluginManagerService.getBBCService(PLUGIN_PRODUCT, DOMAIN_NAME);
    }

    @Test
    public void testEth() throws Exception {
        System.out.println(chainManager.getConfig());
        System.out.println(chainManager.getWeb3j().ethChainId().send().getChainId().toString());
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
//        // 调用 bbcService
        bbcService.startup(chainManager.getBBCContext());
        bbcService.setupAuthMessageContract();
        bbcService.setupSDPMessageContract();
        System.out.println("authMessageContract address: " + bbcService.getContext().getAuthMessageContract().getContractAddress());
        System.out.println("sdpMessageContract address: " + bbcService.getContext().getSdpContract().getContractAddress());
    }

    @Test
    public void testServiceInterface() throws Exception {
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
//        // 调用 bbcService
        bbcService.startup(chainManager.getBBCContext());
        System.out.println("startup pass");

        bbcService.setupAuthMessageContract();
        System.out.println("setupAuthMessageContract pass");

        // set up sdp
        bbcService.setupSDPMessageContract();
        System.out.println("setupSDPMessageContract pass");


        // set protocol to am (sdp type: 0)
        bbcService.setProtocol(
                bbcService.getContext().getSdpContract().getContractAddress(),
                "0");
        System.out.println("setProtocol pass");

        // set am to sdp
        bbcService.setAmContract(bbcService.getContext().getAuthMessageContract().getContractAddress());
        System.out.println("setAmContract pass");


        // set local domain to sdp
        bbcService.setLocalDomain("receiverDomain");
        System.out.println("setLocalDomain pass");
    }

    @Test
    public void testEthPluginTestTool() throws Exception {
        EthPluginTestTool ethPluginTestTool = new EthPluginTestTool(chainManager.getBBCContext(), (AbstractBBCService) bbcService);

//        ethPluginTestTool.startupTest();
//        ethPluginTestTool.shutdownTest();
//        ethPluginTestTool.getcontextTest();
//        ethPluginTestTool.setupamcontractTest();
//        ethPluginTestTool.setupsdpcontractTest();
//        ethPluginTestTool.setprotocolTest();
//        ethPluginTestTool.querysdpmessageseqTest();
//        ethPluginTestTool.setamcontractandlocaldomainTest();


//        ethPluginTestTool.readcrosschainmessagereceiptTest();
        ethPluginTestTool.readcrosschainmessagebyheightTest();
//        ethPluginTestTool.relayauthmessageTest();
    }


    @AfterEach
    public void close() {
//        if (chainManagerService != null) {
//            chainManagerService.close();
//        }
//        if (pluginManagerService != null) {
//            pluginManagerService.close();
//        }
    }
}
