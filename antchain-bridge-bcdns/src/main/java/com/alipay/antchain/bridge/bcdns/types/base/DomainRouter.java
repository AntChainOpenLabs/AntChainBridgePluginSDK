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

package com.alipay.antchain.bridge.bcdns.types.base;

import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DomainRouter {

    private static final short TLV_TYPE_DOMAIN_ROUTER_DEST_DOMAIN = 0x0000;

    private static final short TLV_TYPE_DOMAIN_ROUTER_DEST_RELAYER = 0x0001;

    @TLVField(tag = TLV_TYPE_DOMAIN_ROUTER_DEST_DOMAIN, type = TLVTypeEnum.BYTES)
    private CrossChainDomain destDomain;

    @TLVField(tag = TLV_TYPE_DOMAIN_ROUTER_DEST_RELAYER, type = TLVTypeEnum.BYTES, order = 1)
    private Relayer destRelayer;
}
