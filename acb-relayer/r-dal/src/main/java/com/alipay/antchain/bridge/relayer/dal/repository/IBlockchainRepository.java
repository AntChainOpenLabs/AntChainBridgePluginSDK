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

package com.alipay.antchain.bridge.relayer.dal.repository;

import java.math.BigInteger;
import java.util.List;

import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.*;

public interface IBlockchainRepository {

    Long getAnchorProcessHeight(String product, String blockchainId, String heightType);

    Long getAnchorProcessHeight(String product, String blockchainId, String heightType, CrossChainLane tpbtaLane);

    Long getAnchorProcessHeightWithoutFlush(String product, String blockchainId, String heightType, CrossChainLane tpbtaLane);

    AnchorProcessHeights getAnchorProcessHeights(String product, String blockchainId);

    void setAnchorProcessHeight(String product, String blockchainId, String heightType, Long height);

    void setAnchorProcessHeight(String product, String blockchainId, String heightType, CrossChainLane tpbtaLane, Long height);

    void saveBlockchainMeta(BlockchainMeta blockchainMeta);

    boolean updateBlockchainMeta(BlockchainMeta blockchainMeta);

    List<BlockchainMeta> getAllBlockchainMeta();

    List<BlockchainMeta> getBlockchainMetaByState(BlockchainStateEnum state);

    BlockchainMeta getBlockchainMetaByDomain(String domain);

    boolean hasBlockchain(String domain);

    List<BlockchainMeta> getBlockchainMetaByPluginServerId(String pluginServerId);

    BlockchainMeta getBlockchainMeta(String product, String blockchainId);

    boolean hasBlockchain(String product, String blockchainId);

    String getBlockchainDomain(String product, String blockchainId);

    List<String> getBlockchainDomainsByState(BlockchainStateEnum state);

    DomainCertWrapper getDomainCert(String domain);

    boolean hasDomainCert(String domain);

    void saveDomainCert(DomainCertWrapper domainCertWrapper);

    void updateBlockchainInfoOfDomainCert(String domain, String product, String blockchainId);

    void saveBta(BtaDO btaDO);

    BtaDO getBta(CrossChainDomain domain);

    boolean hasBta(CrossChainDomain domain);

    boolean hasBta(CrossChainDomain domain, int subjectVersion);

    ValidatedConsensusStateDO getValidatedConsensusState(String ptcServiceId, String domain, CrossChainLane tpbtaLane, BigInteger height);

    ValidatedConsensusStateDO getValidatedConsensusState(String ptcServiceId, String domain, CrossChainLane tpbtaLane, String hash);

    void setValidatedConsensusState(ValidatedConsensusStateDO validatedConsensusStateDO);

    boolean hasValidatedConsensusState(String ptcServiceId, String domain, CrossChainLane tpbtaLane, BigInteger height);

    boolean hasValidatedConsensusState(String ptcServiceId, String domain, CrossChainLane tpbtaLane, String hash);
}
