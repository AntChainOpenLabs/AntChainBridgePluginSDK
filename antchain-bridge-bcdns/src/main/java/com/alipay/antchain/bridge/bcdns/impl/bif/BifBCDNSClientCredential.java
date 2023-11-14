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

package com.alipay.antchain.bridge.bcdns.impl.bif;

import java.io.ByteArrayInputStream;
import java.security.PrivateKey;
import java.security.Signature;

import cn.hutool.crypto.PemUtil;
import com.alipay.antchain.bridge.bcdns.types.exception.AntChainBridgeBCDNSException;
import com.alipay.antchain.bridge.bcdns.types.exception.BCDNSErrorCodeEnum;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.ICredentialSubject;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import lombok.Getter;

@Getter
public class BifBCDNSClientCredential {

    private AbstractCrossChainCertificate clientCert;

    private ICredentialSubject clientCredentialSubject;

    private PrivateKey clientKey;

    private String sigAlgo;

    public BifBCDNSClientCredential(
        String clientCertPem,
        String privateKeyPem,
        String sigAlgo
    ) {
        this.clientCert = CrossChainCertificateUtil.readCrossChainCertificateFromPem(
                clientCertPem.getBytes()
        );
        this.clientCredentialSubject = clientCert.getCredentialSubjectInstance();
        this.clientKey = PemUtil.readPemPrivateKey(new ByteArrayInputStream(privateKeyPem.getBytes()));
        this.sigAlgo = sigAlgo;
    }

    public byte[] signRequest(byte[] rawRequest) {
        try {
            Signature signer = Signature.getInstance(sigAlgo);
            signer.initSign(clientKey);
            signer.update(rawRequest);
            return signer.sign();
        } catch (Exception e) {
            throw new AntChainBridgeBCDNSException(
                    BCDNSErrorCodeEnum.BCDNS_SIGN_REQUEST_FAILED,
                    "failed to sign for request: ",
                    e
            );
        }
    }
}
