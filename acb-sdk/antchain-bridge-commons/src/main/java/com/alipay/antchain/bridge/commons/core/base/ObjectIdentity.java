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

package com.alipay.antchain.bridge.commons.core.base;

import java.security.Security;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.HexUtil;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ObjectIdentity {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static final short TLV_TYPE_OID_TYPE = 0x0000;

    public static final short TLV_TYPE_OID_RAW_ID = 0x0001;

    public static ObjectIdentity decode(byte[] raw) {
        return TLVUtils.decode(raw, ObjectIdentity.class);
    }

    public static ObjectIdentity decodeFromHex(String hex) {
        return decode(HexUtil.decodeHex(hex));
    }

    @TLVField(tag = TLV_TYPE_OID_TYPE, type = TLVTypeEnum.UINT8)
    private ObjectIdentityType type;

    @TLVField(tag = TLV_TYPE_OID_RAW_ID, type = TLVTypeEnum.BYTES, order = 1)
    private byte[] rawId;

    public byte[] encode() {
        return TLVUtils.encode(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ObjectIdentity) {
            return this.type == ((ObjectIdentity) obj).type && ArrayUtil.equals(this.rawId, ((ObjectIdentity) obj).rawId);
        }
        return false;
    }

    public String toHex() {
        return HexUtil.encodeHexStr(encode());
    }
}
