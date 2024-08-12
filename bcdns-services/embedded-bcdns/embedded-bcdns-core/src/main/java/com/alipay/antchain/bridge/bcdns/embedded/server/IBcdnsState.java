/*
 * Copyright 2024 Ant Group
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

package com.alipay.antchain.bridge.bcdns.embedded.server;

import com.alipay.antchain.bridge.bcdns.embedded.types.enums.ApplicationStateEnum;
import com.alipay.antchain.bridge.bcdns.embedded.types.models.CertApplicationResult;
import com.alipay.antchain.bridge.bcdns.types.base.DomainRouter;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateTypeEnum;

public interface IBcdnsState {

    void saveApplication(AbstractCrossChainCertificate csr, String receipt, ApplicationStateEnum state);

    void updateApplication(String receipt, ApplicationStateEnum state);

    void saveCrossChainCert(AbstractCrossChainCertificate crossChainCertificate);

    CertApplicationResult queryApplication(String receipt);

    AbstractCrossChainCertificate queryCrossChainCert(String certId, CrossChainCertificateTypeEnum certificateType);

    AbstractCrossChainCertificate queryDomainCert(String domain);

    void registerDomainRouter(DomainRouter domainRouter);

    void registerTPBTA(byte[] rawTpBta);

    DomainRouter queryDomainRouter(String destDomain);

    byte[] queryTpBta(String request);

    boolean ifDomainExist(String domain);
}
