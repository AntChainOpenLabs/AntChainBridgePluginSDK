package com.alipay.antchain.bridge.plugintestrunner;


import com.alipay.antchain.bridge.plugintestrunner.exception.PluginManagerException;
import com.alipay.antchain.bridge.plugintestrunner.service.PluginManagerService;
import com.alipay.antchain.bridge.plugintestrunner.util.PTRLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PluginManagerServiceTest {

    private PluginManagerService pluginManagerService;

    private final String PLUGIN_DIRECTORY = "src/test/resources/plugins";
    private final String JAR_PATH = "simple-ethereum-bbc-0.2.0-plugin.jar";
    private final String PLUGIN_PRODUCT = "simple-ethereum";
    private final String DOMAIN_NAME = "simple-ethereum-bbc";


    @BeforeEach
    public void init(){
        PTRLogger logger = PTRLogger.getInstance();
        pluginManagerService = new PluginManagerService(logger, PLUGIN_DIRECTORY);
    }

    @Test
    public void testLoadPlugin() throws PluginManagerException {
        pluginManagerService.testLoadPlugin(JAR_PATH);
    }

    @Test
    public void testStartPlugin() throws PluginManagerException {
        pluginManagerService.testLoadPlugin(JAR_PATH);
        pluginManagerService.testStartPlugin(JAR_PATH);
        assert pluginManagerService.hasPlugin(PLUGIN_PRODUCT);
    }

    @Test
    public void testStopPlugin() throws PluginManagerException {
        pluginManagerService.testLoadPlugin(JAR_PATH);
        pluginManagerService.testStartPlugin(JAR_PATH);
        assert pluginManagerService.hasPlugin(PLUGIN_PRODUCT);
        pluginManagerService.testStopPlugin(PLUGIN_PRODUCT);
        assert !pluginManagerService.hasPlugin(PLUGIN_PRODUCT);
    }

    @Test
    public void testStartPluginFromStop() throws PluginManagerException {
        pluginManagerService.testLoadPlugin(JAR_PATH);
        pluginManagerService.testStartPlugin(JAR_PATH);
        assert pluginManagerService.hasPlugin(PLUGIN_PRODUCT);
        pluginManagerService.testStopPlugin(PLUGIN_PRODUCT);
        assert !pluginManagerService.hasPlugin(PLUGIN_PRODUCT);
        pluginManagerService.testStartPluginFromStop(PLUGIN_PRODUCT);
        assert pluginManagerService.hasPlugin(PLUGIN_PRODUCT);
    }

    @Test
    public void testCreateBBCService() throws PluginManagerException {
        pluginManagerService.testLoadPlugin(JAR_PATH);
        pluginManagerService.testStartPlugin(JAR_PATH);
        assert pluginManagerService.hasPlugin(PLUGIN_PRODUCT);
        pluginManagerService.testCreateBBCService(PLUGIN_PRODUCT, DOMAIN_NAME);
        assert pluginManagerService.hasBBCService(PLUGIN_PRODUCT, DOMAIN_NAME);
    }
}
