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

package com.alipay.antchain.bridge.relayer.core.service.archive;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.AuthMsgWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgWrapper;
import com.alipay.antchain.bridge.relayer.core.manager.blockchain.IBlockchainManager;
import com.alipay.antchain.bridge.relayer.dal.repository.ICrossChainMessageRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.impl.BlockchainIdleDCache;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class ArchiveService {
    private static final String SESSION_AM_LOCK = "archive_am_lock_";

    @Value("${relayer.service.archive.batch_size:64}")
    private int archiveBatchSize;

    @Resource
    private IBlockchainManager blockchainManager;

    @Resource
    private BlockchainIdleDCache blockchainIdleDCache;

    @Resource
    private ICrossChainMessageRepository crossChainMessageRepository;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private RedissonClient redisson;

    /**
     * 执行指定区块的分布式调度任务
     *
     * @param blockchainProduct
     * @param blockchainId
     */
    public void process(String blockchainProduct, String blockchainId) {

        log.debug("begin archive {}-{}", blockchainProduct, blockchainId);

        try {
            String domain = blockchainManager.getBlockchainDomain(blockchainProduct, blockchainId);
            if (StrUtil.isEmpty(domain)) {
                log.info("blockchain has no domain cert so skip it this time: {}-{}", blockchainProduct,
                        blockchainId);
                return;
            }

            if (blockchainIdleDCache.ifAMArchiveIdle(blockchainProduct, blockchainId)) {
                log.debug("archive process : blockchain is idle {}-{}.", blockchainProduct, blockchainId);
                return;
            }

            // 分别捞出待处理流水
            List<SDPMsgWrapper> sdpMsgWrappers = crossChainMessageRepository.peekTxFinishedSDPMessageIds(
                    blockchainProduct,
                    blockchainId,
                    archiveBatchSize
            );

            if (sdpMsgWrappers.isEmpty()) {
                blockchainIdleDCache.setLastEmptyAMArchiveTime(blockchainProduct, blockchainId);
                log.debug("sdp msgs to archive is empty for {}-{}", blockchainProduct, blockchainId);
                return;
            }

            log.info("archive msg size {} for {}-{}", blockchainProduct, blockchainId, sdpMsgWrappers.size());

            transactionTemplate.execute(
                    new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus status) {

                            Lock lock = getArchiveSessionLock(domain);
                            if (!lock.tryLock()) {
                                log.info("AMCommitter: unable to get the archive lock: {}", domain);
                                return;
                            }
                            try {
                                List<Long> sdpIds = sdpMsgWrappers.stream().map(SDPMsgWrapper::getId).collect(Collectors.toList());
                                int updateCount = crossChainMessageRepository.archiveSDPMessages(sdpIds);
                                if (updateCount != sdpMsgWrappers.size()) {
                                    log.debug("failed to archive sdp msg ids : {}", StrUtil.join(",", sdpIds));
                                    throw new RuntimeException(
                                            StrUtil.format(
                                                    "sdp archive count is not equal to batch size : batch size is {}, update count is {}",
                                                    sdpIds.size(), updateCount
                                            )
                                    );
                                }

                                updateCount = crossChainMessageRepository.deleteSDPMessages(sdpIds);
                                if (updateCount != sdpIds.size()) {
                                    log.debug("failed to delete sdp msg ids : {}", StrUtil.join(",", sdpIds));
                                    throw new RuntimeException(
                                            StrUtil.format(
                                                    "sdp archive count is not equal to batch size : batch size is {}, update count is {}",
                                                    sdpIds.size(), updateCount
                                            )
                                    );
                                }

                                List<Long> amIds = sdpMsgWrappers.stream()
                                        .map(SDPMsgWrapper::getAuthMsgWrapper)
                                        .map(AuthMsgWrapper::getAuthMsgId)
                                        .collect(Collectors.toList());
                                updateCount = crossChainMessageRepository.archiveAuthMessages(amIds);
                                if (updateCount != amIds.size()) {
                                    log.debug("failed to archive am msg ids : {}", StrUtil.join(",", amIds));
                                    throw new RuntimeException(
                                            StrUtil.format(
                                                    "am archive count is not equal to batch size : batch size is {}, update count is {}",
                                                    amIds.size(), updateCount
                                            )
                                    );
                                }

                                updateCount = crossChainMessageRepository.deleteAuthMessages(amIds);
                                if (updateCount != amIds.size()) {
                                    log.debug("failed to delete am msg ids : {}", StrUtil.join(",", amIds));
                                    throw new RuntimeException(
                                            StrUtil.format(
                                                    "am archive count is not equal to batch size : batch size is {}, update count is {}",
                                                    amIds.size(), updateCount
                                            )
                                    );
                                }
                            } finally {
                                lock.unlock();
                            }
                        }
                    }
            );
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.SERVICE_ARCHIVE_PRECESS_FAILED,
                    e,
                    "failed to process archive task for {}-{}",
                    blockchainProduct, blockchainId
            );
        }

    }

    private Lock getArchiveSessionLock(String domain) {
        return redisson.getLock(SESSION_AM_LOCK + domain);
    }
}
