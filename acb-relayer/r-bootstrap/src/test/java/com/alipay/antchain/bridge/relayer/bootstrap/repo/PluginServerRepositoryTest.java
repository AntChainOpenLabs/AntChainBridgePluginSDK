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

import java.nio.charset.StandardCharsets;

import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import com.alipay.antchain.bridge.relayer.bootstrap.TestBase;
import com.alipay.antchain.bridge.relayer.commons.constant.PluginServerStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.PluginServerDO;
import com.alipay.antchain.bridge.relayer.commons.model.PluginServerInfo;
import com.alipay.antchain.bridge.relayer.dal.repository.IPluginServerRepository;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class PluginServerRepositoryTest extends TestBase {

    public static PluginServerDO pluginServerDO;

    @BeforeClass
    public static void setUp() throws Exception {
        pluginServerDO = new PluginServerDO();
        pluginServerDO.setPsId(testchain1Meta.getPluginServerId());
        pluginServerDO.setAddress("localhost:9090");
        pluginServerDO.setState(PluginServerStateEnum.INIT);
        PluginServerDO.PluginServerProperties properties = new PluginServerDO.PluginServerProperties();
        properties.setPluginServerCert(FileUtil.readString("node_keys/ps/relayer.crt", StandardCharsets.UTF_8));
        pluginServerDO.setProperties(properties);
    }

    @Resource
    private IPluginServerRepository pluginServerRepository;

    @Test
    public void testInsertNewPluginServer() {
        pluginServerRepository.insertNewPluginServer(pluginServerDO);
    }

    @Test
    public void testUpdatePluginServerInfo() {
        savePS();

        pluginServerRepository.updatePluginServerInfo(
                pluginServerDO.getPsId(),
                new PluginServerInfo(
                        PluginServerStateEnum.READY,
                        ListUtil.toList(testchain1Meta.getProduct()),
                        ListUtil.toList(antchainSubject.getDomainName().getDomain())
                )
        );

        PluginServerInfo pluginServerInfo = pluginServerRepository.getPluginServerInfo(pluginServerDO.getPsId());
        Assert.assertNotNull(pluginServerInfo);
        Assert.assertEquals(
                PluginServerStateEnum.READY,
                pluginServerInfo.getState()
        );
        Assert.assertEquals(
                testchain1Meta.getProduct(),
                pluginServerInfo.getProducts().get(0)
        );
        Assert.assertEquals(
                antchainSubject.getDomainName().getDomain(),
                pluginServerInfo.getDomains().get(0)
        );
    }

    private void savePS() {
        pluginServerRepository.insertNewPluginServer(pluginServerDO);
    }
}
