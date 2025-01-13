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

import java.math.BigInteger;

import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyBlockchainTrustAnchor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TpBtaDO {

    private String blockchainProduct;

    private String blockchainId;

    private String ptcServiceId;

    private ThirdPartyBlockchainTrustAnchor tpbta;

    public CrossChainLane getCrossChainLane() {
        return tpbta.getCrossChainLane();
    }

    public Integer getVersion() {
        return tpbta.getVersion();
    }

    public Integer getTpBtaVersion() {
        return tpbta.getTpbtaVersion();
    }

    public BigInteger getVerifyAnchorVersion() {
        return tpbta.getPtcVerifyAnchorVersion();
    }

    public Integer getBtaSubjectVersion() {
        return tpbta.getBtaSubjectVersion();
    }
}
