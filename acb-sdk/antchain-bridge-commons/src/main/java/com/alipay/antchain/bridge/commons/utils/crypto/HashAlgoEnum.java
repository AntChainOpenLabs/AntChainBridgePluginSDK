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

package com.alipay.antchain.bridge.commons.utils.crypto;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVCreator;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVMapping;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.jcajce.provider.digest.SHA3;

@TLVMapping(fieldName = "name")
@Getter
@AllArgsConstructor
public enum HashAlgoEnum {

    SHA2_256(
            "SHA2-256",
            DigestUtil::sha256
    ),
    SHA3_256(
            "SHA3-256",
            data -> new SHA3.Digest256().digest(data)
    ),
    SM3(
            "SM3",
            data -> new org.bouncycastle.jcajce.provider.digest.SM3.Digest().digest(data)
    ),
    KECCAK_256(
            "KECCAK-256",
            data -> new Keccak.Digest256().digest(data)
    );

    private final String name;

    private final IHashFunc hashFunc;

    @JSONField
    public String getName() {
        return name;
    }

    public byte[] hash(byte[] raw) {
        return hashFunc.hash(raw);
    }

    @JSONCreator
    @TLVCreator
    public static HashAlgoEnum getByName(String name) {
        for (HashAlgoEnum hashAlgoEnum : HashAlgoEnum.values()) {
            if (StrUtil.equalsIgnoreCase(hashAlgoEnum.getName(), name)) {
                return hashAlgoEnum;
            }
        }
        throw new AntChainBridgeCommonsException(
                CommonsErrorCodeEnum.UNSUPPORTED_HASH_TYPE_ERROR,
                "Unsupported hash type: " + name
        );
    }
}
