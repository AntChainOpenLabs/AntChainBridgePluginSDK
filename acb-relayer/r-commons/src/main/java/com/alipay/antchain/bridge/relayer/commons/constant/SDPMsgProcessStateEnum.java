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
import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum SDPMsgProcessStateEnum {
    PENDING("am_msg_pending"),

    MSG_ILLEGAL("am_msg_fail"),

    MSG_REJECTED("am_msg_rejected"),

    REMOTE_PENDING("remote_pending"),

    TX_PENDING("tx_pending"),

    TX_SUCCESS("tx_success"),

    TX_FAILED("tx_fail"),

    TIMEOUT("timeout"),

    VERIFIED_HEIGHT_SYNC("vh_sync"),

    ROLLBACK("rollback"),

    ROLLBACK_FAILED("rollback_failed");

    @EnumValue
    private final String code;

    @JSONField
    public String getCode() {
        return code;
    }

    @JSONCreator
    public static SDPMsgProcessStateEnum fromCode(String code) {
        for (SDPMsgProcessStateEnum value : values()) {
            if (StrUtil.equals(value.code, code)) {
                return value;
            }
        }
        return null;
    }
}
