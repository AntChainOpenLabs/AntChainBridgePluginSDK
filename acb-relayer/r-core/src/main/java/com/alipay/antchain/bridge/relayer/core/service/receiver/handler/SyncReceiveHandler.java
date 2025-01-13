package com.alipay.antchain.bridge.relayer.core.service.receiver.handler;

import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.core.base.UniformCrosschainPacket;
import com.alipay.antchain.bridge.relayer.commons.constant.UniformCrosschainPacketStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.AuthMsgWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.UniformCrosschainPacketContext;
import com.alipay.antchain.bridge.relayer.core.service.process.ProcessService;
import com.alipay.antchain.bridge.relayer.dal.repository.ICrossChainMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Slf4j
public class SyncReceiveHandler {

    @Resource
    private ProcessService processService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private ICrossChainMessageRepository crossChainMessageRepository;

    public void receiveOffChainAMRequest(AuthMsgWrapper authMsg, UniformCrosschainPacket ucp, String senderRelayerNodeId) {

        log.info("receive a off-chain am request with ucp id {} from blockchain {}", authMsg.getUcpId(), authMsg.getDomain());

        authMsg.setNetworkAM(true);

        try {
            transactionTemplate.execute(
                    new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus status) {
                            if (ObjectUtil.isNotNull(ucp)) {
                                log.info("save UCP {} from remote relayer", authMsg.getUcpId());

                                UniformCrosschainPacketContext ucpContext = new UniformCrosschainPacketContext();
                                ucpContext.setUcpId(authMsg.getUcpId());
                                if (ObjectUtil.isNotNull(ucp.getTpProof())) {
                                    ucpContext.setTpbtaLaneKey(ucp.getTpProof().getTpbtaCrossChainLane().getLaneKey());
                                    ucpContext.setTpbtaVersion(ucp.getTpProof().getTpbtaVersion());
                                }
                                ucpContext.setFromNetwork(true);
                                ucpContext.setRelayerId(senderRelayerNodeId);
                                ucpContext.setUcp(ucp);
                                ucpContext.setProcessState(UniformCrosschainPacketStateEnum.PROVED);

                                crossChainMessageRepository.putUniformCrosschainPacket(ucpContext);
                            }
                            authMsg.setAuthMsgId(
                                    crossChainMessageRepository.putAuthMessageWithIdReturned(authMsg)
                            );
                            if (!processService.getAuthenticMessageProcess().doProcess(authMsg)) {
                                throw new RuntimeException(
                                        StrUtil.format(
                                                "process off-chain am request with ucp id {} from blockchain {} failed",
                                                authMsg.getUcpId(), authMsg.getDomain()
                                        )
                                );
                            }
                            log.info("process off-chain am request with ucp id {} from blockchain {} success", authMsg.getUcpId(), authMsg.getDomain());
                        }
                    }
            );
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.SERVICE_MULTI_ANCHOR_PROCESS_REMOTE_AM_PROCESS_FAILED,
                    e,
                    "failed to process remote am request with ucp id {} from blockchain {}",
                    authMsg.getUcpId(), authMsg.getDomain()
            );
        }
    }
}