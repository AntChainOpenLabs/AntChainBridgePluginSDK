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


import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.Getter;
import lombok.Setter;

/**
 * A request needs to be signed by {@code Relayer}, {@code PTC}
 * or {@code DomainName} owner. And the signature or other kinds
 * proof of authentication
 */
@Getter
@Setter
public abstract class AuthorizedRequest {

    public static final short TLV_TYPE_AUTHORIZED_REQUEST_SENDER_CERT = Short.MAX_VALUE - 1;

    public static final short TLV_TYPE_AUTHORIZED_REQUEST_SENDER_AUTH_PROOF = Short.MAX_VALUE;

    @TLVField(
            tag = TLV_TYPE_AUTHORIZED_REQUEST_SENDER_CERT,
            type = TLVTypeEnum.BYTES,
            order = TLV_TYPE_AUTHORIZED_REQUEST_SENDER_CERT
    )
    private AbstractCrossChainCertificate senderCert;

    @TLVField(
            tag = TLV_TYPE_AUTHORIZED_REQUEST_SENDER_AUTH_PROOF,
            type = TLVTypeEnum.BYTES,
            order = TLV_TYPE_AUTHORIZED_REQUEST_SENDER_AUTH_PROOF
    )
    private byte[] senderAuthProof;

    abstract byte[] getEncodedToSign();
}
