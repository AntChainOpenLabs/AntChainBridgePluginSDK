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

import java.nio.charset.Charset;

import cn.ac.caict.bid.model.BIDDocumentOperation;
import cn.bif.common.JsonUtils;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.bcdns.impl.bif.conf.BifCertificationServiceConfig;
import com.alipay.antchain.bridge.bcdns.impl.bif.conf.BifChainConfig;
import com.alipay.antchain.bridge.bcdns.types.resp.ApplicationResult;
import com.alipay.antchain.bridge.bcdns.types.resp.ApplyDomainNameCertificateResponse;
import com.alipay.antchain.bridge.bcdns.types.resp.QueryBCDNSTrustRootCertificateResponse;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateTypeEnum;
import com.alipay.antchain.bridge.commons.bcdns.DomainNameCredentialSubject;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentityType;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BifBCDNSClientTest {

    private static BifBCDNSClient bifBCDNSClient;

    private static final String KEY_TYPE = "Ed25519";

    private static final String SIG_ALGO = "Ed25519";

    private static final ObjectIdentityType OID_TYPE = ObjectIdentityType.BID;

    private static final String ANTCHAIN_DOMAIN = "antchain";

    private static AbstractCrossChainCertificate ANTCHAIN_CSR;

    private static String receiptId;

    @BeforeClass
    public static void setup() {
        bifBCDNSClient = new BifBCDNSClient(
                new BifCertificationServiceConfig(
                        "http://0.0.0.0:8112",
                        FileUtil.readString(getCertsPath() + "relayer.crt", Charset.defaultCharset()),
                        FileUtil.readString(getCertsPath() + "private_key.pem", Charset.defaultCharset()),
                        SIG_ALGO,
                        FileUtil.readString(getCertsPath() + "private_key.pem", Charset.defaultCharset()),
                        FileUtil.readString(getCertsPath() + "public_key.pem", Charset.defaultCharset()),
                        SIG_ALGO
                ),
                new BifChainConfig(
                        "",
                        null,
                        "",
                        "",
                        "",
                        "",
                        "",
                        ""
                )
        );

        ANTCHAIN_CSR = CrossChainCertificateFactory.createDomainNameCertificateSigningRequest(
                DomainNameCredentialSubject.CURRENT_VERSION,
                new CrossChainDomain(CrossChainDomain.ROOT_DOMAIN_SPACE),
                new CrossChainDomain(ANTCHAIN_DOMAIN),
                bifBCDNSClient.getCertificationServiceClient().getClientCredential().getClientCert()
                        .getCredentialSubjectInstance().getApplicant(),
                bifBCDNSClient.getCertificationServiceClient().getClientCredential().getClientCert()
                        .getCredentialSubjectInstance().getSubject()
        );
    }

    private static String getCertsPath() {
        return StrUtil.format(
                "cccerts/{}/{}/",
                OID_TYPE == ObjectIdentityType.X509_PUBLIC_KEY_INFO ? "x509" : OID_TYPE.name(),
                KEY_TYPE
        ).toLowerCase();
    }

    @Test
    public void test0ApplyDomainCert() {
        ApplyDomainNameCertificateResponse response = bifBCDNSClient.applyDomainNameCertificate(ANTCHAIN_CSR);
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getApplyReceipt());
        Assert.assertFalse(response.getApplyReceipt().isEmpty());

        receiptId = response.getApplyReceipt();
    }

    @Test
    @SneakyThrows
    public void test1QueryStatus() {
        Assert.assertFalse(StrUtil.isEmpty(receiptId));
        ApplicationResult result = null;
        while (ObjectUtil.isNull(result) || !result.isFinalResult()) {
            result = bifBCDNSClient.queryDomainNameCertificateApplicationResult(receiptId);
            Assert.assertNotNull(result);
            Thread.sleep(3_000);
        }

        Assert.assertNotNull(result.getCertificate());
        Assert.assertEquals(
                CrossChainCertificateTypeEnum.DOMAIN_NAME_CERTIFICATE,
                result.getCertificate().getType()
        );
        Assert.assertNotNull(result.getCertificate().getIssuer());
        Assert.assertNotNull(result.getCertificate().getProof());
        // TODO: maybe validate the sig by the public key
    }

    @Test
    public void test2QueryTheBCNDSRoot() {
        QueryBCDNSTrustRootCertificateResponse response = bifBCDNSClient.queryBCDNSTrustRootCertificate();
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getBcdnsTrustRootCertificate());
        Assert.assertEquals(
                CrossChainCertificateTypeEnum.BCDNS_TRUST_ROOT_CERTIFICATE,
                response.getBcdnsTrustRootCertificate().getType()
        );
        BIDDocumentOperation bidDocumentOperation = JsonUtils.toJavaObject(
                new String(
                        response.getBcdnsTrustRootCertificate().getCredentialSubjectInstance().getSubject()
                ),
                BIDDocumentOperation.class
        );

        Assert.assertNotNull(bidDocumentOperation);
        Assert.assertNotNull(bidDocumentOperation.getPublicKey());
        Assert.assertEquals(1, bidDocumentOperation.getPublicKey().length);
    }
}
