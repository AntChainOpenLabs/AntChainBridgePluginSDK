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

package com.alipay.antchain.bridge.pluginserver;

import cn.hutool.core.collection.ListUtil;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.pluginserver.managementservice.*;
import com.alipay.antchain.bridge.pluginserver.server.CrossChainServiceImpl;
import com.alipay.antchain.bridge.pluginserver.server.PluginManagementServiceImpl;
import com.alipay.antchain.bridge.pluginserver.server.ResponseBuilder;
import com.alipay.antchain.bridge.pluginserver.server.exception.ServerErrorCodeEnum;
import com.alipay.antchain.bridge.pluginserver.service.CallBBCRequest;
import com.alipay.antchain.bridge.pluginserver.service.CallBBCResponse;
import com.alipay.antchain.bridge.pluginserver.service.Response;
import com.alipay.antchain.bridge.pluginserver.service.StartUpRequest;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

@SpringBootTest(classes = AntChainBridgePluginServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AntChainBridgePluginManagementApplicationTests {

    private static final String DEFAULT_PRODUCT = "testchain";
    private static final String TEST_PRODUCT = "testchain1";
    private static final String TEST_DOMAIN = "domain";

    private static final String TEST_PLUGIN_PATH = Paths.get("src/test/resources/testPlugins/plugin-testchain1-0.1-SNAPSHOT-plugin.jar").toAbsolutePath().toString();

    @Autowired
    private PluginManagementServiceImpl pluginManagementService;

    @Autowired
    private CrossChainServiceImpl crossChainService;

    private StreamObserver<ManageResponse> mngResponseStreamObserver;

    private StreamObserver<Response> responseStreamObserver;

    @Test
    @DirtiesContext
    public void testManageLoadAndStartPluginsReq() {
        PluginManageRequest pluginManageRequest;

        // 1. load plugins（接口可以正常调用，但是会打印重复加载的异常信息，这是因为服务启动时默认已经执行过loadplugins）
        pluginManageRequest = PluginManageRequest.newBuilder()
                .setType(PluginManageRequest.Type.LOAD_PLUGINS).build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.managePlugin(pluginManageRequest, mngResponseStreamObserver);
        Mockito.verify(mngResponseStreamObserver).onNext(ResponseBuilder.buildPluginManageSuccessResp(PluginManageResp.newBuilder()));
        Mockito.verify(mngResponseStreamObserver).onCompleted();

        // 2. start plugins
        pluginManageRequest = PluginManageRequest.newBuilder()
                .setType(PluginManageRequest.Type.START_PLUGINS).build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.managePlugin(pluginManageRequest, mngResponseStreamObserver);
        Mockito.verify(mngResponseStreamObserver).onNext(ResponseBuilder.buildPluginManageSuccessResp(PluginManageResp.newBuilder()));
        Mockito.verify(mngResponseStreamObserver).onCompleted();
    }

    @Test
    @DirtiesContext
    public void testManagePluginReq() {
        PluginManageRequest pluginManageRequest;

        // 1. load plugin
        pluginManageRequest = PluginManageRequest.newBuilder()
                .setType(PluginManageRequest.Type.LOAD_PLUGIN)
                .setPath(TEST_PLUGIN_PATH).build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.managePlugin(pluginManageRequest, mngResponseStreamObserver);
        Mockito.verify(mngResponseStreamObserver).onNext(ResponseBuilder.buildPluginManageSuccessResp(PluginManageResp.newBuilder()));
        Mockito.verify(mngResponseStreamObserver).onCompleted();

        // 2. start plugin
        pluginManageRequest = PluginManageRequest.newBuilder()
                .setType(PluginManageRequest.Type.START_PLUGIN)
                .setPath(TEST_PLUGIN_PATH).build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.managePlugin(pluginManageRequest, mngResponseStreamObserver);
        Mockito.verify(mngResponseStreamObserver).onNext(ResponseBuilder.buildPluginManageSuccessResp(PluginManageResp.newBuilder()));
        Mockito.verify(mngResponseStreamObserver).onCompleted();

        // 3. stop plugin
        pluginManageRequest = PluginManageRequest.newBuilder()
                .setType(PluginManageRequest.Type.STOP_PLUGIN)
                .setProduct(TEST_PRODUCT).build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.managePlugin(pluginManageRequest, mngResponseStreamObserver);
        Mockito.verify(mngResponseStreamObserver).onNext(ResponseBuilder.buildPluginManageSuccessResp(PluginManageResp.newBuilder()));
        Mockito.verify(mngResponseStreamObserver).onCompleted();

        // 4. start plugin from stop
        pluginManageRequest = PluginManageRequest.newBuilder()
                .setType(PluginManageRequest.Type.START_PLUGIN_FROM_STOP)
                .setProduct(TEST_PRODUCT).build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.managePlugin(pluginManageRequest, mngResponseStreamObserver);
        Mockito.verify(mngResponseStreamObserver).onNext(ResponseBuilder.buildPluginManageSuccessResp(PluginManageResp.newBuilder()));
        Mockito.verify(mngResponseStreamObserver).onCompleted();

        // 5. stop plugin and reload plugin
        // 5.1 stop
        pluginManageRequest = PluginManageRequest.newBuilder()
                .setType(PluginManageRequest.Type.STOP_PLUGIN)
                .setProduct(TEST_PRODUCT).build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.managePlugin(pluginManageRequest, mngResponseStreamObserver);
        Mockito.verify(mngResponseStreamObserver).onNext(ResponseBuilder.buildPluginManageSuccessResp(PluginManageResp.newBuilder()));
        Mockito.verify(mngResponseStreamObserver).onCompleted();
        // 5.2 reload plugin
        pluginManageRequest = PluginManageRequest.newBuilder()
                .setType(PluginManageRequest.Type.RELOAD_PLUGIN)
                .setProduct(TEST_PRODUCT).build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.managePlugin(pluginManageRequest, mngResponseStreamObserver);
        Mockito.verify(mngResponseStreamObserver).onNext(ResponseBuilder.buildPluginManageSuccessResp(PluginManageResp.newBuilder()));
        Mockito.verify(mngResponseStreamObserver).onCompleted();

        // 6. stop plugin and reload plugin in new path
        // 6.1 stop plugin
        pluginManageRequest = PluginManageRequest.newBuilder()
                .setType(PluginManageRequest.Type.STOP_PLUGIN)
                .setProduct(TEST_PRODUCT).build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.managePlugin(pluginManageRequest, mngResponseStreamObserver);
        Mockito.verify(mngResponseStreamObserver).onNext(ResponseBuilder.buildPluginManageSuccessResp(PluginManageResp.newBuilder()));
        Mockito.verify(mngResponseStreamObserver).onCompleted();

        // 6.2 reload plugin in new path
        pluginManageRequest = PluginManageRequest.newBuilder()
                .setType(PluginManageRequest.Type.RELOAD_PLUGIN_IN_NEW_PATH)
                .setProduct(TEST_PRODUCT)
                .setPath(TEST_PLUGIN_PATH).build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.managePlugin(pluginManageRequest, mngResponseStreamObserver);
        Mockito.verify(mngResponseStreamObserver).onNext(ResponseBuilder.buildPluginManageSuccessResp(PluginManageResp.newBuilder()));
        Mockito.verify(mngResponseStreamObserver).onCompleted();
    }

    @Test
    @DirtiesContext
    public void testPluginQueryReq() {
        PluginManageRequest pluginManageRequest;

        // 1. All plugins in the default path are automatically loaded when the service starts

        // 2. has plugin
        HasPluginsRequest hasPluginsRequest = HasPluginsRequest.newBuilder()
                .addAllProducts(ListUtil.toList(DEFAULT_PRODUCT, TEST_PRODUCT)).build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.hasPlugins(hasPluginsRequest, mngResponseStreamObserver);
        Mockito.verify(mngResponseStreamObserver).onNext(ResponseBuilder.buildHasPluginsResp(
                HasPluginsResp.newBuilder().putAllResults(new HashMap<String, Boolean>() {{
                    put(DEFAULT_PRODUCT, true);
                    put(TEST_PRODUCT, false);
                }})
        ));
        Mockito.verify(mngResponseStreamObserver).onCompleted();

        // 3. load and start test plugin
        // 3.1 load
        pluginManageRequest = PluginManageRequest.newBuilder()
                .setType(PluginManageRequest.Type.LOAD_PLUGIN)
                .setPath(TEST_PLUGIN_PATH).build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.managePlugin(pluginManageRequest, mngResponseStreamObserver);
        Mockito.verify(mngResponseStreamObserver).onNext(ResponseBuilder.buildPluginManageSuccessResp(PluginManageResp.newBuilder()));
        Mockito.verify(mngResponseStreamObserver).onCompleted();
        // 3.2 start
        pluginManageRequest = PluginManageRequest.newBuilder()
                .setType(PluginManageRequest.Type.START_PLUGIN)
                .setPath(TEST_PLUGIN_PATH).build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.managePlugin(pluginManageRequest, mngResponseStreamObserver);
        Mockito.verify(mngResponseStreamObserver).onNext(ResponseBuilder.buildPluginManageSuccessResp(PluginManageResp.newBuilder()));
        Mockito.verify(mngResponseStreamObserver).onCompleted();

        // 4. all plugin
        AllPluginsRequest allPluginsRequest = AllPluginsRequest.newBuilder().build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.allPlugins(allPluginsRequest, mngResponseStreamObserver);
        Mockito.verify(mngResponseStreamObserver).onNext(ResponseBuilder.buildAllPluginsResp(
                AllPluginsResp.newBuilder().addAllProducts(ListUtil.toList(TEST_PRODUCT, DEFAULT_PRODUCT).stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList()))
        ));
        Mockito.verify(mngResponseStreamObserver).onCompleted();

        // 5. has domain
        HasDomainsRequest hasDomainsRequest = HasDomainsRequest.newBuilder()
                .addAllDomains(ListUtil.toList(TEST_DOMAIN)).build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.hasDomains(hasDomainsRequest, mngResponseStreamObserver);
        Mockito.verify(mngResponseStreamObserver).onNext(ResponseBuilder.buildHasDomainsResp(
                HasDomainsResp.newBuilder().putAllResults(new HashMap<String, Boolean>() {{
                    put(TEST_DOMAIN, false);
                }})
        ));
        Mockito.verify(mngResponseStreamObserver).onCompleted();

        // 6. all domain
        AllDomainsRequest allDomainsRequest = AllDomainsRequest.newBuilder().build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.allDomains(allDomainsRequest, mngResponseStreamObserver);
        Mockito.verify(mngResponseStreamObserver).onNext(ResponseBuilder.buildAllDomainsResp(
                AllDomainsResp.newBuilder().addAllDomains(ListUtil.toList())
        ));
        Mockito.verify(mngResponseStreamObserver).onCompleted();

        // 7. create service with test domain
        AbstractBBCContext mockCtx = AntChainBridgePluginServerApplicationTests.mockInitCtx();
        CallBBCRequest callBBCRequest = CallBBCRequest.newBuilder()
                .setProduct(DEFAULT_PRODUCT)
                .setDomain(TEST_DOMAIN)
                .setStartUpReq(StartUpRequest.newBuilder().setRawContext(ByteString.copyFrom(mockCtx.encodeToBytes()))).build();
        responseStreamObserver = Mockito.mock(StreamObserver.class);
        crossChainService.bbcCall(callBBCRequest, responseStreamObserver);
        Mockito.verify(responseStreamObserver).onNext(ResponseBuilder.buildBBCSuccessResp(CallBBCResponse.newBuilder()));
        Mockito.verify(responseStreamObserver).onCompleted();

        // 8. all domain
        allDomainsRequest = AllDomainsRequest.newBuilder().build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.allDomains(allDomainsRequest, mngResponseStreamObserver);
        Mockito.verify(mngResponseStreamObserver).onNext(ResponseBuilder.buildAllDomainsResp(
                AllDomainsResp.newBuilder().addAllDomains(ListUtil.toList(TEST_DOMAIN).stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList()))
        ));
        Mockito.verify(mngResponseStreamObserver).onCompleted();
    }

    @Test
    @DirtiesContext
    public void testRestartBBC() {
        // 1. failed to restart a domain or product not exist
        RestartBBCRequest request = RestartBBCRequest.newBuilder()
                .setDomain("domain")
                .setProduct("product")
                .build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.restartBBC(request, mngResponseStreamObserver);
        Mockito.verify(mngResponseStreamObserver)
                .onNext(ResponseBuilder.buildFailManageResp(ServerErrorCodeEnum.MANAGE_RESTART_BBC_ERROR, "product not found"));
        Mockito.verify(mngResponseStreamObserver).onCompleted();

        // 2. green case
        PluginManageRequest pluginManageRequest = PluginManageRequest.newBuilder()
                .setType(PluginManageRequest.Type.LOAD_PLUGIN)
                .setPath(TEST_PLUGIN_PATH).build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.managePlugin(pluginManageRequest, mngResponseStreamObserver);
        pluginManageRequest = PluginManageRequest.newBuilder()
                .setType(PluginManageRequest.Type.START_PLUGIN)
                .setPath(TEST_PLUGIN_PATH).build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.managePlugin(pluginManageRequest, mngResponseStreamObserver);

        responseStreamObserver = Mockito.mock(StreamObserver.class);
        crossChainService.bbcCall(
                CallBBCRequest.newBuilder()
                        .setDomain("domain")
                        .setProduct("testchain")
                        .setStartUpReq(StartUpRequest.newBuilder().setRawContext(ByteString.copyFromUtf8("{\"raw_conf\": \"\"}")))
                        .build(),
                responseStreamObserver
        );

        request = RestartBBCRequest.newBuilder()
                .setDomain("domain")
                .setProduct("testchain")
                .build();
        mngResponseStreamObserver = Mockito.mock(StreamObserver.class);
        pluginManagementService.restartBBC(request, mngResponseStreamObserver);

        Mockito.verify(mngResponseStreamObserver).onNext(ResponseBuilder.buildRestartBBCResp(RestartBBCResp.newBuilder()));
        Mockito.verify(mngResponseStreamObserver).onCompleted();
    }
}
