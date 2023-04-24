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

import com.alipay.antchain.bridge.plugins.manager.core.IAntChainBridgePluginManager;
import org.junit.Assert;
import org.junit.Test;

public class AntChainBridgePluginManagerFactoryTest {
    private static final String PLUGIN_TYPE_PF4J = "pf4j";

    private static final String PLUGIN_PATH = "src/test/resources/plugins/";

    private static final String PLUGIN_PRODUCT = "testchain";


    @Test
    public void testCreatePluginManagerWithType(){
        IAntChainBridgePluginManager pluginManager = AntChainBridgePluginManagerFactory.createPluginManager(PLUGIN_PATH);
        Assert.assertNotNull(pluginManager);
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
        Assert.assertTrue(pluginManager.hasPlugin(PLUGIN_PRODUCT));
    }
    @Test
    public void testCreatePluginManager(){
        IAntChainBridgePluginManager pluginManager = AntChainBridgePluginManagerFactory.createPluginManager(PLUGIN_PATH);
        Assert.assertNotNull(pluginManager);
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
        Assert.assertTrue(pluginManager.hasPlugin(PLUGIN_PRODUCT));
    }
}
