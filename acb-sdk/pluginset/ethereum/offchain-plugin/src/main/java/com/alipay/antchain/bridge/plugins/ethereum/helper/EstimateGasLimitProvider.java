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

package com.alipay.antchain.bridge.plugins.ethereum.helper;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.SneakyThrows;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;

import java.math.BigInteger;

@AllArgsConstructor
@Builder
public class EstimateGasLimitProvider implements IGasLimitProvider {

    private Web3j web3j;

    private String fromAddress;

    private String toAddress;

    private String dataHex;

    private long extraGasLimit;

    @Override
    public BigInteger getGasLimit(String contractFunc) {
        return getGasLimitLogic(contractFunc);
    }

    @Override
    public BigInteger getGasLimit() {
        return getGasLimitLogic("");
    }

    @SneakyThrows
    private BigInteger getGasLimitLogic(String contractFunc) {
        EthEstimateGas ethEstimateGas;
        if (StrUtil.equals(contractFunc, "deploy")) {
            ethEstimateGas = web3j.ethEstimateGas(
                    Transaction.createEthCallTransaction(
                            fromAddress,
                            toAddress,
                            dataHex
                    )
            ).send();
        } else {
            ethEstimateGas = web3j.ethEstimateGas(
                    Transaction.createEthCallTransaction(
                            fromAddress,
                            toAddress,
                            dataHex
                    )
            ).send();
        }
        if (ethEstimateGas.hasError()) {
            throw new RuntimeException(StrUtil.format("failed to estimate gas for {} : {}", contractFunc, ethEstimateGas.getError().getMessage()));
        }

        return ethEstimateGas.getAmountUsed().add(BigInteger.valueOf(extraGasLimit));
    }
}
