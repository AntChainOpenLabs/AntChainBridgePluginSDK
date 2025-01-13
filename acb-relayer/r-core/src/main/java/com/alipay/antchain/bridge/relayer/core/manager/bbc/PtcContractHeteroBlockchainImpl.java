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

package com.alipay.antchain.bridge.relayer.core.manager.bbc;

import java.math.BigInteger;
import java.util.Set;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTypeEnum;
import com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyBlockchainTrustAnchor;
import com.alipay.antchain.bridge.relayer.commons.model.PtcTrustRootDO;
import com.alipay.antchain.bridge.relayer.core.types.pluginserver.IBBCServiceClient;

public class PtcContractHeteroBlockchainImpl implements IPtcContract {

    private IBBCServiceClient bbcServiceClient;

    public PtcContractHeteroBlockchainImpl(IBBCServiceClient bbcServiceClient) {
        this.bbcServiceClient = bbcServiceClient;
    }

    @Override
    public void deployContract() {
        bbcServiceClient.setupPTCContract();
    }

    @Override
    public boolean checkIfTpBtaOnChain(CrossChainLane tpbtaLane, int tpbtaVersion) {
        return this.bbcServiceClient.hasTpBta(tpbtaLane, tpbtaVersion);
    }

    @Override
    public void addTpBtaOnChain(ThirdPartyBlockchainTrustAnchor tpBta) {
        this.bbcServiceClient.addTpBta(tpBta);
    }

    @Override
    public boolean checkIfVerifyAnchorOnChain(ObjectIdentity ptcOwnerOid, BigInteger verifyAnchorVersion) {
        return this.bbcServiceClient.hasPTCVerifyAnchor(ptcOwnerOid, verifyAnchorVersion);
    }

    @Override
    public boolean checkIfPtcTrustRootOnChain(ObjectIdentity ptcOwnerOid) {
        return this.bbcServiceClient.hasPTCTrustRoot(ptcOwnerOid);
    }

    @Override
    public void updatePtcTrustRoot(PtcTrustRootDO ptcTrustRootDO) {
        this.bbcServiceClient.updatePTCTrustRoot(ptcTrustRootDO.getPtcTrustRoot());
    }

    @Override
    public boolean checkIfPtcTypeSupportOnChain(PTCTypeEnum ptcType) {
        Set<PTCTypeEnum> supportedPtcTypeSet = this.bbcServiceClient.getSupportedPTCType();
        if (ObjectUtil.isNull(supportedPtcTypeSet)) {
            throw new RuntimeException("supported ptc type set is null");
        }
        return supportedPtcTypeSet.contains(ptcType);
    }
}
