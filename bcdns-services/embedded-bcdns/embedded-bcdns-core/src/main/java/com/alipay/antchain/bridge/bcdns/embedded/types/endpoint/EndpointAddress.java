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

package com.alipay.antchain.bridge.bcdns.embedded.types.endpoint;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EndpointAddress {

    private String host;

    private int port;

    private ProtocolHeaderEnum protocolHeader;

    public EndpointAddress(String url) {
        url = StrUtil.trim(url);
        int headerIdx = StrUtil.lastIndexOfIgnoreCase(url, "/");
        protocolHeader = ProtocolHeaderEnum.parseFrom(StrUtil.sub(url, 0, headerIdx + 1));
        String address = StrUtil.sub(url, headerIdx + 1, url.length());
        String[] arr = address.split(":");
        if (arr.length != 2) {
            throw new IllegalArgumentException("Invalid url: " + url);
        }
        host = arr[0];
        port = Integer.parseInt(arr[1]);
    }

    public String getUrl() {
        return protocolHeader.getHeader() + host + ":" + port;
    }
}