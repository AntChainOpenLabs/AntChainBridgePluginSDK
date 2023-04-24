/*
 * Copyright 2023 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.bridge.plugins.manager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.plugins.manager.core.IAntChainBridgePlugin;
import com.alipay.antchain.bridge.plugins.manager.pf4j.Pf4jAntChainBridgePluginManager;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import org.junit.Assert;
import org.junit.Test;

public class Pf4jAntChainBridgePluginManagerTest {

    private static final String PLUGIN_PATH = "src/test/resources/plugins/";

    private static final String PLUGIN_NAME = "plugin-testchain-0.1-SNAPSHOT-plugin.jar";

    private static final String PLUGIN1_NAME = "plugin-testchain1-0.1-SNAPSHOT-plugin.jar";

    private static final String PLUGIIN_PRODUCT = "testchain";

    private static final String PLUGIIN1_PRODUCT = "testchain1";

    private static final CrossChainDomain DOMAIN1 = new CrossChainDomain("domain1");

    private static final CrossChainDomain DOMAIN2 = new CrossChainDomain("domain2");


    @Test
    public void testLoadAndStartPlugins() {
        Pf4jAntChainBridgePluginManager manager = new Pf4jAntChainBridgePluginManager(PLUGIN_PATH);

        manager.loadPlugins();
        manager.startPlugins();

        Assert.assertTrue(manager.hasPlugin(PLUGIIN_PRODUCT));
        Assert.assertTrue(manager.hasPlugin(PLUGIIN1_PRODUCT));
        IAntChainBridgePlugin plugin = manager.getPlugin(PLUGIIN_PRODUCT);
        Assert.assertNotNull(plugin);
        Assert.assertEquals(1, plugin.getProducts().size());
        Assert.assertEquals(PLUGIIN_PRODUCT, plugin.getProducts().get(0));
    }

    @Test
    public void testLoadStartAndStopPlugin() {
        Pf4jAntChainBridgePluginManager manager = new Pf4jAntChainBridgePluginManager(PLUGIN_PATH);

        Path path = Paths.get(PLUGIN_PATH, PLUGIN_NAME).toAbsolutePath();
        manager.loadPlugin(path);
        manager.startPlugin(path);

        Assert.assertTrue(manager.hasPlugin(PLUGIIN_PRODUCT));
        Assert.assertFalse(manager.hasPlugin(PLUGIIN1_PRODUCT));
        IAntChainBridgePlugin plugin = manager.getPlugin(PLUGIIN_PRODUCT);
        Assert.assertNotNull(plugin);
        Assert.assertEquals(1, plugin.getProducts().size());
        Assert.assertEquals(PLUGIIN_PRODUCT, plugin.getProducts().get(0));

        manager.stopPlugin(PLUGIIN_PRODUCT);
        Assert.assertFalse(manager.hasPlugin(PLUGIIN_PRODUCT));

        manager.startPluginFromStop(PLUGIIN_PRODUCT);
        Assert.assertTrue(manager.hasPlugin(PLUGIIN_PRODUCT));
    }

    @Test
    public void testReloadPlugin() {
        Pf4jAntChainBridgePluginManager manager = new Pf4jAntChainBridgePluginManager(PLUGIN_PATH);
        Path path = Paths.get(PLUGIN_PATH, PLUGIN_NAME).toAbsolutePath();

        manager.loadPlugin(path);
        manager.startPlugin(path);
        Assert.assertTrue(manager.hasPlugin(PLUGIIN_PRODUCT));

        manager.stopPlugin(PLUGIIN_PRODUCT);
        Assert.assertFalse(manager.hasPlugin(PLUGIIN_PRODUCT));

        // reload plugin
        manager.reloadPlugin(PLUGIIN_PRODUCT);
        Assert.assertTrue(manager.hasPlugin(PLUGIIN_PRODUCT));

        manager.stopPlugin(PLUGIIN_PRODUCT);
        Assert.assertFalse(manager.hasPlugin(PLUGIIN_PRODUCT));

        // reload plugin in new path
        path = Paths.get(PLUGIN_PATH, PLUGIN1_NAME).toAbsolutePath();
        manager.reloadPlugin(PLUGIIN_PRODUCT, path);
        Assert.assertTrue(manager.hasPlugin(PLUGIIN_PRODUCT));
    }

    @Test
    public void testCreateBBCService() {
        Pf4jAntChainBridgePluginManager manager = new Pf4jAntChainBridgePluginManager(PLUGIN_PATH);
        Path path = Paths.get(PLUGIN_PATH, PLUGIN_NAME).toAbsolutePath();

        manager.loadPlugin(path);
        manager.startPlugin(path);
        Assert.assertTrue(manager.hasPlugin(PLUGIIN_PRODUCT));

        IBBCService serviceCrt = manager.createBBCService(PLUGIIN_PRODUCT, DOMAIN1);
        IBBCService serviceGet = manager.getBBCService(PLUGIIN_PRODUCT, DOMAIN1);
        Assert.assertEquals(serviceGet, serviceCrt);
        Assert.assertTrue(manager.hasDomain(DOMAIN1));
        Assert.assertFalse(manager.hasDomain(DOMAIN2));

        List<CrossChainDomain> domainList = manager.allRunningDomains();
        Assert.assertEquals(1, domainList.size());
        Assert.assertEquals(DOMAIN1, domainList.get(0));

        List<String> products = manager.allSupportProducts();
        Assert.assertEquals(1, products.size());
        Assert.assertEquals(PLUGIIN_PRODUCT, products.get(0));
    }
}
