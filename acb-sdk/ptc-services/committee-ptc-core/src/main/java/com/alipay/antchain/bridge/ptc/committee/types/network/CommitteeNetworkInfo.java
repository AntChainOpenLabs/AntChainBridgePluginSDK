/*
 * Copyright 2024 Ant Group
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

package com.alipay.antchain.bridge.ptc.committee.types.network;

import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CommitteeNetworkInfo {

    public static CommitteeNetworkInfo decode(byte[] rawData) {
        return JSON.parseObject(rawData, CommitteeNetworkInfo.class);
    }

    public CommitteeNetworkInfo(String committeeId) {
        this.committeeId = committeeId;
    }

    @JSONField(name = "committee_id")
    private String committeeId;

    @JSONField
    private List<EndpointInfo> nodes = new ArrayList<>();

    public boolean hasEndpoint(String nodeId) {
        return nodes.stream().anyMatch(endpointInfo -> StrUtil.equals(endpointInfo.getNodeId(), nodeId));
    }

    public void removeEndpoint(String nodeId) {
        nodes.removeIf(endpointInfo -> StrUtil.equals(endpointInfo.getNodeId(), nodeId));
    }

    public void addEndpoint(String nodeId, String endpointUrl, String nodeServerTLSCert) {
        if (hasEndpoint(nodeId)) {
            removeEndpoint(nodeId);
        }
        EndpointInfo endpointInfo = new EndpointInfo();
        endpointInfo.setNodeId(nodeId);
        endpointInfo.setEndpoint(new EndpointAddress(endpointUrl));
        endpointInfo.setTlsCert(nodeServerTLSCert);
        nodes.add(endpointInfo);
    }

    public byte[] encode() {
        return JSON.toJSONBytes(this);
    }
}
