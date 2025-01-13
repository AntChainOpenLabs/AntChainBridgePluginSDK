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

package com.alipay.antchain.bridge.relayer.core.service.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.constant.UniformCrosschainPacketStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.UniformCrosschainPacketContext;
import com.alipay.antchain.bridge.relayer.core.manager.blockchain.IBlockchainManager;
import com.alipay.antchain.bridge.relayer.core.utils.ProcessUtils;
import com.alipay.antchain.bridge.relayer.dal.repository.ICrossChainMessageRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.impl.BlockchainIdleDCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class ValidationService {

    @Resource
    private IBlockchainManager blockchainManager;

    @Resource
    private BlockchainIdleDCache blockchainIdleDCache;

    @Resource
    private ICrossChainMessageRepository crossChainMessageRepository;

    @Resource(name = "validationServiceThreadsPool")
    private ExecutorService validationServiceThreadsPool;

    @Resource
    private UniformCrosschainPacketValidator uniformCrosschainPacketValidator;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Value("${relayer.service.validation.ccmsg.batch_size:64}")
    private int ccmsgBatchSize;

    public void process(String blockchainProduct, String blockchainId) {
        log.debug("validation service run with blockchain {}-{}", blockchainProduct, blockchainId);

        List<UniformCrosschainPacketContext> ucpContexts = new ArrayList<>();

        String domainName = blockchainManager.getBlockchainDomain(blockchainProduct, blockchainId);

        if (this.blockchainIdleDCache.ifUCPProcessIdle(blockchainProduct, blockchainId)) {
            log.debug("validation process : blockchain is idle {}-{}.", blockchainProduct, blockchainId);
        } else if (StrUtil.isNotEmpty(domainName)) {
            ucpContexts = crossChainMessageRepository.peekUCPMessages(
                    domainName,
                    UniformCrosschainPacketStateEnum.PENDING,
                    ccmsgBatchSize
            );
        }

        if (!ucpContexts.isEmpty()) {
            log.info("peek {} UCP msg from pool: {}-{}", ucpContexts.size(), blockchainProduct, blockchainId);
        } else {
            this.blockchainIdleDCache.setLastEmptyUCPPoolTime(blockchainProduct, blockchainId);
            log.debug("{}-{} for auth msg is idle", blockchainProduct, blockchainId);
        }

        // 使用线程池并发执行
        ProcessUtils.waitAllFuturesDone(
                blockchainProduct,
                blockchainId,
                ucpContexts.stream().map(
                        ucpContext -> validationServiceThreadsPool.submit(
                                wrapUCPTask(ucpContext.getUcpId())
                        )
                ).collect(Collectors.toList()),
                log
        );
    }

    public Runnable wrapUCPTask(String ucpId) {
        return () -> transactionTemplate.execute(
                new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        UniformCrosschainPacketContext ucpContext = crossChainMessageRepository.getUniformCrosschainPacket(ucpId, true);
                        if (ObjectUtil.isNull(ucpContext)) {
                            log.error("none UCP message found for ucp id {}", ucpId);
                            return;
                        }

                        try {
                            uniformCrosschainPacketValidator.doProcess(ucpContext);
                        } catch (AntChainBridgeRelayerException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new AntChainBridgeRelayerException(
                                    RelayerErrorCodeEnum.SERVICE_CORE_PROCESS_PROCESS_CCMSG_FAILED,
                                    e,
                                    "failed to process UCP message for ucp id {}",
                                    ucpId
                            );
                        }
                    }
                }
        );
    }
}
