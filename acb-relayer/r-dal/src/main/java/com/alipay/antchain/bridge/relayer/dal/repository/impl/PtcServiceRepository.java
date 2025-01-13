/*
 * Copyright 2024 Ant Group
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
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainIdentity;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.relayer.commons.constant.PtcServiceStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.PtcServiceDO;
import com.alipay.antchain.bridge.relayer.commons.model.PtcTrustRootDO;
import com.alipay.antchain.bridge.relayer.commons.model.PtcVerifyAnchorDO;
import com.alipay.antchain.bridge.relayer.commons.model.TpBtaDO;
import com.alipay.antchain.bridge.relayer.dal.entities.PtcServiceEntity;
import com.alipay.antchain.bridge.relayer.dal.entities.PtcTrustRootEntity;
import com.alipay.antchain.bridge.relayer.dal.entities.PtcVerifyAnchorEntity;
import com.alipay.antchain.bridge.relayer.dal.entities.TpBtaEntity;
import com.alipay.antchain.bridge.relayer.dal.mapper.PtcServiceMapper;
import com.alipay.antchain.bridge.relayer.dal.mapper.PtcTrustRootMapper;
import com.alipay.antchain.bridge.relayer.dal.mapper.PtcVerifyAnchorMapper;
import com.alipay.antchain.bridge.relayer.dal.mapper.TpBtaMapper;
import com.alipay.antchain.bridge.relayer.dal.repository.IPtcServiceRepository;
import com.alipay.antchain.bridge.relayer.dal.utils.ConvertUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class PtcServiceRepository implements IPtcServiceRepository {

    @Resource
    private PtcServiceMapper ptcServiceMapper;

    @Resource
    private TpBtaMapper tpBtaMapper;

    @Resource
    private PtcTrustRootMapper ptcTrustRootMapper;

    @Resource
    private PtcVerifyAnchorMapper ptcVerifyAnchorMapper;

    private final Cache<String, TpBtaDO> tpBtaDOCache;

    public PtcServiceRepository(
            @Value("${relayer.dal.ptc.tpbta.cache.size:32}") int cacheSize,
            @Value("${relayer.dal.ptc.tpbta.cache.timeout:30000}") long cacheTimeout
    ) {
        this.tpBtaDOCache = CacheUtil.newLRUCache(cacheSize, cacheTimeout);
    }

    @Override
    public void savePtcServiceData(PtcServiceDO ptcServiceDO) {
        try {
            ptcServiceMapper.insert(ConvertUtil.convertFromPtcServiceDO(ptcServiceDO));
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PTC_SERVICE_ERROR,
                    e,
                    "save ptc service data error, serviceId: {}",
                    ptcServiceDO.getServiceId()
            );
        }
    }

    @Override
    public PtcServiceDO getPtcServiceData(String serviceId) {
        try {
            PtcServiceEntity entity = ptcServiceMapper.selectOne(
                    new LambdaQueryWrapper<PtcServiceEntity>()
                            .eq(PtcServiceEntity::getServiceId, serviceId)
            );
            if (ObjectUtil.isNull(entity)) {
                return null;
            }
            return ConvertUtil.convertFromPtcServiceEntity(entity);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PTC_SERVICE_ERROR,
                    e,
                    "get ptc service data error, serviceId: {}",
                    serviceId
            );
        }
    }

    @Override
    public PtcServiceDO getPtcServiceData(ObjectIdentity ptcOwnerOid) {
        try {
            PtcServiceEntity entity = ptcServiceMapper.selectOne(
                    new LambdaQueryWrapper<PtcServiceEntity>()
                            .eq(PtcServiceEntity::getOwnerIdHex, ptcOwnerOid.toHex())
            );
            if (ObjectUtil.isNull(entity)) {
                return null;
            }
            return ConvertUtil.convertFromPtcServiceEntity(entity);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PTC_SERVICE_ERROR,
                    e,
                    "get ptc service data error, ptcOwnerOid: {}",
                    ptcOwnerOid.toHex()
            );
        }
    }

    @Override
    public void removePtcServiceData(String serviceId) {
        try {
            ptcServiceMapper.delete(
                    new LambdaQueryWrapper<PtcServiceEntity>()
                            .eq(PtcServiceEntity::getServiceId, serviceId)
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PTC_SERVICE_ERROR,
                    e,
                    "remove ptc service data error, serviceId: {}",
                    serviceId
            );
        }
    }

    @Override
    public boolean hasPtcServiceData(String serviceId) {
        return ptcServiceMapper.exists(
                new LambdaQueryWrapper<PtcServiceEntity>()
                        .eq(PtcServiceEntity::getServiceId, serviceId)
        );
    }

    @Override
    public PtcServiceStateEnum queryPtcServiceState(String serviceId) {
        try {
            PtcServiceEntity entity = this.ptcServiceMapper.selectOne(
                    new LambdaQueryWrapper<PtcServiceEntity>()
                            .select(ListUtil.toList(PtcServiceEntity::getState))
                            .eq(PtcServiceEntity::getServiceId, serviceId)
            );
            if (ObjectUtil.isNull(entity)) {
                throw new RuntimeException(StrUtil.format("no ptc service data found for service id: {}", serviceId));
            }
            /**/
            return entity.getState();
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PTC_SERVICE_ERROR,
                    e,
                    "query ptc service state error, serviceId: {}",
                    serviceId
            );
        }
    }

    @Override
    public void updatePtcServiceState(String serviceId, PtcServiceStateEnum state) {
        try {
            PtcServiceEntity entity = new PtcServiceEntity();
            entity.setState(state);
            ptcServiceMapper.update(
                    entity,
                    new LambdaQueryWrapper<PtcServiceEntity>()
                            .eq(PtcServiceEntity::getServiceId, serviceId)
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PTC_SERVICE_ERROR,
                    e,
                    "update ptc service state error, serviceId: {}, state: {}",
                    serviceId,
                    state.name()
            );
        }
    }

    @Override
    public TpBtaDO getMatchedTpBta(CrossChainLane lane) {
        try {
            List<TpBtaEntity> entityList = searchTpBta(lane, -1);
            if (ObjectUtil.isEmpty(entityList)) {
                return null;
            }
            return ConvertUtil.convertFromTpBtaEntity(
                    entityList.stream().max(Comparator.comparingInt(TpBtaEntity::getTpbtaVersion)).get()
            );

        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PTC_SERVICE_ERROR,
                    e, "Failed to get tpbta for lane {}", lane.getLaneKey()
            );
        }
    }

    @Override
    public TpBtaDO getMatchedTpBta(CrossChainLane lane, int tpbtaVersion) {
        try {
            List<TpBtaEntity> entityList = searchTpBta(lane, tpbtaVersion);
            if (ObjectUtil.isEmpty(entityList)) {
                return null;
            }
            return ConvertUtil.convertFromTpBtaEntity(
                    entityList.stream().max(Comparator.comparingInt(TpBtaEntity::getTpbtaVersion)).get()
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PTC_SERVICE_ERROR,
                    e, "Failed to get tpbta for lane {} and version {}", lane.getLaneKey(), tpbtaVersion
            );
        }
    }

    @Override
    public TpBtaDO getExactTpBta(CrossChainLane lane) {
        return getExactTpBta(lane, -1);
    }

    @Override
    public TpBtaDO getExactTpBta(CrossChainLane lane, int tpbtaVersion) {
        try {
            LambdaQueryWrapper<TpBtaEntity> wrapper = new LambdaQueryWrapper<TpBtaEntity>()
                    .eq(TpBtaEntity::getSenderDomain, lane.getSenderDomain().getDomain())
                    .eq(TpBtaEntity::getSenderId, ObjectUtil.isNull(lane.getSenderId()) ? "" : lane.getSenderId().toHex())
                    .eq(TpBtaEntity::getReceiverDomain, ObjectUtil.isNull(lane.getReceiverDomain()) ? "" : lane.getReceiverDomain().getDomain())
                    .eq(TpBtaEntity::getReceiverId, ObjectUtil.isNull(lane.getReceiverId()) ? "" : lane.getReceiverId().toHex());
            List<TpBtaEntity> entityList = tpBtaMapper.selectList(
                    tpbtaVersion == -1 ? wrapper : wrapper.eq(TpBtaEntity::getTpbtaVersion, tpbtaVersion)
            );
            if (ObjectUtil.isEmpty(entityList)) {
                return null;
            }

            TpBtaDO tpBtaDO = ConvertUtil.convertFromTpBtaEntity(
                    entityList.stream().max(Comparator.comparingInt(TpBtaEntity::getTpbtaVersion)).get()
            );
            tpBtaDOCache.put(tpbtaCacheKey(tpBtaDO.getCrossChainLane(), tpBtaDO.getTpBtaVersion()), tpBtaDO);
            return tpBtaDO;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PTC_SERVICE_ERROR,
                    e, "Failed to get tpbta for lane {} and version {}", lane.getLaneKey(), tpbtaVersion
            );
        }
    }

    @Override
    public void setTpBta(TpBtaDO tpBtaDO) {
        try {
            CrossChainLane lane = tpBtaDO.getCrossChainLane();
            if (hasTpBta(lane, tpBtaDO.getTpbta().getTpbtaVersion())) {
                tpBtaDOCache.put(tpbtaCacheKey(lane, tpBtaDO.getTpBtaVersion()), tpBtaDO);
                tpBtaMapper.update(
                        ConvertUtil.convertFromTpBtaDO(tpBtaDO),
                        new LambdaUpdateWrapper<TpBtaEntity>()
                                .eq(TpBtaEntity::getSenderDomain, lane.getSenderDomain().getDomain())
                                .eq(ObjectUtil.isNotNull(lane.getSenderIdHex()), TpBtaEntity::getSenderId, lane.getSenderIdHex())
                                .eq(TpBtaEntity::getSenderId, ObjectUtil.isNull(lane.getSenderId()) ? "" : lane.getSenderId().toHex())
                                .eq(TpBtaEntity::getReceiverDomain, ObjectUtil.isNull(lane.getReceiverDomain()) ? "" : lane.getReceiverDomain().getDomain())
                                .eq(TpBtaEntity::getReceiverId, ObjectUtil.isNull(lane.getReceiverId()) ? "" : lane.getReceiverId().toHex())
                                .eq(TpBtaEntity::getTpbtaVersion, tpBtaDO.getTpbta().getTpbtaVersion())
                );
                return;
            }
            tpBtaMapper.insert(ConvertUtil.convertFromTpBtaDO(tpBtaDO));
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PTC_SERVICE_ERROR,
                    e, "Failed to save tpbta for lane {}", tpBtaDO.getCrossChainLane().getLaneKey()
            );
        }
    }

    @Override
    public boolean hasTpBta(CrossChainLane lane, int tpbtaVersion) {
        try {
            if (tpBtaDOCache.containsKey(tpbtaCacheKey(lane, tpbtaVersion))) {
                return true;
            }
            return tpBtaMapper.exists(
                    new LambdaQueryWrapper<TpBtaEntity>()
                            .eq(TpBtaEntity::getSenderDomain, lane.getSenderDomain().getDomain())
                            .eq(ObjectUtil.isNotNull(lane.getSenderIdHex()), TpBtaEntity::getSenderId, lane.getSenderIdHex())
                            .eq(TpBtaEntity::getSenderId, ObjectUtil.isNull(lane.getSenderId()) ? "" : lane.getSenderId().toHex())
                            .eq(TpBtaEntity::getReceiverDomain, ObjectUtil.isNull(lane.getReceiverDomain()) ? "" : lane.getReceiverDomain().getDomain())
                            .eq(TpBtaEntity::getReceiverId, ObjectUtil.isNull(lane.getReceiverId()) ? "" : lane.getReceiverId().toHex())
                            .eq(TpBtaEntity::getTpbtaVersion, tpbtaVersion)
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PTC_SERVICE_ERROR,
                    e, "Failed to check if tpbta for lane {} and version {} exist", lane.getLaneKey(), tpbtaVersion
            );
        }
    }

    @Override
    public List<String> getAllPtcServiceIdForDomain(String domain) {
        try {
            List<TpBtaEntity> entities = tpBtaMapper.selectList(
                    new LambdaQueryWrapper<TpBtaEntity>()
                            .apply("DISTINCT " + TpBtaEntity.Fields.ptcServiceId)
                            .eq(TpBtaEntity::getSenderDomain, domain)
            );
            if (ObjectUtil.isEmpty(entities)) {
                return ListUtil.empty();
            }
            return entities.stream().map(TpBtaEntity::getPtcServiceId).collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PTC_SERVICE_ERROR,
                    e, "Failed to get all ptc service id for domain {}", domain
            );
        }
    }

    @Override
    public List<PtcServiceDO> getAllWorkingPtcServices() {
        List<PtcServiceEntity> entities = ptcServiceMapper.selectList(
                new LambdaQueryWrapper<PtcServiceEntity>()
                        .eq(PtcServiceEntity::getState, PtcServiceStateEnum.WORKING)
        );
        if (ObjectUtil.isEmpty(entities)) {
            return ListUtil.empty();
        }
        return entities.stream().map(ConvertUtil::convertFromPtcServiceEntity).collect(Collectors.toList());
    }

    @Override
    public List<TpBtaDO> getAllTpBtaByDomain(String ptcServiceId, String domain) {
        try {
            List<TpBtaEntity> entities = tpBtaMapper.selectList(
                    new LambdaQueryWrapper<TpBtaEntity>()
                            .eq(TpBtaEntity::getPtcServiceId, ptcServiceId)
                            .eq(TpBtaEntity::getSenderDomain, domain)
            );
            if (ObjectUtil.isEmpty(entities)) {
                return ListUtil.empty();
            }
            return entities.stream().map(ConvertUtil::convertFromTpBtaEntity).collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_PTC_SERVICE_ERROR,
                    e, "Failed to get all ptc service id for domain {}", domain
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public List<TpBtaDO> getAllValidTpBtaForDomain(CrossChainDomain domain) {
        TpBtaDO tpBtaDO = getExactTpBta(new CrossChainLane(domain));
        if (ObjectUtil.isNotNull(tpBtaDO)) {
            return ListUtil.toList(tpBtaDO);
        }

        List<TpBtaEntity> entities = tpBtaMapper.selectList(
                new LambdaQueryWrapper<TpBtaEntity>()
                        .eq(TpBtaEntity::getSenderDomain, domain.getDomain())
                        .ne(TpBtaEntity::getReceiverDomain, "")
                        .eq(TpBtaEntity::getSenderId, "")
                        .eq(TpBtaEntity::getReceiverId, "")
        );

        Map<String, TpBtaEntity> channelLevelMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(entities)) {
            channelLevelMap = entities.stream()
                    .collect(Collectors.groupingBy(TpBtaEntity::getReceiverDomain, Collectors.toList()))
                    .entrySet().stream().map(
                            entry -> MapUtil.entry(entry.getKey(), entry.getValue().stream().max(Comparator.comparingInt(TpBtaEntity::getTpbtaVersion)))
                    ).collect(Collectors.toMap(Map.Entry::getKey, o -> o.getValue().orElseThrow(() -> new RuntimeException("unexpected internal error"))));
        }

        entities = tpBtaMapper.selectList(
                new LambdaQueryWrapper<TpBtaEntity>()
                        .eq(TpBtaEntity::getSenderDomain, domain.getDomain())
                        .ne(TpBtaEntity::getReceiverDomain, "")
                        .notIn(!channelLevelMap.isEmpty(), TpBtaEntity::getReceiverDomain, channelLevelMap.keySet())
                        .ne(TpBtaEntity::getSenderId, "")
                        .ne(TpBtaEntity::getReceiverId, "")
        );

        Map<String, TpBtaEntity> laneLevelMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(entities)) {
            laneLevelMap = entities.stream()
                    .collect(Collectors.groupingBy(
                            entity -> new CrossChainLane(
                                    new CrossChainDomain(entity.getSenderDomain()),
                                    new CrossChainDomain(entity.getReceiverDomain()),
                                    new CrossChainIdentity(HexUtil.decodeHex(entity.getSenderId())),
                                    new CrossChainIdentity(HexUtil.decodeHex(entity.getReceiverId()))
                            ).getLaneKey(), Collectors.toList()
                    )).entrySet().stream().map(
                            entry -> MapUtil.entry(entry.getKey(), entry.getValue().stream().max(Comparator.comparingInt(TpBtaEntity::getTpbtaVersion)))
                    ).collect(Collectors.toMap(Map.Entry::getKey, o -> o.getValue().orElseThrow(() -> new RuntimeException("unexpected internal error"))));
        }

        List<TpBtaDO> result = new ArrayList<>();
        result.addAll(channelLevelMap.values().stream().map(ConvertUtil::convertFromTpBtaEntity).collect(Collectors.toList()));
        result.addAll(laneLevelMap.values().stream().map(ConvertUtil::convertFromTpBtaEntity).collect(Collectors.toList()));

        return result;
    }

    private List<TpBtaEntity> searchTpBta(CrossChainLane lane, int tpbtaVersion) {
        // search the blockchain level first
        LambdaQueryWrapper<TpBtaEntity> wrapper = new LambdaQueryWrapper<TpBtaEntity>()
                .eq(TpBtaEntity::getSenderDomain, lane.getSenderDomain().getDomain())
                .eq(TpBtaEntity::getReceiverDomain, "")
                .eq(TpBtaEntity::getSenderId, "")
                .eq(TpBtaEntity::getReceiverId, "");
        List<TpBtaEntity> entityList = tpBtaMapper.selectList(
                tpbtaVersion == -1 ? wrapper : wrapper.eq(TpBtaEntity::getTpbtaVersion, tpbtaVersion)
        );
        if (ObjectUtil.isNotEmpty(entityList)) {
            return entityList;
        }

        if (ObjectUtil.isNull(lane.getReceiverDomain()) || ObjectUtil.isEmpty(lane.getReceiverDomain().getDomain())) {
            return ListUtil.empty();
        }
        // search the channel level
        wrapper = new LambdaQueryWrapper<TpBtaEntity>()
                .eq(TpBtaEntity::getSenderDomain, lane.getSenderDomain().getDomain())
                .eq(TpBtaEntity::getReceiverDomain, lane.getReceiverDomain().getDomain())
                .eq(TpBtaEntity::getSenderId, "")
                .eq(TpBtaEntity::getReceiverId, "");
        entityList = tpBtaMapper.selectList(
                tpbtaVersion == -1 ? wrapper : wrapper.eq(TpBtaEntity::getTpbtaVersion, tpbtaVersion)
        );
        if (ObjectUtil.isNotEmpty(entityList)) {
            return entityList;
        }

        if (ObjectUtil.isNull(lane.getSenderId()) || ObjectUtil.isNull(lane.getReceiverId())
                || ObjectUtil.isEmpty(lane.getSenderId().getRawID()) || ObjectUtil.isEmpty(lane.getReceiverId().getRawID())) {
            return ListUtil.empty();
        }
        // search the lane level
        wrapper = new LambdaQueryWrapper<TpBtaEntity>()
                .eq(TpBtaEntity::getSenderDomain, lane.getSenderDomain().getDomain())
                .eq(TpBtaEntity::getSenderId, lane.getSenderId().toHex())
                .eq(TpBtaEntity::getReceiverDomain, lane.getReceiverDomain().getDomain())
                .eq(TpBtaEntity::getReceiverId, lane.getReceiverId().toHex());
        entityList = tpBtaMapper.selectList(
                tpbtaVersion == -1 ? wrapper : wrapper.eq(TpBtaEntity::getTpbtaVersion, tpbtaVersion)
        );
        if (ObjectUtil.isNotEmpty(entityList)) {
            return entityList;
        }

        return ListUtil.empty();
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void savePtcTrustRoot(PtcTrustRootDO ptcTrustRootDO) {
        if (ptcTrustRootMapper.exists(
                new LambdaQueryWrapper<PtcTrustRootEntity>()
                        .eq(PtcTrustRootEntity::getOwnerIdHex, ptcTrustRootDO.getOwnerOidHex())
        )) {
            log.info("ptc trust root for {} already exists, update it", ptcTrustRootDO.getOwnerOidHex());
            ptcTrustRootMapper.update(
                    ConvertUtil.convertFromPtcTrustRootDO(ptcTrustRootDO),
                    new LambdaUpdateWrapper<PtcTrustRootEntity>()
                            .eq(PtcTrustRootEntity::getOwnerIdHex, ptcTrustRootDO.getOwnerOidHex())
            );
        } else {
            log.info("ptc trust root for {} not exists, insert it", ptcTrustRootDO.getOwnerOidHex());
            ptcTrustRootMapper.insert(ConvertUtil.convertFromPtcTrustRootDO(ptcTrustRootDO));
        }

        ptcTrustRootDO.getPtcTrustRoot().getVerifyAnchorMap().values()
                .forEach(verifyAnchor -> addPtcVerifyAnchor(
                        PtcVerifyAnchorDO.builder()
                                .ptcServiceId(ptcTrustRootDO.getPtcServiceId())
                                .ownerOid(ObjectIdentity.decodeFromHex(ptcTrustRootDO.getOwnerOidHex()))
                                .ptcVerifyAnchor(verifyAnchor)
                                .build()
                ));
    }

    private void addPtcVerifyAnchor(PtcVerifyAnchorDO ptcVerifyAnchorDO) {
        if (ptcVerifyAnchorMapper.exists(
                new LambdaQueryWrapper<PtcVerifyAnchorEntity>()
                        .eq(PtcVerifyAnchorEntity::getOwnerIdHex, ptcVerifyAnchorDO.getOwnerOid().toHex())
                        .eq(PtcVerifyAnchorEntity::getVersionNum, ptcVerifyAnchorDO.getPtcVerifyAnchor().getVersion().toString())
        )) {
            log.info("ptc verify anchor with version {} for {} already exists, skip it",
                    ptcVerifyAnchorDO.getPtcVerifyAnchor().getVersion().toString(), ptcVerifyAnchorDO.getOwnerOid().toHex());
            return;
        }
        log.info("add ptc verify anchor for {} with version {}", ptcVerifyAnchorDO.getOwnerOid().toHex(), ptcVerifyAnchorDO.getPtcVerifyAnchor().getVersion().toString());

        ptcVerifyAnchorMapper.insert(ConvertUtil.convertFromPtcVerifyAnchorDO(ptcVerifyAnchorDO));
    }

    @Override
    public boolean hasPtcTrustRoot(ObjectIdentity ownerOid) {
        return ptcTrustRootMapper.exists(
                new LambdaQueryWrapper<PtcTrustRootEntity>()
                        .eq(PtcTrustRootEntity::getOwnerIdHex, ownerOid.toHex())
        );
    }

    @Override
    public PtcTrustRootDO getPtcTrustRoot(ObjectIdentity ownerOid) {
        PtcTrustRootEntity entity = ptcTrustRootMapper.selectOne(
                new LambdaQueryWrapper<PtcTrustRootEntity>()
                        .eq(PtcTrustRootEntity::getOwnerIdHex, ownerOid.toHex())
        );
        if (entity == null) {
            return null;
        }
        PtcTrustRootDO ptcTrustRootDO = ConvertUtil.convertFromPtcTrustRootEntity(entity);
        List<PtcVerifyAnchorEntity> entities = ptcVerifyAnchorMapper.selectList(
                new LambdaQueryWrapper<PtcVerifyAnchorEntity>()
                        .eq(PtcVerifyAnchorEntity::getOwnerIdHex, ownerOid.toHex())
        );
        if (ObjectUtil.isNotEmpty(entities)) {
            entities.stream()
                    .map(ConvertUtil::convertFromPtcVerifyAnchorEntity)
                    .forEach(ptcTrustRootDO::addVerifyAnchor);
        }
        return ptcTrustRootDO;
    }

    @Override
    public PtcTrustRootDO getPtcTrustRoot(String ptcServiceId) {
        PtcTrustRootEntity entity = ptcTrustRootMapper.selectOne(
                new LambdaQueryWrapper<PtcTrustRootEntity>()
                        .eq(PtcTrustRootEntity::getPtcServiceId, ptcServiceId)
        );
        if (entity == null) {
            return null;
        }
        PtcTrustRootDO ptcTrustRootDO = ConvertUtil.convertFromPtcTrustRootEntity(entity);
        List<PtcVerifyAnchorEntity> entities = ptcVerifyAnchorMapper.selectList(
                new LambdaQueryWrapper<PtcVerifyAnchorEntity>()
                        .eq(PtcVerifyAnchorEntity::getPtcServiceId, ptcServiceId)
        );
        if (ObjectUtil.isNotEmpty(entities)) {
            entities.stream()
                    .map(ConvertUtil::convertFromPtcVerifyAnchorEntity)
                    .forEach(ptcTrustRootDO::addVerifyAnchor);
        }
        return ptcTrustRootDO;
    }

    @Override
    public PtcVerifyAnchorDO getPtcVerifyAnchor(ObjectIdentity ptcOwnerOid, BigInteger version) {
        PtcVerifyAnchorEntity entity = ptcVerifyAnchorMapper.selectOne(
                new LambdaQueryWrapper<PtcVerifyAnchorEntity>()
                        .eq(PtcVerifyAnchorEntity::getOwnerIdHex, ptcOwnerOid.toHex())
        );
        if (ObjectUtil.isNull(entity)) {
            return null;
        }
        return ConvertUtil.convertFromPtcVerifyAnchorEntity(entity);
    }

    @Override
    public BigInteger getMaxPtcVerifyAnchorVersion(ObjectIdentity ptcOwnerOid) {
        List<PtcVerifyAnchorEntity> entities = ptcVerifyAnchorMapper.selectList(
                new LambdaQueryWrapper<PtcVerifyAnchorEntity>()
                        .select(ListUtil.toList(PtcVerifyAnchorEntity::getVersionNum))
                        .eq(PtcVerifyAnchorEntity::getOwnerIdHex, ptcOwnerOid.toHex())
        );
        if (ObjectUtil.isEmpty(entities)) {
            return null;
        }
        return entities.stream().map(entity -> new BigInteger(entity.getVersionNum())).max(BigInteger::compareTo).get();
    }

    private String tpbtaCacheKey(CrossChainLane lane, int tpbtaVersion) {
        return StrUtil.format("TpBta:{}:{}", lane.getLaneKey(), tpbtaVersion);
    }
}
