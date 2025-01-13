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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RelayerHealthInfo {

    private long activateLength;

    private long lastActiveTime;

    private String nodeIpAddress;

    private int nodePort;

    private boolean active;

    public RelayerHealthInfo(
            String nodeIpAddress,
            int nodePort,
            long lastActiveTime,
            long activateLength
    ) {
        this.nodeIpAddress = nodeIpAddress;
        this.nodePort = nodePort;
        this.lastActiveTime = lastActiveTime;
        this.activateLength = activateLength;
        this.active = (System.currentTimeMillis() - lastActiveTime) <= activateLength;
    }
}
