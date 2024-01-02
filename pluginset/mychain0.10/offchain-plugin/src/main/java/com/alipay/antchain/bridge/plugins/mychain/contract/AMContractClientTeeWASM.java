/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package com.alipay.antchain.bridge.plugins.mychain.contract;

import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.plugins.mychain.sdk.Mychain010Client;
import com.alipay.mychain.sdk.common.VMTypeEnum;
import com.alipay.mychain.sdk.domain.transaction.TransactionReceipt;
import com.alipay.mychain.sdk.message.transaction.TransactionReceiptResponse;
import com.alipay.mychain.sdk.vm.WASMParameter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * @author hanghang.whh
 */
public class AMContractClientTeeWASM extends AMContractClientWASM {

    private static final String AM_TEE_WASM_CONTRACT_PREFIX = "AM_TEE_WASM_CONTRACT_";

    private static final Logger LOGGER = LoggerFactory.getLogger(AMContractClientTeeWASM.class);

    public AMContractClientTeeWASM(Mychain010Client mychain010Client) {
        super(mychain010Client);
        this.mychain010Client = mychain010Client;
    }

    @Override
    public boolean deployContract() {
        // 如果am client已部署，就不需要重新部署
        if (StringUtils.isEmpty(this.getContractAddress())) {

            String contractPath = MychainContractBinaryVersionEnum.selectBinaryByVersion(
                            mychain010Client.getConfig().getMychainContractBinaryVersion()).getAmClientTeeWasm();
            String contractName = AM_TEE_WASM_CONTRACT_PREFIX + UUID.randomUUID().toString();

            if (mychain010Client.deployContract(
                    contractPath,
                    contractName,
                    VMTypeEnum.WASM,
                    new WASMParameter("init"))) {
                this.setContractAddress(contractName);
                this.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
                return true;
            } else {
                return false;
            }
        }

        return true;
    }

    @Override
    protected TransactionReceiptResponse doCallWasmContract(WASMParameter parameters, boolean sync) {
        return mychain010Client.callTeeWasmContract(
                this.getContractAddress(),
                parameters,
                sync);
    }

    @Override
    protected TransactionReceipt doLocalCallWasmContract(WASMParameter parameters) {
        return mychain010Client.localCallTeeWasmContract(
                this.getContractAddress(),
                parameters);
    }

}