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

package com.alipay.antchain.bridge.commons;

import java.security.KeyPair;
import java.util.Date;

import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.KeyUtil;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.BCDNSTrustRootCredentialSubject;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateV1;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentityType;
import org.junit.Test;

public class BCDNSTest {

    @Test
    public void testCrossChainCertificate() throws Exception {
        KeyPair keyPair = KeyUtil.generateKeyPair("SM2");
        keyPair.getPublic().getEncoded();

        AbstractCrossChainCertificate certificate = CrossChainCertificateFactory.createCrossChainCertificate(
                CrossChainCertificateV1.MY_VERSION,
                "test",
                new ObjectIdentity(ObjectIdentityType.X509_PUBLIC_KEY_INFO, keyPair.getPublic().getEncoded()),
                DateUtil.currentSeconds(),
                DateUtil.offsetDay(new Date(), 365).second(),
                new BCDNSTrustRootCredentialSubject(
                        "bif",
                        new ObjectIdentity(ObjectIdentityType.X509_PUBLIC_KEY_INFO, keyPair.getPublic().getEncoded()),
                        new byte[]{}
                )
        );
        certificate.setProof(AbstractCrossChainCertificate.IssueProof.EMPTY_PROOF);
        System.out.println(CrossChainCertificateUtil.formatCrossChainCertificateToPem(certificate));


    }
}
