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

package com.alipay.antchain.bridge.relayer.dal.repository;

import java.math.BigInteger;
import java.util.List;

import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.relayer.commons.constant.PtcServiceStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.PtcServiceDO;
import com.alipay.antchain.bridge.relayer.commons.model.PtcTrustRootDO;
import com.alipay.antchain.bridge.relayer.commons.model.PtcVerifyAnchorDO;
import com.alipay.antchain.bridge.relayer.commons.model.TpBtaDO;

public interface IPtcServiceRepository {

    void savePtcServiceData(PtcServiceDO ptcServiceDO);

    PtcServiceDO getPtcServiceData(String serviceId);

    PtcServiceDO getPtcServiceData(ObjectIdentity ptcOwnerOid);

    boolean hasPtcServiceData(String serviceId);

    PtcServiceStateEnum queryPtcServiceState(String serviceId);

    void updatePtcServiceState(String serviceId, PtcServiceStateEnum state);

    void removePtcServiceData(String serviceId);

    TpBtaDO getMatchedTpBta(CrossChainLane lane);

    TpBtaDO getMatchedTpBta(CrossChainLane lane, int tpbtaVersion);

    TpBtaDO getExactTpBta(CrossChainLane lane);

    TpBtaDO getExactTpBta(CrossChainLane lane, int tpbtaVersion);

    List<String> getAllPtcServiceIdForDomain(String domain);

    List<PtcServiceDO> getAllWorkingPtcServices();

    void setTpBta(TpBtaDO tpBtaDO);

    boolean hasTpBta(CrossChainLane lane, int tpbtaVersion);

    List<TpBtaDO> getAllTpBtaByDomain(String ptcServiceId, String domain);

    List<TpBtaDO> getAllValidTpBtaForDomain(CrossChainDomain domain);

    void savePtcTrustRoot(PtcTrustRootDO ptcTrustRootDO);

    boolean hasPtcTrustRoot(ObjectIdentity ownerOid);

    PtcTrustRootDO getPtcTrustRoot(ObjectIdentity ownerOid);

    PtcTrustRootDO getPtcTrustRoot(String ptcServiceId);

    PtcVerifyAnchorDO getPtcVerifyAnchor(ObjectIdentity ptcOwnerOid, BigInteger version);

    BigInteger getMaxPtcVerifyAnchorVersion(ObjectIdentity ptcOwnerOid);
}
