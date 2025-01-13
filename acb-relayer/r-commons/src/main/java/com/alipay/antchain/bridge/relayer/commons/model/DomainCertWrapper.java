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

package com.alipay.antchain.bridge.relayer.commons.model;

import cn.hutool.core.lang.Assert;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.DomainNameCredentialSubject;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DomainCertWrapper {

    public DomainCertWrapper(AbstractCrossChainCertificate crossChainCertificate) {
        Assert.isTrue(CrossChainCertificateUtil.isDomainCert(crossChainCertificate));
        this.crossChainCertificate = crossChainCertificate;
        this.domainNameCredentialSubject = (DomainNameCredentialSubject) crossChainCertificate.getCredentialSubjectInstance();
        this.domain = this.domainNameCredentialSubject.getDomainName().getDomain();
        this.domainSpace = this.domainNameCredentialSubject.getParentDomainSpace().getDomain();
    }

    private AbstractCrossChainCertificate crossChainCertificate;

    private DomainNameCredentialSubject domainNameCredentialSubject;

    private String blockchainProduct;

    private String blockchainId;

    private String domain;

    private String domainSpace;
}
