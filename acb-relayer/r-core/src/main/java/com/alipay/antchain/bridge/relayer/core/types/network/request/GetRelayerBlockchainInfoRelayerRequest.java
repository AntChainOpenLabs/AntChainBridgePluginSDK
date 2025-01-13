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

import cn.hutool.core.bean.BeanUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GetRelayerBlockchainInfoRelayerRequest extends RelayerRequest {

    private String domainToQuery;

    public static GetRelayerBlockchainInfoRelayerRequest createFrom(RelayerRequest relayerRequest) {
        GetRelayerBlockchainInfoRelayerRequest request = BeanUtil.copyProperties(
                relayerRequest,
                GetRelayerBlockchainInfoRelayerRequest.class
        );
        request.setDomainToQuery(new String(relayerRequest.getRequestPayload()));
        return request;
    }

    public GetRelayerBlockchainInfoRelayerRequest(
            String domainToQuery
    ) {
        super(
                RelayerRequestType.GET_RELAYER_BLOCKCHAIN_INFO
        );
        this.setRequestPayload(domainToQuery.getBytes());
    }
}
