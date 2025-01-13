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

package com.alipay.antchain.bridge.plugins.spi.ptc.core;

import com.alipay.antchain.bridge.commons.core.base.ConsensusState;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.bta.IBlockchainTrustAnchor;

/**
 * {@code IAntChainBridgeDataVerifier} provides functions that verify the integrity of odats data.
 *
 * <p>
 *     Before PTC endorse for the cross-chain message, it needs to prepare two things
 *     for the verification of cross-chain messages: <b>trust anchor and proof of existence.</b>
 * </p>
 * <ol>
 *     <li>The trust anchor refers to the verification information of the blockchain consensus,
 *      such as the consensus node public key collection of the current height. </li>
 *     <li>The existence proof is used to verify the legitimacy of the ledger data containing
 *     cross-chain information.</li>
 * </ol>
 *
 * <p>
 *     The relationship between the two is that trust anchor provides
 *     a source of trust for existence proofs.
 * </p>
 * <p>
 *     PTC needs to verify the first consensus state like a blockheader on
 *     anchor height and save it as source which used to verify the next.
 *     And when the consensus state changed, PTC need to verify the new
 *     consensus state and update it for verifying the cross-chain messages
 *     confirmed on blockchain after the consensus state changed.
 * </p>
 * <p>
 *     The {@link ConsensusState} saved and endorsed by the PTC is used to
 *     verify the {@link CrossChainMessage}, like SPV or other algorithms for
 *     ledger data containing {@link CrossChainMessage}.
 * </p>
 */
public interface IAntChainBridgeDataVerifier {

    VerifyResult verifyAnchorConsensusState(IBlockchainTrustAnchor bta, ConsensusState anchorState);

    VerifyResult verifyConsensusState(ConsensusState stateToVerify, ConsensusState parentState);

    VerifyResult verifyCrossChainMessage(CrossChainMessage message, ConsensusState currState);
}
