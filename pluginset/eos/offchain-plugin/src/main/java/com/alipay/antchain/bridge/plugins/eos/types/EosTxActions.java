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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class EosTxActions {

    public static List<EosTxActions> parseTxActionsListByNameAndAcc(List<EosTxActions> eosTxActionsList, String actionName, String account) {
        return eosTxActionsList.stream()
                .filter(actions -> actions.hasActionWithName(actionName))
                .map(
                        actions -> new EosTxActions(
                                actions.getSubActionsByNameAndAcc(actionName, account),
                                actions.getTxId(),
                                actions.getStatus()
                        )
                ).collect(Collectors.toList());
    }

    public static EosTxActions convertFrom(String resp) {
        JSONObject rawTx = JSON.parseObject(resp);
        List<EosTxAction> actions = rawTx.getJSONArray("traces").toJavaList(JSONObject.class).stream()
                .map(o -> o.getJSONObject("act"))
                .map(o -> o.toJavaObject(EosTxAction.class))
                .collect(Collectors.toList());
        return new EosTxActions(
                actions,
                rawTx.getString("id"),
                EosTransactionStatusEnum.parse(
                        rawTx.getJSONObject("trx").getJSONObject("receipt").getString("status")
                )
        );
    }

    private List<EosTxAction> actions;

    private String txId;

    private EosTransactionStatusEnum status;

    public List<EosTxAction> getSubActionsByNameAndAcc(String actionName, String account) {
        if (ObjectUtil.isEmpty(this.actions)) {
            return new ArrayList<>();
        }
        return this.actions.stream()
                .filter(
                        action -> StrUtil.equals(actionName, action.getName()) && StrUtil.equals(account, action.getAccount())
                ).collect(Collectors.toList());
    }

    public boolean hasActionWithName(String actionName) {
        if (ObjectUtil.isEmpty(this.actions)) {
            return false;
        }

        return this.actions.stream().anyMatch(action -> StrUtil.equals(actionName, action.getName()));
    }
}
