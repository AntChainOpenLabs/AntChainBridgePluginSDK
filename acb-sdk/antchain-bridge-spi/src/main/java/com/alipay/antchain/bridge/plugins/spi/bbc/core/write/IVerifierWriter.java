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

package com.alipay.antchain.bridge.plugins.spi.bbc.core.write;

import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyBlockchainTrustAnchor;

/**
 * Through {@code IVerifierWriter}, you can write data
 * to the storage of the VerifierContract.
 *
 * @author zouxyan
 */
public interface IVerifierWriter {

    /**
     * Add new ptc trust root, if existed on {@link com.alipay.antchain.bridge.commons.bbc.syscontract.PTCContract}
     * , aka {@code PTCHub}, replace the overlapping values and add the non-overlapping ones.
     *
     * @param ptcTrustRoot {@link PTCTrustRoot}
     */
    void updatePTCTrustRoot(PTCTrustRoot ptcTrustRoot);

    /**
     * Add raw <b>Third-Party Blockchain Trust Anchor</b>
     * abbreviated as {@code TPBTA} of a domain. Replace the
     * overlapping values and add the non-overlapping ones.
     *
     * <p>
     *     TPBTA is the trust root for contract ptc to verify the
     *     endorsements from PTC for ACB data like the cross-chain
     *     message. Every blockchain network registered into ACB
     *     needs to apply a domain certificate to own a domain which
     *     is the only representation of the blockchain in the ACB.
     * </p>
     *
     * <p>
     *     From the actual situation, developers need to send a transaction
     *     through your blockchain client to transport the {@code TPBTA}
     *     into the contract PTC.
     * </p>
     *
     * @param tpbta {@link ThirdPartyBlockchainTrustAnchor check this please}
     */
    void addTpBta(ThirdPartyBlockchainTrustAnchor tpbta);
}
