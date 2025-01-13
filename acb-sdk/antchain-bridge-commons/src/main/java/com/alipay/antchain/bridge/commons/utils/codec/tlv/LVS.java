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
import java.util.Map;

public class LVS {

    public static int LENGTH_LENGTH = 4;

    private List<LVItem> lvItems;

    public LVS(List<LVItem> lvItems) {
        this.lvItems = lvItems;
    }

    public byte[] encode() {

        ByteArrayOutputStream byteos = new ByteArrayOutputStream();
        DataOutputStream packetBytes = new DataOutputStream(byteos);
        try {

            ByteArrayOutputStream lvItemsBytes = new ByteArrayOutputStream();
            for (LVItem lvItem : lvItems) {
                lvItemsBytes.write(lvItem.encode());
            }

            packetBytes.writeInt(Integer.reverseBytes(lvItemsBytes.size()));
            packetBytes.write(lvItemsBytes.toByteArray());

            return byteos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] stringMapEncode(Map<String, String> stringMap) {
        if (null == stringMap || stringMap.isEmpty()) {
            return new byte[0];
        }
        List<LVItem> lvItems = new ArrayList<>();
        for (String key : stringMap.keySet()) {
            LVItem lvStringMap = LVItem.fromUTF8StringMap(key, stringMap.get(key));
            lvItems.add(lvStringMap);
        }

        LVS lvs = new LVS(lvItems);
        return lvs.encode();

    }

    public static byte[] intByteMapEncode(Map<Integer, byte[]> intByteMap) {
        List<LVItem> lvItems = new ArrayList<>();

        for (int key : intByteMap.keySet()) {
            LVItem lvIntByteMap = LVItem.fromIntByteMap(key, intByteMap.get(key));
            lvItems.add(lvIntByteMap);
        }

        LVS lvs = new LVS(lvItems);
        return lvs.encode();

    }

    public static LVS decode(byte[] bytes) {

        if (null == bytes || bytes.length == 0) {
            return null;
        }

        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        DataInputStream dataStream = new DataInputStream(stream);

        try {
            int length = Integer.reverseBytes(dataStream.readInt());

            if (length < 0) {
                throw new RuntimeException("illage lv packet, header length is " + length);
            }
            if (length != dataStream.available()) {
                throw new RuntimeException("illage lv packet, header length is " + length
                    + ", but body length is " + dataStream.available());
            }

            byte[] body = new byte[length];
            dataStream.readFully(body);
            List<LVItem> lvItems = LVItem.decode(body);

            return new LVS(lvItems);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<LVItem> getlvItems() {
        return lvItems;
    }

}
