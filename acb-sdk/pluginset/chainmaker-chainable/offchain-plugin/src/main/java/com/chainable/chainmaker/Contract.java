/*
 * Copyright 2024 Chainable
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

package com.chainable.chainmaker;

import org.chainmaker.pb.common.ResultOuterClass;
import org.chainmaker.sdk.ChainClient;
import org.chainmaker.sdk.SdkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;

import java.util.*;

public class Contract {
    private static final Logger LOGGER = LoggerFactory.getLogger(Contract.class);
    private static final String CONTRACT_ARGS_EVM_PARAM = "data";


    public static ResultOuterClass.TxResponse invokeEVMContract(ChainClient chainClient, String contractName, Function function) {
        Map<String, byte[]> params = new HashMap<>();
        String methodDataStr = FunctionEncoder.encode(function);
        String method = methodDataStr.substring(0, 10);
        params.put(CONTRACT_ARGS_EVM_PARAM, methodDataStr.getBytes());

        ResultOuterClass.TxResponse responseInfo = null;
        try {
            responseInfo = chainClient.invokeContract(contractName,
                    method, null, params, ChainmakerSDK.rpcCallTimeout, ChainmakerSDK.syncResultTimeout);
            LOGGER.info(responseInfo.toString());
        } catch (SdkException e) {
            e.printStackTrace();
        }
        LOGGER.info("执行EVM合约结果：");
        LOGGER.info(responseInfo != null ? responseInfo.toString() : null);
        return responseInfo;
    }

    public static void queryContract(ChainClient chainClient, String contractName, String funcName) {
        ResultOuterClass.TxResponse responseInfo = null;
        try {
            responseInfo = chainClient.queryContract(contractName, funcName,
                    null,  null,10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        LOGGER.info("查询合约结果：");
        LOGGER.info(responseInfo.toString());
    }
}