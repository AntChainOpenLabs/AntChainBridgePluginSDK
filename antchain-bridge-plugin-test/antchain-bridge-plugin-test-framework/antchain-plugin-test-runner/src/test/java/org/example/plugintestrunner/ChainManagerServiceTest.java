package org.example.plugintestrunner;

import one.block.eosiojavarpcprovider.error.EosioJavaRpcProviderInitializerError;
import org.chainmaker.sdk.ChainClientException;
import org.chainmaker.sdk.RpcServiceClientException;
import org.chainmaker.sdk.crypto.ChainMakerCryptoSuiteException;
import org.chainmaker.sdk.utils.UtilsException;
import org.example.plugintestrunner.chainmanager.*;
import org.example.plugintestrunner.config.ChainConfig;
import org.example.plugintestrunner.config.ChainConfigManager;
import org.example.plugintestrunner.exception.ChainManagerException;
import org.example.plugintestrunner.util.PTRLogger;
import org.example.plugintestrunner.util.ShellScriptRunner;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.example.plugintestrunner.service.ChainManagerService;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ExecutionException;


public class ChainManagerServiceTest {

    private ChainManagerService chainManagerService;


    @BeforeEach
    public void init() throws IOException {
        ChainConfigManager configManager = ChainConfigManager.getInstance();
        PTRLogger logger = new PTRLogger();
        ShellScriptRunner shellScriptRunner = new ShellScriptRunner(configManager.getProperty("log.directory"),
                configManager.getProperty("script.directory"));
        chainManagerService = new ChainManagerService(logger, shellScriptRunner);
    }


    @Test
    public void testEth() throws IOException, InterruptedException, ChainManagerException {
        String product = "simple-ethereum";
        chainManagerService.startup(product);
        EthChainManager manager = new EthChainManager(ChainConfig.EthChainConfig.getHttpUrl(), ChainConfig.EthChainConfig.privateKeyFile);
        assert manager.isConnected();
        chainManagerService.shutdown(product);
    }

    @Test
    public void testFiscoBcos() throws ChainManagerException, IOException, InterruptedException, ExecutionException {
        String product = "fiscobcos";
        chainManagerService.startup(product);
        FiscoBcosChainManager manager = new FiscoBcosChainManager(ChainConfig.FiscoBcosChainConfig.confFile);
        assert manager.isConnected();
        chainManagerService.shutdown(product);
    }

    @Test
    public void testEOS() throws ChainManagerException, IOException, InterruptedException, EosioJavaRpcProviderInitializerError {
        String product = "eos";
        chainManagerService.startup(product);
        EosChainManager manager = new EosChainManager(ChainConfig.EosChainConfig.getHttpUrl());
        assert manager.isConnected();
        chainManagerService.shutdown(product);
    }


    @Test
    public void testChainMaker() throws Exception {
        String product = "chainmaker";
        chainManagerService.startup(product);
        ChainMakerChainManager manager = new ChainMakerChainManager(ChainConfig.ChainMakerChainConfig.confFile, ChainConfig.ChainMakerChainConfig.chainmakerJsonFile);
        assert manager.isConnected();
        chainManagerService.shutdown(product);
    }


    @Test
    public void testHyperChain() throws ChainManagerException, IOException, InterruptedException, EosioJavaRpcProviderInitializerError, UtilsException, ExecutionException, ChainClientException, ChainMakerCryptoSuiteException, RpcServiceClientException {
        String product = "hyperchain";
        chainManagerService.startup(product);
        HyperchainChainManager manager = new HyperchainChainManager(ChainConfig.HyperChainChainConfig.getHttpUrl());
        assert manager.isConnected();
        chainManagerService.shutdown(product);
    }


    @Test
    public void testFabric() throws ChainManagerException, IOException, InterruptedException, InvalidArgumentException, TransactionException, ProposalException, NoSuchAlgorithmException, InvalidKeySpecException, CryptoException, NoSuchProviderException, ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchMethodException {
//        String product = "fabric";
//        chainManagerService.startup(product);
//        FabricChainManager manager = new FabricChainManager(ChainConfig.FabricChainConfig.privateKeyFile, ChainConfig.FabricChainConfig.certFile, ChainConfig.FabricChainConfig.peerTlsCertFile, ChainConfig.FabricChainConfig.ordererTlsCertFile);
//        assert manager.isConnected();
//        chainManagerService.shutdown(product);
    }
}