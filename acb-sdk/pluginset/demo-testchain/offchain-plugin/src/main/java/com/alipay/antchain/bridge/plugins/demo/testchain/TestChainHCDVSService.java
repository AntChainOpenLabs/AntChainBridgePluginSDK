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

package com.alipay.antchain.bridge.plugins.demo.testchain;

import com.alipay.antchain.bridge.commons.core.base.ConsensusState;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.bta.IBlockchainTrustAnchor;
import com.alipay.antchain.bridge.plugins.lib.HeteroChainDataVerifierService;
import com.alipay.antchain.bridge.plugins.spi.ptc.AbstractHCDVSService;
import com.alipay.antchain.bridge.plugins.spi.ptc.core.VerifyResult;

/**
 * This {@code TestChainHCDVSService} is a demo implementation of {@link HeteroChainDataVerifierService}.
 * <p>
 * It's a <b>{@code Blockchain Bridge Component}</b> plugin for the heterogeneous blockchain <b>testchain</b>
 * in this demo.
 * </p>
 */
@HeteroChainDataVerifierService(products = "testchain", pluginId = "testchain_hcdvsservice")
public class TestChainHCDVSService extends AbstractHCDVSService {

    private TestChainSDK sdk;

    private AbstractHCDVSService hcdvsContext;

    @Override
    public VerifyResult verifyAnchorConsensusState(IBlockchainTrustAnchor bta, ConsensusState anchorState) {
        return VerifyResult.builder()
                .success(true).build();
    }

    @Override
    public VerifyResult verifyConsensusState(ConsensusState stateToVerify, ConsensusState parentState) {
        return VerifyResult.builder()
                .success(true).build();
    }

    @Override
    public VerifyResult verifyCrossChainMessage(CrossChainMessage message, ConsensusState currState) {
        return VerifyResult.builder()
                .success(true).build();
    }

    @Override
    public byte[] parseMessageFromLedgerData(byte[] ledgerData) {
        return new byte[0];
    }
}

