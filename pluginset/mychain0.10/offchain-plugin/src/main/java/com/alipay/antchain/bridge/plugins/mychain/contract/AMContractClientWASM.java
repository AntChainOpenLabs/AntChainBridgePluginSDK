/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package com.alipay.antchain.bridge.plugins.mychain.contract;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import com.alipay.antchain.bridge.commons.bbc.syscontract.AuthMessageContract;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.plugins.mychain.sdk.Mychain010Client;
import com.alipay.mychain.sdk.api.utils.Utils;
import com.alipay.mychain.sdk.common.VMTypeEnum;
import com.alipay.mychain.sdk.domain.account.Identity;
import com.alipay.mychain.sdk.domain.block.Block;
import com.alipay.mychain.sdk.domain.transaction.LogEntry;
import com.alipay.mychain.sdk.domain.transaction.TransactionReceipt;
import com.alipay.mychain.sdk.message.transaction.TransactionReceiptResponse;
import com.alipay.mychain.sdk.vm.EVMOutput;
import com.alipay.mychain.sdk.vm.WASMParameter;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ccen
 * @version $Id: AMClientContractMY010WASM.java, v 0.1 2019-08-31 12:35 PM ccen Exp $$
 */
public class AMContractClientWASM extends AuthMessageContract  implements AbstractAMContractClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(AMContractClientWASM.class);

    // 合约名称前缀
    public static final String AM_WASM_CONTRACT_PREFIX = "AM_WASM_CONTRACT_";

    // 合约接口名
    public static final String ADD_RELAYERS_WASM_METHOD = "AddRelayers";
    public static final String SET_PROTOCOL_WASM_METHOD = "SetProtocol";
    public static final String AM_RECV_PKG_WASM_METHOD = "RecvPkgFromRelayer";

    // 合约跨链事件
    public static final String AM_MSG_SEND_WASM_SIGN_RAW = "SendAuthMessage";
    public static final String AM_MSG_SEND_WASM_SIGN_HEX = Hex.toHexString(AM_MSG_SEND_WASM_SIGN_RAW.getBytes());

    public static final String AM_MSG_RECV_SIGN_HEX = Hex.toHexString("RecvAuthMessage".getBytes());

    protected Mychain010Client mychain010Client;

    public AMContractClientWASM(Mychain010Client mychain010Client) {
        this.mychain010Client = mychain010Client;
    }

    @Override
    public boolean addRelayers(String relayer) {
        WASMParameter parameters = new WASMParameter(ADD_RELAYERS_WASM_METHOD);
        parameters.addIdentity(Utils.getIdentityByName(
                relayer,
                mychain010Client.getConfig().getMychainHashType()));

        return doCallWasmContract(parameters,true).isSuccess();
    }

    /**
     * 设置上层协议信息
     *
     * @param protocolContractName 协议合约地址
     * @param protocolType         协议类型，协议类型为`0`时表示`p2p`协议
     * @return
     */
    @Override
    public boolean setProtocol(String protocolContractName, String protocolType) {
        WASMParameter parameters = new WASMParameter(SET_PROTOCOL_WASM_METHOD);
        parameters.addIdentity(Utils.getIdentityByName(
                protocolContractName,
                mychain010Client.getConfig().getMychainHashType()));
        parameters.addUInt32(
                BigInteger.valueOf(Integer.valueOf(protocolType)));

        if (doCallWasmContract(parameters, true).isSuccess()) {
            this.setStatus(ContractStatusEnum.CONTRACT_READY);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public TransactionReceiptResponse recvPkgFromRelayer(byte[] pkg) {
        WASMParameter parameters = new WASMParameter(AM_RECV_PKG_WASM_METHOD);
        parameters.addBytes(pkg);

        return doCallWasmContract(parameters, false);
    }


    @Override
    public boolean deployContract() {

        if (StringUtils.isEmpty(this.getContractAddress())) {
            String contractPath = MychainContractBinaryVersionEnum.selectBinaryByVersion(
                    mychain010Client.getConfig().getMychainContractBinaryVersion()).getAmClientWasm();
            String contractName = AM_WASM_CONTRACT_PREFIX + UUID.randomUUID().toString();

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


    protected TransactionReceipt doLocalCallWasmContract(WASMParameter parameters) {
        return mychain010Client.localCallContract(
                this.getContractAddress(),
                parameters);
    }

    @Override
    public List<CrossChainMessage> parseCrossChainMessage(Block block) {
        List<CrossChainMessage> msgList = Lists.newArrayList();

        Identity amContractId = Utils.getIdentityByName(
                this.getContractAddress(),
                mychain010Client.getConfig().getMychainHashType());

        // 每个区块里有多个receipt，每个receipt里可能有多个log
        for (int i = 0; i < block.getBlockBody().getReceiptList().size(); i++) {
            TransactionReceipt receipt = block.getBlockBody().getReceiptList().get(i);

            if (receipt.getResult() != 0) {
                LOGGER.info("[notify] paas fail transaction, error code is {}", receipt.getResult());
                continue;
            }

            LOGGER.debug("receipt log size {}", receipt.getLogs().size());

            for (LogEntry logEntry : receipt.getLogs()) {

                // 判断是否存在 AM 合约相关的消息
                if (!amContractId.hexStrValue()
                        .equals(logEntry.getTo().hexStrValue())) {
                    LOGGER.debug("[notify] no am wasm contract address log. sp:{} - ex:{}",
                            amContractId.hexStrValue(),
                            logEntry.getTo().hexStrValue());
                    continue;
                }

                LOGGER.debug("[notify] am wasm contract address log. {}", logEntry.getTo().hexStrValue());

                if (logEntry.getTopics().size() == 0) {
                    continue;
                }

                String logSign = logEntry.getTopics().get(0);

                // 判断是否存在 AM 跨链消息
                if (!AM_MSG_SEND_WASM_SIGN_HEX.equals(logSign)) {
                    LOGGER.debug("[notify] no AuthenticMessage log {}. sp: {} - ex:{}]",
                            AM_MSG_SEND_WASM_SIGN_RAW,
                            logSign,
                            AM_MSG_SEND_WASM_SIGN_HEX);
                    continue;
                }

                LOGGER.debug("[notify] AuthenticMessage log. {}]", logSign);

                msgList.add(CrossChainMessage.createCrossChainMessage(
                        CrossChainMessage.CrossChainMessageType.AUTH_MSG,
                        block.getBlockHeader().getNumber().longValue(),
                        block.getBlockHeader().getTimestamp(),
                        block.getBlockHeader().getHash().getValue(),
                        // evm 和 wasm 合约的不同之处
                        new EVMOutput(logEntry.getTopics().get(1)).getBytes(),
                        // todo: put ledger data, for SPV or other attestations
                        "this time we need no verify. it's ok to set it with empty bytes".getBytes(),
                        // todo: put proof data
                        "this time we need no proof data. it's ok to set it with empty bytes".getBytes(),
                        block.getBlockBody().getTransactionList().get(i).getHash().getValue()
                ));
            }
        }

        return msgList;
    }

}