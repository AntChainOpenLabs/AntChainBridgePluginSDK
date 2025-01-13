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
import java.util.concurrent.locks.Lock;

import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.constant.PluginServerStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.PluginServerDO;
import com.alipay.antchain.bridge.relayer.commons.model.PluginServerInfo;
import com.alipay.antchain.bridge.relayer.dal.entities.PluginServerObjectsEntity;
import com.alipay.antchain.bridge.relayer.dal.mapper.PluginServerObjectsMapper;
import com.alipay.antchain.bridge.relayer.dal.repository.IPluginServerRepository;
import com.alipay.antchain.bridge.relayer.dal.utils.ConvertUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
public class PluginServerRepository implements IPluginServerRepository {

    private static final String PLUGIN_SERVER_HEARTBEAT_LOCK_PREFIX = "plugin_server_heartbeat_lock-";

    @Resource
    private PluginServerObjectsMapper pluginServerObjectsMapper;

    @Resource
    private RedissonClient redisson;

    @Override
    public void insertNewPluginServer(PluginServerDO pluginServerDO) {
        try {
            pluginServerObjectsMapper.insertPluginServer(ConvertUtil.convertFromPluginServerDO(pluginServerDO));
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PLUGINSERVER_ERROR,
                    String.format("failed to insert plugin server %s", pluginServerDO.getPsId()),
                    e
            );
        }
    }

    @Override
    public void updatePluginServerInfo(String psId, PluginServerInfo info) {
        try {
            if (
                    pluginServerObjectsMapper.update(
                            PluginServerObjectsEntity.builder()
                                    .products(StrUtil.join(",", info.getProducts()))
                                    .domains(StrUtil.join(",", info.getDomains()))
                                    .state(info.getState())
                                    .build(),
                            new LambdaUpdateWrapper<PluginServerObjectsEntity>()
                                    .eq(PluginServerObjectsEntity::getPsId, psId)
                    ) != 1
            ) {
                throw new RuntimeException("update db failed");
            }
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PLUGINSERVER_ERROR,
                    String.format("failed to update info for plugin server %s", psId),
                    e
            );
        }
    }

    @Override
    public void deletePluginServer(PluginServerDO pluginServerDO) {
        try {
            pluginServerObjectsMapper.delete(
                    new LambdaUpdateWrapper<PluginServerObjectsEntity>()
                            .eq(PluginServerObjectsEntity::getPsId, pluginServerDO.getPsId())
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PLUGINSERVER_ERROR,
                    String.format("failed to delete plugin server %s", pluginServerDO.getPsId()),
                    e
            );
        }
    }

    @Override
    public PluginServerDO getPluginServer(String psId) {
        try {
            PluginServerObjectsEntity entity = pluginServerObjectsMapper.selectOne(
                    new LambdaQueryWrapper<PluginServerObjectsEntity>()
                            .eq(PluginServerObjectsEntity::getPsId, psId)
            );
            if (ObjectUtil.isNull(entity)) {
                return null;
            }
            return ConvertUtil.convertFromPluginServerObjectsEntity(entity);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PLUGINSERVER_ERROR,
                    String.format("failed to query plugin server %s", psId),
                    e
            );
        }
    }

    @Override
    public void updatePluginServerState(String psId, PluginServerStateEnum stateEnum) {
        try {
            if (
                    pluginServerObjectsMapper.update(
                            PluginServerObjectsEntity.builder()
                                    .state(stateEnum)
                                    .build(),
                            new LambdaUpdateWrapper<PluginServerObjectsEntity>()
                                    .eq(PluginServerObjectsEntity::getPsId, psId)
                    ) != 1
            ) {
                throw new RuntimeException("update db failed");
            }
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PLUGINSERVER_ERROR,
                    String.format("failed to update state for plugin server %s", psId),
                    e
            );
        }
    }

    @Override
    public PluginServerStateEnum getPluginServerStateEnum(String psId) {
        try {
            PluginServerObjectsEntity entity = pluginServerObjectsMapper.selectOne(
                    new LambdaQueryWrapper<PluginServerObjectsEntity>()
                            .select(ListUtil.of(PluginServerObjectsEntity::getState))
                            .eq(PluginServerObjectsEntity::getPsId, psId)
            );
            if (ObjectUtil.isNull(entity)) {
                return PluginServerStateEnum.NOT_FOUND;
            }
            return entity.getState();
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PLUGINSERVER_ERROR,
                    String.format("failed to query state of plugin server %s", psId),
                    e
            );
        }
    }

    @Override
    public List<String> getProductsSupportedOfPluginServer(String psId) {
        try {
            PluginServerObjectsEntity entity = pluginServerObjectsMapper.selectOne(
                    new LambdaQueryWrapper<PluginServerObjectsEntity>()
                            .select(ListUtil.of(PluginServerObjectsEntity::getProducts))
                            .eq(PluginServerObjectsEntity::getPsId, psId)
            );
            if (ObjectUtil.isNull(entity)) {
                return ListUtil.empty();
            }
            return ListUtil.toList(entity.getProducts().split(","));
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PLUGINSERVER_ERROR,
                    String.format("failed to query products of plugin server %s", psId),
                    e
            );
        }
    }

    @Override
    public List<String> getDomainsServingOfPluginServer(String psId) {
        try {
            PluginServerObjectsEntity entity = pluginServerObjectsMapper.selectOne(
                    new LambdaQueryWrapper<PluginServerObjectsEntity>()
                            .select(ListUtil.of(PluginServerObjectsEntity::getDomains))
                            .eq(PluginServerObjectsEntity::getPsId, psId)
            );
            if (ObjectUtil.isNull(entity)) {
                return ListUtil.empty();
            }
            return ListUtil.toList(entity.getDomains().split(","));
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PLUGINSERVER_ERROR,
                    String.format("failed to query domains of plugin server %s", psId),
                    e
            );
        }
    }

    @Override
    public PluginServerInfo getPluginServerInfo(String psId) {
        try {
            PluginServerObjectsEntity entity = pluginServerObjectsMapper.selectOne(
                    new LambdaQueryWrapper<PluginServerObjectsEntity>()
                            .select(ListUtil.of(
                                    PluginServerObjectsEntity::getProducts,
                                    PluginServerObjectsEntity::getDomains,
                                    PluginServerObjectsEntity::getState)
                            ).eq(PluginServerObjectsEntity::getPsId, psId)
            );
            if (ObjectUtil.isNull(entity)) {
                return null;
            }
            return new PluginServerInfo(
                    entity.getState(),
                    StrUtil.split(entity.getProducts(), ","),
                    StrUtil.split(entity.getDomains(), ",")
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PLUGINSERVER_ERROR,
                    String.format("failed to query products of plugin server %s", psId),
                    e
            );
        }
    }

    @Override
    public Lock getHeartbeatLock(String psId) {
        return redisson.getLock(getHeartbeatLockKey(psId));
    }

    private static String getHeartbeatLockKey(String psId) {
        return String.format("%s%s", PLUGIN_SERVER_HEARTBEAT_LOCK_PREFIX, psId);
    }
}
