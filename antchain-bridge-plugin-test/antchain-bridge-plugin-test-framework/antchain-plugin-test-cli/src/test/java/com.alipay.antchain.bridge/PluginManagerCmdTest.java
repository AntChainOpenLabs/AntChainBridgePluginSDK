package com.alipay.antchain.bridge;

import com.alipay.antchain.bridge.App;
import com.alipay.antchain.bridge.plugintestrunner.exception.TestCaseException;
import com.alipay.antchain.bridge.plugintestrunner.service.PluginManagerService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.IOException;

public class PluginManagerCmdTest {

    private CommandLine commandLine;

    private PluginManagerService pluginManagerService;

    private final String PRODUCT_NAME = "simple-ethereum";
    private final String JAR_PATH = "simple-ethereum-bbc-0.2.0-plugin.jar";
    private final String DOMAIN_NAME = "simple-ethereum-domain";

    @BeforeEach
    public void init() throws IOException, TestCaseException {
        App app = new App();
        app.init();
        pluginManagerService = app.pluginTestRunner.getPluginManagerService();
        commandLine = new CommandLine(app);
    }

    @Test
    public void testLoadPlugin() {
        commandLine.execute(createArgs("plugin-manager load -j " + JAR_PATH));
    }

    @Test
    public void testStartPlugin() {
        commandLine.execute(createArgs("plugin-manager load -j " + JAR_PATH));
        commandLine.execute(createArgs("plugin-manager start -j " + JAR_PATH));
        assert pluginManagerService.hasPlugin(PRODUCT_NAME);
    }

    @Test
    public void testStopPlugin() {
        commandLine.execute(createArgs("plugin-manager load -j " + JAR_PATH));
        commandLine.execute(createArgs("plugin-manager start -j " + JAR_PATH));
        assert pluginManagerService.hasPlugin(PRODUCT_NAME);
        commandLine.execute(createArgs("plugin-manager stop -p " + PRODUCT_NAME));
        assert !pluginManagerService.hasPlugin(PRODUCT_NAME);
    }

    @Test
    public void testStartPluginFromStop() {
        commandLine.execute(createArgs("plugin-manager load -j " + JAR_PATH));
        commandLine.execute(createArgs("plugin-manager start -j " + JAR_PATH));
        assert pluginManagerService.hasPlugin(PRODUCT_NAME);
        commandLine.execute(createArgs("plugin-manager stop -p " + PRODUCT_NAME));
        assert !pluginManagerService.hasPlugin(PRODUCT_NAME);
        commandLine.execute(createArgs("plugin-manager start-from-stop -p " + PRODUCT_NAME));
        assert pluginManagerService.hasPlugin(PRODUCT_NAME);
    }

    @Test
    public void testCreateBBCService() {
        commandLine.execute(createArgs("plugin-manager load -j " + JAR_PATH));
        commandLine.execute(createArgs("plugin-manager start -j " + JAR_PATH));
        assert pluginManagerService.hasPlugin(PRODUCT_NAME);
        commandLine.execute(createArgs("plugin-manager create-bbc -p " + PRODUCT_NAME + " -d " + DOMAIN_NAME));
        assert pluginManagerService.hasBBCService(PRODUCT_NAME, DOMAIN_NAME);
    }

    private String[] createArgs(String command) {
        return command.split(" ");
    }
}
