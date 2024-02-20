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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.plugins.manager.core.IBBCServiceFactory;
import com.alipay.antchain.bridge.plugins.manager.exception.AntChainBridgePluginManagerErrorCodeEnum;
import com.alipay.antchain.bridge.plugins.manager.exception.AntChainBridgePluginManagerException;
import com.alipay.antchain.bridge.plugins.manager.pf4j.utils.AntChainBridgePf4jUtils;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import org.slf4j.Logger;

public class Pf4jBBCServiceFactory implements IBBCServiceFactory {

    private Class<? extends IBBCService> baseClz;

    public Pf4jBBCServiceFactory(Class<? extends IBBCService> baseClz) {
        this.baseClz = baseClz;
    }

    @Override
    public IBBCService create() {
        return create(null);
    }

    @Override
    public IBBCService create(Logger logger) {
        if (baseClz.getConstructors().length < 1) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.CREATE_BBCSERVICE_FAILED,
                    "no constructor for class " + baseClz.getName()
            );
        }
        try {
            Constructor<? extends IBBCService> constructor = baseClz.getConstructor();
            IBBCService bbcService = constructor.newInstance();
            if (ObjectUtil.isNotNull(logger) && AbstractBBCService.class.isAssignableFrom(baseClz)) {
                Method method = AbstractBBCService.class.getDeclaredMethod("setLogger", Logger.class);
                method.setAccessible(true);
                method.invoke(bbcService, logger);
            }
            return bbcService;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.CREATE_BBCSERVICE_FAILED,
                    "failed to call constructor with non-parameters for class " + baseClz.getName(),
                    e
            );
        }
    }

    @Override
    public List<String> products() {
        return AntChainBridgePf4jUtils.parseProductsFromClass(this.baseClz);
    }
}
