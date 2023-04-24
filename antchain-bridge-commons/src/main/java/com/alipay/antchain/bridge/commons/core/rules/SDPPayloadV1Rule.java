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

package com.alipay.antchain.bridge.commons.core.rules;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageV1;

public class SDPPayloadV1Rule implements AntChainBridgeRule<SDPMessageV1.SDPPayloadV1> {

    public static final int MAX_DOMAIN_LENGTH = 1000_000;

    @Override
    public boolean check(SDPMessageV1.SDPPayloadV1 obj) {
        return obj.getPayload().length < MAX_DOMAIN_LENGTH && !ObjectUtil.isEmpty(obj.getPayload());
    }
}
