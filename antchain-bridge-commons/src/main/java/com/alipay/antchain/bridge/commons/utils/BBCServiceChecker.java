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

package com.alipay.antchain.bridge.commons.utils;

import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;

public class BBCServiceChecker {

    public static boolean ifBBCServiceReady(AbstractBBCContext bbcState) {
        if (bbcState.getPtcContract().getStatus() != ContractStatusEnum.getServiceReadyStatus()) {
            return false;
        }
        if (bbcState.getSdpContract().getStatus() != ContractStatusEnum.getServiceReadyStatus()) {
            return false;
        }
        if (bbcState.getAuthMessageContract().getStatus() != ContractStatusEnum.getServiceReadyStatus()) {
            return false;
        }

        return true;
    }
}
