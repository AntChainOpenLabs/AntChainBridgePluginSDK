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

package com.alipay.antchain.bridge.relayer.bootstrap.manager;

import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import com.alipay.antchain.bridge.relayer.bootstrap.TestBase;
import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.CrossChainChannelDO;
import com.alipay.antchain.bridge.relayer.commons.constant.CrossChainChannelStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.DomainRouterSyncStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNetwork;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNodeInfo;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerNetworkManager;
import com.alipay.antchain.bridge.relayer.core.types.network.IRelayerClientPool;
import com.alipay.antchain.bridge.relayer.core.types.network.RelayerClient;
import com.alipay.antchain.bridge.relayer.dal.entities.DomainCertEntity;
import com.alipay.antchain.bridge.relayer.dal.mapper.DomainCertMapper;
import com.alipay.antchain.bridge.relayer.dal.repository.IBCDNSRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.IBlockchainRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.ISystemConfigRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;

public class RelayerNetworkManagerTest extends TestBase {

    @Resource
    private IRelayerNetworkManager relayerNetworkManager;

    @Resource
    private IBlockchainRepository blockchainRepository;

    @Resource
    private IBCDNSRepository bcdnsRepository;

    @Resource
    private DomainCertMapper domainCertMapper;

    @Resource
    private ISystemConfigRepository systemConfigRepository;

    @MockBean
    private IRelayerClientPool relayerClientPool;

    @Value("${relayer.network.node.sig_algo:KECCAK256_WITH_SECP256K1}")
    private SignAlgoEnum localNodeSigAlgo;

    @Value("${relayer.network.node.local_endpoints:}")
    private String localEndpoints;

    @Test
    public void testGetRelayerNodeInfo() {
        RelayerNodeInfo relayerNodeInfo = relayerNetworkManager.getRelayerNodeInfo();
        Assert.assertNotNull(relayerNodeInfo);
        Assert.assertEquals(
                RelayerNodeInfo.calculateNodeId(relayerCert),
                relayerNodeInfo.getNodeId()
        );
        Assert.assertEquals(
                1,
                relayerNodeInfo.getDomains().size()
        );
        Assert.assertEquals(
                antChainDotComDomain,
                relayerNodeInfo.getDomains().get(0)
        );
        Assert.assertEquals(
                localNodeSigAlgo,
                relayerNodeInfo.getSigAlgo()
        );
        Assert.assertEquals(
                1,
                relayerNodeInfo.getEndpoints().size()
        );
        Assert.assertEquals(
                systemConfigRepository.getLocalEndpoints().get(0),
                relayerNodeInfo.getEndpoints().get(0)
        );
    }

    @Test
    public void testGetRelayerNodeInfoWithContent() {
        RelayerNodeInfo relayerNodeInfo = relayerNetworkManager.getRelayerNodeInfoWithContent();
        Assert.assertNotNull(relayerNodeInfo.getRelayerBlockchainContent());
        Assert.assertEquals(1, relayerNodeInfo.getRelayerBlockchainContent().getRelayerBlockchainInfoTrie().size());
        Assert.assertEquals(2, relayerNodeInfo.getRelayerBlockchainContent().getTrustRootCertTrie().size());
        Assert.assertNotNull(relayerNodeInfo.getRelayerBlockchainContent().getRelayerBlockchainInfo(antChainDotComDomain));
        Assert.assertEquals(
                2,
                relayerNodeInfo.getRelayerBlockchainContent().getRelayerBlockchainInfo(antChainDotComDomain).getDomainSpaceChain().size()
        );
    }

    @Test
    public void testAddRelayerNode() {
        RelayerNodeInfo remoteNodeInfo = relayerNetworkManager.getRelayerNodeInfo();
        relayerNetworkManager.addRelayerNode(remoteNodeInfo);
        RelayerNodeInfo relayerNodeInfo = relayerNetworkManager.getRelayerNode(remoteNodeInfo.getNodeId(), true);
        Assert.assertNotNull(relayerNodeInfo);
        Assert.assertEquals(
                remoteNodeInfo.getNodeId(),
                relayerNodeInfo.getNodeId()
        );
    }

    @Test
    public void testAddRelayerNodeWithQueryRelayer() {
        Mockito.when(relayerNodeInfoCache.containsKey(Mockito.any())).thenReturn(false);
        RelayerNodeInfo remoteNodeInfo = relayerNetworkManager.getRelayerNodeInfo();
        remoteNodeInfo.setRelayerCertId(null);
        remoteNodeInfo.setRelayerCrossChainCertificate(null);
        remoteNodeInfo.setRelayerCredentialSubject(null);
        remoteNodeInfo.setDomains(ListUtil.toList());

        RelayerNodeInfo temp = relayerNetworkManager.getRelayerNodeInfo();
        RelayerClient mockRelayerClient = Mockito.mock(RelayerClient.class);
        Mockito.when(mockRelayerClient.getRelayerNodeInfo()).thenReturn(temp);
        Mockito.when(relayerClientPool.getRelayerClient(Mockito.any(), Mockito.any())).thenReturn(mockRelayerClient);

        relayerNetworkManager.addRelayerNode(remoteNodeInfo);
        RelayerNodeInfo relayerNodeInfo = relayerNetworkManager.getRelayerNode(remoteNodeInfo.getNodeId(), true);
        Assert.assertNotNull(relayerNodeInfo);
        Assert.assertEquals(
                remoteNodeInfo.getNodeId(),
                relayerNodeInfo.getNodeId()
        );
        Assert.assertNotNull(relayerNodeInfo.getRelayerCertId());
        Assert.assertNotNull(relayerNodeInfo.getRelayerCrossChainCertificate());
        Assert.assertNotNull(relayerNodeInfo.getRelayerCredentialSubject());
        Assert.assertFalse(relayerNodeInfo.getDomains().isEmpty());
    }

    @Test
    public void testValidateAndSaveBlockchainContent() {
        Mockito.when(relayerNodeInfoCache.containsKey(Mockito.any())).thenReturn(false);
        Mockito.when(relayerNetworkItemCache.containsKey(Mockito.any())).thenReturn(false);
        RelayerNodeInfo remoteNodeInfo = relayerNetworkManager.getRelayerNodeInfoWithContent();
        relayerNetworkManager.addRelayerNode(remoteNodeInfo);

        relayerNetworkManager.validateAndSaveBlockchainContent(
                "1",
                remoteNodeInfo,
                remoteNodeInfo.getRelayerBlockchainContent(),
                true
        );

        RelayerNetwork relayerNetwork = relayerNetworkManager.getRelayerNetwork("1");
        Assert.assertEquals(1, relayerNetwork.getNetworkItemTable().size());
        Assert.assertTrue(relayerNetwork.getNetworkItemTable().containsKey(antChainDotComDomain));
        Assert.assertNotNull(relayerNetworkManager.getRelayerNodeInfoForDomain(antChainDotComDomain));
        Assert.assertEquals(
                remoteNodeInfo.getNodeId(),
                relayerNetworkManager.findRemoteRelayer(antChainDotComDomain)
        );

        RelayerNetwork.DomainRouterItem item = relayerNetworkManager.findNetworkItemByDomainName(antChainDotComDomain);
        Assert.assertEquals(
                remoteNodeInfo.getNodeId(),
                item.getNodeId()
        );
        Assert.assertEquals(
                DomainRouterSyncStateEnum.SYNC,
                item.getSyncState()
        );
    }

    @Test
    public void testCreateNewCrossChainChannel() {
        relayerNetworkManager.createNewCrossChainChannel(antChainDotComDomain, catChainDotComDomain, "test");

        CrossChainChannelDO crossChainChannelDO = relayerNetworkManager.getCrossChainChannel(antChainDotComDomain, catChainDotComDomain);
        Assert.assertEquals(
                "test",
                crossChainChannelDO.getRelayerNodeId()
        );
        Assert.assertEquals(
                CrossChainChannelStateEnum.CONNECTED,
                crossChainChannelDO.getState()
        );
        Assert.assertEquals(
                antChainDotComDomain,
                crossChainChannelDO.getLocalDomain()
        );
        Assert.assertEquals(
                catChainDotComDomain,
                crossChainChannelDO.getRemoteDomain()
        );
        Assert.assertTrue(relayerNetworkManager.hasCrossChainChannel(antChainDotComDomain, catChainDotComDomain));
    }

    @Before
    public void initBlockchainData() {
        testchain1Meta.getProperties().setAnchorRuntimeStatus(BlockchainStateEnum.RUNNING);
        blockchainRepository.saveBlockchainMeta(testchain1Meta);

        DomainCertEntity entity = new DomainCertEntity();
        entity.setBlockchainId(testchain1Meta.getBlockchainId());
        entity.setProduct(testchain1Meta.getProduct());
        entity.setDomain(antchainSubject.getDomainName().getDomain());
        entity.setDomainSpace(antchainSubject.getParentDomainSpace().getDomain());
        entity.setIssuerOid(antchainDotCommCert.getIssuer().encode());
        entity.setSubjectOid(antchainSubject.getApplicant().encode());
        entity.setDomainCert(antchainDotCommCert.encode());
        domainCertMapper.insert(entity);

        bcdnsRepository.saveBCDNSServiceDO(rootBcdnsServiceDO);
        bcdnsRepository.saveBCDNSServiceDO(dotComBcdnsServiceDO);
    }
}
