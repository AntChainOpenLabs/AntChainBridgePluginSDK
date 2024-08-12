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

package com.alipay.antchain.bridge.bcdns.service;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.bcdns.types.exception.AntChainBridgeBCDNSException;
import com.alipay.antchain.bridge.bcdns.types.exception.BCDNSErrorCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BCDNSTypeEnum {

    /**
     * A simple bcdns embedded in your program
     */
    EMBEDDED("embedded"),

    BIF("bif");

    private final String code;

    public static BCDNSTypeEnum parseFromValue(String value) {
        if (StrUtil.equals(value, BCDNSTypeEnum.BIF.getCode())) {
            return BIF;
        }
        if (StrUtil.equals(value, BCDNSTypeEnum.EMBEDDED.getCode())) {
            return EMBEDDED;
        }
        throw new AntChainBridgeBCDNSException(
                BCDNSErrorCodeEnum.BCDNS_TYPE_UNKNOWN,
                "Invalid value for bcdns type: " + value
        );
    }

    public static BCDNSTypeEnum valueOf(Byte value) {
        switch (value) {
            case 0:
                return BIF;
            case 1:
                return EMBEDDED;
            default:
                return null;
        }
    }
}
