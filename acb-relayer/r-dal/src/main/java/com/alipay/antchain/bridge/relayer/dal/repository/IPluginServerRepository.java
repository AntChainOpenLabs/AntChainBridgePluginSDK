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

package com.alipay.antchain.bridge.relayer.dal.repository;

import java.util.List;
import java.util.concurrent.locks.Lock;

import com.alipay.antchain.bridge.relayer.commons.constant.PluginServerStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.PluginServerDO;
import com.alipay.antchain.bridge.relayer.commons.model.PluginServerInfo;

public interface IPluginServerRepository {

    void insertNewPluginServer(PluginServerDO pluginServerDO);

    void updatePluginServerInfo(String psId, PluginServerInfo info);

    void deletePluginServer(PluginServerDO pluginServerDO);

    PluginServerDO getPluginServer(String psId);

    void updatePluginServerState(String psId, PluginServerStateEnum stateEnum);

    PluginServerStateEnum getPluginServerStateEnum(String psId);

    List<String> getProductsSupportedOfPluginServer(String psId);

    List<String> getDomainsServingOfPluginServer(String psId);

    PluginServerInfo getPluginServerInfo(String psId);

    Lock getHeartbeatLock(String psId);
}
