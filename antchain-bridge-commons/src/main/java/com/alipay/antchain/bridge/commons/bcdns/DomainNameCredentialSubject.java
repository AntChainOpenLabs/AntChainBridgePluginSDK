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

import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DomainNameCredentialSubject implements ICredentialSubject {

    private static final short TLV_TYPE_DOMAIN_NAME_CREDENTIAL_SUBJECT_VERSION = 0x0000;

    private static final short TLV_TYPE_DOMAIN_NAME_CREDENTIAL_SUBJECT_DOMAIN_NAME_TYPE = 0x0001;

    private static final short TLV_TYPE_DOMAIN_NAME_CREDENTIAL_SUBJECT_DOMAIN_NAME = 0x0002;

    private static final short TLV_TYPE_DOMAIN_NAME_CREDENTIAL_SUBJECT_APPLICANT = 0x0003;

    private static final short TLV_TYPE_DOMAIN_NAME_CREDENTIAL_SUBJECT_SUBJECT = 0x0004;

    public DomainNameCredentialSubject decode(byte[] rawData) {
        return TLVUtils.decode(rawData, DomainNameCredentialSubject.class);
    }

    @TLVField(tag = TLV_TYPE_DOMAIN_NAME_CREDENTIAL_SUBJECT_VERSION, type = TLVTypeEnum.STRING)
    private String version;

    @TLVField(tag = TLV_TYPE_DOMAIN_NAME_CREDENTIAL_SUBJECT_DOMAIN_NAME_TYPE, type = TLVTypeEnum.UINT8, order = 1)
    private DomainNameTypeEnum domainNameType;

    @TLVField(tag = TLV_TYPE_DOMAIN_NAME_CREDENTIAL_SUBJECT_DOMAIN_NAME, type = TLVTypeEnum.STRING, order = 2)
    private CrossChainDomain domainName;

    @TLVField(tag = TLV_TYPE_DOMAIN_NAME_CREDENTIAL_SUBJECT_APPLICANT, type = TLVTypeEnum.BYTES, order = 3)
    private ObjectIdentity applicant;

    @TLVField(tag = TLV_TYPE_DOMAIN_NAME_CREDENTIAL_SUBJECT_SUBJECT, type = TLVTypeEnum.BYTES, order = 4)
    private byte[] subject;

    public byte[] encode() {
        return TLVUtils.encode(this);
    }
}
