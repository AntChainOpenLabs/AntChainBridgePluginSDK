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
import java.util.ArrayList;
import java.util.List;

public class TLVItem {

    public static int TYPE_LENGTH = 2;
    public static int LENGTH_LENGTH = 4;

    private short type;
    private int length;
    private byte[] value;

    public TLVItem(short type, int length, byte[] value) {
        if (length != value.length) {
            throw new RuntimeException("length value not equals value length");
        }

        this.type = type;
        this.length = length;
        this.value = value;
    }

    public byte[] encode() {

        ByteArrayOutputStream byteos = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(byteos);
        try {
            os.writeShort(Short.reverseBytes(type));
            os.writeInt(Integer.reverseBytes(length));
            os.write(value);
            return byteos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<TLVItem> decode(byte[] bytes) {

        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        DataInputStream dataStream = new DataInputStream(stream);

        List<TLVItem> items = new ArrayList<TLVItem>();

        try {
            while (dataStream.available() > 0) {
                short tlvType = Short.reverseBytes(dataStream.readShort());
                int tlvLen = Integer.reverseBytes(dataStream.readInt());
                byte[] tlvValue = new byte[tlvLen];
                dataStream.readFully(tlvValue);

                items.add(new TLVItem(tlvType, tlvLen, tlvValue));
            }
            return items;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static TLVItem fromBytesArray(short type, List<byte[]> bytesArray) {
        ByteArrayOutputStream byteos = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(byteos);

        try {
            for (byte[] bytes : bytesArray) {
                os.writeInt(Integer.reverseBytes(bytes.length));
                os.write(bytes);
            }
            return new TLVItem(type, os.size(), byteos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TLVItem fromStringArray(short type, List<String> strArray) {
        ByteArrayOutputStream byteos = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(byteos);

        try {
            for (String str : strArray) {
                os.writeInt(Integer.reverseBytes(str.getBytes().length));
                os.write(str.getBytes());
            }
            return new TLVItem(type, os.size(), byteos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TLVItem fromUint8(short type, byte uint8) {
        return new TLVItem(type, 1, new byte[]{uint8});
    }

    public static TLVItem fromUint16(short type, short uint16) {
        ByteArrayOutputStream byteos = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(byteos);

        try {
            os.writeShort(Short.reverseBytes(uint16));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new TLVItem(type, 2, byteos.toByteArray());
    }

    public static TLVItem fromUint32(short type, int uint32) {
        ByteArrayOutputStream byteos = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(byteos);

        try {
            os.writeInt(Integer.reverseBytes(uint32));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new TLVItem(type, 4, byteos.toByteArray());
    }

    public static TLVItem fromUint64(short type, long uint64) {
        ByteArrayOutputStream byteos = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(byteos);

        try {
            os.writeLong(Long.reverseBytes(uint64));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new TLVItem(type, 8, byteos.toByteArray());
    }

    public static TLVItem fromBytes(short type, byte[] bytes) {
        return new TLVItem(type, bytes.length, bytes);
    }

    public static TLVItem fromUTF8String(short type, String str) {
        try {
            return new TLVItem(type, str.getBytes("UTF-8").length, str.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public short getType() {
        return type;
    }

    public int getLength() {
        return length;
    }

    public byte[] getValue() {
        return value;
    }

    public List<byte[]> getBytesArray() {
        List<byte[]> bytesArray = new ArrayList<>();

        ByteArrayInputStream stream = new ByteArrayInputStream(value);
        DataInputStream dataStream = new DataInputStream(stream);

        try {
            while (dataStream.available() > 0) {
                int lvLen = Integer.reverseBytes(dataStream.readInt());
                byte[] lvValue = new byte[lvLen];
                dataStream.readFully(lvValue);
                bytesArray.add(lvValue);
            }
            return bytesArray;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getStringArray() {
        List<String> strArray = new ArrayList<>();

        ByteArrayInputStream stream = new ByteArrayInputStream(value);
        DataInputStream dataStream = new DataInputStream(stream);

        try {
            while (dataStream.available() > 0) {
                int lvLen = Integer.reverseBytes(dataStream.readInt());
                byte[] lvValue = new byte[lvLen];
                dataStream.readFully(lvValue);
                strArray.add(new String(lvValue));
            }
            return strArray;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte getUint8Value() {
        if (value.length != 1) {
            throw new RuntimeException("not a uint 8 value, value length is " + value.length);
        }

        return value[0];
    }

    public short getUint16Value() {
        if (value.length != 2) {
            throw new RuntimeException("not a uint 16 value, value length is " + value.length);
        }

        return (short) ((value[0] << 0) + (value[1] << 8));
    }

    public int getUint32Value() {
        if (value.length != 4) {
            throw new RuntimeException("not a uint32 value, value length is " + value.length);
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(value);
        DataInputStream is = new DataInputStream(bis);
        try {
            return Integer.reverseBytes(is.readInt());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public long getUint64Value() {
        if (value.length != 8) {
            throw new RuntimeException("not a uint64 value, value length is " + value.length);
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(value);
        DataInputStream is = new DataInputStream(bis);
        try {
            return Long.reverseBytes(is.readLong());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUtf8String() {
        try {
            return new String(this.value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
