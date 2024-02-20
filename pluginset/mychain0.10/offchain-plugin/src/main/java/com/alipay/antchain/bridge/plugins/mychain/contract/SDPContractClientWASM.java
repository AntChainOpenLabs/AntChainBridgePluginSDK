/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package com.alipay.antchain.bridge.plugins.mychain.contract;

import java.util.UUID;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.bbc.syscontract.SDPContract;
import com.alipay.antchain.bridge.plugins.mychain.sdk.Mychain010Client;
import com.alipay.mychain.sdk.api.utils.Utils;
import com.alipay.mychain.sdk.common.VMTypeEnum;
import com.alipay.mychain.sdk.crypto.hash.Hash;
import com.alipay.mychain.sdk.domain.transaction.TransactionReceipt;
import com.alipay.mychain.sdk.message.transaction.TransactionReceiptResponse;
import com.alipay.mychain.sdk.vm.WASMOutput;
import com.alipay.mychain.sdk.vm.WASMParameter;
import lombok.Getter;
import lombok.Setter;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;

@Getter
@Setter
public class SDPContractClientWASM extends SDPContract implements AbstractSDPContractClient {
    private static final String SDP_WASM_CONTRACT_PREFIX = "SDP_WASM_CONTRACT_";
    private static final String SET_AM_CONTRACT_AND_DOMAIN_WASM_SIGN = "SetAmContractAndDomain";
    private static final String QUERY_SDP_SEQ_WASM_SIGN = "QueryP2PMsgSeqOnChain";

    protected Mychain010Client mychain010Client;

    public String localDomain;

    private Logger logger;

    public SDPContractClientWASM(Mychain010Client mychain010Client, Logger logger) {
        this.mychain010Client = mychain010Client;
        this.logger = logger;
    }

    @Override
    public boolean setAmContractAndDomain(String amContractName) {
        WASMParameter parameters = new WASMParameter(SET_AM_CONTRACT_AND_DOMAIN_WASM_SIGN);
        parameters.addIdentity(Utils.getIdentityByName(
                amContractName,
                mychain010Client.getConfig().getMychainHashType()));
        parameters.addString(localDomain);

        if (doCallWasmContract(parameters, true).isSuccess()) {
            this.setStatus(ContractStatusEnum.CONTRACT_READY);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public long queryP2PMsgSeqOnChain(String senderDomain,
                                      String from,
                                      String receiverDomain,
                                      String to) {
        WASMParameter parameters = new WASMParameter(QUERY_SDP_SEQ_WASM_SIGN);
        parameters.addString(senderDomain);
        parameters.addIdentity(new Hash(from));
        parameters.addString(receiverDomain);
        parameters.addIdentity(new Hash(to));

        // TODO: handle errors
        TransactionReceipt receipt = doLocalCallContract(parameters);
        WASMOutput output = new WASMOutput(Hex.toHexString(receipt.getOutput()));
        return output.getUint32().intValue();
    }

    @Override
    public boolean deployContract() {
        if (StrUtil.isEmpty(this.getContractAddress())) {

            String contractPath = MychainContractBinaryVersionEnum.selectBinaryByVersion(
                    mychain010Client.getConfig().getMychainContractBinaryVersion()).getSdpWasm();
            String contractName = SDP_WASM_CONTRACT_PREFIX + UUID.randomUUID().toString();

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

    protected TransactionReceiptResponse doCallWasmContract(WASMParameter parameters, boolean sync) {
        return mychain010Client.callContract(
                this.getContractAddress(),
                parameters,
                sync);
    }


    protected TransactionReceipt doLocalCallContract(WASMParameter parameters) {
        return mychain010Client.localCallContract(
                this.getContractAddress(),
                parameters);
    }
}