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
import com.alipay.antchain.bridge.plugins.spi.ptc.IHeteroChainDataVerifierService;
import org.junit.Assert;
import org.junit.Test;

public class Pf4jAntChainBridgePluginManagerTest {

    private static final String PLUGIN_PATH = "src/test/resources/plugins/";

    private static final String PLUGIN_NAME = "plugin-testchain-0.1-SNAPSHOT-plugin.jar";

    private static final String PLUGIN1_NAME = "plugin-testchain1-0.1-SNAPSHOT-plugin.jar";

    private static final String PLUGIN2_NAME = "plugin-testchain2-0.1-SNAPSHOT-plugin.jar";

    private static final String PLUGIN_PRODUCT = "testchain";

    private static final String PLUGIN1_PRODUCT = "testchain1";

    private static final String PLUGIN2_PRODUCT = "testchain2";

    private static final CrossChainDomain DOMAIN1 = new CrossChainDomain("domain1");

    private static final CrossChainDomain DOMAIN2 = new CrossChainDomain("domain2");

    @Test
    public void testLoadAndStartPlugins() {
        Pf4jAntChainBridgePluginManager manager = new Pf4jAntChainBridgePluginManager(PLUGIN_PATH);

        manager.loadPlugins();
        manager.startPlugins();

        Assert.assertTrue(manager.hasPlugin(PLUGIN_PRODUCT));
        Assert.assertTrue(manager.hasPlugin(PLUGIN1_PRODUCT));
        IAntChainBridgePlugin plugin = manager.getPlugin(PLUGIN_PRODUCT);
        Assert.assertNotNull(plugin);
        Assert.assertEquals(1, plugin.getProducts().size());
        Assert.assertEquals(PLUGIN_PRODUCT, plugin.getProducts().get(0));
    }

    @Test
    public void testLoadStartAndStopPlugin() {
        Pf4jAntChainBridgePluginManager manager = new Pf4jAntChainBridgePluginManager(PLUGIN_PATH);

        Path path = Paths.get(PLUGIN_PATH, PLUGIN_NAME).toAbsolutePath();
        manager.loadPlugin(path);
        manager.startPlugin(path);

        Assert.assertTrue(manager.hasPlugin(PLUGIN_PRODUCT));
        Assert.assertFalse(manager.hasPlugin(PLUGIN1_PRODUCT));
        IAntChainBridgePlugin plugin = manager.getPlugin(PLUGIN_PRODUCT);
        Assert.assertNotNull(plugin);
        Assert.assertEquals(1, plugin.getProducts().size());
        Assert.assertEquals(PLUGIN_PRODUCT, plugin.getProducts().get(0));

        manager.stopPlugin(PLUGIN_PRODUCT);
        Assert.assertFalse(manager.hasPlugin(PLUGIN_PRODUCT));

        manager.startPluginFromStop(PLUGIN_PRODUCT);
        Assert.assertTrue(manager.hasPlugin(PLUGIN_PRODUCT));
    }

    @Test
    public void testReloadPlugin() {
        Pf4jAntChainBridgePluginManager manager = new Pf4jAntChainBridgePluginManager(PLUGIN_PATH);
        Path path = Paths.get(PLUGIN_PATH, PLUGIN_NAME).toAbsolutePath();

        manager.loadPlugin(path);
        manager.startPlugin(path);
        Assert.assertTrue(manager.hasPlugin(PLUGIN_PRODUCT));

        manager.stopPlugin(PLUGIN_PRODUCT);
        Assert.assertFalse(manager.hasPlugin(PLUGIN_PRODUCT));

        // reload plugin
        manager.reloadPlugin(PLUGIN_PRODUCT);
        Assert.assertTrue(manager.hasPlugin(PLUGIN_PRODUCT));

        manager.stopPlugin(PLUGIN_PRODUCT);
        Assert.assertFalse(manager.hasPlugin(PLUGIN_PRODUCT));

        // reload plugin in new path
        path = Paths.get(PLUGIN_PATH, PLUGIN1_NAME).toAbsolutePath();
        manager.reloadPlugin(PLUGIN_PRODUCT, path);
        Assert.assertTrue(manager.hasPlugin(PLUGIN_PRODUCT));
    }

    @Test
    public void testCreateBBCService() {
        Pf4jAntChainBridgePluginManager manager = new Pf4jAntChainBridgePluginManager(PLUGIN_PATH);
        Path path = Paths.get(PLUGIN_PATH, PLUGIN_NAME).toAbsolutePath();

        manager.loadPlugin(path);
        manager.startPlugin(path);
        Assert.assertTrue(manager.hasPlugin(PLUGIN_PRODUCT));

        IBBCService serviceCrt = manager.createBBCService(PLUGIN_PRODUCT, DOMAIN1);
        IBBCService serviceGet = manager.getBBCService(PLUGIN_PRODUCT, DOMAIN1);
        Assert.assertEquals(serviceGet, serviceCrt);
        Assert.assertTrue(manager.hasDomain(DOMAIN1));
        Assert.assertFalse(manager.hasDomain(DOMAIN2));

        List<CrossChainDomain> domainList = manager.allRunningDomains();
        Assert.assertEquals(1, domainList.size());
        Assert.assertEquals(DOMAIN1, domainList.get(0));

        List<String> products = manager.allSupportProducts();
        Assert.assertEquals(1, products.size());
        Assert.assertEquals(PLUGIN_PRODUCT, products.get(0));
    }

    @Test
    public void testCreateHCDVSService() {
        Pf4jAntChainBridgePluginManager manager = new Pf4jAntChainBridgePluginManager(PLUGIN_PATH);
        Path path = Paths.get(PLUGIN_PATH, PLUGIN2_NAME).toAbsolutePath();

        manager.loadPlugin(path);
        manager.startPlugin(path);
        Assert.assertTrue(manager.hasPlugin(PLUGIN2_PRODUCT));

        IHeteroChainDataVerifierService serviceCrt = manager.createHCDVSService(PLUGIN2_PRODUCT);
        IHeteroChainDataVerifierService serviceGet = manager.getHCDVSService(PLUGIN2_PRODUCT);
        Assert.assertEquals(serviceCrt, serviceGet);
        Assert.assertTrue(manager.hasProduct(PLUGIN2_PRODUCT)); // yuechi: hasDomain写死了,暂时先添了个接口
        Assert.assertFalse(manager.hasProduct(PLUGIN1_PRODUCT));

        List<String> products = manager.allSupportProducts(); // yuechi: 好像与bbc无关
        Assert.assertEquals(1, products.size());
        Assert.assertEquals(PLUGIN2_PRODUCT, products.get(0));
    }
}
