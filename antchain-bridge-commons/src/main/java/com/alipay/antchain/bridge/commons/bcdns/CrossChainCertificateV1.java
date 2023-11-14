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

package com.alipay.antchain.bridge.commons.bcdns;

import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CrossChainCertificateV1 extends AbstractCrossChainCertificate {

    public static final String MY_VERSION = "1";

    public CrossChainCertificateV1(
            String id,
            CrossChainCertificateTypeEnum type,
            ObjectIdentity issuer,
            long issuanceDate,
            long expirationDate,
            ICredentialSubject credentialSubject
    ) {
        super(
                MY_VERSION,
                id,
                type,
                issuer,
                issuanceDate,
                expirationDate,
                credentialSubject.encode(),
                null
        );
    }
}
