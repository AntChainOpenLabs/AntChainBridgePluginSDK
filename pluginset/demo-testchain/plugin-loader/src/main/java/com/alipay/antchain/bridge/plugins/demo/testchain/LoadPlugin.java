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

package com.alipay.antchain.bridge.plugins.demo.testchain;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Assert;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.plugins.manager.AntChainBridgePluginManagerFactory;
import com.alipay.antchain.bridge.plugins.manager.core.IAntChainBridgePluginManager;
import com.alipay.antchain.bridge.plugins.manager.core.AntChainBridgePluginState;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import org.pf4j.ClassLoadingStrategy;

public class LoadPlugin {

    public static final String CHAIN_PRODUCT = "testchain";

    public static final String PLUGINS_PATH = "src/main/resources/plugins/";

    public static final String NEW_PLUGIN_PATH = "src/main/resources/newplugin/plugin-testchain1-0.1-SNAPSHOT-plugin.jar";

    public static void main(String[] args) {

        // smoke here
        Map<ClassLoadingStrategy.Source, Set<String>> pathPrefixBannedMap = new HashMap<>();
        pathPrefixBannedMap.put(ClassLoadingStrategy.Source.APPLICATION, new HashSet<>(ListUtil.of("META-INF/services/io.grpc.")));

        IAntChainBridgePluginManager manager = AntChainBridgePluginManagerFactory.createPluginManager(
                Paths.get(PLUGINS_PATH).toAbsolutePath().toString(),
                pathPrefixBannedMap
        );

        manager.loadPlugins();
        manager.startPlugins();

        Assert.isTrue(manager.hasPlugin(CHAIN_PRODUCT));
        Assert.notNull(manager.createBBCService(CHAIN_PRODUCT, new CrossChainDomain("domain1")));
        Assert.notNull(manager.createBBCService(CHAIN_PRODUCT, new CrossChainDomain("domain2")));
        Assert.isTrue(manager.hasDomain(new CrossChainDomain("domain1")));
        Assert.notEmpty(manager.allRunningDomains());
        Assert.notEmpty(manager.allSupportProducts());

        manager.stopPlugin(CHAIN_PRODUCT);
        Assert.equals(manager.getPlugin(CHAIN_PRODUCT).getCurrState(), AntChainBridgePluginState.STOP);
        try {
            manager.createBBCService(CHAIN_PRODUCT, new CrossChainDomain("domain3"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        manager.startPluginFromStop(CHAIN_PRODUCT);
        Assert.notNull(manager.createBBCService(CHAIN_PRODUCT, new CrossChainDomain("domain3")));

        Path newPlugin = Paths.get(NEW_PLUGIN_PATH);
        manager.loadPlugin(newPlugin);
        manager.startPlugin(newPlugin);

        Assert.isTrue(manager.hasPlugin("testchain1"));
        Assert.notNull(manager.createBBCService("testchain1", new CrossChainDomain("domain4")));

        IBBCService service2 = manager.getBBCService(CHAIN_PRODUCT, new CrossChainDomain("domain2"));
        manager.stopPlugin(CHAIN_PRODUCT);
        Assert.isFalse(manager.hasPlugin(CHAIN_PRODUCT));
        manager.reloadPlugin(CHAIN_PRODUCT);
        Assert.isTrue(manager.hasPlugin(CHAIN_PRODUCT));
        IBBCService newService2 = manager.createBBCService(CHAIN_PRODUCT, new CrossChainDomain("domain2"));
        Assert.notEquals(newService2, service2);
    }
}
