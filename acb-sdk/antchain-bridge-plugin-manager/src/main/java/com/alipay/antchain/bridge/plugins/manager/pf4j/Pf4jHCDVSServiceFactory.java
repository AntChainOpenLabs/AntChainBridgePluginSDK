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
import com.alipay.antchain.bridge.plugins.manager.core.IHCDVSServiceFactory;
import com.alipay.antchain.bridge.plugins.manager.exception.AntChainBridgePluginManagerErrorCodeEnum;
import com.alipay.antchain.bridge.plugins.manager.exception.AntChainBridgePluginManagerException;
import com.alipay.antchain.bridge.plugins.manager.pf4j.utils.AntChainBridgePf4jUtils;
import com.alipay.antchain.bridge.plugins.spi.ptc.AbstractHCDVSService;
import com.alipay.antchain.bridge.plugins.spi.ptc.IHeteroChainDataVerifierService;
import org.slf4j.Logger;

public class Pf4jHCDVSServiceFactory implements IHCDVSServiceFactory {

    private Class<? extends IHeteroChainDataVerifierService> baseClz;

    public Pf4jHCDVSServiceFactory(Class<? extends IHeteroChainDataVerifierService> baseClz) {
        this.baseClz = baseClz;
    }

    @Override
    public IHeteroChainDataVerifierService create() {
        return create(null);
    }

    @Override
    public IHeteroChainDataVerifierService create(Logger logger) {
        if (baseClz.getConstructors().length < 1) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.CREATE_HCDVSSERVICE_FAILED,
                    "no constructor for class " + baseClz.getName()
            );
        }
        try {
            Constructor<? extends IHeteroChainDataVerifierService> constructor = baseClz.getConstructor();
            IHeteroChainDataVerifierService hcdvsService = constructor.newInstance();
            if (ObjectUtil.isNotNull(logger) && AbstractHCDVSService.class.isAssignableFrom(baseClz)) {
                Method method = AbstractHCDVSService.class.getDeclaredMethod("setLogger", Logger.class);
                method.setAccessible(true);
                method.invoke(hcdvsService, logger);
            }
            return hcdvsService;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new AntChainBridgePluginManagerException(
                    AntChainBridgePluginManagerErrorCodeEnum.CREATE_HCDVSSERVICE_FAILED,
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
