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

package com.alipay.antchain.bridge.relayer.core.manager.network;

import java.security.PrivateKey;
import java.util.Set;
import javax.annotation.Resource;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.RelayerCredentialSubject;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.core.types.network.request.RelayerRequest;
import com.alipay.antchain.bridge.relayer.core.types.network.response.RelayerResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class RelayerCredentialManager implements IRelayerCredentialManager {

    @Value("#{relayerCoreConfig.localRelayerCrossChainCertificate}")
    private AbstractCrossChainCertificate localRelayerCertificate;

    @Value("#{relayerCoreConfig.localRelayerCredentialSubject}")
    private RelayerCredentialSubject localRelayerCredentialSubject;

    @Value("#{relayerCoreConfig.localPrivateKey}")
    private PrivateKey localRelayerPrivateKey;

    @Value("#{relayerCoreConfig.localRelayerNodeId}")
    private String localNodeId;

    @Value("${relayer.network.node.sig_algo:KECCAK256_WITH_SECP256K1}")
    private SignAlgoEnum localNodeSigAlgo;

    @Value("#{relayerCoreConfig.localRelayerIssuerDomainSpace}")
    private String localRelayerIssuerDomainSpace;

    @Resource
    private IBCDNSManager bcdnsManager;

    private final Set<String> validatedCertIdCache = new ConcurrentHashSet<>();

    @Override
    public void signRelayerRequest(RelayerRequest relayerRequest) {
        try {
            relayerRequest.setNodeId(localNodeId);
            relayerRequest.setSenderRelayerCertificate(localRelayerCertificate);
            relayerRequest.setSigAlgo(localNodeSigAlgo);
            relayerRequest.setSignature(
                    localNodeSigAlgo.getSigner().sign(localRelayerPrivateKey, relayerRequest.rawEncode())
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    e,
                    "failed to sign for request type {}", relayerRequest.getRequestType().getCode()
            );
        }
    }

    @Override
    public void signRelayerResponse(RelayerResponse relayerResponse) {
        try {
            relayerResponse.setRemoteRelayerCertificate(localRelayerCertificate);
            relayerResponse.setSigAlgo(localNodeSigAlgo);
            relayerResponse.setSignature(
                    localNodeSigAlgo.getSigner().sign(
                            localRelayerPrivateKey,
                            relayerResponse.rawEncode()
                    )
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    "failed to sign response",
                    e
            );
        }
    }

    @Override
    public byte[] signHelloRand(byte[] rand) {
        try {
            return localNodeSigAlgo.getSigner().sign(localRelayerPrivateKey, rand);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    "failed to sign hello rand",
                    e
            );
        }
    }

    @Override
    public boolean validateRelayerRequest(RelayerRequest relayerRequest) {

        if (!validatedCertIdCache.contains(relayerRequest.calcRelayerNodeId())) {
            if (!bcdnsManager.validateCrossChainCertificate(relayerRequest.getSenderRelayerCertificate())) {
                return false;
            }
        }

        if (!CrossChainCertificateUtil.isRelayerCert(relayerRequest.getSenderRelayerCertificate())) {
            return false;
        }

        if (relayerRequest.verify()) {
            validatedCertIdCache.add(relayerRequest.calcRelayerNodeId());
            return true;
        }
        return false;
    }

    @Override
    public boolean validateRelayerResponse(RelayerResponse relayerResponse) {
        if (!validatedCertIdCache.contains(relayerResponse.calcRelayerNodeId())) {
            if (!bcdnsManager.validateCrossChainCertificate(relayerResponse.getRemoteRelayerCertificate())) {
                return false;
            }
        }

        if (!CrossChainCertificateUtil.isRelayerCert(relayerResponse.getRemoteRelayerCertificate())) {
            return false;
        }

        if (relayerResponse.verify()) {
            validatedCertIdCache.add(relayerResponse.calcRelayerNodeId());
            return true;
        }
        return false;
    }
}
