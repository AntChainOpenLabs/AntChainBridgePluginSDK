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

import cn.hutool.crypto.digest.DigestUtil;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.jcajce.provider.digest.SM3;

public class HashUtil {

    public static byte[] hash(HashAlgoEnum hashAlgo, byte[] data) {
        switch (hashAlgo) {
            case SHA2_256:
                return DigestUtil.sha256(data);
            case SM3:
                return new SM3.Digest().digest(data);
            case SHA3_256:
                return new SHA3.Digest256().digest(data);
            case KECCAK_256:
                return new Keccak.Digest256().digest(data);
            default:
                throw new IllegalArgumentException("Unsupported hash algorithm: " + hashAlgo);
        }
    }
}
