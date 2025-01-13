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

package com.alipay.antchain.bridge.relayer.commons.constant;

import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PluginServerStateEnum {
    INIT(0),
    READY(1),
    STOP(2),
    HEARTBEAT_LOST(3),
    NOT_FOUND(4);

    @EnumValue
    private final int code;

    public static PluginServerStateEnum parseFromValue(int value) {
        if (value == INIT.code) {
            return INIT;
        } else if (value == READY.code) {
            return READY;
        } else if (value == STOP.code) {
            return STOP;
        } else if (value == HEARTBEAT_LOST.code) {
            return HEARTBEAT_LOST;
        } else if (value == NOT_FOUND.code){
            return NOT_FOUND;
        }
        throw new AntChainBridgeRelayerException(RelayerErrorCodeEnum.UNKNOWN_INTERNAL_ERROR, "Invalid value for plugin server state: " + value);
    }
}