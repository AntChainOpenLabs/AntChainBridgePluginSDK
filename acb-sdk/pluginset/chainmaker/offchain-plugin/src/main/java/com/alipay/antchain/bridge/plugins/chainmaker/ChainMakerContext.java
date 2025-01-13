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

package com.alipay.antchain.bridge.plugins.chainmaker;

import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChainMakerContext extends AbstractBBCContext {
    private String amContractName;
    private String sdpContractName;


    public ChainMakerContext(AbstractBBCContext context) {
        this.setSdpContract(context.getSdpContract());
        this.setPtcContract(context.getPtcContract());
        this.setAuthMessageContract(context.getAuthMessageContract());
        this.setConfForBlockchainClient(context.getConfForBlockchainClient());
        if (context instanceof ChainMakerContext) {
            // 如果 context 是 ChainMakerContext，可以直接进行赋值

            this.setAmContractName(((ChainMakerContext) context).getAmContractName());
            this.setSdpContractName(((ChainMakerContext) context).getSdpContractName());
        }
    }

    @Override
    public void decodeFromBytes(byte[] raw) {
        AbstractBBCContext state = JSON.parseObject(raw, this.getClass());
        this.setSdpContract(state.getSdpContract());
        this.setPtcContract(state.getPtcContract());
        this.setAuthMessageContract(state.getAuthMessageContract());
        this.setConfForBlockchainClient(state.getConfForBlockchainClient());
        if (state instanceof ChainMakerContext) {
            // 如果 context 是 ChainMakerContext，可以直接进行赋值
            this.setAmContractName(((ChainMakerContext) state).getAmContractName());
            this.setSdpContractName(((ChainMakerContext) state).getSdpContractName());
        }
    }
}
