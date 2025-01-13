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

package com.alipay.antchain.bridge.plugins.spi.ptc;

import com.alipay.antchain.bridge.plugins.spi.ptc.core.IAntChainBridgeDataVerifier;
import com.alipay.antchain.bridge.plugins.spi.ptc.core.ILedgerParser;
import com.alipay.antchain.bridge.plugins.spi.utils.pf4j.Pf4jMarker;

/**
 * This interface is one of the most important core logic in
 * plugin develop for a specific blockchain architecture.
 * The {@code IHeteroChainDataVerifierService} provides functions to verify
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
 *     loads the {@code plugin} including the {@code IHeteroChainDataVerifierService}
 *     and creates instances of {@code IHeteroChainDataVerifierService} to verify the validity of ledger
 *     data from a heterogeneous blockchain. For example, verify a
 *     transaction do exist in the ledger like using
 *     <a href="https://en.bitcoinwiki.org/wiki/Simplified_Payment_Verification">SPV</a>
 * </p>
 *
 * <p><b>We highly recommend that implement {@code IHeteroChainDataVerifierService} stateless. </b></p>
 *
 * <p>
 *     All {@code IHeteroChainDataVerifierService} functions are split into interfaces {@link IAntChainBridgeDataVerifier}.
 * </p>
 *
 * @author zouxyan
 */
public interface IHeteroChainDataVerifierService extends Pf4jMarker, IAntChainBridgeDataVerifier, ILedgerParser {}
