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

package com.alipay.antchain.bridge.relayer.bootstrap.repo;

import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.HexUtil;
import com.alipay.antchain.bridge.relayer.bootstrap.TestBase;
import com.alipay.antchain.bridge.relayer.commons.model.CrossChainChannelDO;
import com.alipay.antchain.bridge.relayer.commons.constant.CrossChainChannelStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.DomainRouterSyncStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNetwork;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNodeInfo;
import com.alipay.antchain.bridge.relayer.dal.repository.IRelayerNetworkRepository;
import org.junit.Assert;
import org.junit.Test;

public class RelayerNetworkRepositoryTest extends TestBase {

    public static RelayerNodeInfo remoteNodeInfo = new RelayerNodeInfo(
            relayerCert,
            relayerCert.getProof().getSigAlgo(),
            ListUtil.toList("https://0.0.0.0:8082"),
            ListUtil.toList(antChainDotComDomain)
    );

    @Resource
    private IRelayerNetworkRepository relayerNetworkRepository;

    @Test
    public void testAddNetworkItem() {
        relayerNetworkRepository.addNetworkItem("1", antChainDotComDomain, remoteNodeInfo.getNodeId(), DomainRouterSyncStateEnum.SYNC);

        Assert.assertTrue(relayerNetworkRepository.hasNetworkItem("1", antChainDotComDomain, remoteNodeInfo.getNodeId()));
        RelayerNetwork.DomainRouterItem item = relayerNetworkRepository.getNetworkItem(antChainDotComDomain);
        Assert.assertNotNull(item);
        Assert.assertEquals(remoteNodeInfo.getNodeId(), item.getNodeId());
    }

    @Test
    public void testUpdateNetworkItem() {
        relayerNetworkRepository.addNetworkItem("1", antChainDotComDomain, remoteNodeInfo.getNodeId(), DomainRouterSyncStateEnum.SYNC);

        relayerNetworkRepository.updateNetworkItem(
                "1",
                antChainDotComDomain,
                remoteNodeInfo.getNodeId(),
                DomainRouterSyncStateEnum.INIT
        );
        RelayerNetwork.DomainRouterItem item = relayerNetworkRepository.getNetworkItem(antChainDotComDomain);
        Assert.assertEquals(DomainRouterSyncStateEnum.INIT, item.getSyncState());
    }

    @Test
    public void testAddRelayerNode() {
        relayerNetworkRepository.addRelayerNode(remoteNodeInfo);

        Assert.assertTrue(relayerNetworkRepository.hasRelayerNode(remoteNodeInfo.getNodeId()));
        RelayerNodeInfo relayerNodeInfo = relayerNetworkRepository.getRelayerNode(remoteNodeInfo.getNodeId(), true);
        Assert.assertNotNull(relayerNodeInfo);
        Assert.assertEquals(remoteNodeInfo.getNodeId(), relayerNodeInfo.getNodeId());
        Assert.assertEquals(
                HexUtil.encodeHexStr(remoteNodeInfo.getEncode()),
                HexUtil.encodeHexStr(relayerNodeInfo.getEncode())
        );
    }

    @Test
    public void testAddCrossChainChannel() {
        relayerNetworkRepository.addCrossChainChannel(new CrossChainChannelDO(
                catChainDotComDomain,
                antChainDotComDomain,
                remoteNodeInfo.getNodeId(),
                CrossChainChannelStateEnum.CONNECTED
        ));

        Assert.assertTrue(relayerNetworkRepository.hasCrossChainChannel(catChainDotComDomain, antChainDotComDomain));
        CrossChainChannelDO crossChainChannelDO = relayerNetworkRepository.getCrossChainChannel(catChainDotComDomain, antChainDotComDomain);
        Assert.assertNotNull(crossChainChannelDO);
        Assert.assertEquals(remoteNodeInfo.getNodeId(), crossChainChannelDO.getRelayerNodeId());
        Assert.assertEquals(CrossChainChannelStateEnum.CONNECTED, crossChainChannelDO.getState());
    }
}
