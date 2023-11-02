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
public class ObjectIdentity {

    public static final short TLV_TYPE_OID_TYPE = 0x0000;

    public static final short TLV_TYPE_OID_RAW_ID = 0x0001;

    public static ObjectIdentity decode(byte[] raw) {

//        ObjectIdentity oid = new ObjectIdentity();
//
//        byte[] rawType = new byte[4];
//        System.arraycopy(raw, offset, rawType, 2, 2);
//
//        oid.setType(ObjectIdentityType.parseFromValue(ByteUtil.bytesToInt(rawType, ByteOrder.BIG_ENDIAN)));
//
//        offset += 2;
//
//        byte[] rawLength = new byte[4];
//        System.arraycopy(raw, offset, rawLength, 0, 4);
//
//        int length = ByteUtil.bytesToInt(rawLength, ByteOrder.BIG_ENDIAN);
//
//        offset += 4;
//        oid.rawId = new byte[length];
//        System.arraycopy(raw, offset, oid.rawId, 0, length);

        return TLVUtils.decode(raw, ObjectIdentity.class);
    }

    @TLVField(tag = TLV_TYPE_OID_TYPE, type = TLVTypeEnum.UINT8)
    private ObjectIdentityType type;

    @TLVField(tag = TLV_TYPE_OID_RAW_ID, type = TLVTypeEnum.BYTES, order = 1)
    private byte[] rawId;

    public byte[] encode() {
//        int offset = 0;
//        byte[] raw = new byte[2 + 4 + rawId.length];
//
//        byte[] rawType = ByteUtil.intToBytes(type.ordinal(), ByteOrder.BIG_ENDIAN);
//        raw[offset++] = rawType[0];
//        raw[offset++] = rawType[1];
//
//        System.arraycopy(ByteUtil.intToBytes(rawId.length, ByteOrder.BIG_ENDIAN), 0, raw, offset, 4);
//
//        offset += 4;
//        System.arraycopy(this.rawId, 0, raw, offset, this.rawId.length);
        return TLVUtils.encode(this);
    }
}
