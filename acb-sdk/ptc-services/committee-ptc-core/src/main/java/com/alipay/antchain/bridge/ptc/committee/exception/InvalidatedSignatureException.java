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

package com.alipay.antchain.bridge.ptc.committee.exception;

import java.security.PublicKey;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;

public class InvalidatedSignatureException extends CommitteeBaseException {

    private String committeeId;

    private String nodeId;

    private String sigBase64;

    private PublicKey publicKey;

    public InvalidatedSignatureException(String committeeId, String nodeId, String sigBase64, PublicKey publicKey) {
        super(
                StrUtil.format("Invalidated signature, committeeId: {}, nodeId: {}, sigBase64: {}, publicKey: {}",
                        committeeId, nodeId, sigBase64, Base64.encode(publicKey.getEncoded()))
        );
    }
}
