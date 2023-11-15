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

import java.security.PublicKey;

import com.alipay.antchain.bridge.commons.bcdns.utils.ObjectIdentityUtil;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RelayerCredentialSubject implements ICredentialSubject {

    public static final String CURRENT_VERSION = "1.0";

    public static final short TLV_TYPE_PTC_CREDENTIAL_SUBJECT_VERSION = 0x0000;

    public static final short TLV_TYPE_PTC_CREDENTIAL_SUBJECT_NAME = 0x0001;

    public static final short TLV_TYPE_PTC_CREDENTIAL_SUBJECT_TYPE = 0x0002;

    public static final short TLV_TYPE_PTC_CREDENTIAL_SUBJECT_APPLICANT = 0x0003;

    public static final short TLV_TYPE_PTC_CREDENTIAL_SUBJECT_SUBJECT_INFO = 0x0004;

    public static RelayerCredentialSubject decode(byte[] rawData) {
        return TLVUtils.decode(rawData, RelayerCredentialSubject.class);
    }

    @TLVField(tag = TLV_TYPE_PTC_CREDENTIAL_SUBJECT_VERSION, type = TLVTypeEnum.STRING)
    private String version;

    @TLVField(tag = TLV_TYPE_PTC_CREDENTIAL_SUBJECT_NAME, type = TLVTypeEnum.STRING, order = 1)
    private String name;

    @TLVField(tag = TLV_TYPE_PTC_CREDENTIAL_SUBJECT_APPLICANT, type = TLVTypeEnum.BYTES, order = 2)
    private ObjectIdentity applicant;

    @TLVField(tag = TLV_TYPE_PTC_CREDENTIAL_SUBJECT_SUBJECT_INFO, type = TLVTypeEnum.BYTES, order = 3)
    private byte[] subjectInfo;

    @Override
    public byte[] encode() {
        return TLVUtils.encode(this);
    }

    @Override
    public PublicKey getSubjectPublicKey() {
        return ObjectIdentityUtil.getPublicKeyFromSubject(applicant, subjectInfo);
    }

    @Override
    public byte[] getRawSubjectPublicKey() {
        return ObjectIdentityUtil.getRawPublicKeyFromSubject(applicant, subjectInfo);
    }
}
