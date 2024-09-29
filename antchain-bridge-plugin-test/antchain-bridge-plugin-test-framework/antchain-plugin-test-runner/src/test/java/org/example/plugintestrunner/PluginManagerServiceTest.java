package org.example.plugintestrunner;

import org.example.plugintestrunner.exception.PluginManagerException;
import org.example.plugintestrunner.service.PluginManagerService;
import org.example.plugintestrunner.util.PTRLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PluginManagerServiceTest {

    private PluginManagerService pluginManagerService;

    private final String PLUGIN_DIRECTORY = "src/test/resources/plugins";
    private final String JAR_PATH = "plugin-testchain-0.1-SNAPSHOT-plugin.jar";
    private final String PLUGIN_PRODUCT = "testchain";
    private final String DOMAIN_NAME = "domain";


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
