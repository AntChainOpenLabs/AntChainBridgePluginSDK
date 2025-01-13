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

import com.alipay.antchain.bridge.pluginserver.managementservice.*;
import com.alipay.antchain.bridge.pluginserver.server.exception.ServerErrorCodeEnum;
import com.alipay.antchain.bridge.pluginserver.service.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResponseBuilder {

    public static Response buildBBCSuccessResp(CallBBCResponse.Builder respBuilder) {
        log.debug("call bbc service response success");

        return Response.newBuilder()
                .setCode(ServerErrorCodeEnum.SUCCESS.getErrorCode())
                .setErrorMsg(ServerErrorCodeEnum.SUCCESS.getShortMsg())
                .setBbcResp(respBuilder).build();
    }

    public static Response buildHeartbeatSuccessResp(HeartbeatResponse.Builder respBuilder) {
        log.debug("HeartbeatResponse, domains: {}, product: {}", respBuilder.getDomainsList(), respBuilder.getProductsList());

        return Response.newBuilder()
                .setCode(ServerErrorCodeEnum.SUCCESS.getErrorCode())
                .setErrorMsg(ServerErrorCodeEnum.SUCCESS.getShortMsg())
                .setHeartbeatResp(respBuilder).build();
    }

    public static Response buildIfProductSupportSuccessResp(IfProductSupportResponse.Builder respBuilder) {
        log.debug("IfProductSupportResponse: {}", respBuilder.getResults());

        return Response.newBuilder()
                .setCode(ServerErrorCodeEnum.SUCCESS.getErrorCode())
                .setErrorMsg(ServerErrorCodeEnum.SUCCESS.getShortMsg())
                .setIfProductSupportResp(respBuilder).build();
    }

    public static Response buildIfDomainAliveSuccessResp(IfDomainAliveResponse.Builder respBuilder) {
        log.debug("IfDomainAliveResponse: {}", respBuilder.getResults());

        return Response.newBuilder()
                .setCode(ServerErrorCodeEnum.SUCCESS.getErrorCode())
                .setErrorMsg(ServerErrorCodeEnum.SUCCESS.getShortMsg())
                .setIfDomainAliveResp(respBuilder).build();
    }

    public static Response buildFailResp(ServerErrorCodeEnum errorCodeEnum) {

        return Response.newBuilder()
                .setCode(errorCodeEnum.getErrorCode())
                .setErrorMsg(errorCodeEnum.getShortMsg()).build();
    }

    public static Response buildFailResp(ServerErrorCodeEnum errorCodeEnum, String longMsg) {

        return Response.newBuilder()
                .setCode(errorCodeEnum.getErrorCode())
                .setErrorMsg(longMsg).build();
    }

    // ManageResponse builder ======================================

    public static ManageResponse buildPluginManageSuccessResp(PluginManageResp.Builder respBuilder) {
        log.debug("plugin manage response success");

        return ManageResponse.newBuilder()
                .setCode(ServerErrorCodeEnum.SUCCESS.getErrorCode())
                .setErrorMsg(ServerErrorCodeEnum.SUCCESS.getShortMsg())
                .setPluginManageResp(respBuilder).build();
    }

    public static ManageResponse buildHasPluginsResp(HasPluginsResp.Builder respBuilder) {
        log.debug("HasPluginsResponse: {}", respBuilder.getResults());

        return ManageResponse.newBuilder()
                .setCode(ServerErrorCodeEnum.SUCCESS.getErrorCode())
                .setErrorMsg(ServerErrorCodeEnum.SUCCESS.getShortMsg())
                .setHasPluginsResp(respBuilder).build();
    }

    public static ManageResponse buildAllPluginsResp(AllPluginsResp.Builder respBuilder) {
        log.debug("AllPluginResponse: {}", respBuilder.getProductsList());

        return ManageResponse.newBuilder()
                .setCode(ServerErrorCodeEnum.SUCCESS.getErrorCode())
                .setErrorMsg(ServerErrorCodeEnum.SUCCESS.getShortMsg())
                .setAllPluginsResp(respBuilder).build();
    }

    public static ManageResponse buildHasDomainsResp(HasDomainsResp.Builder respBuilder) {
        log.debug("HasDomainsResponse: {}", respBuilder.getResults());

        return ManageResponse.newBuilder()
                .setCode(ServerErrorCodeEnum.SUCCESS.getErrorCode())
                .setErrorMsg(ServerErrorCodeEnum.SUCCESS.getShortMsg())
                .setHasDomainsResp(respBuilder).build();
    }

    public static ManageResponse buildAllDomainsResp(AllDomainsResp.Builder respBuilder) {
        log.debug("AllDomainsResponse: {}", respBuilder.getDomainsList());

        return ManageResponse.newBuilder()
                .setCode(ServerErrorCodeEnum.SUCCESS.getErrorCode())
                .setErrorMsg(ServerErrorCodeEnum.SUCCESS.getShortMsg())
                .setAllDomainsResp(respBuilder).build();
    }

    public static ManageResponse buildRestartBBCResp(RestartBBCResp.Builder respBuilder) {
        log.debug("restart bbc service success");
        return ManageResponse.newBuilder()
                .setCode(ServerErrorCodeEnum.SUCCESS.getErrorCode())
                .setErrorMsg(ServerErrorCodeEnum.SUCCESS.getShortMsg())
                .setRestartBBCResp(respBuilder)
                .build();
    }

    public static ManageResponse buildFailManageResp(ServerErrorCodeEnum errorCodeEnum) {

        return ManageResponse.newBuilder()
                .setCode(errorCodeEnum.getErrorCode())
                .setErrorMsg(errorCodeEnum.getShortMsg()).build();
    }

    public static ManageResponse buildFailManageResp(ServerErrorCodeEnum errorCodeEnum, String longMsg) {

        return ManageResponse.newBuilder()
                .setCode(errorCodeEnum.getErrorCode())
                .setErrorMsg(longMsg).build();
    }
}
