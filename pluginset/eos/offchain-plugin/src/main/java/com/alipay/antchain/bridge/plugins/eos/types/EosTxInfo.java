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

package com.alipay.antchain.bridge.plugins.eos.types;

import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class EosTxInfo {

    @JSONField(name = "id")
    private String txId;

    private EosTransactionStatusEnum status;

    @JSONField(name = "block_num")
    private long blockNum;

    @JSONField(name = "last_irreversible_block")
    private long irreversibleNum;

    private List<EosTxAction> actions;

    public boolean isSuccess() {
        return status == EosTransactionStatusEnum.EXECUTED;
    }

    public boolean isConfirmed() {
        return irreversibleNum >= blockNum;
    }

    public boolean containsAction(String account, String name) {
        return actions.stream().anyMatch(
                eosTxAction -> StrUtil.equals(eosTxAction.getAccount(), account) && StrUtil.equals(eosTxAction.getName(), name)
        );
    }
}
