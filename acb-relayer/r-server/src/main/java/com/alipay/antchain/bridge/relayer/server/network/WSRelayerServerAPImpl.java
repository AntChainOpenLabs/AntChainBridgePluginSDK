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

package com.alipay.antchain.bridge.relayer.server.network;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.relayer.commons.constant.SDPMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerBlockchainContent;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerBlockchainInfo;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNodeInfo;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgWrapper;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerCredentialManager;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerNetworkManager;
import com.alipay.antchain.bridge.relayer.core.service.receiver.ReceiverService;
import com.alipay.antchain.bridge.relayer.core.types.network.exception.RejectRequestException;
import com.alipay.antchain.bridge.relayer.core.types.network.request.*;
import com.alipay.antchain.bridge.relayer.core.types.network.response.ChannelStartRespPayload;
import com.alipay.antchain.bridge.relayer.core.types.network.response.HelloStartRespPayload;
import com.alipay.antchain.bridge.relayer.core.types.network.response.QueryCrossChainMsgReceiptsRespPayload;
import com.alipay.antchain.bridge.relayer.core.types.network.response.RelayerResponse;
import com.alipay.antchain.bridge.relayer.dal.repository.ICrossChainMessageRepository;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.ByteArrayCodec;

@WebService(targetNamespace = "http://ws.offchainapi.oracle.mychain.alipay.com/")
@SOAPBinding
@Slf4j
@NoArgsConstructor
public class WSRelayerServerAPImpl extends BaseRelayerServer implements WSRelayerServerAPI {

    private static final String RELAYER_HELLO_RAND_KEY_PREFIX = "RELAYER_HELLO_RAND_";

    public WSRelayerServerAPImpl(
            IRelayerNetworkManager relayerNetworkManager,
            IBCDNSManager bcdnsManager,
            IRelayerCredentialManager relayerCredentialManager,
            ReceiverService receiverService,
            ICrossChainMessageRepository crossChainMessageRepository,
            RedissonClient redisson,
            String defaultNetworkId,
            boolean isDiscoveryServer
    ) {
        super(relayerNetworkManager, bcdnsManager, relayerCredentialManager, receiverService, crossChainMessageRepository, redisson, defaultNetworkId, isDiscoveryServer);
    }

    @Override
    @WebMethod
    public String request(@WebParam(name = "relayerRequest") String relayerRequest) {

        log.debug("receive ws request");

        byte[] rawResponse = doRequest(Base64.decode(relayerRequest));

        String response = Base64.encode(rawResponse);

        log.debug("finish ws request process");

        return response;
    }

    /**
     * 处理请求
     *
     * @param rawRequest
     * @return
     */
    private byte[] doRequest(byte[] rawRequest) {

        try {
            RelayerRequest request = RelayerRequest.decode(rawRequest, RelayerRequest.class);
            if (ObjectUtil.isNull(request)) {
                log.error("Invalid relayer request that failed to decode");
                return RelayerResponse.createFailureResponse(
                        "failed to decode request",
                        getRelayerCredentialManager()
                ).encode();
            }

            checkSenderRelayerReqVersionAsync(request);

            switch (request.getRequestType()) {
                case GET_RELAYER_NODE_INFO:
                    return processGetRelayerNodeInfo().encode();
                case GET_RELAYER_BLOCKCHAIN_INFO:
                    return processGetRelayerBlockchainInfo(
                            GetRelayerBlockchainInfoRelayerRequest.createFrom(request)
                    ).encode();
                case GET_RELAYER_BLOCKCHAIN_CONTENT:
                    return processGetRelayerBlockchainContent(
                            GetRelayerBlockchainContentRelayerRequest.createFrom(request)
                    ).encode();
                case PROPAGATE_CROSSCHAIN_MESSAGE:
                    return processPropagateCrossChainMsgRequest(
                            PropagateCrossChainMsgRequest.createFrom(request)
                    ).encode();
                case QUERY_CROSSCHAIN_MSG_RECEIPT:
                    return processCrossChainMsgReceiptsQuery(
                            QueryCrossChainMsgReceiptRequest.createFrom(request)
                    ).encode();
                case HELLO_START:
                    return processHelloStart(HelloStartRequest.createFrom(request)).encode();
                case HELLO_COMPLETE:
                    return processHelloComplete(HelloCompleteRequest.createFrom(request)).encode();
                case CROSSCHAIN_CHANNEL_START:
                    return processChannelStart(ChannelStartRequest.createFrom(request)).encode();
                case CROSSCHAIN_CHANNEL_COMPLETE:
                    return processChannelComplete(ChannelCompleteRequest.createFrom(request)).encode();
                default:
                    return RelayerResponse.createFailureResponse(
                            "request type not supported: " + request.getRequestType().getCode(),
                            getRelayerCredentialManager()
                    ).encode();
            }
        } catch (Exception e) {
            log.error("unexpected exception happened: ", e);
            return RelayerResponse.createFailureResponse(
                    "unexpected exception happened",
                    getRelayerCredentialManager()
            ).encode();
        }
    }

    private RelayerResponse processGetRelayerNodeInfo() {
        return RelayerResponse.createSuccessResponse(
                () -> Base64.encode(getRelayerNetworkManager().getRelayerNodeInfo().getEncode()),
                getRelayerCredentialManager()
        );
    }

    private RelayerResponse processGetRelayerBlockchainInfo(GetRelayerBlockchainInfoRelayerRequest request) {
        if (!getRelayerCredentialManager().validateRelayerRequest(request)) {
            log.error("failed to validate {} request from relayer {}", request.getRequestType().getCode(), request.calcRelayerNodeId());
            return RelayerResponse.createFailureResponse(
                    "verify crosschain cert failed",
                    getRelayerCredentialManager()
            );
        }

        RelayerBlockchainInfo blockchainInfo;
        try {
            blockchainInfo = getRelayerNetworkManager().getRelayerBlockchainInfo(
                    request.getDomainToQuery()
            );
        } catch (AntChainBridgeRelayerException e) {
            log.error("failed to query blockchain info for domain {}", request.getDomainToQuery(), e);
            return RelayerResponse.createFailureResponse(
                    e.getMsg(),
                    getRelayerCredentialManager()
            );
        }
        if (ObjectUtil.isNull(blockchainInfo)) {
            return RelayerResponse.createFailureResponse(
                    "empty result",
                    getRelayerCredentialManager()
            );
        }

        return RelayerResponse.createSuccessResponse(
                () -> new RelayerBlockchainContent(
                        MapUtil.builder(blockchainInfo.getDomainCert().getDomain(), blockchainInfo).build(),
                        getBcdnsManager().getTrustRootCertChain(blockchainInfo.getDomainCert().getDomainSpace())
                ).encodeToJson(),
                getRelayerCredentialManager()
        );
    }

    private RelayerResponse processGetRelayerBlockchainContent(GetRelayerBlockchainContentRelayerRequest request) {
        if (!getRelayerCredentialManager().validateRelayerRequest(request)) {
            log.error("failed to validate {} request from relayer {}", request.getRequestType().getCode(), request.calcRelayerNodeId());
            return RelayerResponse.createFailureResponse(
                    "verify crosschain cert failed",
                    getRelayerCredentialManager()
            );
        }

        RelayerBlockchainContent blockchainContent;
        try {
            blockchainContent = getRelayerNetworkManager().getRelayerNodeInfoWithContent()
                    .getRelayerBlockchainContent();
        } catch (AntChainBridgeRelayerException e) {
            log.error("failed to query local blockchain content", e);
            return RelayerResponse.createFailureResponse(
                    e.getMsg(),
                    getRelayerCredentialManager()
            );
        }
        if (ObjectUtil.isNull(blockchainContent)) {
            return RelayerResponse.createFailureResponse(
                    "empty result",
                    getRelayerCredentialManager()
            );
        }

        return RelayerResponse.createSuccessResponse(
                blockchainContent::encodeToJson,
                getRelayerCredentialManager()
        );
    }

    private RelayerResponse processPropagateCrossChainMsgRequest(PropagateCrossChainMsgRequest request) {
        if (!getRelayerCredentialManager().validateRelayerRequest(request)) {
            log.error("failed to validate {} request from relayer {}", request.getRequestType().getCode(), request.calcRelayerNodeId());
            return RelayerResponse.createFailureResponse(
                    "verify crosschain cert failed",
                    getRelayerCredentialManager()
            );
        }

        try {
            propagateCrossChainMsg(
                    request.getDomainName(),
                    request.getUcpId(),
                    request.getAuthMsg(),
                    request.getRawUcp(),
                    request.getLedgerInfo(),
                    request.calcRelayerNodeId()
            );
        } catch (RejectRequestException e) {
            log.error(
                    "reject am request from (blockchain: {}, relayer: {}) failed: ",
                    request.getDomainName(), request.calcRelayerNodeId(),
                    e
            );
            return RelayerResponse.createFailureResponse(
                    e.getErrorMsg(),
                    getRelayerCredentialManager()
            );
        } catch (AntChainBridgeRelayerException e) {
            log.error(
                    "handle am request from (blockchain: {}, relayer: {}) failed: ",
                    request.getDomainName(), request.calcRelayerNodeId(),
                    e
            );
            return RelayerResponse.createFailureResponse(
                    e.getMsg(),
                    getRelayerCredentialManager()
            );
        }

        log.info("handle am request from relayer {} success: ", request.calcRelayerNodeId());

        return RelayerResponse.createSuccessResponse(
                () -> null,
                getRelayerCredentialManager()
        );
    }

    private RelayerResponse processCrossChainMsgReceiptsQuery(QueryCrossChainMsgReceiptRequest request) {
        if (!getRelayerCredentialManager().validateRelayerRequest(request)) {
            log.error("failed to validate {} request from relayer {}", request.getRequestType().getCode(), request.calcRelayerNodeId());
            return RelayerResponse.createFailureResponse(
                    "verify crosschain cert failed",
                    getRelayerCredentialManager()
            );
        }

        Map<String, CrossChainMessageReceipt> receiptMap = new HashMap<>();
        Map<String, SDPMsgProcessStateEnum> stateMap = new HashMap<>();
        for (String ucpId : request.getUcpIds()) {
            SDPMsgWrapper sdpMsgWrapper = getCrossChainMessageRepository().querySDPMessage(ucpId);
            if (ObjectUtil.isNotNull(sdpMsgWrapper)) {
                CrossChainMessageReceipt receipt = new CrossChainMessageReceipt();
                receipt.setTxhash(sdpMsgWrapper.getTxHash());
                receipt.setConfirmed(
                        sdpMsgWrapper.getProcessState() == SDPMsgProcessStateEnum.TX_SUCCESS ||
                                sdpMsgWrapper.getProcessState() == SDPMsgProcessStateEnum.TX_FAILED ||
                                sdpMsgWrapper.getProcessState() == SDPMsgProcessStateEnum.MSG_REJECTED ||
                                sdpMsgWrapper.getProcessState() == SDPMsgProcessStateEnum.MSG_ILLEGAL
                );
                receipt.setSuccessful(sdpMsgWrapper.isTxSuccess());
                receipt.setErrorMsg(sdpMsgWrapper.getTxFailReason());

                receiptMap.put(ucpId, receipt);
                stateMap.put(ucpId, sdpMsgWrapper.getProcessState());
            }
        }

        return RelayerResponse.createSuccessResponse(
                new QueryCrossChainMsgReceiptsRespPayload(receiptMap, stateMap),
                getRelayerCredentialManager()
        );
    }

    private RelayerResponse processHelloStart(HelloStartRequest request) {
        log.info("process hello start from {}", request.getRelayerNodeId());

        byte[] myRand = RandomUtil.randomBytes(32);
        setMyRelayerHelloRand(request.getRelayerNodeId(), myRand);
        return RelayerResponse.createSuccessResponse(
                new HelloStartRespPayload(
                        Base64.encode(getRelayerNetworkManager().getRelayerNodeInfo().getEncode()),
                        getBcdnsManager().getTrustRootCertChain(getRelayerCredentialManager().getLocalRelayerIssuerDomainSpace()),
                        getRelayerCredentialManager().getLocalNodeSigAlgo(),
                        getRelayerCredentialManager().signHelloRand(request.getRand()),
                        myRand
                ),
                getRelayerCredentialManager()
        );
    }

    private RelayerResponse processHelloComplete(HelloCompleteRequest request) {
        RelayerNodeInfo remoteNodeInfo = RelayerNodeInfo.decode(
                Base64.decode(request.getRemoteNodeInfo())
        );
        log.info("process hello complete from {}-{}", remoteNodeInfo.getNodeId(), String.join(",", remoteNodeInfo.getEndpoints()));

        if (
                !getBcdnsManager().validateCrossChainCertificate(
                        remoteNodeInfo.getRelayerCrossChainCertificate(),
                        request.getDomainSpaceCertPath()
                )
        ) {
            throw new RuntimeException(
                    StrUtil.format(
                            "failed to verify the relayer {} 's cert {} with cert path [ {} ]",
                            remoteNodeInfo.getNodeId(),
                            remoteNodeInfo.getRelayerCrossChainCertificate().encodeToBase64(),
                            request.getDomainSpaceCertPath().entrySet().stream()
                                    .map(entry -> StrUtil.format("{} : {}", entry.getKey(), entry.getValue().encodeToBase64()))
                                    .reduce((s1, s2) -> s1 + ", " + s2)
                                    .orElse("")
                    )
            );
        }

        byte[] myRand = getMyRelayerHelloRand(remoteNodeInfo.getNodeId());
        if (ObjectUtil.isEmpty(myRand)) {
            throw new RuntimeException("none my rand found");
        }

        try {
            if (!request.getSigAlgoType().getSigner().verify(
                    CrossChainCertificateUtil.getPublicKeyFromCrossChainCertificate(remoteNodeInfo.getRelayerCrossChainCertificate()),
                    myRand,
                    request.getSig()
            )) {
                throw new RuntimeException("not pass");
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    StrUtil.format("failed to verify sig for rand: ( rand: {}, sig: {} )",
                            HexUtil.encodeHexStr(myRand), HexUtil.encodeHexStr(request.getSig()), e)
            );
        }

        if (ObjectUtil.isNull(remoteNodeInfo.getProperties())) {
            remoteNodeInfo.setProperties(new RelayerNodeInfo.RelayerNodeProperties());
        }
        remoteNodeInfo.getProperties().setRelayerReqVersion(request.getRequestVersion());
        getRelayerNetworkManager().addRelayerNode(remoteNodeInfo);

        return RelayerResponse.createSuccessResponse(
                () -> null,
                getRelayerCredentialManager()
        );
    }

    private RelayerResponse processChannelStart(ChannelStartRequest request) {
        if (!getRelayerCredentialManager().validateRelayerRequest(request)) {
            log.error("failed to validate {} request from relayer {}", request.getRequestType().getCode(), request.calcRelayerNodeId());
            return RelayerResponse.createFailureResponse(
                    "verify crosschain cert failed",
                    getRelayerCredentialManager()
            );
        }

        RelayerBlockchainInfo blockchainInfo;
        try {
            blockchainInfo = getRelayerNetworkManager().getRelayerBlockchainInfo(request.getDomain());
        } catch (AntChainBridgeRelayerException e) {
            log.error("failed to process channel start request for domain {} from relayer {}", request.getDomain(), request.calcRelayerNodeId(), e);
            return RelayerResponse.createFailureResponse(
                    e.getMsg(),
                    getRelayerCredentialManager()
            );
        }
        if (ObjectUtil.isNull(blockchainInfo)) {
            return RelayerResponse.createFailureResponse(
                    "empty result",
                    getRelayerCredentialManager()
            );
        }

        return RelayerResponse.createSuccessResponse(
                new ChannelStartRespPayload(
                        new RelayerBlockchainContent(
                                MapUtil.builder(blockchainInfo.getDomainCert().getDomain(), blockchainInfo).build(),
                                getBcdnsManager().getTrustRootCertChain(blockchainInfo.getDomainCert().getDomainSpace())
                        ).encodeToJson()
                ),
                getRelayerCredentialManager()
        );
    }

    private RelayerResponse processChannelComplete(ChannelCompleteRequest request) {
        if (!getRelayerCredentialManager().validateRelayerRequest(request)) {
            log.error("failed to validate {} request from relayer {}", request.getRequestType().getCode(), request.calcRelayerNodeId());
            return RelayerResponse.createFailureResponse(
                    "verify crosschain cert failed",
                    getRelayerCredentialManager()
            );
        }

        RelayerBlockchainContent content = RelayerBlockchainContent.decodeFromJson(request.getRawContentWithSingleBlockchain());
        if (ObjectUtil.isNull(content)) {
            log.error("null relayer blockchain content in request from relayer {} for domain {}", request.calcRelayerNodeId(), request.getSenderDomain());
            return RelayerResponse.createFailureResponse(
                    "null relayer blockchain content",
                    getRelayerCredentialManager()
            );
        }
        if (ObjectUtil.isNull(content.getRelayerBlockchainInfo(request.getSenderDomain()))) {
            log.error("null relayer blockchain content in request from relayer {} for domain {}", request.calcRelayerNodeId(), request.getSenderDomain());
            return RelayerResponse.createFailureResponse(
                    "null relayer blockchain info",
                    getRelayerCredentialManager()
            );
        }

        getRelayerNetworkManager().validateAndSaveBlockchainContent(
                getDefaultNetworkId(),
                getRelayerNetworkManager().getRelayerNode(request.getNodeId(),false),
                content,
                false
        );
        getRelayerNetworkManager().createNewCrossChainChannel(
                request.getReceiverDomain(),
                request.getSenderDomain(),
                request.calcRelayerNodeId()
        );

        return RelayerResponse.createSuccessResponse(
                () -> null,
                getRelayerCredentialManager()
        );
    }

    private void setMyRelayerHelloRand(String relayerNodeId, byte[] myRand) {
        RBucket<byte[]> bucket = getRedisson().getBucket(
                RELAYER_HELLO_RAND_KEY_PREFIX + relayerNodeId,
                ByteArrayCodec.INSTANCE
        );
        bucket.set(myRand, Duration.of(3, ChronoUnit.MINUTES));
    }

    private byte[] getMyRelayerHelloRand(String relayerNodeId) {
        RBucket<byte[]> bucket = getRedisson().getBucket(
                RELAYER_HELLO_RAND_KEY_PREFIX + relayerNodeId,
                ByteArrayCodec.INSTANCE
        );
        return bucket.getAndDelete();
    }
}
