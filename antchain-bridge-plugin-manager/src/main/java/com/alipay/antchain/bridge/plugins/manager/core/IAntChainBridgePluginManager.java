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

package com.alipay.antchain.bridge.plugins.manager.core;

import java.nio.file.Path;
import java.util.List;

import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;

public interface IAntChainBridgePluginManager {

    void loadPlugins();

    void startPlugins();

    void loadPlugin(Path path);

    void startPlugin(Path path);

    void stopPlugin(String product);

    void startPluginFromStop(String product);

    void reloadPlugin(String product);

    void reloadPlugin(String product, Path path);

    IAntChainBridgePlugin getPlugin(String product);

    boolean hasPlugin(String product);

    IBBCService createBBCService(String product, CrossChainDomain domain);

    IBBCService getBBCService(String product, CrossChainDomain domain);

    boolean hasDomain(CrossChainDomain domain);

    List<CrossChainDomain> allRunningDomains();

    List<String> allSupportProducts();
}
