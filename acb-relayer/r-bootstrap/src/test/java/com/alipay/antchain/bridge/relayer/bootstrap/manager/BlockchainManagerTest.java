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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alipay.antchain.bridge.pluginserver.service.CrossChainServiceGrpc;
import com.alipay.antchain.bridge.relayer.bootstrap.TestBase;
import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.Constants;
import com.alipay.antchain.bridge.relayer.commons.constant.OnChainServiceStatusEnum;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import com.alipay.antchain.bridge.relayer.commons.model.DomainCertWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.PluginServerDO;
import com.alipay.antchain.bridge.relayer.core.manager.bbc.IBBCPluginManager;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.core.manager.blockchain.IBlockchainManager;
import com.alipay.antchain.bridge.relayer.dal.repository.IBlockchainRepository;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class BlockchainManagerTest extends TestBase {

    @Resource
    private IBlockchainManager blockchainManager;

    @Resource
    private IBCDNSManager bcdnsManager;

    @Resource
    private IBlockchainRepository blockchainRepository;

    @Resource
    private IBBCPluginManager bbcPluginManager;

    @Mock
    public CrossChainServiceGrpc.CrossChainServiceBlockingStub crossChainServiceBlockingStub;

    public MockedStatic<CrossChainServiceGrpc> mockedStaticCrossChainServiceGrpc = Mockito.mockStatic(CrossChainServiceGrpc.class);

    @Test
    public void testAddBlockchain() {

        Assert.assertTrue(blockchainManager.hasBlockchain(antChainDotComDomain));

        BlockchainMeta blockchainMeta = blockchainManager.getBlockchainMetaByDomain(antChainDotComDomain);
        Assert.assertEquals(antChainDotComProduct, blockchainMeta.getProduct());
        Assert.assertEquals(antChainDotComBlockchainId, blockchainMeta.getBlockchainId());
        Assert.assertEquals(PS_ID, blockchainMeta.getPluginServerId());

        Assert.assertNotNull(blockchainManager.getBlockchainMeta(antChainDotComProduct, antChainDotComBlockchainId));
    }

    @Test
    public void testUpdateBlockchainAnchor() throws IOException {

        Assert.assertTrue(blockchainManager.hasBlockchain(antChainDotComDomain));

        BlockchainMeta blockchainMeta = blockchainManager.getBlockchainMetaByDomain(antChainDotComDomain);
        String amAddr = "{\\\"evm\\\":\\\"AM_EVM_CONTRACT_bccb26ef-4179-42cf-aabb-082f83c4082e\\\", \\\"wasm\\\":\\\"AM_WASM_CONTRACT_9643c28d-6aac-4ef8-b7a7-edc81a402442\\\"}";
        blockchainMeta.getProperties().setAmClientContractAddress(amAddr);
        String clientConfig = JSONObject.toJSONString(blockchainMeta.getProperties(), SerializerFeature.PrettyFormat);
        if (!JSONUtil.isTypeJSON(clientConfig.trim())) {
            clientConfig = new String(Files.readAllBytes(Paths.get(clientConfig)));
        }

        blockchainManager.updateBlockchain(
                antChainDotComProduct,
                antChainDotComBlockchainId,
                "",
                blockchainMeta.getAlias(),
                blockchainMeta.getDesc(),
                JSONObject.parseObject(clientConfig)
                        .entrySet().stream().collect(
                                Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> entry.getValue().toString()
                                )
                        )
        );

        blockchainMeta = blockchainManager.getBlockchainMetaByDomain(antChainDotComDomain);

        Assert.assertNotNull(blockchainMeta);
        Assert.assertEquals(amAddr, blockchainMeta.getProperties().getAmClientContractAddress());
        Assert.assertEquals(antChainDotComProduct, blockchainMeta.getProduct());
        Assert.assertEquals(antChainDotComBlockchainId, blockchainMeta.getBlockchainId());
        Assert.assertEquals(PS_ID, blockchainMeta.getPluginServerId());
    }

    @Test
    public void testUpdateBlockchainProperty() {

        blockchainManager.updateBlockchainProperty(
                antChainDotComProduct,
                antChainDotComBlockchainId,
                Constants.AM_SERVICE_STATUS,
                OnChainServiceStatusEnum.INIT.name()
        );
        BlockchainMeta blockchainMeta = blockchainManager.getBlockchainMeta(antChainDotComProduct, antChainDotComBlockchainId);
        Assert.assertNotNull(blockchainMeta);
        Assert.assertEquals(OnChainServiceStatusEnum.INIT, blockchainMeta.getProperties().getAmServiceStatus());
    }

    @Test
    public void testDeployAMClientContract() {
        blockchainManager.deployAMClientContract(antChainDotComProduct, antChainDotComBlockchainId);
        BlockchainMeta blockchainMeta = blockchainManager.getBlockchainMeta(antChainDotComProduct, antChainDotComBlockchainId);
        Assert.assertEquals(
                blockchainProperties1.getBbcContext().getAuthMessageContract().getContractAddress(),
                blockchainMeta.getProperties().getAmClientContractAddress()
        );
    }

    @Test
    public void testStartBlockchainAnchor() {
        blockchainManager.startBlockchainAnchor(antChainDotComProduct, antChainDotComBlockchainId);
        BlockchainMeta blockchainMeta = blockchainManager.getBlockchainMeta(antChainDotComProduct, antChainDotComBlockchainId);
        Assert.assertEquals(
                BlockchainStateEnum.RUNNING,
                blockchainMeta.getProperties().getAnchorRuntimeStatus()
        );
    }

    @Test
    public void testStopBlockchainAnchor() {
        blockchainManager.stopBlockchainAnchor(antChainDotComProduct, antChainDotComBlockchainId);
        BlockchainMeta blockchainMeta = blockchainManager.getBlockchainMeta(antChainDotComProduct, antChainDotComBlockchainId);
        Assert.assertEquals(
                BlockchainStateEnum.STOPPED,
                blockchainMeta.getProperties().getAnchorRuntimeStatus()
        );
    }

    @Test
    public void testGetAllServingBlockchains() {
        initCatChainDotCom();
        blockchainManager.startBlockchainAnchor(antChainDotComProduct, antChainDotComBlockchainId);
        blockchainManager.startBlockchainAnchor(catChainDotComProduct, catChainDotComBlockchainId);

        List<BlockchainMeta> blockchainMetas = blockchainManager.getAllServingBlockchains();
        Assert.assertEquals(2, blockchainMetas.size());

        blockchainManager.stopBlockchainAnchor(antChainDotComProduct, antChainDotComBlockchainId);

        blockchainMetas = blockchainManager.getAllServingBlockchains();
        Assert.assertEquals(1, blockchainMetas.size());
    }

    @Test
    public void testGetAllStoppedBlockchains() {
        initCatChainDotCom();
        blockchainManager.startBlockchainAnchor(antChainDotComProduct, antChainDotComBlockchainId);
        blockchainManager.startBlockchainAnchor(catChainDotComProduct, catChainDotComBlockchainId);

        List<BlockchainMeta> blockchainMetas = blockchainManager.getAllStoppedBlockchains();
        Assert.assertEquals(0, blockchainMetas.size());

        blockchainManager.stopBlockchainAnchor(antChainDotComProduct, antChainDotComBlockchainId);

        blockchainMetas = blockchainManager.getAllStoppedBlockchains();
        Assert.assertEquals(1, blockchainMetas.size());
    }

    private void initDomain() {
        blockchainRepository.saveDomainCert(new DomainCertWrapper(antchainDotCommCert));
    }

    @Before
    public void initAntChainDotCom() {
        initDomain();

        initBaseBBCMock(crossChainServiceBlockingStub, mockedStaticCrossChainServiceGrpc);

        PluginServerDO.PluginServerProperties properties = new PluginServerDO.PluginServerProperties();
        properties.setPluginServerCert(psCert);
        bbcPluginManager.registerPluginServer(PS_ID, PS_ADDR, properties.toString());

        Mockito.when(
                blockchainMetaCache.containsKey(Mockito.anyString())
        ).thenReturn(false);

        Map<String, String> clientConfig = new HashMap<>();
        clientConfig.put(Constants.HETEROGENEOUS_BBC_CONTEXT, JSON.toJSONString(blockchainProperties1.getBbcContext()));
        bcdnsManager.bindDomainCertWithBlockchain(antChainDotComDomain, antChainDotComProduct, antChainDotComBlockchainId);
        blockchainManager.addBlockchain(
                antChainDotComProduct,
                antChainDotComBlockchainId,
                PS_ID,
                "", "",
                clientConfig
        );
    }

    @After
    public void clearMock() {
        mockedStaticCrossChainServiceGrpc.close();
    }

    private void initCatChainDotCom() {
        blockchainRepository.saveDomainCert(new DomainCertWrapper(catchainDotCommCert));

        Map<String, String> clientConfig = new HashMap<>();
        clientConfig.put(Constants.HETEROGENEOUS_BBC_CONTEXT, JSON.toJSONString(blockchainProperties2.getBbcContext()));
        bcdnsManager.bindDomainCertWithBlockchain(catChainDotComDomain, catChainDotComProduct, catChainDotComBlockchainId);
        blockchainManager.addBlockchain(
                catChainDotComProduct,
                catChainDotComBlockchainId,
                PS_ID,
                "", "",
                clientConfig
        );
    }
}
