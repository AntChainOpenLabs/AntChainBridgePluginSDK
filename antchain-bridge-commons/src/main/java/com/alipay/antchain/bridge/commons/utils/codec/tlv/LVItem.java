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
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LVItem {
    /**
     * 日志LOGGER
     */
    public static int LENGTH_LENGTH = 4;

    private int length;
    private byte[] value;

    public LVItem(int length, byte[] value) {
        if (length != value.length) {
            throw new RuntimeException("length value not equals value length");
        }

        this.length = length;
        this.value = value;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public byte[] encode() {

        ByteArrayOutputStream byteos = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(byteos);
        try {
            os.writeInt(Integer.reverseBytes(length));
            os.write(value);
            return byteos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<LVItem> decode(byte[] bytes) {

        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        DataInputStream dataStream = new DataInputStream(stream);

        List<LVItem> items = new ArrayList<LVItem>();

        try {
            while (dataStream.available() > 0) {
                int lvLen = Integer.reverseBytes(dataStream.readInt());
                byte[] lvValue = new byte[lvLen];
                dataStream.readFully(lvValue);

                items.add(new LVItem(lvLen, lvValue));
            }
            return items;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static LVItem fromUTF8StringMap(String key, String value) {
        ByteArrayOutputStream byteos = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(byteos);

        try {
            os.writeInt(Integer.reverseBytes(key.length()));
            os.write(key.getBytes("UTF-8"));

            os.writeInt(Integer.reverseBytes(value.length()));
            os.write(value.getBytes("UTF-8"));
            return new LVItem(os.size(), byteos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static LVItem fromIntByteMap(int key, byte[] value) {
        ByteArrayOutputStream byteos = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(byteos);

        try {
            os.writeInt(Integer.reverseBytes(4));
            os.writeInt(Integer.reverseBytes(key));
            os.writeInt(Integer.reverseBytes(value.length));
            os.write(value);
            return new LVItem(os.size(), byteos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<Integer, byte[]> getIntByteMap() {
        Map<Integer, byte[]> intByteMap = new HashMap<Integer, byte[]>(1);
        ByteArrayInputStream stream = new ByteArrayInputStream(value);
        DataInputStream dataStream = new DataInputStream(stream);
        try {
            while (dataStream.available() > 0) {
                // keyLength
                Integer.reverseBytes(dataStream.readInt());
                int key = Integer.reverseBytes(dataStream.readInt());
                int valueLength = Integer.reverseBytes(dataStream.readInt());
                byte[] value = new byte[valueLength];
                dataStream.readFully(value);

                intByteMap.put(key, value);
            }
            return intByteMap;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public Map<String, String> getUTF8StringMap() {
        Map<String, String> stringMap = new HashMap<>(1);
        ByteArrayInputStream stream = new ByteArrayInputStream(value);
        DataInputStream dataStream = new DataInputStream(stream);

        try {
            while (dataStream.available() > 0) {
                int keyLen = Integer.reverseBytes(dataStream.readInt());
                byte[] lvkey = new byte[keyLen];
                dataStream.readFully(lvkey);
                String key = new String(lvkey);

                int valueLen = Integer.reverseBytes(dataStream.readInt());
                byte[] lvValue = new byte[valueLen];
                dataStream.readFully(lvValue);
                String value = new String(lvValue);

                stringMap.put(key, value);
            }
            return stringMap;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
