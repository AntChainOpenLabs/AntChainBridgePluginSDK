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
import javax.annotation.Resource;

import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.CrossChainMsgACLItem;
import com.alipay.antchain.bridge.relayer.dal.repository.ICrossChainMsgACLRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GovernManager implements IGovernManager {

    @Resource
    private ICrossChainMsgACLRepository crossChainMsgACLRepository;

    @Override
    public boolean verifyCrossChainMsgACL(String ownerDomain, String ownerIdHex, String grantDomain, String grantIdHex) {
        CrossChainMsgACLItem crossChainMsgACLItem = new CrossChainMsgACLItem();
        crossChainMsgACLItem.setOwnerDomain(ownerDomain);
        crossChainMsgACLItem.setOwnerIdentityHex(ownerIdHex);
        crossChainMsgACLItem.setGrantDomain(grantDomain);
        crossChainMsgACLItem.setGrantIdentityHex(grantIdHex);

        return crossChainMsgACLRepository.checkItem(crossChainMsgACLItem);
    }

    @Override
    public void addCrossChainMsgACL(CrossChainMsgACLItem crossChainMsgACLItem) {
        crossChainMsgACLRepository.saveItem(crossChainMsgACLItem);
    }

    @Override
    public void delCrossChainMsgACL(String bizId) {
        crossChainMsgACLRepository.deleteItem(bizId);
    }

    @Override
    public CrossChainMsgACLItem getCrossChainMsgACL(String bizId) {
        return crossChainMsgACLRepository.getItemByBizId(bizId);
    }

    @Override
    public List<CrossChainMsgACLItem> getMatchedCrossChainACLItems(String ownerDomain, String ownerId, String grantDomain, String grantId) {
        try {
            return crossChainMsgACLRepository.getMatchedItems(new CrossChainMsgACLItem(ownerDomain, ownerId, grantDomain, grantId));
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_GOV_ACL_ERROR,
                    e,
                    "failed to check get crosschain ACL entries that match rule: ( owner_domain: {}, owner_id: {}, grant_domain: {}, grant_id: {} )",
                    ownerDomain, ownerId, grantDomain, grantId
            );
        }
    }

    @Override
    public boolean hasCrossChainMsgACL(String bizId) {
        return crossChainMsgACLRepository.hasItemByBizId(bizId);
    }
}
