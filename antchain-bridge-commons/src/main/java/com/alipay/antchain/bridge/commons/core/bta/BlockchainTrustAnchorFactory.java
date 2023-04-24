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

package com.alipay.antchain.bridge.commons.core.bta;

import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;

public class BlockchainTrustAnchorFactory {

    public static IBlockchainTrustAnchor createBTAV0(
            String domain,
            String product,
            int productSVN,
            byte[] bcOwnerPublicKey,
            AbstractBlockchainTrustAnchor.SignType bcOwnerSigAlgo,
            byte[] authMessageID,
            byte[] subjectIdentity,
            byte[] extension
    ) {
        AbstractBlockchainTrustAnchor trustAnchor = new BlockchainTrustAnchorV0();
        trustAnchor.setDomain(new CrossChainDomain(domain));
        trustAnchor.setSubjectProductID(product);
        trustAnchor.setSubjectProductSVN(productSVN);
        trustAnchor.setBcOwnerPublicKey(bcOwnerPublicKey);
        trustAnchor.setBcOwnerSigAlgo(bcOwnerSigAlgo);
        trustAnchor.setAuthMessageID(authMessageID);
        trustAnchor.setSubjectIdentity(subjectIdentity);
        trustAnchor.setExtension(extension);

        return trustAnchor;
    }

    public static IBlockchainTrustAnchor createBTAV0(byte[] raw) {
        IBlockchainTrustAnchor bta = new BlockchainTrustAnchorV0();
        bta.decode(raw);

        return bta;
    }
}
