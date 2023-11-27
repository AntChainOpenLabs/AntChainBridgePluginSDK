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

package com.alipay.antchain.bridge.bcdns.types.req;

import com.alipay.antchain.bridge.bcdns.types.base.DomainRouter;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class RegisterDomainRouterRequest {

    private static final short TLV_TYPE_DOMAIN_ROUTER = 0x0000;

    private static final short TLV_TYPE_DOMAIN_CERT = 0x0001;

    @TLVField(tag = TLV_TYPE_DOMAIN_ROUTER, type = TLVTypeEnum.BYTES)
    private DomainRouter router;

    @TLVField(tag = TLV_TYPE_DOMAIN_CERT, type = TLVTypeEnum.BYTES)
    private AbstractCrossChainCertificate domainCert;
}
