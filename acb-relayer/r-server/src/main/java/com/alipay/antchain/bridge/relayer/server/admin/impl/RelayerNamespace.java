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

package com.alipay.antchain.bridge.relayer.server.admin.impl;

import java.util.List;
import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNetwork;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNodeInfo;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerCredentialManager;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerNetworkManager;
import com.alipay.antchain.bridge.relayer.dal.repository.ISystemConfigRepository;
import com.alipay.antchain.bridge.relayer.server.admin.AbstractNamespace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RelayerNamespace extends AbstractNamespace {

    @Resource
    private IRelayerNetworkManager relayerNetworkManager;

    @Resource
    private IRelayerCredentialManager relayerCredentialManager;

    @Resource
    private ISystemConfigRepository systemConfigRepository;

    public RelayerNamespace() {
        addCommand("setLocalEndpoints", this::setLocalEndpoints);
        addCommand("getLocalEndpoints", this::getLocalEndpoints);
        addCommand("getLocalRelayerId", this::getLocalRelayerId);
        addCommand("getLocalRelayerCrossChainCertificate", this::getLocalRelayerCrossChainCertificate);
        addCommand("getLocalDomainRouter", this::getLocalDomainRouter);
        addCommand("getCrossChainChannel", this::getCrossChainChannel);
        addCommand("getRemoteRelayerInfo", this::getRemoteRelayerInfo);
    }

    Object setLocalEndpoints(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }

        String endpoints = args[0];
        try {
            List<String> inputEndpoints = StrUtil.split(endpoints, ",");
            if (ObjectUtil.isEmpty(inputEndpoints) || inputEndpoints.stream().anyMatch(StrUtil::isEmpty)) {
                return "empty input";
            }
            systemConfigRepository.setLocalEndpoints(StrUtil.split(endpoints, ","));
        } catch (Exception e) {
            log.error("failed to set local endpoints: {}", endpoints, e);
            return "unexpected error : " + e.getMessage();
        }
        return "success";
    }

    Object getLocalEndpoints(String... args) {
        try {
            return StrUtil.join(", ", systemConfigRepository.getLocalEndpoints());
        } catch (Throwable e) {
            log.error("failed to get local endpoints: ", e);
            return "internal error";
        }
    }

    Object getLocalRelayerId(String... args) {
        try {
            return relayerCredentialManager.getLocalNodeId();
        } catch (Throwable e) {
            log.error("failed to get local relayer ID: ", e);
            return "internal error";
        }
    }

    Object getLocalRelayerCrossChainCertificate(String... args) {
        try {
            return JSON.toJSONString(relayerCredentialManager.getLocalRelayerCertificate());
        } catch (Throwable e) {
            log.error("failed to get local relayer crosschain cert: ", e);
            return "internal error";
        }
    }

    Object getLocalDomainRouter(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }
        String domain = args[0];
        try {
            RelayerNetwork.DomainRouterItem item = relayerNetworkManager.findNetworkItemByDomainName(domain);
            if (ObjectUtil.isNull(item)) {
                return "not found";
            }
            return JSON.toJSONString(item);
        } catch (Throwable e) {
            log.error("failed to get local domain router for domain {}: ", domain, e);
            return "internal error";
        }
    }

    Object getRemoteRelayerInfo(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }
        String nodeId = args[0];
        try {
            RelayerNodeInfo info = relayerNetworkManager.getRelayerNode(nodeId, false);
            if (ObjectUtil.isNull(info)) {
                return "not found";
            }
            return JSON.toJSONString(info);
        } catch (Throwable e) {
            log.error("failed to get remote relayer info for node id {}: ", nodeId, e);
            return "internal error";
        }
    }

    Object getCrossChainChannel(String... args) {
        if (args.length != 2) {
            return "wrong number of arguments";
        }

        String localDomain = args[0];
        String remoteDomain = args[1];

        try {
            if (!relayerNetworkManager.hasCrossChainChannel(localDomain, remoteDomain)) {
                return "not exist";
            }
            return JSON.toJSONString(relayerNetworkManager.getCrossChainChannel(localDomain, remoteDomain));
        } catch (Exception e) {
            log.error("failed to get crosschain channel ( local: {}, remote: {} )", localDomain, remoteDomain, e);
            return "unexpected error : " + e.getMessage();
        }
    }
}
