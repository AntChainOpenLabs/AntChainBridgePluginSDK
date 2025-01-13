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

import java.math.BigInteger;

import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;

public class BlockchainTrustAnchorFactory {

    public static IBlockchainTrustAnchor createBTAv1(
            String domain,
            String product,
            int subjectVersion,
            byte[] bcOwnerPublicKey,
            SignAlgoEnum bcOwnerSigAlgo,
            byte[] subjectIdentity,
            BigInteger initHeight,
            byte[] initBlockHash,
            byte[] amId,
            ObjectIdentity ptcOid,
            byte[] extension
    ) {
        AbstractBlockchainTrustAnchor trustAnchor = new BlockchainTrustAnchorV1();
        trustAnchor.setDomain(new CrossChainDomain(domain));
        trustAnchor.setSubjectProduct(product);
        trustAnchor.setSubjectVersion(subjectVersion);
        trustAnchor.setBcOwnerPublicKey(bcOwnerPublicKey);
        trustAnchor.setBcOwnerSigAlgo(bcOwnerSigAlgo);
        trustAnchor.setSubjectIdentity(subjectIdentity);
        trustAnchor.setInitHeight(initHeight);
        trustAnchor.setInitBlockHash(initBlockHash);
        trustAnchor.setAmId(amId);
        trustAnchor.setPtcOid(ptcOid);
        trustAnchor.setExtension(extension);

        return trustAnchor;
    }

    public static IBlockchainTrustAnchor createBTAv1(byte[] raw) {
        IBlockchainTrustAnchor bta = new BlockchainTrustAnchorV1();
        bta.decode(raw);

        return bta;
    }

    public static IBlockchainTrustAnchor createBTA(byte[] raw) {
        switch (AbstractBlockchainTrustAnchor.decodeVersionFromBytes(raw)) {
            case BlockchainTrustAnchorV1.MY_VERSION:
                return createBTAv1(raw);
            default:
                throw new AntChainBridgeCommonsException(
                        CommonsErrorCodeEnum.INCORRECT_BTA,
                        "Unsupported blockchain trust anchor version"
                );
        }
    }
}
