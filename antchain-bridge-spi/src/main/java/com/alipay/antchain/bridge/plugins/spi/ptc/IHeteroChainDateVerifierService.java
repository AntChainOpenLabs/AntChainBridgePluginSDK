package com.alipay.antchain.bridge.plugins.spi.ptc;

import com.alipay.antchain.bridge.plugins.spi.ptc.core.IAntChainBridgeDataVerifier;
import com.alipay.antchain.bridge.plugins.spi.ptc.core.ILedgerParser;
import com.alipay.antchain.bridge.plugins.spi.utils.pf4j.Pf4jMarker;

/**
 * This interface is one of the most important core logic in
 * plugin develop for a specific blockchain architecture.
 * The {@code IHeteroChainDateVerifierService} provides functions to verify
 * the cross-chain messages if they are confirmed on blockchain
 * or not.
 *
 * <p>
 *     <b>Proof Transform Component</b>, short as {@code PTC}, converts
 *     different types of proofs on heterogeneous chains to the same kind
 *     proof defined by ODATS. This kind proof from {@code PTC} can be verified
 *     by system contracts on the receiver chain. And this makes the
 *     cross-chain messages trustworthy and immutable.
 * </p>
 *
 * <p>
 *     A {@code PTC} program like poa-oracle provided by ODATS group
 *     loads the {@code plugin} including the {@code IHeteroChainDateVerifierService}
 *     and creates instances of {@code IHeteroChainDateVerifierService} to verify the validity of ledger
 *     data from a heterogeneous blockchain. For example, verify a
 *     transaction do exist in the ledger like using
 *     <a href="https://en.bitcoinwiki.org/wiki/Simplified_Payment_Verification">SPV</a>
 * </p>
 *
 * <p><b>We highly recommend that implement {@code IHeteroChainDateVerifierService} stateless. </b></p>
 *
 * <p>
 *     All {@code IHeteroChainDateVerifierService} functions are split into interfaces {@link IAntChainBridgeDataVerifier}.
 * </p>
 *
 * @author zouxyan
 */
public interface IHeteroChainDateVerifierService extends Pf4jMarker, IAntChainBridgeDataVerifier, ILedgerParser {}
