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

package com.alipay.antchain.bridge.commons.core.am;

import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;

public enum AuthMessageTrustLevelEnum {

    ZERO_TRUST,

    POSITIVE_TRUST,

    NEGATIVE_TRUST;

    public static AuthMessageTrustLevelEnum parseFromValue(int value) {
        if (value == ZERO_TRUST.ordinal()) {
            return ZERO_TRUST;
        } else if (value == POSITIVE_TRUST.ordinal()) {
            return POSITIVE_TRUST;
        } else if (value == NEGATIVE_TRUST.ordinal()) {
            return NEGATIVE_TRUST;
        }
        throw new AntChainBridgeCommonsException(
                CommonsErrorCodeEnum.AUTH_MESSAGE_DECODE_ERROR,
                "Invalid value for am trust level: " + value
        );
    }
}
