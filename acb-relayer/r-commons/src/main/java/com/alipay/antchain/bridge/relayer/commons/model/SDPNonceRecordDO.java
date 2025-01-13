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

import java.math.BigInteger;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class SDPNonceRecordDO {

    public static String calcHashValFrom(String senderDomain, String senderHex, String receiverDomain, String receiverHex, BigInteger nonce) {
        return DigestUtil.sha256Hex(
                StrUtil.format(
                        "{}:{}->{}:{}${}",
                        senderDomain, senderHex.toLowerCase(), receiverDomain, receiverHex.toLowerCase(), nonce
                )
        );
    }

    public SDPNonceRecordDO(String messageId, String senderDomain, String senderHex, String receiverDomain, String receiverHex, BigInteger nonce) {
        this.messageId = messageId;
        this.senderDomain = senderDomain;
        this.senderIdentity = senderHex;
        this.receiverDomain = receiverDomain;
        this.receiverIdentity = receiverHex;
        this.nonce = nonce;
        this.hashVal = calcHashValFrom(senderDomain, senderHex, receiverDomain, receiverHex, nonce);
    }

    private String messageId;

    private String senderDomain;

    private String senderIdentity;

    private String receiverDomain;

    private String receiverIdentity;

    private BigInteger nonce;

    private String hashVal;
}
