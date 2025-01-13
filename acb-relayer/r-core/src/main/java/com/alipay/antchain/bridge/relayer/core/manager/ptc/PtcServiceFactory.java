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

package com.alipay.antchain.bridge.relayer.core.manager.ptc;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.PTCCredentialSubject;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTypeEnum;
import com.alipay.antchain.bridge.ptc.committee.CommitteePTCService;
import com.alipay.antchain.bridge.ptc.committee.config.CommitteePtcConfig;
import com.alipay.antchain.bridge.ptc.committee.types.network.CommitteeNetworkInfo;
import com.alipay.antchain.bridge.ptc.service.IPTCService;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import lombok.NonNull;

public class PtcServiceFactory {

    public static byte[] buildPtcConfig(
            @NonNull AbstractCrossChainCertificate ptcCert,
            PTCTrustRoot ptcTrustRoot,
            byte[] rawConfig
    ) {
        PTCTypeEnum ptcType = ((PTCCredentialSubject) ptcCert.getCredentialSubjectInstance()).getType();
        switch (ptcType) {
            case COMMITTEE:
                CommitteePtcConfig ptcConfig = CommitteePtcConfig.parseFrom(rawConfig);
                if (ObjectUtil.isNull(ptcConfig.getCommitteeNetworkInfo())) {
                    if (ObjectUtil.isNull(ptcTrustRoot) || ObjectUtil.isEmpty(ptcTrustRoot.getNetworkInfo())) {
                        throw new AntChainBridgeRelayerException(RelayerErrorCodeEnum.CORE_PTC_SERVICE_CONFIG_INVALID, "Committee network info is null");
                    }
                    ptcConfig.setCommitteeNetworkInfo(CommitteeNetworkInfo.decode(ptcTrustRoot.getNetworkInfo()));
                }
                ptcConfig.setPtcCertificate(ptcCert);
                return ptcConfig.encode();
            default:
                throw new AntChainBridgeRelayerException(RelayerErrorCodeEnum.CORE_PTC_SERVICE_TYPE_NOT_SUPPORT, "Unsupported PTC type: {}", ptcType.name());
        }
    }

    public static IPTCService createPtcServiceStub(
            AbstractCrossChainCertificate ptcCert,
            byte[] rawConfig
    ) {
        switch (((PTCCredentialSubject) ptcCert.getCredentialSubjectInstance()).getType()) {
            case COMMITTEE:
                CommitteePTCService ptcService = new CommitteePTCService();
                ptcService.startup(rawConfig);
                return ptcService;
            default:
                throw new AntChainBridgeRelayerException(
                        RelayerErrorCodeEnum.CORE_PTC_SERVICE_TYPE_NOT_SUPPORT,
                        "Unsupported PTC type: {}", ((PTCCredentialSubject) ptcCert.getCredentialSubjectInstance()).getType().name()
                );
        }
    }
}
