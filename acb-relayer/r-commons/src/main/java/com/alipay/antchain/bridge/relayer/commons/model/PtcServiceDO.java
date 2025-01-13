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

package com.alipay.antchain.bridge.relayer.commons.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTypeEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.PtcServiceStateEnum;
import com.alipay.antchain.bridge.relayer.commons.utils.json.CrossChainCertDeserializer;
import com.alipay.antchain.bridge.relayer.commons.utils.json.CrossChainCertSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PtcServiceDO {

    private String serviceId;

    private PTCTypeEnum type;

    @JSONField(serialize = false, deserialize = false)
    private ObjectIdentity ownerId;

    private String issuerBcdnsDomainSpace;

    private PtcServiceStateEnum state;

    @JSONField(serializeUsing = CrossChainCertSerializer.class, deserializeUsing = CrossChainCertDeserializer.class)
    private AbstractCrossChainCertificate ptcCert;

    private byte[] clientConfig;
}
