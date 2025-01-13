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

package com.alipay.antchain.bridge.relayer.dal.repository.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import javax.annotation.Resource;

import cn.hutool.cache.Cache;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.dal.entities.SystemConfigEntity;
import com.alipay.antchain.bridge.relayer.dal.mapper.SystemConfigMapper;
import com.alipay.antchain.bridge.relayer.dal.repository.ISystemConfigRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SystemConfigRepository implements ISystemConfigRepository {

    /**
     * relayer的endpoints信息
     */
    public static final String LOCAL_ENDPOINTS_KEY = "local_endpoints";

    public static final String DEFAULT_RELAYER_NETWORK_ID_KEY = "default_network_id";

    @Resource
    private RedissonClient redisson;

    @Resource
    private SystemConfigMapper systemConfigMapper;

    @Value("${relayer.network.id:1}")
    private String networkIdInConfig;

    @Resource(name = "systemConfigCache")
    private Cache<String, String> systemConfigCache;

    @Value("${relayer.network.node.local_endpoints:}")
    private String localEndpoints;

    @Override
    public String getSystemConfig(String key) {
        try {
            if (systemConfigCache.containsKey(key)) {
                return systemConfigCache.get(key, false);
            }

            SystemConfigEntity entity = systemConfigMapper.selectOne(
                    new LambdaQueryWrapper<SystemConfigEntity>()
                            .select(ListUtil.of(SystemConfigEntity::getConfValue))
                            .eq(SystemConfigEntity::getConfKey, key)
            );
            if (ObjectUtil.isNull(entity)) {
                return StrUtil.EMPTY;
            }
            systemConfigCache.put(key, entity.getConfValue());
            return entity.getConfValue();
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_SYSTEM_CONFIG_ERROR,
                    StrUtil.format("failed to get system config for {}", key),
                    e
            );
        }
    }

    @Override
    public boolean hasSystemConfig(String key) {
        try {
            return systemConfigCache.containsKey(key) || systemConfigMapper.exists(
                    new LambdaQueryWrapper<SystemConfigEntity>()
                            .eq(SystemConfigEntity::getConfKey, key)
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_SYSTEM_CONFIG_ERROR,
                    StrUtil.format("failed to judge that has config about {} or not", key),
                    e
            );
        }
    }

    @Override
    public void setSystemConfig(Map<String, String> configs) {
        try {
            configs.forEach((key, value) -> {
                systemConfigMapper.insert(
                        SystemConfigEntity.builder()
                                .confKey(key)
                                .confValue(value)
                                .build()
                );
                systemConfigCache.put(key, value);
            });
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_SYSTEM_CONFIG_ERROR,
                    StrUtil.format("failed to set system config map {}", JSON.toJSONString(configs)),
                    e
            );
        }
    }

    @Override
    public void setSystemConfig(String key, String value) {
        try {
            if (hasSystemConfig(key)) {
                systemConfigMapper.update(
                        SystemConfigEntity.builder()
                                .confValue(value)
                                .build(),
                        new LambdaUpdateWrapper<SystemConfigEntity>()
                                .eq(SystemConfigEntity::getConfKey, key)
                );
            } else {
                systemConfigMapper.insert(
                        SystemConfigEntity.builder()
                                .confKey(key)
                                .confValue(value)
                                .build()
                );
            }
            systemConfigCache.put(key, value);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_SYSTEM_CONFIG_ERROR,
                    StrUtil.format("failed to set system config {} : {}", key, value),
                    e
            );
        }
    }

    @Override
    public Lock getDistributedLockForDeployTask(String product, String blockchainId) {
        return redisson.getLock(getDeployLockKey(product, blockchainId));
    }

    @Override
    public List<String> getLocalEndpoints() {
        String val = getSystemConfig(LOCAL_ENDPOINTS_KEY);
        if (StrUtil.isEmpty(val) && StrUtil.isEmpty(localEndpoints)) {
            throw new RuntimeException("no local endpoints set");
        }
        return StrUtil.split(StrUtil.isNotEmpty(val) ? val : localEndpoints, "^");
    }

    @Override
    public void setLocalEndpoints(List<String> endpoints) {
        setSystemConfig(LOCAL_ENDPOINTS_KEY, StrUtil.join("^", endpoints));
    }

    @Override
    public String getDefaultNetworkId() {
        return StrUtil.emptyToDefault(getSystemConfig(DEFAULT_RELAYER_NETWORK_ID_KEY), networkIdInConfig);
    }

    private String getDeployLockKey(String product, String blockchainId) {
        return StrUtil.format("RELAYER_ASYNC_TASK_LOCK_{}_{}", product, blockchainId);
    }
}
