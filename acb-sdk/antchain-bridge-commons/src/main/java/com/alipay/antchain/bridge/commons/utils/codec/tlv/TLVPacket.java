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

package com.alipay.antchain.bridge.commons.utils.codec.tlv;

import java.io.*;
import java.util.List;

public class TLVPacket {

    public static int VERSION_LENGTH = 2;
    public static int LENGTH_LENGTH = 4;

    private short version;
    private List<TLVItem> tlvItems;

    public TLVPacket(short version, List<TLVItem> tlvItems) {
        this.version = version;
        this.tlvItems = tlvItems;
    }

    public byte[] encode() {

        ByteArrayOutputStream byteos = new ByteArrayOutputStream();
        DataOutputStream packetBytes = new DataOutputStream(byteos);
        try {
            packetBytes.writeShort(Short.reverseBytes(version));

            ByteArrayOutputStream tlvItemsBytes = new ByteArrayOutputStream();
            for (TLVItem tlvItem : tlvItems) {
                tlvItemsBytes.write(tlvItem.encode());
            }

            packetBytes.writeInt(Integer.reverseBytes(tlvItemsBytes.size()));
            packetBytes.write(tlvItemsBytes.toByteArray());

            return byteos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TLVPacket decode(byte[] bytes) {

        if (null == bytes || bytes.length == 0) {
            return null;
        }

        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        DataInputStream dataStream = new DataInputStream(stream);

        try {
            short version = Short.reverseBytes(dataStream.readShort());
            int length = Integer.reverseBytes(dataStream.readInt());

            if (length < 0) {
                throw new RuntimeException("illage tlv packet, header length is " + length);
            }
            if (length != dataStream.available()) {
                throw new RuntimeException("illage tlv packet, header length is " + length
                        + ", but body length is " + dataStream.available());
            }

            byte[] body = new byte[length];
            dataStream.readFully(body);
            List<TLVItem> tlvItems = TLVItem.decode(body);

            return new TLVPacket(version, tlvItems);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public short getVersion() {
        return version;
    }

    public List<TLVItem> getTlvItems() {
        return tlvItems;
    }

    public boolean containTypes(List<Short> types) {
        if (this.tlvItems.size() < types.size()) {
            return false;
        }

        return types.stream().allMatch(
                type -> this.tlvItems.stream().anyMatch(tlvItem -> tlvItem.getType() == type)
        );
    }

    public TLVItem getItemForTag(short tag) {
        for (TLVItem tlvItem : tlvItems) {
            if (tlvItem.getType() == tag) {
                return tlvItem;
            }
        }
        return null;
    }
}
