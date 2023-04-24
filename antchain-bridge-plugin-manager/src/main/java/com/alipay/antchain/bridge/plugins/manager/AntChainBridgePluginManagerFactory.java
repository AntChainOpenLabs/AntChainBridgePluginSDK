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

package com.alipay.antchain.bridge.plugins.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.plugins.manager.core.IAntChainBridgePluginManager;
import com.alipay.antchain.bridge.plugins.manager.exception.AntChainBridgePluginManagerErrorCodeEnum;
import com.alipay.antchain.bridge.plugins.manager.exception.AntChainBridgePluginManagerException;
import com.alipay.antchain.bridge.plugins.manager.pf4j.Pf4jAntChainBridgePluginManager;
import org.pf4j.ClassLoadingStrategy;

public class AntChainBridgePluginManagerFactory {

    private static final String PLUGIN_TYPE_PF4J = "pf4j";

    public static IAntChainBridgePluginManager createPluginManager(String dirPath) {
        return createPluginManager(dirPath, new HashMap<>());
    }

    public static IAntChainBridgePluginManager createPluginManager(String dirPath, Map<ClassLoadingStrategy.Source, Set<String>> pathPrefixBannedMap) {
        return createPluginManager(PLUGIN_TYPE_PF4J, dirPath, pathPrefixBannedMap);
    }

    @SuppressWarnings("unchecked")
    public static IAntChainBridgePluginManager createPluginManager(String type, Object... args) {
        switch (type.toLowerCase()) {
            case PLUGIN_TYPE_PF4J:
                if (args.length != 2) {
                    throw new AntChainBridgePluginManagerException(
                            AntChainBridgePluginManagerErrorCodeEnum.CREATE_PLUGIN_MANAGER_FIELD,
                            "wrong length of args to create plugin manager"
                    );
                }

                if (!(args[0] instanceof String) || !(args[1] instanceof Map)) {
                    throw new AntChainBridgePluginManagerException(
                            AntChainBridgePluginManagerErrorCodeEnum.CREATE_PLUGIN_MANAGER_FIELD,
                            "wrong args to create plugin manager"
                    );
                }
                String dirPath = (String) args[0];
                Map<ClassLoadingStrategy.Source, Set<String>> pathPrefixBannedMap = (Map<ClassLoadingStrategy.Source, Set<String>>) args[1];
                if (StrUtil.isEmpty(dirPath)) {
                    throw new AntChainBridgePluginManagerException(
                            AntChainBridgePluginManagerErrorCodeEnum.CREATE_PLUGIN_MANAGER_FIELD,
                            "wrong path for pf4j manager"
                    );
                }

                return createPf4jAntChainBridgePluginManager(dirPath, pathPrefixBannedMap);
            default:
                throw new AntChainBridgePluginManagerException(
                        AntChainBridgePluginManagerErrorCodeEnum.PLUGIN_MANAGER_TYPE_NOT_SUPPORT,
                        "plugin manager not support: " + type
                );
        }
    }

    private static Pf4jAntChainBridgePluginManager createPf4jAntChainBridgePluginManager(String path, Map<ClassLoadingStrategy.Source, Set<String>> pathPrefixBannedMap) {
        return new Pf4jAntChainBridgePluginManager(path, pathPrefixBannedMap);
    }
}
