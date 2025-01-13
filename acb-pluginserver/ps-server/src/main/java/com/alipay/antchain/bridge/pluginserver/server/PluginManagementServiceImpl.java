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

package com.alipay.antchain.bridge.pluginserver.server;

import java.util.Comparator;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import com.alipay.antchain.bridge.pluginserver.managementservice.*;
import com.alipay.antchain.bridge.pluginserver.pluginmanager.IPluginManagerWrapper;
import com.alipay.antchain.bridge.pluginserver.server.exception.ServerErrorCodeEnum;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PluginManagementServiceImpl extends ManagementServiceGrpc.ManagementServiceImplBase {

    @Resource
    private IPluginManagerWrapper pluginManagerWrapper;

    /**
     * <pre>
     * maintenance personnel may invoke this interface to load, unload, start, and stop plugins
     * </pre>
     */
    public void managePlugin(PluginManageRequest request, StreamObserver<ManageResponse> responseObserver) {
        String path = request.getPath();
        String product = request.getProduct();
        log.info("ManageRequest [path: {}, product: {}, request: {}]", path, product, request.getType());

        ManageResponse resp;

        switch (request.getType()){
            case LOAD_PLUGINS:
                resp = handleLoadPlugins();
                break;
            case START_PLUGINS:
                resp = handleStartPlugins();
                break;
            case LOAD_PLUGIN:
                resp = handleLoadPlugin(path);
                break;
            case START_PLUGIN:
                resp = handleStartPlugin(path);
                break;
            case STOP_PLUGIN:
                resp = handleStopPlugin(product);
                break;
            case START_PLUGIN_FROM_STOP:
                resp = handleStartPluginFromStop(product);
                break;
            case RELOAD_PLUGIN:
                resp = handleReloadPlugin(product);
                break;
            case RELOAD_PLUGIN_IN_NEW_PATH:
                resp = handleReloadPluginInNewPath(product, path);
                break;
            default:
                log.error("managePlugin fail [path: {}, product: {}, request: {}, errorCode: {}, errorMsg: {}]", path, product, request.getType(), ServerErrorCodeEnum.UNSUPPORTED_MANAGE_REQUEST_ERROR.getErrorCode(), ServerErrorCodeEnum.UNSUPPORTED_MANAGE_REQUEST_ERROR.getShortMsg());
                resp = ResponseBuilder.buildFailManageResp(ServerErrorCodeEnum.UNSUPPORTED_MANAGE_REQUEST_ERROR);
                break;
        }

        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    private ManageResponse handleLoadPlugins() {
        try {
            pluginManagerWrapper.loadPlugins();
            return ResponseBuilder.buildPluginManageSuccessResp(PluginManageResp.newBuilder());
        } catch (Exception e){
            log.error("manage(handleLoadPlugins) fail [errorCode: {}, errorMsg: {}]", ServerErrorCodeEnum.MANAGE_LOAD_PLUGINS_ERROR.getErrorCode(), ServerErrorCodeEnum.MANAGE_LOAD_PLUGINS_ERROR.getShortMsg(), e);
            return ResponseBuilder.buildFailManageResp(ServerErrorCodeEnum.MANAGE_LOAD_PLUGINS_ERROR, e.toString());
        }
    }

    private ManageResponse handleStartPlugins() {
        try {
            pluginManagerWrapper.startPlugins();
            return ResponseBuilder.buildPluginManageSuccessResp(PluginManageResp.newBuilder());
        } catch (Exception e){
            log.error("manage(handleStartPlugins) fail [errorCode: {}, errorMsg: {}]", ServerErrorCodeEnum.MANAGE_START_PLUGINS_ERROR.getErrorCode(), ServerErrorCodeEnum.MANAGE_START_PLUGINS_ERROR.getShortMsg(), e);
            return ResponseBuilder.buildFailManageResp(ServerErrorCodeEnum.MANAGE_START_PLUGINS_ERROR, e.toString());
        }
    }

    private ManageResponse handleLoadPlugin(String path) {
        try {
            pluginManagerWrapper.loadPlugin(path);
            return ResponseBuilder.buildPluginManageSuccessResp(PluginManageResp.newBuilder());
        } catch (Exception e){
             log.error("manage(handleLoadPlugin) fail [path: {}, errorCode: {}, errorMsg: {}]", path, ServerErrorCodeEnum.MANAGE_LOAD_PLUGIN_ERROR.getErrorCode(), ServerErrorCodeEnum.MANAGE_LOAD_PLUGIN_ERROR.getShortMsg(), e);
            return ResponseBuilder.buildFailManageResp(ServerErrorCodeEnum.MANAGE_LOAD_PLUGIN_ERROR, e.toString());
        }
    }

    private ManageResponse handleStartPlugin(String path) {
        try {
            pluginManagerWrapper.startPlugin(path);
            return ResponseBuilder.buildPluginManageSuccessResp(PluginManageResp.newBuilder());
        } catch (Exception e){
            log.error("manage(handleStartPlugin) fail [path: {}, errorCode: {}, errorMsg: {}]", path, ServerErrorCodeEnum.MANAGE_START_PLUGIN_ERROR.getErrorCode(), ServerErrorCodeEnum.MANAGE_START_PLUGIN_ERROR.getShortMsg(), e);
            return ResponseBuilder.buildFailManageResp(ServerErrorCodeEnum.MANAGE_START_PLUGIN_ERROR, e.toString());
        }
    }

    private ManageResponse handleStopPlugin(String product) {
        try {
            pluginManagerWrapper.stopPlugin(product);
            return ResponseBuilder.buildPluginManageSuccessResp(PluginManageResp.newBuilder());
        } catch (Exception e){
            log.error("manage(handleStopPlugin) fail [product: {}, errorCode: {}, errorMsg: {}]", product, ServerErrorCodeEnum.MANAGE_STOP_PLUGIN_ERROR.getErrorCode(), ServerErrorCodeEnum.MANAGE_STOP_PLUGIN_ERROR.getShortMsg(), e);
            return ResponseBuilder.buildFailManageResp(ServerErrorCodeEnum.MANAGE_STOP_PLUGIN_ERROR, e.toString());
        }
    }

    private ManageResponse handleStartPluginFromStop(String product) {
        try {
            pluginManagerWrapper.startPluginFromStop(product);
            return ResponseBuilder.buildPluginManageSuccessResp(PluginManageResp.newBuilder());
        } catch (Exception e){
            log.error("manage(handleStartPluginFromStop) fail [product: {}, errorCode: {}, errorMsg: {}]", product, ServerErrorCodeEnum.MANAGE_START_PLUGIN_FROM_STOP_ERROR.getErrorCode(), ServerErrorCodeEnum.MANAGE_START_PLUGIN_FROM_STOP_ERROR.getShortMsg(), e);
            return ResponseBuilder.buildFailManageResp(ServerErrorCodeEnum.MANAGE_START_PLUGIN_FROM_STOP_ERROR, e.toString());
        }
    }

    private ManageResponse handleReloadPlugin(String product) {
        try {
            pluginManagerWrapper.reloadPlugin(product);
            return ResponseBuilder.buildPluginManageSuccessResp(PluginManageResp.newBuilder());
        } catch (Exception e){
            log.error("manage(handleReloadPlugin) fail [product: {}, errorCode: {}, errorMsg: {}]", product, ServerErrorCodeEnum.MANAGE_RELOAD_PLUGIN_ERROR.getErrorCode(), ServerErrorCodeEnum.MANAGE_RELOAD_PLUGIN_ERROR.getShortMsg(), e);
            return ResponseBuilder.buildFailManageResp(ServerErrorCodeEnum.MANAGE_RELOAD_PLUGIN_ERROR, e.toString());
        }
    }

    private ManageResponse handleReloadPluginInNewPath(String product, String path) {
        try {
            pluginManagerWrapper.reloadPlugin(product, path);
            return ResponseBuilder.buildPluginManageSuccessResp(PluginManageResp.newBuilder());
        } catch (Exception e){
            log.error("manage(handleReloadPluginInNewPath) fail [product: {}, path: {}, errorCode: {}, errorMsg: {}]", ServerErrorCodeEnum.MANAGE_RELOAD_PLUGIN_IN_NEW_PATH_ERROR.getErrorCode(), product, path, ServerErrorCodeEnum.MANAGE_RELOAD_PLUGIN_IN_NEW_PATH_ERROR.getShortMsg(), e);
            return ResponseBuilder.buildFailManageResp(ServerErrorCodeEnum.MANAGE_RELOAD_PLUGIN_IN_NEW_PATH_ERROR, e.toString());
        }
    }

    /**
     * <pre>
     * return whether the plugins of the products are supported
     * </pre>
     */
    @Override
    public void hasPlugins(HasPluginsRequest request, StreamObserver<ManageResponse> responseObserver) {
        responseObserver.onNext(
                ResponseBuilder.buildHasPluginsResp(
                        HasPluginsResp.newBuilder()
                                .putAllResults(
                                        request.getProductsList().stream()
                                                .distinct()
                                                .collect(Collectors.toMap(p -> p, p -> pluginManagerWrapper.hasPlugin(p)))
                                )
                )
        );
        responseObserver.onCompleted();
    }

    /**
     * <pre>
     * return all supported plugin products
     * </pre>
     */
    @Override
    public void allPlugins(AllPluginsRequest request, StreamObserver<ManageResponse> responseObserver) {
        responseObserver.onNext(
                ResponseBuilder.buildAllPluginsResp(
                        AllPluginsResp.newBuilder().addAllProducts(pluginManagerWrapper.allSupportProducts().stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList()))
                )
        );
        responseObserver.onCompleted();
    }

    /**
     * <pre>
     * return whether the chains of the domains are running
     * </pre>
     */
    @Override
    public void hasDomains(HasDomainsRequest request, StreamObserver<ManageResponse> responseObserver) {
        responseObserver.onNext(ResponseBuilder.buildHasDomainsResp(HasDomainsResp.newBuilder()
                        .putAllResults(request.getDomainsList().stream().distinct().collect(Collectors.toMap(d -> d, d -> pluginManagerWrapper.hasDomain(d))))
                )
        );
        responseObserver.onCompleted();
    }

    /**
     * <pre>
     * return domains of all running chains
     * </pre>
     */
    @Override
    public void allDomains(AllDomainsRequest request, StreamObserver<ManageResponse> responseObserver) {
        responseObserver.onNext(
                ResponseBuilder.buildAllDomainsResp(
                        AllDomainsResp.newBuilder().addAllDomains(pluginManagerWrapper.allRunningDomains().stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList()))
                )
        );
        responseObserver.onCompleted();
    }

    @Override
    public void restartBBC(RestartBBCRequest request, StreamObserver<ManageResponse> responseObserver) {
        if (!pluginManagerWrapper.hasPlugin(request.getProduct())) {
            responseObserver.onNext(
                    ResponseBuilder.buildFailManageResp(ServerErrorCodeEnum.MANAGE_RESTART_BBC_ERROR, "product not found")
            );
            responseObserver.onCompleted();
            return;
        }
        if (!pluginManagerWrapper.hasDomain(request.getDomain())) {
            responseObserver.onNext(
                    ResponseBuilder.buildFailManageResp(ServerErrorCodeEnum.MANAGE_RESTART_BBC_ERROR, "domain not found")
            );
            responseObserver.onCompleted();
            return;
        }
        try {
            IBBCService oldBbcService = pluginManagerWrapper.getBBCService(request.getProduct(), request.getDomain());
            if (ObjectUtil.isNull(oldBbcService)) {
                throw new RuntimeException("null BBC service for domain " + request.getDomain());
            }
            IBBCService newBbcService = pluginManagerWrapper.createBBCService(request.getProduct(), request.getDomain());
            newBbcService.startup(oldBbcService.getContext());
        } catch (Exception e) {
            log.error("restartBBC fail [errorCode: {}, errorMsg: {}]",
                    ServerErrorCodeEnum.MANAGE_RESTART_BBC_ERROR.getErrorCode(),
                    ServerErrorCodeEnum.MANAGE_RESTART_BBC_ERROR.getShortMsg(), e);
            responseObserver.onNext(
                    ResponseBuilder.buildFailManageResp(ServerErrorCodeEnum.MANAGE_RESTART_BBC_ERROR, "UNKNOWN")
            );
            responseObserver.onCompleted();
            return;
        }
        responseObserver.onNext(
                ResponseBuilder.buildRestartBBCResp(RestartBBCResp.newBuilder())
        );
        responseObserver.onCompleted();
    }
}
