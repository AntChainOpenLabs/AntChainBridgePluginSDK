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

package com.alipay.antchain.bridge.plugins.spi.ptc.core;

import cn.hutool.core.util.StrUtil;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VerifyResult {

    public static VerifyResult fail(String errorMsg) {
        return VerifyResult.builder().success(false).errorMsg(errorMsg).build();
    }

    public static VerifyResult fail(String format, Object... args) {
        return VerifyResult.builder().success(false).errorMsg(StrUtil.format(format, args)).build();
    }

    public static VerifyResult success() {
        return VerifyResult.builder().success(true).errorMsg("").build();
    }

    private boolean success;
    private String errorMsg = "";
}
