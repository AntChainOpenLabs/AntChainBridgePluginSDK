package com.alipay.antchain.bridge.relayer.core.service.reliable;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.commons.core.rcc.ReliableCrossChainMessage;
import com.alipay.antchain.bridge.commons.core.rcc.ReliableCrossChainMsgProcessStateEnum;
import com.alipay.antchain.bridge.commons.core.sdp.AtomicFlagEnum;
import com.alipay.antchain.bridge.commons.core.sdp.TimeoutMeasureEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.SDPMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgCommitResult;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.SDPNonceRecordDO;
import com.alipay.antchain.bridge.relayer.core.manager.blockchain.IBlockchainManager;
import com.alipay.antchain.bridge.relayer.core.manager.ptc.PtcManager;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.AbstractBlockchainClient;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.BlockchainClientPool;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.HeteroBlockchainClient;
import com.alipay.antchain.bridge.relayer.dal.repository.IBlockchainRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.ICrossChainMessageRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Getter
public class ReliableProcessService {

    @Resource
    private ICrossChainMessageRepository crossChainMessageRepository;

    @Resource
    private IBlockchainRepository blockchainRepository;

    @Resource
    private BlockchainClientPool blockchainClientPool;

    @Resource
    private IBlockchainManager blockchainManager;

    @Resource
    private PtcManager ptcManager;

    @Value("${relayer.service.process.reliable.ccmsg.batch_size:64}")
    private int rccmsgBatchSize;

    @Value("${relayer.service.process.reliable.ccmsg.valid_period:7}")
    private int rccmsgValidPeriod;

    /**
     * 执行指定区块的分布式调度任务
     *
     * @param blockchainProduct
     * @param blockchainId
     */
    @Transactional(rollbackFor = Exception.class)
    public void process(String blockchainProduct, String blockchainId) {
        log.debug("reliable process service is running");

        String domainName = blockchainManager.getBlockchainDomain(blockchainProduct, blockchainId);

        // 1. 获取pending的跨链消息
        List<ReliableCrossChainMessage> reliableCrossChainMessageList =
                crossChainMessageRepository.peekPendingReliableMessages(
                        domainName,
                        rccmsgBatchSize
                );

        if (!reliableCrossChainMessageList.isEmpty()) {
            log.info("peek {} reliable cross chain message from pool", reliableCrossChainMessageList.size());

            // 2. 获取异构链 bbcClient
            AbstractBlockchainClient client = blockchainClientPool.getClient(blockchainProduct, blockchainId);
            if (ObjectUtil.isNull(client)) {
                client = blockchainClientPool.createClient(blockchainManager.getBlockchainMeta(blockchainProduct, blockchainId));
            }
            HeteroBlockchainClient heteroBlockchainClient = (HeteroBlockchainClient) client;

            // 需要更新的可靠消息记录
            List<ReliableCrossChainMessage> rccMsgResults = new ArrayList<>();
            // 需要更新的sdp消息记录
            List<SDPMsgCommitResult> sdpCommitResults = new ArrayList<>();
            List<SDPNonceRecordDO> sdpNonceRecordsToSave = new ArrayList<>();

            // 3. 调用bbc服务接口重试pending跨链消息
            for (ReliableCrossChainMessage msg : reliableCrossChainMessageList) {
                log.info("begin to process pending message: hash {}, idempotentInfo {}",
                        msg.getOriginalHash(),
                        msg.getIdempotentInfo().getInfo());

                SDPMsgWrapper sdpMsgWrapper = crossChainMessageRepository.getSDPMessage(msg.getOriginalHash());
                if (ObjectUtil.isNull(sdpMsgWrapper)) {
                    log.info("no sdp found in pool, so skip reliable relay process cause that sdp committed with tx {} should be archived",
                            msg.getOriginalHash());
                    // could be failed
                    msg.setStatus(ReliableCrossChainMsgProcessStateEnum.SUCCESS);
                    rccMsgResults.add(msg);
                    continue;
                }
                if (sdpMsgWrapper.getProcessState() == SDPMsgProcessStateEnum.TX_SUCCESS
                        || sdpMsgWrapper.getProcessState() == SDPMsgProcessStateEnum.TX_FAILED) {
                    log.info("sdp message {} is already committed with tx {} success or failed, so skip reliable relay process",
                            sdpMsgWrapper.getMessageId(), msg.getOriginalHash());
                    msg.setStatus(sdpMsgWrapper.getProcessState() == SDPMsgProcessStateEnum.TX_SUCCESS ?
                            ReliableCrossChainMsgProcessStateEnum.SUCCESS : ReliableCrossChainMsgProcessStateEnum.FAILED);
                    rccMsgResults.add(msg);
                    continue;
                }

                // 重试提交消息，同时需要相应更新数据库状态
                CrossChainMessageReceipt receipt = heteroBlockchainClient.reliableRetry(msg);
                if (ObjectUtil.isNotEmpty(receipt) && receipt.isConfirmed()) {
                    // 重试成功，更新跨链消息记录和sdp消息记录
                    log.info("ReliableProcessService: success to retry commit reliable cross-chain msg: {}, {}",
                            msg.getIdempotentInfo().getInfo(),
                            receipt.getTxhash());

                    msg.setStatus(receipt.isSuccessful() ? ReliableCrossChainMsgProcessStateEnum.SUCCESS : ReliableCrossChainMsgProcessStateEnum.FAILED);
                    msg.setCurrentHash(receipt.getTxhash());
                    msg.setRetryTime(StrUtil.equals(receipt.getTxhash(), msg.getCurrentHash()) ? msg.getRetryTime() : msg.getRetryTime() + 1);
                    msg.setTxTimestamp(receipt.getTxTimestamp());
                    msg.setErrorMsg(receipt.getErrorMsg());
                    rccMsgResults.add(msg);

                    if (!StrUtil.equalsIgnoreCase(msg.getCurrentHash(), msg.getOriginalHash())) {
                        SDPMsgCommitResult sdpMsgCommitResult = new SDPMsgCommitResult(
                                blockchainProduct,
                                blockchainId,
                                receipt.getTxhash(),
                                true,
                                receipt.getErrorMsg(),
                                receipt.getTxTimestamp()
                        );
                        if (sdpMsgWrapper.getVersion() > 2 && sdpMsgWrapper.getSdpMessage().getTimeoutMeasure() != TimeoutMeasureEnum.NO_TIMEOUT
                                && sdpMsgWrapper.getSdpMessage().getAtomicFlag().ordinal() < AtomicFlagEnum.ACK_SUCCESS.ordinal()
                                && !receipt.isSuccessful() && blockchainManager.checkAndProcessMessageTimeouts(sdpMsgWrapper)) {
                            sdpMsgCommitResult.setTimeout(true);
                        }
                        sdpCommitResults.add(sdpMsgCommitResult);
                        if (receipt.isSuccessful() && msg.getIdempotentInfo().getNonce() != -1) {
                            sdpNonceRecordsToSave.add(new SDPNonceRecordDO(
                                    sdpMsgWrapper.getMessageId(),
                                    sdpMsgWrapper.getSenderBlockchainDomain(),
                                    sdpMsgWrapper.getMsgSender(),
                                    sdpMsgWrapper.getReceiverBlockchainDomain(),
                                    sdpMsgWrapper.getMsgReceiver(),
                                    sdpMsgWrapper.getNonce()
                            ));
                        }
                    }
                } else {
                    // 重试不成功
                    log.info("ReliableProcessService: can not find cross chain message [{}] result, hash {}, receipt {}",
                            msg.getIdempotentInfo().getInfo(),
                            msg.getCurrentHash(),
                            receipt == null ?
                                    "" :
                                    StrUtil.format("txHash: {}, isSuccessful: {}, isConfirmed: {}, errMsg: {}",
                                            receipt.getTxhash(),
                                            receipt.isSuccessful(),
                                            receipt.isConfirmed(),
                                            receipt.getErrorMsg())
                    );

                    if (sdpMsgWrapper.getVersion() > 2
                            && sdpMsgWrapper.getSdpMessage().getAtomicFlag().ordinal() < AtomicFlagEnum.ACK_SUCCESS.ordinal()
                            && sdpMsgWrapper.getSdpMessage().getTimeoutMeasure() != TimeoutMeasureEnum.NO_TIMEOUT) {
                        boolean isTimeout = blockchainManager.checkAndProcessMessageTimeouts(sdpMsgWrapper);
                        // 判断交易超时
                        if (isTimeout) {
                            log.info("ReliableProcessService: msg {} is timeout, so skip reliable relay process",
                                    msg.getIdempotentInfo().getInfo());
                            msg.setStatus(ReliableCrossChainMsgProcessStateEnum.FAILED);
                            rccMsgResults.add(msg);
                            sdpCommitResults.add(new SDPMsgCommitResult(msg.getOriginalHash(), false, true));
                        }
                    }
                }
            }

            // 更新sdp消息记录
            crossChainMessageRepository.updateSDPMessageResults(sdpCommitResults);
            crossChainMessageRepository.updateRCCMessageBatch(rccMsgResults);
            sdpNonceRecordsToSave.forEach(
                    r -> crossChainMessageRepository.saveSDPNonceRecord(r)
            );
        }

        // 删除过期的可靠上链记录
        crossChainMessageRepository.deleteExpiredReliableMessages(rccmsgValidPeriod);
    }
}
