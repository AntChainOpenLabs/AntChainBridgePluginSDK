
/*
 * Copyright (C) 2012-present the original author or authors.
 * Modified by Ant Group in 2023
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

package com.alipay.antchain.bridge.plugins.manager.pf4j.finder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.alipay.antchain.bridge.plugins.lib.pf4j.processor.ExtensionStorage;
import com.alipay.antchain.bridge.plugins.lib.pf4j.processor.LegacyExtensionStorage;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All extensions declared in a plugin are indexed in a file {@code META-INF/extensions.idx}.
 * This class lookup extensions in all extensions index files {@code META-INF/extensions.idx}.
 *
 */
public class LegacyExtensionFinder extends AbstractExtensionFinder {

    private static final Logger log = LoggerFactory.getLogger(LegacyExtensionFinder.class);

    public static final String EXTENSIONS_RESOURCE = LegacyExtensionStorage.EXTENSIONS_RESOURCE;

    public LegacyExtensionFinder(PluginManager pluginManager) {
        super(pluginManager);
    }

    @Override
    public Map<String, Set<String>> readClasspathStorages() {
        log.debug("Reading extensions storages from classpath");
        Map<String, Set<String>> result = new LinkedHashMap<>();

        Set<String> bucket = new HashSet<>();
        try {
            Enumeration<URL> urls = getClass().getClassLoader().getResources(EXTENSIONS_RESOURCE);
            if (urls.hasMoreElements()) {
                collectExtensions(urls, bucket);
            } else {
                log.debug("Cannot find '{}'", EXTENSIONS_RESOURCE);
            }

            debugExtensions(bucket);

            result.put(null, bucket);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return result;
    }

    @Override
    public Map<String, Set<String>> readPluginsStorages() {
        log.debug("Reading extensions storages from plugins");
        Map<String, Set<String>> result = new LinkedHashMap<>();

        List<PluginWrapper> plugins = pluginManager.getPlugins();
        for (PluginWrapper plugin : plugins) {
            String pluginId = plugin.getDescriptor().getPluginId();
            log.debug("Reading extensions storage from plugin '{}'", pluginId);
            Set<String> bucket = new HashSet<>();

            try {
                log.debug("Read '{}'", EXTENSIONS_RESOURCE);
                ClassLoader pluginClassLoader = plugin.getPluginClassLoader();
                try (InputStream resourceStream = pluginClassLoader.getResourceAsStream(EXTENSIONS_RESOURCE)) {
                    if (resourceStream == null) {
                        log.debug("Cannot find '{}'", EXTENSIONS_RESOURCE);
                    } else {
                        collectExtensions(resourceStream, bucket);
                    }
                }

                debugExtensions(bucket);

                result.put(pluginId, bucket);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }

        return result;
    }

    private void collectExtensions(Enumeration<URL> urls, Set<String> bucket) throws IOException {
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            log.debug("Read '{}'", url.getFile());
            collectExtensions(url.openStream(), bucket);
        }
    }

    private void collectExtensions(InputStream inputStream, Set<String> bucket) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            ExtensionStorage.read(reader, bucket);
        }
    }

}
