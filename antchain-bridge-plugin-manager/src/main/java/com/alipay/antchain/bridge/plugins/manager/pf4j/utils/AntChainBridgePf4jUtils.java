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

package com.alipay.antchain.bridge.plugins.manager.pf4j.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.alipay.antchain.bridge.plugins.lib.BBCService;
import com.alipay.antchain.bridge.plugins.manager.exception.AntChainBridgePluginManagerErrorCodeEnum;
import com.alipay.antchain.bridge.plugins.manager.exception.AntChainBridgePluginManagerException;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;

public class AntChainBridgePf4jUtils {
    public static List<String> parseProductsFromClass(Class<? extends IBBCService> aClass) {
        Optional<Annotation> bbcServiceOptional = Arrays.stream(aClass.getAnnotations())
                .filter(
                        annotation -> annotation.annotationType().getName().equals(BBCService.class.getName())
                ).findFirst();
        if (!bbcServiceOptional.isPresent()) {
            return ListUtil.toList();
        }

        String[] products;
        try {
            products = (String[]) ReflectUtil.getMethodByName(bbcServiceOptional.get().annotationType(), "products")
                    .invoke(bbcServiceOptional.get());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.PLUGIN_INIT_FAILED,
                    String.format("failed to decode products from annotation class %s from impl %s",
                            BBCService.class.getName(), aClass.getName()),
                    e
            );
        }
        if (ObjectUtil.isEmpty(products)) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.PLUGIN_INIT_FAILED,
                    String.format("none products found from annotation class %s from impl %s",
                            BBCService.class.getName(), aClass.getName())
            );
        }
        return ListUtil.toList(products);
    }
}
