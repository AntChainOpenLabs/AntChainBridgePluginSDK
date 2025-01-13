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

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.alipay.antchain.bridge.plugins.lib.BBCService;
import com.alipay.antchain.bridge.plugins.lib.HeteroChainDataVerifierService;
import com.alipay.antchain.bridge.plugins.manager.exception.AntChainBridgePluginManagerErrorCodeEnum;
import com.alipay.antchain.bridge.plugins.manager.exception.AntChainBridgePluginManagerException;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import com.alipay.antchain.bridge.plugins.spi.ptc.IHeteroChainDataVerifierService;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class AntChainBridgePf4jUtils {
    public static List<String> parseProductsFromClass(Class<?> aClass) {
        if (IBBCService.class.isAssignableFrom(aClass)) {
            return parseProductsFromBBC((Class<? extends IBBCService>) aClass);
        } else if (IHeteroChainDataVerifierService.class.isAssignableFrom(aClass)) {
            return parseProductsFromHCDVS((Class<? extends IHeteroChainDataVerifierService>) aClass);
        } else {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.PLUGIN_INIT_FAILED,
                    String.format("failed to locate service class %s", aClass.getName())
            );
        }
    }

    private static List<String> parseProductsFromBBC(Class<? extends IBBCService> aClass) {
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

    private static List<String> parseProductsFromHCDVS(Class<? extends IHeteroChainDataVerifierService> aClass) {
        Optional<Annotation> hcdvsServiceOptional = Arrays.stream(aClass.getAnnotations())
                .filter(
                        annotation -> annotation.annotationType().getName().equals(HeteroChainDataVerifierService.class.getName())
                ).findFirst();
        if(!hcdvsServiceOptional.isPresent()) {
            return ListUtil.toList();
        }

        String[] products;
        try {
            products = (String[]) ReflectUtil.getMethodByName(hcdvsServiceOptional.get().annotationType(), "products")
                    .invoke(hcdvsServiceOptional.get());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.PLUGIN_INIT_FAILED,
                    String.format("failed to decode products from annotation class %s from impl %s",
                            HeteroChainDataVerifierService.class.getName(), aClass.getName()),
                    e
            );
        }

        if (ObjectUtil.isEmpty(products)) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.PLUGIN_INIT_FAILED,
                    String.format("none products found from annotation class %s from impl %s",
                            HeteroChainDataVerifierService.class.getName(), aClass.getName())
            );
        }
        return ListUtil.toList(products);
    }

}
