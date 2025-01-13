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

package com.alipay.antchain.bridge.relayer.core.types.network;

import java.util.List;
import java.util.Map;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.commons.core.base.UniformCrosschainPacket;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerBlockchainContent;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNodeInfo;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerCredentialManager;
import com.alipay.antchain.bridge.relayer.core.types.network.request.*;
import com.alipay.antchain.bridge.relayer.core.types.network.response.ChannelStartRespPayload;
import com.alipay.antchain.bridge.relayer.core.types.network.response.HelloStartRespPayload;
import com.alipay.antchain.bridge.relayer.core.types.network.response.QueryCrossChainMsgReceiptsRespPayload;
import com.alipay.antchain.bridge.relayer.core.types.network.response.RelayerResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public abstract class BaseRelayerClient implements RelayerClient {

    private RelayerNodeInfo remoteNodeInfo;

    private IRelayerCredentialManager relayerCredentialManager;

    private String defaultNetworkId;

    public BaseRelayerClient(
            RelayerNodeInfo remoteNodeInfo,
            IRelayerCredentialManager relayerCredentialManager,
            String defaultNetworkId
    ) {
        this.remoteNodeInfo = remoteNodeInfo;
        this.relayerCredentialManager = relayerCredentialManager;
        this.defaultNetworkId = defaultNetworkId;
    }

    /**
     * 发送请求
     *
     * @param relayerRequest
     * @return
     */
    public abstract RelayerResponse sendRequest(RelayerRequest relayerRequest);

    public abstract void startup();

    public abstract boolean shutdown();

    @Override
    public RelayerNodeInfo getRelayerNodeInfo() {
        RelayerRequest request = new GetRelayerNodeInfoRelayerRequest();
        relayerCredentialManager.signRelayerRequest(request);

        RelayerResponse response = validateRelayerResponse(sendRequest(request));
        if (ObjectUtil.isNull(response) || !response.isSuccess()) {
            throw new RuntimeException(
                    StrUtil.format(
                            "failed to getRelayerNodeInfo: {} - {}",
                            response.getResponseCode(), response.getResponseMessage()
                    )
            );
        }

        return RelayerNodeInfo.decode(Base64.decode(response.getResponsePayload()));
    }

    @Override
    public RelayerBlockchainContent getRelayerBlockchainInfo(String domainToQuery) {
        RelayerRequest request = new GetRelayerBlockchainInfoRelayerRequest(domainToQuery);
        relayerCredentialManager.signRelayerRequest(request);

        RelayerResponse response = validateRelayerResponse(sendRequest(request));
        if (ObjectUtil.isNull(response) || !response.isSuccess()) {
            throw new RuntimeException(
                    StrUtil.format(
                            "getRelayerBlockchainInfo for domain {} failed: {} - {}",
                            domainToQuery,
                            response.getResponseCode(),
                            response.getResponseMessage()
                    )
            );
        }

        return RelayerBlockchainContent.decodeFromJson(response.getResponsePayload());
    }

    @Override
    public RelayerBlockchainContent getRelayerBlockchainContent() {
        RelayerRequest request = new GetRelayerBlockchainContentRelayerRequest();
        relayerCredentialManager.signRelayerRequest(request);

        RelayerResponse response = validateRelayerResponse(sendRequest(request));
        if (ObjectUtil.isNull(response) || !response.isSuccess()) {
            throw new RuntimeException(
                    StrUtil.format(
                            "getRelayerBlockchainContent from relayer {} failed: {} - {}",
                            remoteNodeInfo.getNodeId(),
                            response.getResponseCode(),
                            response.getResponseMessage()
                    )
            );
        }

        return RelayerBlockchainContent.decodeFromJson(response.getResponsePayload());
    }

    @Override
    public void propagateCrossChainMsg(String domainName, String ucpId, IAuthMessage authMessage, UniformCrosschainPacket ucp, String ledgerInfo) {
        RelayerRequest request = new PropagateCrossChainMsgRequest(
                ucpId,
                this.getRemoteNodeInfo().getProperties().getRelayerReqVersion() < 1 ? authMessage : null,
                this.getRemoteNodeInfo().getProperties().getRelayerReqVersion() >= 1 ? ucp : null,
                domainName,
                ledgerInfo
        );
        relayerCredentialManager.signRelayerRequest(request);

        RelayerResponse response = validateRelayerResponse(sendRequest(request));
        if (ObjectUtil.isNull(response)) {
            throw new RuntimeException(
                    StrUtil.format(
                            "am request from domain {} failed: empty response found",
                            domainName
                    )
            );
        } else if (!response.isSuccess()) {
            throw new RuntimeException(
                    StrUtil.format("am request from domain {} failed: (code: {}, msg: {})",
                            domainName, response.getResponseCode(), response.getResponseMessage()
                    )
            );
        }
    }

    @Override
    public QueryCrossChainMsgReceiptsRespPayload queryCrossChainMessageReceipts(List<String> ucpIds) {
        RelayerRequest request = new QueryCrossChainMsgReceiptRequest(ucpIds);
        relayerCredentialManager.signRelayerRequest(request);

        RelayerResponse response = sendRequest(request);
        if (ObjectUtil.isNull(response)) {
            throw new RuntimeException(
                    "query cc msg receipts but get null response"
            );
        } else if (!response.isSuccess()) {
            throw new RuntimeException(
                    StrUtil.format(
                            "failed to query crosschain msg receipts [{}] from remote relayer: (code: {}, msg: {})",
                            StrUtil.join(", ", ucpIds), response.getResponseCode(), response.getResponseMessage()
                    )
            );
        }
        QueryCrossChainMsgReceiptsRespPayload respPayload = QueryCrossChainMsgReceiptsRespPayload.decodeFromJson(response.getResponsePayload());
        if (ObjectUtil.isNull(respPayload)) {
            throw new RuntimeException("payload is null for query cc msg receipt response");
        }
        return respPayload;
    }

    @Override
    public HelloStartRespPayload helloStart(byte[] rand, String relayerNodeId) {
        RelayerResponse response = sendRequest(new HelloStartRequest(rand, relayerNodeId));
        if (ObjectUtil.isNull(response)) {
            throw new RuntimeException(
                    "say hello that ask remote relayer to sign the rand bytes with null response"
            );
        } else if (!response.isSuccess()) {
            throw new RuntimeException(
                    String.format("failed to say hello that ask remote relayer to sign the rand bytes: (code: %d, msg: %s)",
                            response.getResponseCode(), response.getResponseMessage()
                    )
            );
        }
        HelloStartRespPayload helloStartRespPayload = HelloStartRespPayload.decodeFromJson(response.getResponsePayload());
        if (ObjectUtil.isNull(helloStartRespPayload)) {
            throw new RuntimeException("payload is null for hello ask response");
        }
        return helloStartRespPayload;
    }

    @Override
    public void helloComplete(RelayerNodeInfo localRelayerNodeInfo, Map<String, AbstractCrossChainCertificate> domainSpaceCertPath, byte[] remoteRand) {
        RelayerResponse response = sendRequest(
                new HelloCompleteRequest(
                        localRelayerNodeInfo,
                        domainSpaceCertPath,
                        relayerCredentialManager.signHelloRand(remoteRand)
                )
        );
        if (ObjectUtil.isNull(response)) {
            throw new RuntimeException(
                    "sending hello complete request to remote relayer but get null response"
            );
        } else if (!response.isSuccess()) {
            throw new RuntimeException(
                    String.format("failed to send hello complete request to remote relayer : (code: %d, msg: %s)",
                            response.getResponseCode(), response.getResponseMessage()
                    )
            );
        }
    }

    @Override
    public RelayerBlockchainContent channelStart(String destDomain) {
        RelayerRequest request = new ChannelStartRequest(destDomain);
        relayerCredentialManager.signRelayerRequest(request);
        RelayerResponse response = validateRelayerResponse(sendRequest(request));
        if (ObjectUtil.isNull(response)) {
            throw new RuntimeException(
                    "sending channel start request to remote relayer but get null response"
            );
        } else if (!response.isSuccess()) {
            throw new RuntimeException(
                    String.format("failed to send channel start complete request to remote relayer : (code: %d, msg: %s)",
                            response.getResponseCode(), response.getResponseMessage()
                    )
            );
        }
        ChannelStartRespPayload channelStartRespPayload = ChannelStartRespPayload.decodeFromJson(response.getResponsePayload());
        if (ObjectUtil.isNull(channelStartRespPayload)) {
            throw new RuntimeException("payload is null for channel start response");
        }
        return RelayerBlockchainContent.decodeFromJson(channelStartRespPayload.getContentWithSingleBlockchain());
    }

    @Override
    public void channelComplete(String senderDomain, String receiverDomain, RelayerBlockchainContent contentWithSenderBlockchain) {
        RelayerRequest request = new ChannelCompleteRequest(senderDomain, receiverDomain, contentWithSenderBlockchain);
        relayerCredentialManager.signRelayerRequest(request);
        RelayerResponse response = validateRelayerResponse(sendRequest(request));
        if (ObjectUtil.isNull(response)) {
            throw new RuntimeException(
                    "sending channel complete request to remote relayer but get null response"
            );
        } else if (!response.isSuccess()) {
            throw new RuntimeException(
                    String.format("failed to send channel complete request to remote relayer : (code: %d, msg: %s)",
                            response.getResponseCode(), response.getResponseMessage()
                    )
            );
        }
    }

    private RelayerResponse validateRelayerResponse(RelayerResponse relayerResponse) {
        if (!relayerCredentialManager.validateRelayerResponse(relayerResponse)) {
            throw new RuntimeException(
                    StrUtil.format(
                            "response from relayer {} sig is invalid",
                            relayerResponse.calcRelayerNodeId()
                    )
            );
        }
        return relayerResponse;
    }
}
