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

package com.alipay.antchain.bridge.relayer.commons.model;

import java.util.Comparator;
import java.util.HashMap;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.PTCCredentialSubject;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.commons.core.ptc.PTCVerifyAnchor;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PtcTrustRootDO {

    private String ptcServiceId;

    private PTCTrustRoot ptcTrustRoot;

    public String getOwnerOidHex() {
        return HexUtil.encodeHexStr(ptcTrustRoot.getPtcCredentialSubject().getApplicant().encode());
    }

    public PTCVerifyAnchor getLatestVerifyAnchor() {
        return this.ptcTrustRoot.getVerifyAnchorMap().values().stream()
                .max(Comparator.comparing(PTCVerifyAnchor::getVersion)).orElse(null);
    }

    public CrossChainDomain getIssuerBcdnsDomainSpace() {
        return this.ptcTrustRoot.getIssuerBcdnsDomainSpace();
    }

    public byte[] getNetworkInfo() {
        return this.ptcTrustRoot.getNetworkInfo();
    }

    public PTCCredentialSubject getPtcCredentialSubject() {
        return this.ptcTrustRoot.getPtcCredentialSubject();
    }

    public AbstractCrossChainCertificate getPtcCrossChainCert() {
        return this.ptcTrustRoot.getPtcCrossChainCert();
    }

    public SignAlgoEnum getTrustRootSignAlgo() {
        return this.ptcTrustRoot.getSigAlgo();
    }

    public byte[] getTrustRootSignature() {
        return this.ptcTrustRoot.getSig();
    }

    public void addVerifyAnchor(PtcVerifyAnchorDO ptcVerifyAnchorDO) {
        if (ObjectUtil.isEmpty(this.ptcTrustRoot.getVerifyAnchorMap())) {
            this.ptcTrustRoot.setVerifyAnchorMap(new HashMap<>());
        }
        this.ptcTrustRoot.getVerifyAnchorMap().put(ptcVerifyAnchorDO.getPtcVerifyAnchor().getVersion(), ptcVerifyAnchorDO.getPtcVerifyAnchor());
    }
}
