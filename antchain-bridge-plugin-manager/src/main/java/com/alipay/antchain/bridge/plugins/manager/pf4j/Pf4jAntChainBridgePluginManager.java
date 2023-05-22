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

package com.alipay.antchain.bridge.plugins.manager.pf4j;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.PathUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.plugins.manager.core.IAntChainBridgePlugin;
import com.alipay.antchain.bridge.plugins.manager.core.IAntChainBridgePluginManager;
import com.alipay.antchain.bridge.plugins.manager.core.AntChainBridgePluginState;
import com.alipay.antchain.bridge.plugins.manager.exception.AntChainBridgePluginManagerErrorCodeEnum;
import com.alipay.antchain.bridge.plugins.manager.exception.AntChainBridgePluginManagerException;
import com.alipay.antchain.bridge.plugins.manager.pf4j.finder.LegacyExtensionFinder;
import com.alipay.antchain.bridge.plugins.manager.pf4j.finder.AntChainBridgePluginDescriptorFinder;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import org.pf4j.*;

/**
 * {@code Pf4jAntChainBridgePluginManager} is the implementation for {@link IAntChainBridgePluginManager}
 * based on plugin framework {@code PF4J}.
 *
 */
public class Pf4jAntChainBridgePluginManager implements IAntChainBridgePluginManager {

    private final DefaultPluginManager pf4jPluginManager;

    private final Map<CrossChainDomain, IBBCService> bbcServiceMap = MapUtil.newConcurrentHashMap();

    private final Map<String, IAntChainBridgePlugin> antChainBridgePluginStartedMap = MapUtil.newConcurrentHashMap();

    private final Map<String, IAntChainBridgePlugin> antChainBridgePluginInitializedMapWithPf4jId = MapUtil.newConcurrentHashMap();

    public Pf4jAntChainBridgePluginManager(String path) {
        this(path, null);
    }

    public Pf4jAntChainBridgePluginManager(String path, Map<ClassLoadingStrategy.Source, Set<String>> packagePrefixBannedMap) {
        this.pf4jPluginManager = new DefaultPluginManager(Paths.get(path)) {

            protected ExtensionFinder createExtensionFinder() {
                LegacyExtensionFinder extensionFinder = new LegacyExtensionFinder(this);
                addPluginStateListener(extensionFinder);

                return extensionFinder;
            }

            protected PluginDescriptorFinder createPluginDescriptorFinder() {
                return (new CompoundPluginDescriptorFinder()).add(new AntChainBridgePluginDescriptorFinder());
            }

            @Override
            protected PluginLoader createPluginLoader() {
                return new CompoundPluginLoader()
                        .add(new DevelopmentPluginLoader(this), this::isDevelopment)
                        .add(new PrefixBannedJarPluginLoader(packagePrefixBannedMap, this), this::isNotDevelopment)
                        .add(new DefaultPluginLoader(this), this::isNotDevelopment);
            }
        };
    }

    @Override
    public void loadPlugins() {
        this.pf4jPluginManager.loadPlugins();
        this.pf4jPluginManager.getPlugins().forEach(
                pluginWrapper -> {
                    if (pluginWrapper.getPluginState() == PluginState.RESOLVED) {
                        antChainBridgePluginInitializedMapWithPf4jId.put(pluginWrapper.getPluginId(), new Pf4jAntChainBridgePlugin(pluginWrapper));
                    }
                }
        );
    }

    @Override
    public void loadPlugin(Path path) {
        if (!FileUtil.exist(path.toFile())) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.PLUGIN_INIT_FAILED,
                    String.format("file not exist : %s", path)
            );
        }
        if (this.antChainBridgePluginStartedMap.values().stream()
                .anyMatch(plugin -> PathUtil.equals(((Pf4jAntChainBridgePlugin) plugin).getPluginPath(), path))) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.PLUGIN_INIT_FAILED,
                    String.format("plugin from path %s already start", path)
            );
        }

        Optional<Map.Entry<String, IAntChainBridgePlugin>> antChainBridgePluginOptional = this.antChainBridgePluginInitializedMapWithPf4jId.entrySet().stream()
                .filter(entry -> PathUtil.equals(((Pf4jAntChainBridgePlugin) entry.getValue()).getPluginPath(), path))
                .findFirst();

        IAntChainBridgePlugin antChainBridgePlugin;
        if (antChainBridgePluginOptional.isPresent()) {
            antChainBridgePlugin = antChainBridgePluginOptional.get().getValue();
            antChainBridgePlugin.unload();
        } else {
            antChainBridgePlugin = new Pf4jAntChainBridgePlugin();
        }

        String pid = this.pf4jPluginManager.loadPlugin(path);
        PluginWrapper pluginWrapper = this.pf4jPluginManager.getPlugin(pid);
        if (pluginWrapper.getPluginState() != PluginState.RESOLVED) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.PLUGIN_INIT_FAILED,
                    String.format("failed to load plugin from path %s end with state %s", path, pluginWrapper.getPluginState())
            );
        }

        ((Pf4jAntChainBridgePlugin) antChainBridgePlugin).setPluginWrapper(pluginWrapper);
        ((Pf4jAntChainBridgePlugin) antChainBridgePlugin).setPf4jPluginManager(pluginWrapper.getPluginManager());

        this.antChainBridgePluginInitializedMapWithPf4jId.put(pid, antChainBridgePlugin);
    }

    @Override
    public void startPlugin(Path path) {
        if (!FileUtil.exist(path.toFile())) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.PLUGIN_START_FAILED,
                    String.format("file not exist : %s", path)
            );
        }

        Optional<Map.Entry<String, IAntChainBridgePlugin>> antChainBridgePluginOptional = this.antChainBridgePluginInitializedMapWithPf4jId.entrySet().stream()
                .filter(entry -> PathUtil.equals(((Pf4jAntChainBridgePlugin) entry.getValue()).getPluginPath(), path))
                .findFirst();
        if (!antChainBridgePluginOptional.isPresent()) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.PLUGIN_START_FAILED,
                    String.format("plugin from path %s not loaded", path)
            );
        }

        String pid = antChainBridgePluginOptional.get().getKey();
        IAntChainBridgePlugin plugin = antChainBridgePluginOptional.get().getValue();

        plugin.start();
        this.archiveAntChainBridgePluginByProduct(pid, plugin);
        this.antChainBridgePluginInitializedMapWithPf4jId.remove(pid);
    }

    @Override
    public void startPlugins() {
        this.antChainBridgePluginInitializedMapWithPf4jId.forEach(
                (pf4jId, pf4jAntChainBridgePlugin) -> {
                    pf4jAntChainBridgePlugin.start();
                    if (pf4jAntChainBridgePlugin.getCurrState() != AntChainBridgePluginState.START) {
                        throw new AntChainBridgePluginManagerException(
                                AntChainBridgePluginManagerErrorCodeEnum.PLUGIN_START_FAILED,
                                "failed to start plugin " + pf4jId
                        );
                    }
                    this.archiveAntChainBridgePluginByProduct(pf4jId, pf4jAntChainBridgePlugin);
                }
        );
        this.antChainBridgePluginInitializedMapWithPf4jId.clear();
    }

    private void archiveAntChainBridgePluginByProduct(String pf4jId, IAntChainBridgePlugin pf4jAntChainBridgePlugin) {
        pf4jAntChainBridgePlugin.getProducts().forEach(
                product -> {
                    if (this.antChainBridgePluginStartedMap.containsKey(product)) {
                        throw new AntChainBridgePluginManagerException(
                                AntChainBridgePluginManagerErrorCodeEnum.BLOCKCHAIN_PRODUCT_CONFLICT_BETWEEN_PLUGINS,
                                String.format("conflict product %s for plugin %s", product, pf4jId)
                        );
                    }
                    this.antChainBridgePluginStartedMap.put(product, pf4jAntChainBridgePlugin);
                }
        );
    }

    @Override
    public void stopPlugin(String product) {
        IAntChainBridgePlugin plugin = this.antChainBridgePluginStartedMap.get(product);
        if (ObjectUtil.isEmpty(plugin)) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.NONE_PLUGIN_FOUND_FOR_BLOCKCHAIN_PRODUCT,
                    "No plugin for such product " + product
            );
        }
        plugin.stop();
    }

    @Override
    public void startPluginFromStop(String product) {
        IAntChainBridgePlugin plugin = this.antChainBridgePluginStartedMap.get(product);
        if (ObjectUtil.isEmpty(plugin)) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.NONE_PLUGIN_FOUND_FOR_BLOCKCHAIN_PRODUCT,
                    "No plugin for such product " + product
            );
        }
        plugin.start();
    }

    @Override
    public void reloadPlugin(String product) {
        this.reloadPlugin(product, null);
    }

    @Override
    public void reloadPlugin(String product, Path path) {
        if (!this.antChainBridgePluginStartedMap.containsKey(product)) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.NONE_PLUGIN_FOUND_FOR_BLOCKCHAIN_PRODUCT,
                    String.format("No plugin for blockchain %s", product)
            );
        }

        if (ObjectUtil.isNull(path)) {
            this.antChainBridgePluginStartedMap.get(product).reload();
            return;
        }
        this.antChainBridgePluginStartedMap.get(product).reload(path);
    }

    @Override
    public IAntChainBridgePlugin getPlugin(String product) {
        return this.antChainBridgePluginStartedMap.get(product);
    }

    @Override
    public boolean hasPlugin(String product) {
        return this.antChainBridgePluginStartedMap.containsKey(product)
                && this.antChainBridgePluginStartedMap.get(product).getCurrState() == AntChainBridgePluginState.START;
    }

    @Override
    public IBBCService createBBCService(String product, CrossChainDomain domain) {
        if (!hasPlugin(product)) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.NONE_PLUGIN_FOUND_FOR_BLOCKCHAIN_PRODUCT,
                    String.format("No plugin for blockchain %s with domain %s", product, domain)
            );
        }

        IBBCService service = this.getPlugin(product).createBBCService();
        this.bbcServiceMap.put(domain, service);

        return service;
    }

    @Override
    public IBBCService getBBCService(String product, CrossChainDomain domain) {
        if (!hasPlugin(product)) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.NONE_PLUGIN_FOUND_FOR_BLOCKCHAIN_PRODUCT,
                    String.format("No plugin for blockchain %s with domain %s", product, domain)
            );
        }

        if (this.bbcServiceMap.containsKey(domain)) {
            return this.bbcServiceMap.get(domain);
        }
        return null;
    }

    @Override
    public boolean hasDomain(CrossChainDomain domain) {
        return this.bbcServiceMap.containsKey(domain);
    }

    @Override
    public List<CrossChainDomain> allRunningDomains() {
        return ListUtil.toList(this.bbcServiceMap.keySet());
    }

    @Override
    public List<String> allSupportProducts() {
        return this.antChainBridgePluginStartedMap.entrySet().stream()
                .filter(entry -> entry.getValue().getCurrState() == AntChainBridgePluginState.START)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
