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

package com.alipay.antchain.bridge.relayer.core.manager.gov;

import java.util.List;

import com.alipay.antchain.bridge.relayer.commons.model.CrossChainMsgACLItem;

public interface IGovernManager {

    boolean verifyCrossChainMsgACL(String ownerDomain, String ownerIdHex, String grantDomain, String grantIdHex);

    void addCrossChainMsgACL(CrossChainMsgACLItem crossChainMsgACLItem);

    void delCrossChainMsgACL(String bizId);

    CrossChainMsgACLItem getCrossChainMsgACL(String bizId);

    List<CrossChainMsgACLItem> getMatchedCrossChainACLItems(String ownerDomain, String ownerId, String grantDomain, String grantId);

    boolean hasCrossChainMsgACL(String bizId);
}
