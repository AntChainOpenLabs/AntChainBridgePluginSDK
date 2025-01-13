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

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BlockchainStateEnum {

    INIT("INIT"),

    RUNNING("RUNNING"),

    STOPPED("STOP"),

    UPDATE("UPDATE");

    @JSONField
    private final String code;

    public static BlockchainStateEnum parseFromValue(String value) {
        if (StrUtil.equals(value, RUNNING.code)) {
            return RUNNING;
        } else if (StrUtil.equals(value, STOPPED.code)) {
            return STOPPED;
        }
        throw new AntChainBridgeRelayerException(
                RelayerErrorCodeEnum.UNKNOWN_INTERNAL_ERROR,
                "Invalid value for blockchain state: " + value
        );
    }
}
