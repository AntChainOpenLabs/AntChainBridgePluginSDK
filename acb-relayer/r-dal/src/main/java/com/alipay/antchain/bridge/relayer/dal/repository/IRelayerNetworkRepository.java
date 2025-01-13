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
import java.util.Map;

import com.alipay.antchain.bridge.relayer.commons.model.CrossChainChannelDO;
import com.alipay.antchain.bridge.relayer.commons.constant.CrossChainChannelStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.DomainRouterSyncStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerHealthInfo;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNetwork;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNodeInfo;

public interface IRelayerNetworkRepository {

    void addNetworkItems(String networkId, Map<String, RelayerNetwork.DomainRouterItem> relayerNetworkItems);

    boolean deleteNetworkItem(String domain, String nodeId);

    RelayerNetwork.DomainRouterItem getNetworkItem(String networkId, String domain, String nodeId);

    RelayerNetwork.DomainRouterItem getNetworkItem(String domain);

    void addNetworkItem(String networkId, String domain, String nodeId, DomainRouterSyncStateEnum syncState);

    void updateNetworkItem(String networkId, String domain, String nodeId, DomainRouterSyncStateEnum syncState);

    Map<String, RelayerNetwork.DomainRouterItem> getNetworkItems(String networkId);

    boolean hasNetworkItem(String networkId, String domain, String nodeId);

    List<RelayerNetwork> getAllNetworks();

    RelayerNetwork getRelayerNetwork(String networkId);

    RelayerNetwork getRelayerNetworkByDomain(String domain);

    String getRelayerNodeIdForDomain(String domain);

    void addRelayerNode(RelayerNodeInfo nodeInfo);

    void updateRelayerNode(RelayerNodeInfo nodeInfo);

    void updateRelayerNodeProperty(String nodeId, String key, String value);

    RelayerNodeInfo getRelayerNode(String nodeId, boolean lock);

    RelayerNodeInfo getRelayerNodeByCertId(String relayerCertId, boolean lock);

    boolean hasRelayerNodeByCertId(String relayerCertId);

    boolean hasRelayerNode(String nodeId);

    List<RelayerHealthInfo> getAllRelayerHealthInfo();

    boolean hasCrossChainChannel(String localDomain, String remoteDomain);

    void addCrossChainChannel(CrossChainChannelDO crossChainChannelDO);

    void updateCrossChainChannel(CrossChainChannelDO crossChainChannelDO);

    CrossChainChannelDO getCrossChainChannel(String localDomain, String remoteDomain);

    void updateCrossChainChannelState(String localDomain, String remoteDomain, CrossChainChannelStateEnum state);
}
