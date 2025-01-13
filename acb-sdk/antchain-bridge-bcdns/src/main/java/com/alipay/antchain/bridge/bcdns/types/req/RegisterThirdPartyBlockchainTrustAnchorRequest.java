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

import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyBlockchainTrustAnchor;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class RegisterThirdPartyBlockchainTrustAnchorRequest {

    private static final short TLV_TYPE_PTC_ID = 0x0000;

    private static final short TLV_TYPE_DOMAIN = 0x0001;

    private static final short TLV_TYPE_TPBTA = 0x0002;

    @TLVField(tag = TLV_TYPE_PTC_ID, type = TLVTypeEnum.BYTES)
    private ObjectIdentity ptcId;

    @TLVField(tag = TLV_TYPE_DOMAIN, type = TLVTypeEnum.BYTES, order = TLV_TYPE_DOMAIN)
    private CrossChainDomain domain;

    @TLVField(tag = TLV_TYPE_TPBTA, type = TLVTypeEnum.BYTES, order = TLV_TYPE_TPBTA)
    private ThirdPartyBlockchainTrustAnchor tpbta;
}
