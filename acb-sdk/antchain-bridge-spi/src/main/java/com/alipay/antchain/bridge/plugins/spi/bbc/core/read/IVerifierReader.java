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

package com.alipay.antchain.bridge.plugins.spi.bbc.core.read;

import java.math.BigInteger;
import java.util.Set;

import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTypeEnum;
import com.alipay.antchain.bridge.commons.core.ptc.PTCVerifyAnchor;
import com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyBlockchainTrustAnchor;

/**
 * Through {@code IVerifierReader}, you can query the state of the PTCContract.
 *
 * @author zouxyan
 */
public interface IVerifierReader {

    /**
     * Determine if there is a {@code TPBTA} for the domain
     * name in the contract.
     *
     * @param tpbtaLane the crosschain lane which endorsed by TpBta
     * @param tpBtaVersion the tpbta version
     * @return boolean yes or no
     */
    boolean hasTpBta(CrossChainLane tpbtaLane, int tpBtaVersion);

    /**
     * Get the raw {@code TPBTA} for the domain.
     *
     * @param tpbtaLane the crosschain lane which endorsed by TpBta
     * @param tpBtaVersion the tpbta version
     * @return {@link ThirdPartyBlockchainTrustAnchor} {@code TPBTA}
     */
    ThirdPartyBlockchainTrustAnchor getTpBta(CrossChainLane tpbtaLane, int tpBtaVersion);

    /**
     * Show the ptc type list which this BBC onchain-plugin supports.
     *
     * @return the set of ptc type
     */
    Set<PTCTypeEnum> getSupportedPTCType();

    /**
     * Get the {@link PTCTrustRoot} for the certificate {@code ptcCrossChainCert}
     *
     * @param ptcOwnerOid who runs the PTC identified by {@link PTCTrustRoot}
     * @return PTCTrustRoot
     */
    PTCTrustRoot getPTCTrustRoot(ObjectIdentity ptcOwnerOid);

    boolean hasPTCTrustRoot(ObjectIdentity ptcOwnerOid);

    PTCVerifyAnchor getPTCVerifyAnchor(ObjectIdentity ptcOwnerOid, BigInteger version);

    /**
     * Return true if the verifier contract of {@code BBCService} contains the input version
     * {@link com.alipay.antchain.bridge.commons.core.ptc.PTCVerifyAnchor} for the input {@code ptcCrossChainCert}.
     *
     * @param ptcOwnerOid who runs the PTC identified by {@link PTCTrustRoot}
     * @param version VerifyAnchor's version
     * @return contains or not
     */
    boolean hasPTCVerifyAnchor(ObjectIdentity ptcOwnerOid, BigInteger version);
}
