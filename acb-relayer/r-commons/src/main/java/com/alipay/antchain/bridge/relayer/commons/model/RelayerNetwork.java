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

package com.alipay.antchain.bridge.relayer.commons.model;

import java.util.Map;

import cn.hutool.core.map.MapUtil;
import com.alipay.antchain.bridge.relayer.commons.constant.DomainRouterSyncStateEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Relayer网络
 */
@Getter
@Setter
public class RelayerNetwork {

    private String networkId;

    private Map<String, DomainRouterItem> networkItemTable = MapUtil.newHashMap();

    public RelayerNetwork(String networkId) {
        this.networkId = networkId;
    }

    public void addItem(String domain, String nodeId) {
        this.networkItemTable.put(domain, new DomainRouterItem(nodeId));
    }

    public void addItem(String domain, String nodeId, DomainRouterSyncStateEnum syncState) {
        this.networkItemTable.put(domain, new DomainRouterItem(nodeId, syncState));
    }

    public void addItem(Map<String, DomainRouterItem> table) {
        this.networkItemTable.putAll(table);
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class DomainRouterItem {

        private String nodeId;

        private DomainRouterSyncStateEnum syncState;

        public DomainRouterItem(String nodeId) {
            this.nodeId = nodeId;
            this.syncState = DomainRouterSyncStateEnum.INIT;
        }

        public DomainRouterItem(String nodeId, String syncState) {
            this.nodeId = nodeId;
            this.syncState = DomainRouterSyncStateEnum.parseFromValue(syncState);
        }
    }
}
