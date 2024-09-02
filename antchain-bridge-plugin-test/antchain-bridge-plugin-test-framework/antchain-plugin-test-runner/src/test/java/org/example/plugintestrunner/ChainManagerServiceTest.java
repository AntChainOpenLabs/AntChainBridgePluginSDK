package org.example.plugintestrunner;

import org.example.plugintestrunner.chainmanager.EthChainManager;
import org.example.plugintestrunner.config.ChainConfig;
import org.example.plugintestrunner.config.ChainConfigManager;
import org.example.plugintestrunner.exception.ChainManagerException;
import org.example.plugintestrunner.util.PTRLogger;
import org.example.plugintestrunner.util.ShellScriptRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.example.plugintestrunner.service.ChainManagerService;

import java.io.IOException;


public class ChainManagerServiceTest {

    private ChainManagerService chainManagerService;

    private EthChainManager ethChainManager;

    private final String PRODUCT = "simple-ethereum";

    @BeforeEach
    public void init() throws IOException {
        ChainConfigManager configManager = ChainConfigManager.getInstance();
        PTRLogger logger = new PTRLogger();
        ShellScriptRunner shellScriptRunner = new ShellScriptRunner(configManager.getProperty("log.directory"),
                configManager.getProperty("script.directory"));
        chainManagerService = new ChainManagerService(logger, shellScriptRunner);
    }


    @Test
    public void testStartupAndShutdown() throws IOException, InterruptedException, ChainManagerException {
        chainManagerService.startup(PRODUCT);
        ethChainManager = new EthChainManager(ChainConfig.EthChainConfig.getHttpUrl());
        assert ethChainManager.isConnected();
        chainManagerService.shutdown(PRODUCT);
    }
}