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

package com.alipay.antchain.bridge.plugins.manager.pf4j.asm;

import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class holds the parameters of an {@link com.alipay.antchain.bridge.plugins.lib.BBCService}
 * annotation defined for a certain class.
 *
 */
public final class BBCServiceInfo implements IAntChainBridgeServiceInfo {

    private static final Logger log = LoggerFactory.getLogger(BBCServiceInfo.class);

    private final String className;

    public List<String> getProducts() {
        return products;
    }

    List<String> products = new ArrayList<>();

    private BBCServiceInfo(String className) {
        this.className = className;
    }

    /**
     * Get the name of the class, for which extension info was created.
     *
     * @return absolute class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Load an {@link BBCServiceInfo} for a certain class.
     *
     * @param className absolute class name
     * @param classLoader class loader to access the class
     * @return the {@link BBCServiceInfo}, if the class was annotated with an 
     * {@link com.alipay.antchain.bridge.plugins.lib.BBCService}, otherwise null
     */
    public static BBCServiceInfo load(String className, ClassLoader classLoader) {
        try (InputStream input = classLoader.getResourceAsStream(className.replace('.', '/') + ".class")) {
            BBCServiceInfo info = new BBCServiceInfo(className);
            new ClassReader(input).accept(new BBCServiceVisitor(info), ClassReader.SKIP_DEBUG);

            return info;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

}
