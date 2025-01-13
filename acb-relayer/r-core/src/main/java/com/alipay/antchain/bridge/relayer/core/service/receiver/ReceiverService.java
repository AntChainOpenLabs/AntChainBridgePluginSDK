package com.alipay.antchain.bridge.relayer.core.service.receiver;

import java.math.BigInteger;
import java.util.List;
import javax.annotation.Resource;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageFactory;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.base.UniformCrosschainPacket;
import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgTrustLevelEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.AuthMsgWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgCommitResult;
import com.alipay.antchain.bridge.relayer.commons.model.TpBtaDO;
import com.alipay.antchain.bridge.relayer.commons.model.UniformCrosschainPacketContext;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerCredentialManager;
import com.alipay.antchain.bridge.relayer.core.service.receiver.handler.AsyncReceiveHandler;
import com.alipay.antchain.bridge.relayer.core.service.receiver.handler.SyncReceiveHandler;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.HeterogeneousBlock;
import com.alipay.antchain.bridge.relayer.core.types.network.exception.RejectRequestException;
import com.alipay.antchain.bridge.relayer.dal.repository.impl.BlockchainIdleDCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ReceiverService {

    private static final Logger log = LoggerFactory.getLogger(ReceiverService.class);
    /**
     * 同步receiver处理器
     */
    @Resource
    private SyncReceiveHandler syncReceiveHandler;

    /**
     * 异步receiver处理器
     */
    @Resource
    private AsyncReceiveHandler asyncReceiveHandler;

    @Resource
    private BlockchainIdleDCache blockchainIdleDCache;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private IRelayerCredentialManager relayerCredentialManager;

    /**
     * 链外请求receive接口
     *
     * @param authMsg
     * @param authMsg
     * @return
     */
    public void receiveOffChainAMRequest(String domainName, String ucpId, String authMsg, String rawUcp, String ledgerInfo, String senderRelayerNodeId) {
        try {
            UniformCrosschainPacket ucp = null;
            if (StrUtil.isNotEmpty(rawUcp)) {
                ucp = UniformCrosschainPacket.decode(Base64.decode(rawUcp));
                if (ucp.getSrcMessage().getType() != CrossChainMessage.CrossChainMessageType.AUTH_MSG) {
                    throw new RejectRequestException(-1, "only support auth msg");
                }
            }

            AuthMsgWrapper authMsgWrapper = AuthMsgWrapper.buildFrom(
                    StrUtil.EMPTY,
                    StrUtil.EMPTY,
                    domainName,
                    ucpId,
                    AuthMessageFactory.createAuthMessage(ObjectUtil.isNull(ucp) ?
                            Base64.decode(authMsg) : ucp.getSrcMessage().getMessage())
            );
            authMsgWrapper.setLedgerInfo(ledgerInfo);
            if (authMsgWrapper.getTrustLevel() != AuthMsgTrustLevelEnum.POSITIVE_TRUST) {
                authMsgWrapper.setProcessState(AuthMsgProcessStateEnum.PROVED);
            } else {
                authMsgWrapper.setProcessState(AuthMsgProcessStateEnum.PENDING);
            }

            syncReceiveHandler.receiveOffChainAMRequest(authMsgWrapper, ucp, senderRelayerNodeId);
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.SERVICE_MULTI_ANCHOR_PROCESS_REMOTE_AM_PROCESS_FAILED,
                    e,
                    "failed to process remote am request from blockchain {}",
                    domainName
            );
        }
    }

    public void receiveBlock(HeterogeneousBlock block, TpBtaDO tpBtaDO) {
        transactionTemplate.execute(
                new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        List<UniformCrosschainPacketContext> ucps = ObjectUtil.isNull(tpBtaDO) ? block.getUniformCrosschainPacketContexts()
                                : block.getUcpsByTpBta(tpBtaDO, relayerCredentialManager.getLocalNodeId());
                        if (ucps.isEmpty()) {
                            return;
                        }
                        receiveUCP(ucps);

                        List<AuthMsgWrapper> authMessages = HeterogeneousBlock.toAuthMsgWrappers(
                                block.getProduct(),
                                block.getBlockchainId(),
                                block.getDomain(),
                                BigInteger.valueOf(block.getHeight()),
                                ucps
                        );
                        if (authMessages.isEmpty()) {
                            return;
                        }
                        receiveAM(authMessages);
                    }
                }
        );
    }

    /**
     * 接收am消息的接口
     *
     * @param authMsgWrappers
     * @return
     */
    private void receiveAM(List<AuthMsgWrapper> authMsgWrappers) {
        asyncReceiveHandler.receiveAuthMessages(authMsgWrappers);
        if (!authMsgWrappers.isEmpty()) {
            blockchainIdleDCache.setLastAMReceiveTime(
                    authMsgWrappers.get(0).getProduct(),
                    authMsgWrappers.get(0).getBlockchainId()
            );
        }
    }

    private void receiveUCP(List<UniformCrosschainPacketContext> ucpContexts) {
        asyncReceiveHandler.receiveUniformCrosschainPackets(ucpContexts);
        if (!ucpContexts.isEmpty()) {
            blockchainIdleDCache.setLastUCPReceiveTime(
                    ucpContexts.get(0).getProduct(),
                    ucpContexts.get(0).getBlockchainId()
            );
        }
    }

    /**
     * receive am client receipt接口
     *
     * @param commitResults
     * @return
     */
    public boolean receiveAMClientReceipts(List<SDPMsgCommitResult> commitResults) {

        if (!commitResults.isEmpty()) {
            blockchainIdleDCache.setLastAMResponseTime(
                    commitResults.get(0).getReceiveProduct(),
                    commitResults.get(0).getReceiveBlockchainId()
            );
        }
        return asyncReceiveHandler.receiveAMClientReceipt(commitResults);
    }
}
