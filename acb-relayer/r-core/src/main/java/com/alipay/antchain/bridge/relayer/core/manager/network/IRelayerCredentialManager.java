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

import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.RelayerCredentialSubject;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import com.alipay.antchain.bridge.relayer.core.types.network.request.RelayerRequest;
import com.alipay.antchain.bridge.relayer.core.types.network.response.RelayerResponse;

public interface IRelayerCredentialManager {

    /**
     * @param relayerRequest
     */
    void signRelayerRequest(RelayerRequest relayerRequest);

    /**
     * @param relayerResponse
     */
    void signRelayerResponse(RelayerResponse relayerResponse);

    byte[] signHelloRand(byte[] rand);

    AbstractCrossChainCertificate getLocalRelayerCertificate();

    RelayerCredentialSubject getLocalRelayerCredentialSubject();

    boolean validateRelayerRequest(RelayerRequest relayerRequest);

    boolean validateRelayerResponse(RelayerResponse relayerResponse);

    String getLocalNodeId();

    SignAlgoEnum getLocalNodeSigAlgo();

    PrivateKey getLocalRelayerPrivateKey();

    String getLocalRelayerIssuerDomainSpace();
}
