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

package com.alipay.antchain.bridge.commons.core.ptc;

import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ThirdPartyResp {

    /**
     * 返回内容，PTC返回内容，无AM扩展时是PTC原始返回数据，有AM扩展插件时，填充的是AM插件处理后的数据，如果AM扩展执行发生异常，则BODY被清空填充空数组。
     */
    public static final short PTC_RESP_FIELD_BODY = 0;

    /**
     * 定长 4字节uint32_t，取值0/1, 0 代表访问AM扩展插件失败，OS层面需要重试；
     */
    public static final short PTC_RESP_FIELD_AMEXT_CALL_SUCCESS = 1;

    /**
     * 定长 4字节uint32_t，取值0/1, 0 代表调用执行AM扩展查询发生异常，详细内容见PTC_RESP_FIELD_AMEXT_RETURN
     */
    public static final short PTC_RESP_FIELD_AMEXT_EXEC_SUCCESS = 2;

    /**
     * 字符串，执行AM扩展查询发生异常具体报错信息
     */
    public static final short PTC_RESP_FIELD_AMEXT_EXEC_OUTPUT = 3;

    public static ThirdPartyResp decode(byte[] raw) {
        return TLVUtils.decode(raw, ThirdPartyResp.class);
    }

    /**
     * Crosschain message serialized inside. For example Authentic Message.
     */
    @TLVField(tag = PTC_RESP_FIELD_BODY, type = TLVTypeEnum.BYTES)
    private byte[] body;

    public byte[] encode() {
        return TLVUtils.encode(this);
    }
}
