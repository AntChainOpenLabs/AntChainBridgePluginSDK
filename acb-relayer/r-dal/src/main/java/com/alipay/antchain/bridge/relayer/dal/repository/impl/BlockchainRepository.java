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

package com.alipay.antchain.bridge.relayer.dal.repository.impl;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.cache.Cache;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.*;
import com.alipay.antchain.bridge.relayer.dal.entities.*;
import com.alipay.antchain.bridge.relayer.dal.mapper.AnchorProcessMapper;
import com.alipay.antchain.bridge.relayer.dal.mapper.BtaMapper;
import com.alipay.antchain.bridge.relayer.dal.mapper.DomainCertMapper;
import com.alipay.antchain.bridge.relayer.dal.mapper.ValidatedConsensusStateMapper;
import com.alipay.antchain.bridge.relayer.dal.repository.IBlockchainRepository;
import com.alipay.antchain.bridge.relayer.dal.service.BlockchainService;
import com.alipay.antchain.bridge.relayer.dal.utils.ConvertUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.ByteArrayCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Slf4j
public class BlockchainRepository implements IBlockchainRepository {

    @Resource
    private AnchorProcessMapper anchorProcessMapper;

    @Resource
    private BlockchainService blockchainService;

    @Resource
    private DomainCertMapper domainCertMapper;

    @Resource
    private RedissonClient redisson;

    @Value("${relayer.dal.blockchain.heights_cache.ttl:240000}")
    private long ttlForHeightsCache;

    @Value("${relayer.dal.blockchain.heights_cache.flush_period:3000}")
    private long flushPeriodForHeightsCache;

    @Resource
    private Cache<String, DomainCertWrapper> domainCertWrapperCache;

    @Resource
    private Cache<String, BlockchainMeta> blockchainMetaCache;

    @Resource(name = "blockchainIdToDomainCache")
    private Cache<String, String> blockchainIdToDomainCache;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private BtaMapper btaMapper;

    @Resource
    private ValidatedConsensusStateMapper validatedConsensusStateMapper;

    @Override
    public Long getAnchorProcessHeight(String product, String blockchainId, String heightType) {
        try {
            Long height = getHeightFromCache(product, blockchainId, heightType);
            if (ObjectUtil.isNull(height) || ObjectUtil.equals(0L, height)) {
                height = getAnchorProcessHeightFromDB(product, blockchainId, heightType, null);
                setHeightToCache(product, blockchainId, heightType, height);
            }
            return height;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_ANCHOR_HEIGHTS_ERROR,
                    e,
                    "failed to get heights for ( product: {}, blockchain id: {}, type: {} )",
                    product, blockchainId, heightType
            );
        }
    }

    @Override
    public Long getAnchorProcessHeight(String product, String blockchainId, String heightType, @NonNull CrossChainLane tpbtaLane) {
        try {
            String heightKey = ConvertUtil.getHeightKey(heightType, tpbtaLane);
            Long height = getHeightFromCache(product, blockchainId, heightKey);
            if (ObjectUtil.isNull(height) || ObjectUtil.equals(0L, height)) {
                height = getAnchorProcessHeightFromDB(product, blockchainId, heightType, tpbtaLane);
                setHeightToCache(product, blockchainId, heightKey, height);
            }
            return height;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_ANCHOR_HEIGHTS_ERROR,
                    e,
                    "failed to get heights for ( product: {}, blockchain id: {}, type: {}, tpbta lane: {} )",
                    product, blockchainId, heightType, tpbtaLane.getLaneKey()
            );
        }
    }

    @Override
    public Long getAnchorProcessHeightWithoutFlush(String product, String blockchainId, String heightType, CrossChainLane tpbtaLane) {
        try {
            String heightKey = ConvertUtil.getHeightKey(heightType, tpbtaLane);
            Long height = getHeightFromCache(product, blockchainId, heightKey, false);
            if (ObjectUtil.isNull(height) || ObjectUtil.equals(0L, height)) {
                height = getAnchorProcessHeightFromDB(product, blockchainId, heightType, tpbtaLane);
                setHeightToCache(product, blockchainId, heightKey, height);
            }
            return height;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_ANCHOR_HEIGHTS_ERROR,
                    e,
                    "failed to get heights for ( product: {}, blockchain id: {}, type: {}, tpbta lane: {} )",
                    product, blockchainId, heightType, tpbtaLane.getLaneKey()
            );
        }
    }

    @Override
    public AnchorProcessHeights getAnchorProcessHeights(String product, String blockchainId) {
        try {
            AnchorProcessHeights heights = getAnchorProcessHeightsFromCache(product, blockchainId);
            if (ObjectUtil.isNull(heights)) {
                List<AnchorProcessEntity> entities = anchorProcessMapper
                        .selectList(
                                new LambdaQueryWrapper<AnchorProcessEntity>()
                                        .select(
                                                ListUtil.toList(
                                                        AnchorProcessEntity::getTask,
                                                        AnchorProcessEntity::getTpbtaLaneKey,
                                                        AnchorProcessEntity::getBlockHeight,
                                                        BaseEntity::getGmtModified
                                                )
                                        ).eq(AnchorProcessEntity::getProduct, product)
                                        .eq(AnchorProcessEntity::getBlockchainId, blockchainId)
                        );
                if (ObjectUtil.isEmpty(entities)) {
                    return null;
                }
                heights = new AnchorProcessHeights(product, blockchainId);
                for (AnchorProcessEntity entity : entities) {
                    String heightKey = ConvertUtil.getHeightKey(entity.getTask(), entity.getTpbtaLaneKey());
                    heights.getProcessHeights().put(heightKey, entity.getBlockHeight());
                    heights.getModifiedTimeMap().put(heightKey, entity.getGmtModified().getTime());
                }

                return heights;
            }

            for (Map.Entry<String, Long> entry : heights.getProcessHeights().entrySet()) {
                heights.getModifiedTimeMap().put(entry.getKey(), heights.getLastUpdateTime());
            }
            return heights;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_ANCHOR_HEIGHTS_ERROR,
                    e,
                    "failed to get heights for ( product: {}, blockchain id: {} )",
                    product, blockchainId
            );
        }
    }

    @Override
    public void setAnchorProcessHeight(String product, String blockchainId, String heightType, Long height) {
        setAnchorProcessHeight(product, blockchainId, heightType, null, height);
    }

    @Override
    public void setAnchorProcessHeight(String product, String blockchainId, String heightType, CrossChainLane tpbtaLane, Long height) {
        try {
            String heightKey = ConvertUtil.getHeightKey(heightType, tpbtaLane);
            if (flushPeriodForHeightsCache == 0) {
                flushHeight(product, blockchainId, heightType, tpbtaLane, height);
                setHeightToCache(product, blockchainId, heightKey, height);
                log.debug("flush anchor height ( type: {}, height: {}, tpbta lane: {} ) into DB for blockchain {}-{}",
                        heightType, height, ObjectUtil.isNull(tpbtaLane) ? "" : tpbtaLane.getLaneKey(), product, blockchainId);
            } else {
                AnchorProcessHeights heights = getAnchorProcessHeightsFromCache(product, blockchainId);
                if (ObjectUtil.isNull(heights)) {
                    heights = new AnchorProcessHeights(product, blockchainId);
                }

                heights.getProcessHeights().put(heightKey, height);

                long now = System.currentTimeMillis();
                if (now - heights.getLastUpdateTime() > flushPeriodForHeightsCache) {
                    flushAnchorProcessHeights(heights);
                    log.debug("flush anchor heights into DB for blockchain {}-{}", product, blockchainId);
                    heights.setLastUpdateTime(now);
                }
                setAnchorProcessHeightsToCache(heights);
            }
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_ANCHOR_HEIGHTS_ERROR,
                    String.format("failed to update heights to DB for ( product: %s, blockchain id: %s )", product, blockchainId),
                    e
            );
        }
    }

    @Override
    public void saveBlockchainMeta(BlockchainMeta blockchainMeta) {
        try {
            blockchainService.getBaseMapper().insertBlockchain(
                    blockchainMeta.getProduct(),
                    blockchainMeta.getBlockchainId(),
                    blockchainMeta.getAlias(),
                    blockchainMeta.getDesc(),
                    blockchainMeta.getProperties().encode()
            );
            blockchainMetaCache.put(blockchainMeta.getBlockchainId(), blockchainMeta);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    String.format(
                            "failed to insert blockchain meta to DB for ( product: %s, blockchain id: %s )",
                            blockchainMeta.getProduct(), blockchainMeta.getBlockchainId()
                    ), e
            );
        }
    }

    @Override
    public boolean updateBlockchainMeta(BlockchainMeta blockchainMeta) {
        if (
                blockchainService.getBaseMapper().update(
                        BlockchainEntity.builder()
                                .alias(blockchainMeta.getAlias())
                                .description(blockchainMeta.getDesc())
                                .properties(blockchainMeta.getProperties().encode())
                                .build(),
                        new LambdaUpdateWrapper<BlockchainEntity>()
                                .eq(BlockchainEntity::getBlockchainId, blockchainMeta.getBlockchainId())
                ) == 1
        ) {
            blockchainMetaCache.put(blockchainMeta.getBlockchainId(), blockchainMeta);
            return true;
        }
        return false;
    }

    @Override
    public List<BlockchainMeta> getAllBlockchainMeta() {
        return blockchainService.list().stream()
                .map(ConvertUtil::convertFromBlockchainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<BlockchainMeta> getBlockchainMetaByState(BlockchainStateEnum state) {
        try {
            List<BlockchainEntity> blockchainEntities = blockchainService.lambdaQuery()
                    .like(BlockchainEntity::getProperties, state.getCode())
                    .list();
            if (ObjectUtil.isEmpty(blockchainEntities)) {
                return ListUtil.empty();
            }
            return blockchainEntities.stream()
                    .map(ConvertUtil::convertFromBlockchainEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    e,
                    "failed to query blockchains from DB with state {}", state.getCode()
            );
        }
    }

    @Override
    public BlockchainMeta getBlockchainMetaByDomain(String domain) {
        try {
            if (blockchainMetaCache.containsKey(getDomainBlockchainMetaCacheKey(domain))) {
                BlockchainMeta blockchainMeta = blockchainMetaCache.get(getDomainBlockchainMetaCacheKey(domain), false);
                if (ObjectUtil.isNotNull(blockchainMeta)) {
                    return blockchainMeta;
                }
            }

            BlockchainEntity blockchainEntity = blockchainService.getBaseMapper().queryBlockchainByDomain(domain);
            if (ObjectUtil.isNull(blockchainEntity)) {
                return null;
            }
            BlockchainMeta blockchainMeta = ConvertUtil.convertFromBlockchainEntity(blockchainEntity);
            blockchainMetaCache.put(getDomainBlockchainMetaCacheKey(domain), blockchainMeta);
            if (StrUtil.isAllNotEmpty(domain, blockchainEntity.getBlockchainId())) {
                blockchainIdToDomainCache.put(blockchainEntity.getBlockchainId(), domain);
            }
            return blockchainMeta;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    e,
                    "failed to query blockchains from DB with domain {}", domain
            );
        }
    }

    @Override
    public boolean hasBlockchain(String domain) {
        String blockchainId;
        if (domainCertWrapperCache.containsKey(domain)) {
            blockchainId = domainCertWrapperCache.get(domain).getBlockchainId();
        } else {
            DomainCertEntity domainCertEntity = domainCertMapper.selectOne(
                    new LambdaQueryWrapper<DomainCertEntity>()
                            .select(ListUtil.toList(DomainCertEntity::getBlockchainId))
                            .eq(DomainCertEntity::getDomain, domain)
            );
            if (ObjectUtil.isNull(domainCertEntity)) {
                return false;
            }
            blockchainId = domainCertEntity.getBlockchainId();
        }

        if (StrUtil.isAllNotEmpty(domain, blockchainId)) {
            blockchainIdToDomainCache.put(blockchainId, domain);
        }

        if (blockchainMetaCache.containsKey(getDomainBlockchainMetaCacheKey(domain))) {
            return true;
        }

        return blockchainService.getBaseMapper().exists(
                new LambdaQueryWrapper<BlockchainEntity>()
                        .eq(BlockchainEntity::getBlockchainId, blockchainId)
        );
    }

    @Override
    public List<BlockchainMeta> getBlockchainMetaByPluginServerId(String pluginServerId) {
        try {
            List<BlockchainEntity> blockchainEntities = blockchainService.lambdaQuery()
                    .like(BlockchainEntity::getProperties, pluginServerId)
                    .list();
            if (ObjectUtil.isEmpty(blockchainEntities)) {
                return ListUtil.empty();
            }
            return blockchainEntities.stream()
                    .map(ConvertUtil::convertFromBlockchainEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    e,
                    "failed to query blockchains from DB with plugin server id {}", pluginServerId
            );
        }
    }

    @Override
    public BlockchainMeta getBlockchainMeta(String product, String blockchainId) {
        if (blockchainMetaCache.containsKey(blockchainId)) {
            BlockchainMeta blockchainMeta = blockchainMetaCache.get(blockchainId, false);
            if (ObjectUtil.isNotNull(blockchainMeta)) {
                return blockchainMeta;
            }
        }

        BlockchainEntity blockchainEntity = blockchainService.lambdaQuery()
                .eq(BlockchainEntity::getProduct, product)
                .eq(BlockchainEntity::getBlockchainId, blockchainId)
                .select(
                        ListUtil.of(
                                BlockchainEntity::getProduct,
                                BlockchainEntity::getBlockchainId,
                                BlockchainEntity::getAlias,
                                BlockchainEntity::getDescription,
                                BlockchainEntity::getProperties
                        )
                ).one();
        if (ObjectUtil.isNull(blockchainEntity)) {
            return null;
        }
        BlockchainMeta blockchainMeta = ConvertUtil.convertFromBlockchainEntity(blockchainEntity);
        blockchainMetaCache.put(blockchainId, blockchainMeta);
        return blockchainMeta;
    }

    @Override
    public boolean hasBlockchain(String product, String blockchainId) {
        try {
            if (blockchainMetaCache.containsKey(blockchainId)) {
                return true;
            }

            return blockchainService.exists(
                    new LambdaQueryWrapper<BlockchainEntity>()
                            .eq(BlockchainEntity::getProduct, product)
                            .eq(BlockchainEntity::getBlockchainId, blockchainId)
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    String.format(
                            "failed to query blockchain existence from DB for ( product: %s, blockchain id: %s )",
                            product, blockchainId
                    ), e
            );
        }
    }

    @Override
    public String getBlockchainDomain(String product, String blockchainId) {
        try {
            if (blockchainIdToDomainCache.containsKey(blockchainId)) {
                return blockchainIdToDomainCache.get(blockchainId);
            }
            DomainCertEntity entity = domainCertMapper.selectOne(
                    new LambdaQueryWrapper<DomainCertEntity>()
                            .eq(DomainCertEntity::getProduct, product)
                            .eq(DomainCertEntity::getBlockchainId, blockchainId)
            );
            if (ObjectUtil.isNull(entity)) {
                return "";
            }

            blockchainIdToDomainCache.put(blockchainId, entity.getDomain());

            return entity.getDomain();
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    String.format(
                            "failed to query blockchain existence from DB for ( product: %s, blockchain id: %s )",
                            product, blockchainId
                    ), e
            );
        }
    }

    @Override
    @Transactional(rollbackFor = AntChainBridgeRelayerException.class)
    public List<String> getBlockchainDomainsByState(BlockchainStateEnum state) {
        try {
            List<BlockchainEntity> blockchainEntities = blockchainService.lambdaQuery()
                    .select(ListUtil.toList(BlockchainEntity::getBlockchainId))
                    .like(BlockchainEntity::getProperties, state.getCode())
                    .list();
            if (ObjectUtil.isEmpty(blockchainEntities)) {
                return ListUtil.empty();
            }

            List<DomainCertEntity> domainCertEntities = domainCertMapper.selectList(
                    new LambdaQueryWrapper<DomainCertEntity>()
                            .select(ListUtil.toList(DomainCertEntity::getDomain))
                            .in(
                                    DomainCertEntity::getBlockchainId,
                                    blockchainEntities.stream()
                                            .map(BlockchainEntity::getBlockchainId)
                                            .collect(Collectors.toList())
                            )
            );
            if (ObjectUtil.isEmpty(domainCertEntities)) {
                return ListUtil.empty();
            }

            return domainCertEntities.stream()
                    .map(DomainCertEntity::getDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    e,
                    "failed to query blockchain domain list from DB for state {}",
                    state.getCode()
            );
        }
    }

    @Override
    public DomainCertWrapper getDomainCert(String domain) {
        try {
            if (domainCertWrapperCache.containsKey(domain)) {
                return domainCertWrapperCache.get(domain);
            }

            DomainCertEntity entity = domainCertMapper.selectOne(
                    new LambdaQueryWrapper<DomainCertEntity>()
                            .eq(DomainCertEntity::getDomain, domain)
            );
            if (ObjectUtil.isNull(entity)) {
                return null;
            }

            DomainCertWrapper domainCertWrapper = ConvertUtil.convertFromDomainCertEntity(entity);
            domainCertWrapperCache.put(domain, domainCertWrapper);

            return domainCertWrapper;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    e,
                    "failed to query domain cert from DB for domain {}",
                    domain
            );
        }
    }

    @Override
    public boolean hasDomainCert(String domain) {
        if (domainCertWrapperCache.containsKey(domain)) {
            return true;
        }
        return domainCertMapper.exists(new LambdaQueryWrapper<DomainCertEntity>().eq(DomainCertEntity::getDomain, domain));
    }

    @Override
    public void saveDomainCert(DomainCertWrapper domainCertWrapper) {
        try {
            if (hasDomainCert(domainCertWrapper.getDomain())) {
                throw new RuntimeException(StrUtil.format("domain cert for {} already exist", domainCertWrapper.getDomain()));
            }
            domainCertMapper.insert(ConvertUtil.convertFromDomainCertWrapper(domainCertWrapper));
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    e,
                    "failed to save domain cert to DB for domain {}",
                    domainCertWrapper.getDomain()
            );
        }
    }

    @Override
    public void updateBlockchainInfoOfDomainCert(String domain, String product, String blockchainId) {
        try {
            if (!hasDomainCert(domain)) {
                throw new RuntimeException(StrUtil.format("domain cert for {} not found", domain));
            }
            if (
                    domainCertMapper.update(
                            DomainCertEntity.builder()
                                    .product(product)
                                    .blockchainId(blockchainId)
                                    .build(),
                            new LambdaUpdateWrapper<DomainCertEntity>()
                                    .eq(DomainCertEntity::getDomain, domain)
                    ) != 1
            ) {
                throw new RuntimeException("failed to update the domain cert in DB");
            }
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    e,
                    "failed to update domain cert to DB for domain {}",
                    domain
            );
        }
    }

    @Override
    public void saveBta(BtaDO btaDO) {
        try {
            if (hasBta(btaDO.getDomain(), btaDO.getSubjectVersion())) {
                throw new RuntimeException(StrUtil.format("bta for {} already exist", btaDO.getDomain().getDomain()));
            }
            btaMapper.insert(ConvertUtil.convertFromBtaDO(btaDO));
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    e,
                    "failed to save bta to DB for domain {}",
                    btaDO.getDomain().getDomain()
            );
        }
    }

    @Override
    public BtaDO getBta(CrossChainDomain domain) {
        List<BtaEntity> entities = btaMapper.selectList(
                new LambdaQueryWrapper<BtaEntity>()
                        .eq(BtaEntity::getDomain, domain.getDomain())
        );
        if (ObjectUtil.isEmpty(entities)) {
            return null;
        }
        return entities.stream().max(Comparator.comparing(BtaEntity::getSubjectVersion)).map(ConvertUtil::convertFromBtaEntity).orElse(null);
    }

    @Override
    public boolean hasBta(CrossChainDomain domain) {
        return btaMapper.exists(
                new LambdaQueryWrapper<BtaEntity>()
                        .eq(BtaEntity::getDomain, domain.getDomain())
        );
    }

    @Override
    public boolean hasBta(CrossChainDomain domain, int subjectVersion) {
        return btaMapper.exists(
                new LambdaQueryWrapper<BtaEntity>()
                        .eq(BtaEntity::getDomain, domain.getDomain())
                        .eq(BtaEntity::getSubjectVersion, subjectVersion)
        );
    }

    @Override
    public ValidatedConsensusStateDO getValidatedConsensusState(String ptcServiceId, String domain, CrossChainLane tpbtaLane, BigInteger height) {
        try {
            ValidatedConsensusStateEntity entity = validatedConsensusStateMapper.selectOne(
                    new LambdaQueryWrapper<ValidatedConsensusStateEntity>()
                            .eq(ValidatedConsensusStateEntity::getPtcServiceId, ptcServiceId)
                            .eq(ValidatedConsensusStateEntity::getDomain, domain)
                            .eq(ValidatedConsensusStateEntity::getTpbtaLaneKey, tpbtaLane.getLaneKey())
                            .eq(ValidatedConsensusStateEntity::getHeight, height.toString())
            );
            if (ObjectUtil.isNull(entity)) {
                return null;
            }
            return ConvertUtil.convertFromValidatedConsensusStateEntity(entity);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    e, "Failed to get validated consensus state for ptc {}, domain {} and height {}", ptcServiceId, domain, height
            );
        }
    }

    @Override
    public ValidatedConsensusStateDO getValidatedConsensusState(String ptcServiceId, String domain, CrossChainLane tpbtaLane, String hash) {
        try {
            ValidatedConsensusStateEntity entity = validatedConsensusStateMapper.selectOne(
                    new LambdaQueryWrapper<ValidatedConsensusStateEntity>()
                            .eq(ValidatedConsensusStateEntity::getPtcServiceId, ptcServiceId)
                            .eq(ValidatedConsensusStateEntity::getDomain, domain)
                            .eq(ValidatedConsensusStateEntity::getTpbtaLaneKey, tpbtaLane.getLaneKey())
                            .eq(ValidatedConsensusStateEntity::getHash, hash)
            );
            if (ObjectUtil.isNull(entity)) {
                return null;
            }
            return ConvertUtil.convertFromValidatedConsensusStateEntity(entity);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    e, "Failed to get validated consensus state for ptc {}, domain {} and hash {}", ptcServiceId, domain, hash
            );
        }
    }

    @Override
    @Transactional
    public void setValidatedConsensusState(ValidatedConsensusStateDO validatedConsensusStateDO) {
        try {
            log.info("save validated consensus state (height: {}, hash: {}) from domain {} and tpbta lane {} endorsed by ptc {}",
                    validatedConsensusStateDO.getHeight().toString(),
                    validatedConsensusStateDO.getHashHex(),
                    validatedConsensusStateDO.getDomain().getDomain(),
                    validatedConsensusStateDO.getTpbtaLane().getLaneKey(),
                    validatedConsensusStateDO.getPtcServiceId()
            );
            if (hasValidatedConsensusState(
                    validatedConsensusStateDO.getPtcServiceId(),
                    validatedConsensusStateDO.getDomain().getDomain(),
                    validatedConsensusStateDO.getTpbtaLane(),
                    validatedConsensusStateDO.getHeight()
            )) {
                log.debug("validated consensus state (height: {}, hash: {}) from domain {} endorsed by ptc {} with TpBTA {} already exist, update it instead",
                        validatedConsensusStateDO.getHeight().toString(),
                        validatedConsensusStateDO.getHashHex(),
                        validatedConsensusStateDO.getDomain().getDomain(),
                        validatedConsensusStateDO.getPtcServiceId(),
                        validatedConsensusStateDO.getTpbtaLane().getLaneKey()
                );
                validatedConsensusStateMapper.update(
                        ConvertUtil.convertFromValidatedConsensusStateDO(validatedConsensusStateDO),
                        new LambdaUpdateWrapper<ValidatedConsensusStateEntity>()
                                .eq(ValidatedConsensusStateEntity::getPtcServiceId, validatedConsensusStateDO.getPtcServiceId())
                                .eq(ValidatedConsensusStateEntity::getDomain, validatedConsensusStateDO.getDomain().getDomain())
                                .eq(ValidatedConsensusStateEntity::getTpbtaLaneKey, validatedConsensusStateDO.getTpbtaLane().getLaneKey())
                                .eq(ValidatedConsensusStateEntity::getHeight, validatedConsensusStateDO.getHeight().toString())
                );
            }
            validatedConsensusStateMapper.insert(ConvertUtil.convertFromValidatedConsensusStateDO(validatedConsensusStateDO));
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    e, "Failed to save validated consensus state for ptc {} with TpBTA {}, domain {} and height {}",
                    validatedConsensusStateDO.getPtcServiceId(),
                    validatedConsensusStateDO.getTpbtaLane().getLaneKey(),
                    validatedConsensusStateDO.getDomain(),
                    validatedConsensusStateDO.getHeight()
            );
        }
    }

    @Override
    public boolean hasValidatedConsensusState(String ptcServiceId, String domain, CrossChainLane tpbtaLane, BigInteger height) {
        try {
            return validatedConsensusStateMapper.exists(
                    new LambdaQueryWrapper<ValidatedConsensusStateEntity>()
                            .eq(ValidatedConsensusStateEntity::getPtcServiceId, ptcServiceId)
                            .eq(ValidatedConsensusStateEntity::getDomain, domain)
                            .eq(ValidatedConsensusStateEntity::getTpbtaLaneKey, tpbtaLane.getLaneKey())
                            .eq(ValidatedConsensusStateEntity::getHeight, height.toString())
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    e, "Failed to check if validated consensus state for ptc {}, domain {} and height {} exist", ptcServiceId, domain, height
            );
        }
    }

    @Override
    public boolean hasValidatedConsensusState(String ptcServiceId, String domain, CrossChainLane tpbtaLane, String hash) {
        try {
            return validatedConsensusStateMapper.exists(
                    new LambdaQueryWrapper<ValidatedConsensusStateEntity>()
                            .eq(ValidatedConsensusStateEntity::getPtcServiceId, ptcServiceId)
                            .eq(ValidatedConsensusStateEntity::getDomain, domain)
                            .eq(ValidatedConsensusStateEntity::getTpbtaLaneKey, tpbtaLane.getLaneKey())
                            .eq(ValidatedConsensusStateEntity::getHash, hash)
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    e, "Failed to check if validated consensus state for ptc {}, domain {} and hash {} exist", ptcServiceId, domain, hash
            );
        }
    }

    private void flushAnchorProcessHeights(AnchorProcessHeights heights) {
        for (Map.Entry<String, Long> entry : heights.getProcessHeights().entrySet()) {
            flushHeight(
                    heights.getProduct(),
                    heights.getBlockchainId(),
                    ConvertUtil.getHeightTypeFromHeightKey(entry.getKey()),
                    ConvertUtil.getTpBtaLaneFromHeightKey(entry.getKey()),
                    entry.getValue()
            );
        }
    }

    private void flushHeight(String product, String blockchainId, String heightType, CrossChainLane tpbtaLane, Long height) {
        AnchorProcessEntity entity = new AnchorProcessEntity();
        entity.setBlockHeight(height);
        String tpbtaLaneKey = ObjectUtil.isNull(tpbtaLane) ? "" : tpbtaLane.getLaneKey();
        transactionTemplate.execute(
                new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        if (
                                anchorProcessMapper.exists(
                                        new LambdaQueryWrapper<AnchorProcessEntity>()
                                                .eq(AnchorProcessEntity::getProduct, product)
                                                .eq(AnchorProcessEntity::getBlockchainId, blockchainId)
                                                .eq(AnchorProcessEntity::getTpbtaLaneKey, tpbtaLaneKey)
                                                .eq(AnchorProcessEntity::getTask, heightType)
                                )
                        ) {
                            if (
                                    anchorProcessMapper.update(
                                            entity,
                                            new LambdaUpdateWrapper<AnchorProcessEntity>()
                                                    .eq(AnchorProcessEntity::getProduct, product)
                                                    .eq(AnchorProcessEntity::getBlockchainId, blockchainId)
                                                    .eq(AnchorProcessEntity::getTpbtaLaneKey, tpbtaLaneKey)
                                                    .eq(AnchorProcessEntity::getTask, heightType)
                                    ) != 1
                            ) {
                                throw new RuntimeException(String.format("update ( height type: %s, value: %d ) failed", heightType, height));
                            }
                            return;
                        }

                        entity.setBlockchainId(blockchainId);
                        entity.setTpbtaLaneKey(tpbtaLaneKey);
                        entity.setProduct(product);
                        entity.setTask(heightType);
                        if (anchorProcessMapper.insert(entity) != 1) {
                            throw new RuntimeException(String.format("insert ( height type: %s, value: %d ) failed", heightType, height));
                        }
                    }
                }
        );
    }

    private Long getAnchorProcessHeightFromDB(String product, String blockchainId, String heightType, CrossChainLane tpbtaLane) {
        LambdaQueryWrapper<AnchorProcessEntity> wrapper = new LambdaQueryWrapper<AnchorProcessEntity>()
                .select(ListUtil.toList(AnchorProcessEntity::getBlockHeight))
                .eq(AnchorProcessEntity::getProduct, product)
                .eq(AnchorProcessEntity::getBlockchainId, blockchainId)
                .eq(AnchorProcessEntity::getTask, heightType);
        wrapper = ObjectUtil.isNull(tpbtaLane) ? wrapper : wrapper.eq(AnchorProcessEntity::getTpbtaLaneKey, tpbtaLane.getLaneKey());
        AnchorProcessEntity entity = anchorProcessMapper.selectOne(wrapper);
        if (ObjectUtil.isNull(entity)) {
            return 0L;
        }
        return entity.getBlockHeight();
    }

    private Long getHeightFromCache(String product, String blockchainId, String heightKey) {
       return getHeightFromCache(product, blockchainId, heightKey, true);
    }

    private Long getHeightFromCache(String product, String blockchainId, String heightKey, boolean tryFlush) {
        AnchorProcessHeights heights = getAnchorProcessHeightsFromCache(product, blockchainId);
        if (ObjectUtil.isNull(heights)) {
            return null;
        }
        if (tryFlush) {
            long now = System.currentTimeMillis();
            if (now - heights.getLastUpdateTime() > flushPeriodForHeightsCache) {
                flushAnchorProcessHeights(heights);
                log.debug("flush anchor heights into DB for blockchain {}-{} when reading from cache", product, blockchainId);
                heights.setLastUpdateTime(now);
            }
        }

        return heights.getProcessHeights().getOrDefault(heightKey, null);
    }

    private AnchorProcessHeights getAnchorProcessHeightsFromCache(String product, String blockchainId) {
        RBucket<byte[]> bucket = redisson.getBucket(AnchorProcessHeights.getKey(product, blockchainId), ByteArrayCodec.INSTANCE);
        byte[] rawHeights = bucket.get();
        if (ObjectUtil.isEmpty(rawHeights)) {
            return null;
        }
        bucket.expire(Instant.now().plusMillis(ttlForHeightsCache));
        return AnchorProcessHeights.decode(rawHeights);
    }

    private void setHeightToCache(String product, String blockchainId, String heightKey, Long height) {
        AnchorProcessHeights heights = getAnchorProcessHeightsFromCache(product, blockchainId);
        if (ObjectUtil.isNull(heights)) {
            heights = new AnchorProcessHeights(product, blockchainId);
            heights.getProcessHeights().put(heightKey, height);
        }
        setAnchorProcessHeightsToCache(heights);
    }

    private void setAnchorProcessHeightsToCache(AnchorProcessHeights heights) {
        redisson.getBucket(AnchorProcessHeights.getKey(heights.getProduct(), heights.getBlockchainId()), ByteArrayCodec.INSTANCE)
                .set(heights.encode(), Duration.of(ttlForHeightsCache, ChronoUnit.MILLIS));
    }

    private String getDomainBlockchainMetaCacheKey(String domain) {
        return "%domain%" + domain;
    }
}
