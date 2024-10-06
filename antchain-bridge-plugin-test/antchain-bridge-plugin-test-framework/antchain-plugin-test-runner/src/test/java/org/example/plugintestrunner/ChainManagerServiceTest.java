package org.example.plugintestrunner;

import org.example.plugintestrunner.chainmanager.chainmaker.ChainMakerChainManager;
import org.example.plugintestrunner.chainmanager.eos.EosChainManager;
import org.example.plugintestrunner.chainmanager.eth.EthChainManager;
import org.example.plugintestrunner.chainmanager.fiscobcos.FiscoBcosChainManager;
import org.example.plugintestrunner.chainmanager.hyperchain.HyperchainChainManager;
import org.example.plugintestrunner.config.ChainConfig;
import org.example.plugintestrunner.config.ChainConfigManager;
import org.example.plugintestrunner.exception.ChainManagerException;
import org.example.plugintestrunner.util.PTRLogger;
import org.example.plugintestrunner.util.ShellScriptRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.example.plugintestrunner.service.ChainManagerService;

import java.io.IOException;
import java.util.concurrent.ExecutionException;


public class ChainManagerServiceTest {

    private ChainManagerService chainManagerService;


    @BeforeEach
    public void init() throws IOException {
        ChainConfigManager configManager = ChainConfigManager.getInstance();
        PTRLogger logger = PTRLogger.getInstance();
        ShellScriptRunner shellScriptRunner = new ShellScriptRunner(configManager.getProperty("log.directory"),
                configManager.getProperty("script.directory"));
        chainManagerService = new ChainManagerService(logger, shellScriptRunner);
    }


    @Test
    public void testEth() throws IOException, InterruptedException, ChainManagerException {
        String product = "simple-ethereum";
        chainManagerService.startup(product);
        EthChainManager manager = new EthChainManager(ChainConfig.EthChainConfig.getHttpUrl(), ChainConfig.EthChainConfig.privateKeyFile);
        chainManagerService.shutdown(product);
    }

    @Test
    public void testFiscoBcos() throws ChainManagerException, IOException, InterruptedException, ExecutionException {
        String product = "fiscobcos";
        chainManagerService.startup(product);
        FiscoBcosChainManager manager = new FiscoBcosChainManager(ChainConfig.FiscoBcosChainConfig.confDir);
        chainManagerService.shutdown(product);
    }

    @Test
    public void testEOS() throws ChainManagerException, IOException, InterruptedException {
        String product = "eos";
        chainManagerService.startup(product);
        EosChainManager manager = new EosChainManager(ChainConfig.EosChainConfig.getHttpUrl(), ChainConfig.EosChainConfig.privateKeyFile);
        chainManagerService.shutdown(product);
    }


    @Test
    public void testChainMaker() throws Exception {
        String product = "chainmaker";
        chainManagerService.startup(product);
        ChainMakerChainManager manager = new ChainMakerChainManager(ChainConfig.ChainMakerChainConfig.confFile);
        chainManagerService.shutdown(product);
    }


    @Test
    public void testHyperChain() throws ChainManagerException, IOException, InterruptedException, ExecutionException {
        String product = "hyperchain2";
        chainManagerService.startup(product);
        HyperchainChainManager manager = new HyperchainChainManager(ChainConfig.HyperChainChainConfig.getHttpUrl());
        chainManagerService.shutdown(product);
    }


    @Test
    public void testFabric() {
//        String product = "fabric";
//        chainManagerService.startup(product);
//        FabricChainManager manager = new FabricChainManager(ChainConfig.FabricChainConfig.privateKeyFile, ChainConfig.FabricChainConfig.certFile, ChainConfig.FabricChainConfig.peerTlsCertFile, ChainConfig.FabricChainConfig.ordererTlsCertFile);
//        assert manager.isConnected();
//        chainManagerService.shutdown(product);
    }
}