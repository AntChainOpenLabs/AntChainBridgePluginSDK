package com.alipay.antchain.bridge.relayer.core.types.network;

import java.util.List;
import java.util.Map;

import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.commons.core.base.UniformCrosschainPacket;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerBlockchainContent;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNodeInfo;
import com.alipay.antchain.bridge.relayer.core.types.network.response.HelloStartRespPayload;
import com.alipay.antchain.bridge.relayer.core.types.network.response.QueryCrossChainMsgReceiptsRespPayload;

public interface RelayerClient {

    /**
     * 获取Relayer基本信息
     *
     * @return
     */
    RelayerNodeInfo getRelayerNodeInfo();

    /**
     * 获取支持指定domain的区块链信息，包括oracle等信任根
     *
     * @param supportedDomain
     * @return
     */
    RelayerBlockchainContent getRelayerBlockchainInfo(String supportedDomain);

    /**
     * @return
     */
    RelayerBlockchainContent getRelayerBlockchainContent();

    /**
     * 发送AM请求
     *
     * @param ucpId
     * @param authMessage
     * @param ucp
     * @return
     */
    void propagateCrossChainMsg(String domainName, String ucpId, IAuthMessage authMessage, UniformCrosschainPacket ucp, String ledgerInfo);

    QueryCrossChainMsgReceiptsRespPayload queryCrossChainMessageReceipts(List<String> ucpIds);

    HelloStartRespPayload helloStart(byte[] rand, String relayerNodeId);

    void helloComplete(
            RelayerNodeInfo localRelayerNodeInfo,
            Map<String, AbstractCrossChainCertificate> domainSpaceCertPath,
            byte[] remoteRand
    );

    RelayerBlockchainContent channelStart(String destDomain);

    void channelComplete(String senderDomain, String receiverDomain, RelayerBlockchainContent contentWithSenderBlockchain);
}