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

package com.alipay.antchain.bridge.pluginserver.pluginmanager;

import com.alipay.antchain.bridge.plugins.manager.core.IAntChainBridgePlugin;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;

import java.util.List;

public interface IPluginManagerWrapper {
    void loadPlugins();

    void startPlugins();

    void loadPlugin(String path);

    void startPlugin(String path);

    void stopPlugin(String product);

    void startPluginFromStop(String product);

    void reloadPlugin(String product);

    void reloadPlugin(String product, String path);

    IAntChainBridgePlugin getPlugin(String product);

    boolean hasPlugin(String product);

    List<String> allSupportProducts();

    IBBCService createBBCService(String product, String domain);

    IBBCService getBBCService(String product, String domain);

    boolean hasDomain(String domain);

    List<String> allRunningDomains();

}
