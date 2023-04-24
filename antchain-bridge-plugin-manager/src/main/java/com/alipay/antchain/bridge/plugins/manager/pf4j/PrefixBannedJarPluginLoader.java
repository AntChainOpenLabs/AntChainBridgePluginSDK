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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cn.hutool.core.util.ObjectUtil;
import org.pf4j.*;

public class PrefixBannedJarPluginLoader extends JarPluginLoader {

    private final Map<ClassLoadingStrategy.Source, Set<String>> pathPrefixBannedMap;

    public PrefixBannedJarPluginLoader(Map<ClassLoadingStrategy.Source, Set<String>> pathPrefixBannedMap, PluginManager pluginManager) {
        super(pluginManager);
        this.pathPrefixBannedMap = ObjectUtil.defaultIfNull(pathPrefixBannedMap, new HashMap<>());
    }

    @Override
    public ClassLoader loadPlugin(Path pluginPath, PluginDescriptor pluginDescriptor) {
        PrefixBannedPluginClassloader pluginClassLoader = new PrefixBannedPluginClassloader(
                pluginManager,
                pluginDescriptor,
                getClass().getClassLoader(),
                pathPrefixBannedMap
        );
        pluginClassLoader.addFile(pluginPath.toFile());

        return pluginClassLoader;
    }
}
