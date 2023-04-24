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

package com.alipay.antchain.bridge.plugins.manager.pf4j.finder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.jar.JarFile;

import org.pf4j.PluginDescriptor;
import org.pf4j.PluginDescriptorFinder;
import org.pf4j.PluginRuntimeException;
import org.pf4j.util.FileUtils;

import static com.alipay.antchain.bridge.plugins.lib.pf4j.processor.LegacyExtensionStorage.DESCRIPTOR_RESOURCE;

public class AntChainBridgePluginDescriptorFinder implements PluginDescriptorFinder {
    public static final String DEFAULT_DESCRIPTOR_FILE_NAME = DESCRIPTOR_RESOURCE;

    protected String descriptorFileName;

    public AntChainBridgePluginDescriptorFinder() {
        this(DEFAULT_DESCRIPTOR_FILE_NAME);
    }

    public AntChainBridgePluginDescriptorFinder(String descriptorFileName) {
        this.descriptorFileName = descriptorFileName;
    }

    public boolean isApplicable(Path pluginPath) {
        return Files.exists(pluginPath, new LinkOption[0]) && (Files.isDirectory(pluginPath, new LinkOption[0]) || FileUtils.isZipOrJarFile(pluginPath));
    }

    public PluginDescriptor find(Path pluginPath) {
        return this.createPluginDescriptor(pluginPath);
    }

    protected PluginDescriptor createPluginDescriptor(Path jarPath) {
        AntChainBridgePluginDescriptor pluginDescriptor = this.createPluginDescriptorInstance();
        try {
            JarFile jar = new JarFile(jarPath.toFile());

            InputStream inputStream = jar.getInputStream(jar.getEntry(DEFAULT_DESCRIPTOR_FILE_NAME));
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                String pluginId = line.replaceAll(" ", "");
                if(!pluginId.startsWith("#")){
                    pluginDescriptor.setPluginId(pluginId);
                    // todo: 注入version
                    pluginDescriptor.setPluginVersion("1");
                    break;
                }
            }

            reader.close();
            jar.close();
            return pluginDescriptor;
        } catch (IOException e) {
            throw new PluginRuntimeException(e, "Cannot read pluginDescriptor from {}", new Object[]{jarPath});
        }
    }

    protected AntChainBridgePluginDescriptor createPluginDescriptorInstance() {
        return new AntChainBridgePluginDescriptor();
    }
}

