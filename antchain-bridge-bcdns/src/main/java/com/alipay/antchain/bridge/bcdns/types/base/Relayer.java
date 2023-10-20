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

import com.alipay.antchain.bridge.commons.bcdns.RelayerCredentialSubject;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Relayer {

    private static final short TLV_TYPE_RELAYER_RELAYER_ID = 0x0000;

    private static final short TLV_TYPE_RELAYER_NET_ADDRESS = 0x0001;

    private static final short TLV_TYPE_RELAYER_CREDENTIAL_SUBJECT = 0x0002;

    @TLVField(tag = TLV_TYPE_RELAYER_RELAYER_ID, type = TLVTypeEnum.STRING)
    private String relayerId;

    @TLVField(tag = TLV_TYPE_RELAYER_NET_ADDRESS, type = TLVTypeEnum.STRING, order = TLV_TYPE_RELAYER_NET_ADDRESS)
    private String netAddress;

    @TLVField(tag = TLV_TYPE_RELAYER_CREDENTIAL_SUBJECT, type = TLVTypeEnum.BYTES, order = TLV_TYPE_RELAYER_CREDENTIAL_SUBJECT)
    private RelayerCredentialSubject credentialSubject;
}
