package com.alipay.antchain.bridge.plugintestrunner;


import com.alipay.antchain.bridge.plugintestrunner.chainmanager.eos.EosChainManager;
import com.alipay.antchain.bridge.plugintestrunner.config.ChainConfig;
import com.alipay.antchain.bridge.plugintestrunner.config.ChainConfigManager;
import com.alipay.antchain.bridge.plugintestrunner.exception.ChainManagerException;
import com.alipay.antchain.bridge.plugintestrunner.service.ChainManagerService;
import com.alipay.antchain.bridge.plugintestrunner.util.PTRLogger;
import com.alipay.antchain.bridge.plugintestrunner.util.ShellScriptRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

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
        chainManagerService.shutdown(product);
    }

    @Test
    public void testFiscoBcos() throws ChainManagerException, IOException, InterruptedException {
        String product = "fiscobcos";
        chainManagerService.startup(product);
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
        chainManagerService.shutdown(product);
    }


    @Test
    public void testHyperChain() throws ChainManagerException, IOException, InterruptedException {
        String product = "hyperchain2";
        chainManagerService.startup(product);
        chainManagerService.shutdown(product);
    }


    @Test
    public void testFabric() throws ChainManagerException, IOException, InterruptedException {
        String product = "fabric";
        chainManagerService.startup(product);
        chainManagerService.shutdown(product);
    }
}