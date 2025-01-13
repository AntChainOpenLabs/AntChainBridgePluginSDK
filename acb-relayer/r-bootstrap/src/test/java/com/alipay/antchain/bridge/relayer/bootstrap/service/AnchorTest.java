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

package com.alipay.antchain.bridge.relayer.bootstrap.service;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.pluginserver.service.*;
import com.alipay.antchain.bridge.relayer.bootstrap.TestBase;
import com.alipay.antchain.bridge.relayer.commons.constant.Constants;
import com.alipay.antchain.bridge.relayer.commons.model.DomainCertWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.PluginServerDO;
import com.alipay.antchain.bridge.relayer.core.manager.bbc.IBBCPluginManager;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.core.manager.blockchain.IBlockchainManager;
import com.alipay.antchain.bridge.relayer.dal.repository.IBlockchainRepository;
import com.google.protobuf.ByteString;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@Ignore
public class AnchorTest extends TestBase {

    @Resource
    private IBlockchainRepository blockchainRepository;

    @Resource
    private IBBCPluginManager bbcPluginManager;

    @Resource
    private IBlockchainManager blockchainManager;

    @Resource
    private IBCDNSManager bcdnsManager;

    @Mock
    public CrossChainServiceGrpc.CrossChainServiceBlockingStub crossChainServiceBlockingStub;

    public MockedStatic<CrossChainServiceGrpc> mockedStaticCrossChainServiceGrpc = Mockito.mockStatic(CrossChainServiceGrpc.class);

//    @Test
//    public void test() throws Exception {
//        Thread.sleep(300_000);
//    }

    @Before
    public void initAntChainDotCom() {
        initDomain();

        initBaseBBCMock(crossChainServiceBlockingStub, mockedStaticCrossChainServiceGrpc);

        Mockito.when(crossChainServiceBlockingStub.bbcCall(Mockito.argThat(
                argument -> {
                    if (ObjectUtil.isNull(argument)) {
                        return false;
                    }
                    return argument.hasReadCrossChainMessagesByHeightReq();
                }
        ))).thenReturn(Response.newBuilder().setCode(0).setBbcResp(
                        CallBBCResponse.newBuilder().setReadCrossChainMessagesByHeightResp(
                                ReadCrossChainMessagesByHeightResponse.newBuilder()
                                        .addMessageList(
                                                CrossChainMessage.newBuilder()
                                                        .setType(CrossChainMessageType.AUTH_MSG)
                                                        .setProvableData(
                                                                ProvableLedgerData.newBuilder()
                                                                        .setHeight(currHeight.get())
                                                                        .setBlockHash(ByteString.copyFrom(RandomUtil.randomBytes(32)))
                                                                        .setTimestamp(System.currentTimeMillis())
                                                                        .setTxHash(ByteString.copyFrom(RandomUtil.randomBytes(32)))
                                                                        .setProof(ByteString.EMPTY)
                                                                        .setLedgerData(ByteString.EMPTY)
                                                                        .build()
                                                        ).setMessage(ByteString.copyFrom(authMessageV2.encode()))
                                                        .build()
                                        )
                        ).build()
                ).build()
        );

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
        blockchainManager.startBlockchainAnchor(antChainDotComProduct, antChainDotComBlockchainId);
    }

    private void initDomain() {
        blockchainRepository.saveDomainCert(new DomainCertWrapper(antchainDotCommCert));
    }

    @After
    public void clearMock() {
        mockedStaticCrossChainServiceGrpc.close();
    }
}
