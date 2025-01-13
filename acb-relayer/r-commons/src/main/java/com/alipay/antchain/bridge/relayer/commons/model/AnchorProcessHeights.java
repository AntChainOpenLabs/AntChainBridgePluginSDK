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

package com.alipay.antchain.bridge.relayer.commons.model;

import java.io.*;
import java.util.Map;

import cn.hutool.core.map.MapUtil;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnchorProcessHeights {

    public static AnchorProcessHeights decode(byte[] rawData) {

        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(rawData));

        try {
            String product = stream.readUTF();
            String blockchainId = stream.readUTF();
            long lastUpdateTime = stream.readLong();

            AnchorProcessHeights anchorProcessHeight = new AnchorProcessHeights(product, blockchainId);
            anchorProcessHeight.setLastUpdateTime(lastUpdateTime);

            int size = stream.readInt();

            while (size > 0) {

                String key = stream.readUTF();
                long height = stream.readLong();
                anchorProcessHeight.getProcessHeights().put(key, height);

                size--;
            }

            return anchorProcessHeight;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getKey(String product, String blockchainId) {
        return product + "^" + blockchainId;
    }

    private String product;

    private String blockchainId;

    private final Map<String, Long> processHeights = MapUtil.newHashMap();

    private final Map<String, Long> modifiedTimeMap = MapUtil.newHashMap();

    private long lastUpdateTime = 0L;

    public AnchorProcessHeights(String product, String blockchainId) {
        this.product = product;
        this.blockchainId = blockchainId;
    }

    public byte[] encode() {

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream dstream = new DataOutputStream(stream);

        try {
            dstream.writeUTF(product);
            dstream.writeUTF(blockchainId);
            dstream.writeLong(lastUpdateTime);
            dstream.writeInt(processHeights.size());
            for (String key : processHeights.keySet()) {
                dstream.writeUTF(key);
                dstream.writeLong(processHeights.get(key));
            }

            dstream.flush();

            return stream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
