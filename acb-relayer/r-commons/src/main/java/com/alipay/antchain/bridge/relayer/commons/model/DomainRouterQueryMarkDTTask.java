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

import java.util.List;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DomainRouterQueryMarkDTTask extends MarkDTTask {

    private static final String KEY_SEPARATOR = "^^";

    public static String generateDomainRouterQueryTaskUniqueKey(String senderDomain, String receiverDomain) {
        return StrUtil.format("{}{}{}", senderDomain, KEY_SEPARATOR, receiverDomain);
    }

    private String senderDomain;

    private String receiverDomain;

    public DomainRouterQueryMarkDTTask(MarkDTTask markDTTask) {
        super();
        BeanUtil.copyProperties(markDTTask, this);
        List<String> domains = StrUtil.split(markDTTask.getUniqueKey(), KEY_SEPARATOR);
        senderDomain = domains.get(0);
        receiverDomain = domains.get(1);
    }
}
