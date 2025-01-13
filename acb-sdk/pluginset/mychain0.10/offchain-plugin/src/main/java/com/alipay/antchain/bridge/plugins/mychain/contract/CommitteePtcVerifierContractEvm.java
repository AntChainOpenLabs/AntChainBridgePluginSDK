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

package com.alipay.antchain.bridge.plugins.mychain.contract;

import java.util.UUID;

import com.alipay.antchain.bridge.plugins.mychain.sdk.Mychain010Client;
import com.alipay.mychain.sdk.common.VMTypeEnum;
import com.alipay.mychain.sdk.vm.EVMParameter;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class CommitteePtcVerifierContractEvm {

    private static final String COMMITTEE_VERIFIER_EVM_CONTRACT_PREFIX = "COMMITTEE_VERIFIER_EVM_CONTRACT_";

    private Mychain010Client mychain010Client;

    private final Logger logger;

    @Getter
    @Setter
    private String contractAddress;

    public CommitteePtcVerifierContractEvm(Mychain010Client mychain010Client, Logger logger) {
        this.mychain010Client = mychain010Client;
        this.logger = logger;
    }

    public boolean deployContract() {
        if (StringUtils.isEmpty(this.getContractAddress())) {
            String contractPath = MychainContractBinaryVersionEnum.selectBinaryByVersion(
                    mychain010Client.getConfig().getMychainContractBinaryVersion()
            ).getCommitteeVerifierEvm();
            String contractName = COMMITTEE_VERIFIER_EVM_CONTRACT_PREFIX + UUID.randomUUID();

            logger.info("Deploying CommitteePtcVerifierContractEvm {} with code from {}", contractName, contractPath);

            if (mychain010Client.deployContract(contractPath, contractName, VMTypeEnum.EVM, new EVMParameter())) {
                this.setContractAddress(contractName);
                return true;
            }

            return false;
        }

        return true;
    }
}
