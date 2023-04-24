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
import java.util.Arrays;
import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.plugins.manager.core.AbstractAntChainBridgePlugin;
import com.alipay.antchain.bridge.plugins.manager.core.AntChainBridgePluginState;
import com.alipay.antchain.bridge.plugins.manager.exception.AntChainBridgePluginManagerErrorCodeEnum;
import com.alipay.antchain.bridge.plugins.manager.exception.AntChainBridgePluginManagerException;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;

@NoArgsConstructor
public class Pf4jAntChainBridgePlugin extends AbstractAntChainBridgePlugin {

    private PluginWrapper pluginWrapper;

    private PluginManager pf4jPluginManager;

    public Pf4jAntChainBridgePlugin(PluginWrapper pluginWrapper) {
        super();
        this.pluginWrapper = pluginWrapper;
        this.pf4jPluginManager = pluginWrapper.getPluginManager();
    }

    @Override
    public void load(Path path) {
        this.pluginWrapper = pf4jPluginManager.getPlugin(pf4jPluginManager.loadPlugin(path));
    }

    @Override
    @Synchronized
    public void unload() {
        if (this.getState() == AntChainBridgePluginState.START) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.PLUGIN_UNLOAD_FAILED,
                    String.format("plugin %s is start ", this.pluginWrapper.getPluginId())
            );
        }
        if (!pf4jPluginManager.unloadPlugin(this.pluginWrapper.getPluginId())) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.PLUGIN_UNLOAD_FAILED,
                    "failed to unload pf4j plugin " + this.pluginWrapper.getPluginId()
            );
        }
    }

    @Override
    @Synchronized
    public void reload() {
        this.reload(this.pluginWrapper.getPluginPath());
    }

    @Override
    @Synchronized
    public void reload(Path path) {
        this.unload();
        this.load(path);
        this.start();
    }

    @Override
    @Synchronized
    public void stop() {
        if (ObjectUtil.notEqual(this.getState(), AntChainBridgePluginState.START)) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.PLUGIN_STOP_FAILED,
                    String.format("wrong plugin state %s to stop plugin for products %s failed",
                            this.getState().name(), Arrays.toString(this.getProducts().toArray()))
            );
        }
        this.pf4jPluginManager.stopPlugin(this.pluginWrapper.getPluginId());
        this.setState(AntChainBridgePluginState.STOP);
    }

    @Override
    @Synchronized
    public void start() {
        if (ObjectUtil.notEqual(this.getState(), AntChainBridgePluginState.INIT)
                && ObjectUtil.notEqual(this.getState(), AntChainBridgePluginState.STOP)) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.PLUGIN_START_FAILED,
                    String.format("wrong state %s to start plugin for products %s failed",
                            this.getState().name(), Arrays.toString(this.getProducts().toArray()))
            );
        }

        try {
            this.pf4jPluginManager.startPlugin(this.pluginWrapper.getPluginId());
        } catch (Exception e) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.PLUGIN_START_FAILED,
                    "failed to start pf4j plugin " + pluginWrapper.getPluginId(),
                    e
            );
        }
        this.pf4jPluginManager.getExtensionClasses(IBBCService.class, this.pluginWrapper.getPluginId()).forEach(
                aClass -> this.setBbcServiceFactory(new Pf4jBBCServiceFactory(aClass))
        );

        if (pluginWrapper.getPluginState() == PluginState.STARTED) {
            this.setState(AntChainBridgePluginState.START);
        }
    }

    @Override
    public List<String> getProducts() {
        if (ObjectUtil.notEqual(this.getState(), AntChainBridgePluginState.START)) {
            return null;
        }
        return this.getBbcServiceFactory().products();
    }

    @Override
    @Synchronized
    public IBBCService createBBCService() {
        if (ObjectUtil.notEqual(this.getState(), AntChainBridgePluginState.START)) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.CREATE_BBCSERVICE_FAILED,
                    String.format("plugin is %s", this.getState().name())
            );
        }
        return this.getBbcServiceFactory().create();
    }

    @Override
    public AntChainBridgePluginState getCurrState() {
        return this.getState();
    }

    public Path getPluginPath() {
        return this.pluginWrapper.getPluginPath();
    }

    public void setPluginWrapper(PluginWrapper pluginWrapper) {
        this.pluginWrapper = pluginWrapper;
    }

    public void setPf4jPluginManager(PluginManager pluginManager) {
        this.pf4jPluginManager = pluginManager;
    }
}
