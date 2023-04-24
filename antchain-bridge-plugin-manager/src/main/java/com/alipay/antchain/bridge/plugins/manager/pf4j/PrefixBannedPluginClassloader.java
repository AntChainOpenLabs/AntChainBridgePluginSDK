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

import java.io.IOException;
import java.net.URL;
import java.util.*;

import cn.hutool.core.util.ObjectUtil;
import org.pf4j.ClassLoadingStrategy;
import org.pf4j.PluginClassLoader;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginManager;

public class PrefixBannedPluginClassloader extends PluginClassLoader {

    private static final String JAVA_PACKAGE_PREFIX = "java.";

    private static final String PLUGIN_PACKAGE_PREFIX = "org.pf4j.";

    /**
     * Banned the dependency with the prefix path to read the resource
     */
    private final Map<ClassLoadingStrategy.Source, Set<String>> pathPrefixBannedMap;

    private ClassLoadingStrategy classLoadingStrategy;

    public PrefixBannedPluginClassloader(
            PluginManager pluginManager,
            PluginDescriptor pluginDescriptor,
            ClassLoader parent,
            Map<ClassLoadingStrategy.Source, Set<String>> pathPrefixBannedMap
    ) {
        this(pluginManager, pluginDescriptor, parent, ClassLoadingStrategy.PDA, pathPrefixBannedMap);
        this.classLoadingStrategy = ClassLoadingStrategy.PDA;
    }

    /**
     * classloading according to {@code classLoadingStrategy}
     */
    public PrefixBannedPluginClassloader(
            PluginManager pluginManager,
            PluginDescriptor pluginDescriptor,
            ClassLoader parent,
            ClassLoadingStrategy classLoadingStrategy,
            Map<ClassLoadingStrategy.Source, Set<String>> pathPrefixBannedMap
    ) {
        super(pluginManager, pluginDescriptor, parent, classLoadingStrategy);
        this.pathPrefixBannedMap = pathPrefixBannedMap;
    }

    /**
     * Load the named resource from this plugin.
     * By default, this implementation checks the plugin's classpath first then delegates to the parent.
     * Use {@link #classLoadingStrategy} to change the loading strategy.
     *
     * @param name the name of the resource.
     * @return the URL to the resource, {@code null} if the resource was not found.
     */
    @Override
    public URL getResource(String name) {
        for (ClassLoadingStrategy.Source classLoadingSource : classLoadingStrategy.getSources()) {

            if (
                    this.pathPrefixBannedMap.containsKey(classLoadingSource)
                            && ObjectUtil.isNotEmpty(this.pathPrefixBannedMap.get(classLoadingSource))
                            && this.pathPrefixBannedMap.get(classLoadingSource).stream().anyMatch(name::startsWith)
            ) {
                continue;
            }

            URL url = null;
            switch (classLoadingSource) {
                case APPLICATION:
                    url = super.getResource(name);
                    break;
                case PLUGIN:
                    url = findResource(name);
                    break;
                case DEPENDENCIES:
                    url = findResourceFromDependencies(name);
                    break;
            }

            if (url != null) {
                return url;
            }
        }

        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> resources = new ArrayList<>();

        for (ClassLoadingStrategy.Source classLoadingSource : classLoadingStrategy.getSources()) {
            if (
                    this.pathPrefixBannedMap.containsKey(classLoadingSource)
                            && ObjectUtil.isNotEmpty(this.pathPrefixBannedMap.get(classLoadingSource))
                            && this.pathPrefixBannedMap.get(classLoadingSource).stream().anyMatch(name::startsWith)
            ) {
                continue;
            }

            switch (classLoadingSource) {
                case APPLICATION:
                    if (getParent() != null) {
                        resources.addAll(Collections.list(getParent().getResources(name)));
                    }
                    break;
                case PLUGIN:
                    resources.addAll(Collections.list(findResources(name)));
                    break;
                case DEPENDENCIES:
                    resources.addAll(findResourcesFromDependencies(name));
                    break;
            }
        }

        return Collections.enumeration(resources);
    }
}
