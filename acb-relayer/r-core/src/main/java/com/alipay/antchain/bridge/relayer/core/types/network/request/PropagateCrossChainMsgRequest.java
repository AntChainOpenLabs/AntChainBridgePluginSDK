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
import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.commons.core.base.UniformCrosschainPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PropagateCrossChainMsgRequest extends RelayerRequest {

    public static PropagateCrossChainMsgRequest createFrom(RelayerRequest relayerRequest) {
        PropagateCrossChainMsgRequest request = JSON.parseObject(relayerRequest.getRequestPayload(), PropagateCrossChainMsgRequest.class);
        BeanUtil.copyProperties(relayerRequest, request);
        return request;
    }

    @JSONField
    @Deprecated
    private String udagProof;

    @JSONField
    private String ucpId;

    @JSONField
    @Deprecated
    private String authMsg;

    @JSONField
    private String rawUcp;

    @JSONField
    private String domainName;

    @JSONField
    private String ledgerInfo;

    public PropagateCrossChainMsgRequest(
            String udagProof,
            String ucpId,
            IAuthMessage authMsg,
            String domainName,
            String ledgerInfo
    ) {
        super(
                RelayerRequestType.PROPAGATE_CROSSCHAIN_MESSAGE
        );
        this.udagProof = udagProof;
        this.ucpId = ucpId;
        this.authMsg = Base64.encode(authMsg.encode());
        this.domainName = domainName;
        this.ledgerInfo = ledgerInfo;

        setRequestPayload(
                JSON.toJSONBytes(this)
        );
    }

    public PropagateCrossChainMsgRequest(
            String ucpId,
            IAuthMessage authMsg,
            UniformCrosschainPacket ucp,
            String domainName,
            String ledgerInfo
    ) {
        super(
                RelayerRequestType.PROPAGATE_CROSSCHAIN_MESSAGE
        );
        this.udagProof = "";
        this.ucpId = ucpId;
        if (ObjectUtil.isNotNull(authMsg)) {
            this.authMsg = Base64.encode(authMsg.encode());
        } else {
            this.authMsg = "";
        }
        this.domainName = domainName;
        this.ledgerInfo = ledgerInfo;
        if (ObjectUtil.isNotNull(ucp)) {
            this.rawUcp = Base64.encode(ucp.encode());
        } else {
            this.rawUcp = "";
        }

        setRequestPayload(
                JSON.toJSONBytes(this)
        );
    }
}
