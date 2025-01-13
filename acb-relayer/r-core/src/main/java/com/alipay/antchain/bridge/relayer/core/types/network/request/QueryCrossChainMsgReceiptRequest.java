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

package com.alipay.antchain.bridge.relayer.core.types.network.request;


import java.util.List;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QueryCrossChainMsgReceiptRequest extends RelayerRequest {
    public static QueryCrossChainMsgReceiptRequest createFrom(RelayerRequest relayerRequest) {
        QueryCrossChainMsgReceiptRequest request = JSON.parseObject(relayerRequest.getRequestPayload(), QueryCrossChainMsgReceiptRequest.class);
        BeanUtil.copyProperties(relayerRequest, request);
        return request;
    }

    @JSONField
    private List<String> ucpIds;

    public QueryCrossChainMsgReceiptRequest(List<String> ucpIds) {
        super(
                RelayerRequestType.QUERY_CROSSCHAIN_MSG_RECEIPT
        );
        this.ucpIds = ucpIds;
        setRequestPayload(
                JSON.toJSONBytes(this)
        );
    }
}
