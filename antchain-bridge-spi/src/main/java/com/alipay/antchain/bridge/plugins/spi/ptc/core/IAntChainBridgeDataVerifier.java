package com.alipay.antchain.bridge.plugins.spi.ptc.core;

import com.alipay.antchain.bridge.commons.core.base.ConsensusState;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;

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

    boolean verifyAnchorConsensusState(ConsensusState anchorState);

    boolean verifyConsensusState(ConsensusState stateToVerify, ConsensusState anchorState);

    boolean verifyCrossChainMessage(CrossChainMessage message, ConsensusState anchorState);
}
