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
public enum AuthMsgTrustLevelEnum {

    ZERO_TRUST(0),

    POSITIVE_TRUST(1),

    NEGATIVE_TRUST(2);

    @EnumValue
    private final int code;

    public static AuthMsgTrustLevelEnum parseFromValue(int value) {
        if (value == ZERO_TRUST.code) {
            return ZERO_TRUST;
        } else if (value == POSITIVE_TRUST.code) {
            return POSITIVE_TRUST;
        } else if (value == NEGATIVE_TRUST.code) {
            return NEGATIVE_TRUST;
        }
        throw new AntChainBridgeRelayerException(
                RelayerErrorCodeEnum.UNKNOWN_INTERNAL_ERROR,
                "Invalid value for am trust level: " + value
        );
    }
}
