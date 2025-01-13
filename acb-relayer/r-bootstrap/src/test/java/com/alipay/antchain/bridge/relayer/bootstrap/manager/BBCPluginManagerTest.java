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

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.pluginserver.service.CrossChainServiceGrpc;
import com.alipay.antchain.bridge.relayer.bootstrap.TestBase;
import com.alipay.antchain.bridge.relayer.commons.constant.PluginServerStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.PluginServerDO;
import com.alipay.antchain.bridge.relayer.commons.model.PluginServerInfo;
import com.alipay.antchain.bridge.relayer.core.manager.bbc.IBBCPluginManager;
import com.alipay.antchain.bridge.relayer.dal.repository.IPluginServerRepository;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class BBCPluginManagerTest extends TestBase {

    @Resource
    private IBBCPluginManager bbcPluginManager;

    @Resource
    private IPluginServerRepository pluginServerRepository;

    @Mock
    public CrossChainServiceGrpc.CrossChainServiceBlockingStub crossChainServiceBlockingStub;

    public MockedStatic<CrossChainServiceGrpc> mockedStaticCrossChainServiceGrpc = Mockito.mockStatic(CrossChainServiceGrpc.class);

    @Test
    public void testRegisterPluginServer() {
        PluginServerDO.PluginServerProperties properties = new PluginServerDO.PluginServerProperties();
        properties.setPluginServerCert(psCert);
        bbcPluginManager.registerPluginServer(PS_ID, PS_ADDR, properties.toString());

        Assert.assertNotNull(bbcPluginManager.getPluginServerClient(PS_ID));
        Assert.assertEquals(PluginServerStateEnum.READY, bbcPluginManager.getPluginServerState(PS_ID));
        PluginServerInfo pluginServerInfo = bbcPluginManager.getPluginServerInfo(PS_ID);
        Assert.assertNotNull(pluginServerInfo);
        Assert.assertEquals(PluginServerStateEnum.READY, pluginServerInfo.getState());
        System.out.println(StrUtil.join(",", pluginServerInfo.getDomains()));
        System.out.println(StrUtil.join(",", pluginServerInfo.getProducts()));
    }

    @Test
    public void testStartStopPluginServer() {
        pluginServerDO.setState(PluginServerStateEnum.STOP);
        pluginServerRepository.insertNewPluginServer(pluginServerDO);

        bbcPluginManager.startPluginServer(pluginServerDO.getPsId());
        Assert.assertEquals(
                PluginServerStateEnum.READY,
                bbcPluginManager.getPluginServerState(pluginServerDO.getPsId())
        );
        bbcPluginManager.stopPluginServer(pluginServerDO.getPsId());
        Assert.assertEquals(
                PluginServerStateEnum.STOP,
                bbcPluginManager.getPluginServerState(pluginServerDO.getPsId())
        );
        bbcPluginManager.forceStartPluginServer(pluginServerDO.getPsId());
        Assert.assertEquals(
                PluginServerStateEnum.READY,
                bbcPluginManager.getPluginServerState(pluginServerDO.getPsId())
        );

        pluginServerDO.setState(PluginServerStateEnum.INIT);
    }

    @Before
    public void initMock() {
        initBaseBBCMock(crossChainServiceBlockingStub, mockedStaticCrossChainServiceGrpc);
    }

    @After
    public void clearMock() {
        mockedStaticCrossChainServiceGrpc.close();
    }

}
