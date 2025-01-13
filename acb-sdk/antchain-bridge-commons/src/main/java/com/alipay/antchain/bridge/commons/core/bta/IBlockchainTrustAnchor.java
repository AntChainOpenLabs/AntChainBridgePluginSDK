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
import java.security.PrivateKey;
import java.security.PublicKey;

import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;

public interface IBlockchainTrustAnchor {

    int getVersion();

    CrossChainDomain getDomain();

    String getSubjectProduct();

    int getSubjectVersion();

    byte[] getSubjectIdentity();

    BigInteger getInitHeight();

    byte[] getInitBlockHash();

    byte[] getExtension();

    byte[] getBcOwnerPublicKey();

    PublicKey getBcOwnerPublicKeyObj();

    SignAlgoEnum getBcOwnerSigAlgo();

    byte[] getBcOwnerSig();

    ObjectIdentity getPtcOid();

    byte[] getAmId();

    void sign(PrivateKey privateKey);

    boolean validate();

    void decode(byte[] rawMessage);

    byte[] encode();
}
