package com.alipay.antchain.bridge.plugins.mychain.contract;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.commons.bbc.syscontract.AuthMessageContract;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.plugins.mychain.exceptions.CallContractException;
import com.alipay.antchain.bridge.plugins.mychain.model.CrossChainMsgLedgerData;
import com.alipay.antchain.bridge.commons.core.base.SendResponseResult;
import com.alipay.antchain.bridge.plugins.mychain.sdk.Mychain010Client;
import com.alipay.antchain.bridge.plugins.mychain.utils.MychainUtils;
import com.alipay.mychain.sdk.api.utils.Utils;
import com.alipay.mychain.sdk.common.VMTypeEnum;
import com.alipay.mychain.sdk.crypto.hash.HashFactory;
import com.alipay.mychain.sdk.crypto.hash.HashTypeEnum;
import com.alipay.mychain.sdk.domain.account.Identity;
import com.alipay.mychain.sdk.domain.block.Block;
import com.alipay.mychain.sdk.domain.transaction.LogEntry;
import com.alipay.mychain.sdk.domain.transaction.TransactionReceipt;
import com.alipay.mychain.sdk.message.transaction.TransactionReceiptResponse;
import com.alipay.mychain.sdk.vm.EVMOutput;
import com.alipay.mychain.sdk.vm.EVMParameter;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;

/**
 * mychain0.10的evm版本oracle合约实现
 */
public class AMContractClientEVM extends AuthMessageContract implements AbstractAMContractClient {

    // 合约名称前缀
    public static final String AM_EVM_CONTRACT_PREFIX = "AM_EVM_CONTRACT_";

    // 合约接口名
    public static final String ADD_RELAYERS_METHOD = "addRelayers(identity)";
    public static final String SET_PROTOCOL_METHOD = "setProtocol(identity,uint32)";
    public static final String AM_RECV_PKG_METHOD = "recvPkgFromRelayer(bytes)";
    private static final String SET_PTC_HUB_METHOD = "setPtcHub(identity)";

    // 合约跨链事件
    public static final String AM_MSG_SEND_SIGN_RAW = "SendAuthMessage(bytes)";
    public static final String AM_MSG_SEND_SIGN_HEX = Hex.toHexString(
            HashFactory.getHash(HashTypeEnum.Keccak).hash(AM_MSG_SEND_SIGN_RAW.getBytes()));

    public static final String AM_MSG_RECV_SIGN_RAW = "RecvAuthMessage(identity)";
    public static final String AM_MSG_RECV_SIGN_HEX = Hex.toHexString(
            HashFactory.getHash(HashTypeEnum.Keccak).hash(AM_MSG_RECV_SIGN_RAW.getBytes()));

    protected Mychain010Client mychain010Client;
    
    private final AtomicInteger stateCounter = new AtomicInteger(0);

    private final Logger logger;

    public AMContractClientEVM(Mychain010Client mychain010Client, Logger logger) {
        this.mychain010Client = mychain010Client;
        this.logger = logger;
    }

    @Override
    public boolean addRelayers(String relayer) {
        EVMParameter parameters = new EVMParameter(ADD_RELAYERS_METHOD);
        parameters.addIdentity(Utils.getIdentityByName(
                relayer,
                mychain010Client.getConfig().getMychainHashType()));

        return mychain010Client.callContract(
                        this.getContractAddress(),
                        parameters,
                        true)
                .isSuccess();
    }

    /**
     * 设置上层协议信息
     *
     * @param protocolContractName 协议合约地址
     * @param protocolType         协议类型
     * @return
     */
    @Override
    public boolean setProtocol(String protocolContractName, String protocolType) {
        EVMParameter parameters = new EVMParameter(SET_PROTOCOL_METHOD);
        parameters.addIdentity(Utils.getIdentityByName(
                protocolContractName,
                mychain010Client.getConfig().getMychainHashType()));
        parameters.addUint(
                BigInteger.valueOf(Integer.valueOf(protocolType)));

        if (mychain010Client.callContract(
                        this.getContractAddress(),
                        parameters,
                        true)
                .isSuccess()) {
            if (stateCounter.incrementAndGet() == 2) {
                logger.info("AM contract is ready after set protocol.");
                this.setStatus(ContractStatusEnum.CONTRACT_READY);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void setPtcHub(String ptcHubContractName) {
        EVMParameter parameters = new EVMParameter(SET_PTC_HUB_METHOD);
        parameters.addIdentity(Utils.getIdentityByName(ptcHubContractName, mychain010Client.getConfig().getMychainHashType()));
        SendResponseResult result = mychain010Client.callContract(this.getContractAddress(), parameters, true);
        if (!result.isSuccess()) {
            throw new CallContractException(getContractAddress(), result.getTxId(), result.getErrorMessage());
        }
        if (stateCounter.incrementAndGet() == 2) {
            logger.info("AM contract is ready after set ptc hub.");
            this.setStatus(ContractStatusEnum.CONTRACT_READY);
        }
    }

    @Override
    public SendResponseResult recvPkgFromRelayer(byte[] pkg) {
        EVMParameter parameters = new EVMParameter(AM_RECV_PKG_METHOD);
        parameters.addBytes(pkg);

        return mychain010Client.callContract(
                this.getContractAddress(),
                parameters,
                false);
    }

    /**
     * 部署AM合约
     *
     * @return
     */
    @Override
    public boolean deployContract() {
        if (StringUtils.isEmpty(this.getContractAddress())) {
            String contractPath = MychainContractBinaryVersionEnum.selectBinaryByVersion(
                    mychain010Client.getConfig().getMychainContractBinaryVersion()).getAmClientEvm();
            String contractName = AM_EVM_CONTRACT_PREFIX + UUID.randomUUID().toString();

            if (mychain010Client.deployContract(
                    contractPath,
                    contractName,
                    VMTypeEnum.EVM,
                    new EVMParameter())) {
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
    public List<CrossChainMessage> parseCrossChainMessage(Block block) {
        List<CrossChainMessage> msgList = Lists.newArrayList();

        Identity amContractId = Utils.getIdentityByName(
                this.getContractAddress(),
                mychain010Client.getConfig().getMychainHashType());

        byte[] receiptProof = null;
        // 每个区块里有多个receipt，每个receipt里可能有多个log
        for (int i = 0; i < block.getBlockBody().getReceiptList().size(); i++) {
            TransactionReceipt receipt = block.getBlockBody().getReceiptList().get(i);

            if (receipt.getResult() != 0) {
                logger.info("[notify] paas fail transaction, error code is {}", receipt.getResult());
                continue;
            }

            logger.debug("receipt log size {}", receipt.getLogs().size());

            for (int j = 0; j < receipt.getLogs().size(); j++) {
                LogEntry logEntry = receipt.getLogs().get(j);

                // 判断是否存在 AM 合约相关的消息
                if (!amContractId.hexStrValue()
                        .equals(logEntry.getTo().hexStrValue())) {
                    logger.debug("[notify] no am evm contract address log. sp:{} - ex:{}",
                            amContractId.hexStrValue(),
                            logEntry.getTo().hexStrValue());
                    continue;
                }

                logger.debug("[notify] am evm contract address log. {}", logEntry.getTo().hexStrValue());

                if (logEntry.getTopics().isEmpty()) {
                    continue;
                }

                String logSign = logEntry.getTopics().get(0);

                // 判断是否存在 AM 跨链消息
                if (!AM_MSG_SEND_SIGN_HEX.equals(logSign)) {
                    logger.debug("[notify] no AuthenticMessage log {}. sp: {} - ex:{}]",
                            AM_MSG_SEND_SIGN_RAW,
                            logSign,
                            AM_MSG_SEND_SIGN_HEX);
                    continue;
                }

                logger.debug("[notify] AuthenticMessage log. {}]", logSign);

                if (ObjectUtil.isEmpty(receiptProof)) {
                    receiptProof = MychainUtils.getReceiptProof(
                            block,
                            mychain010Client.getConfig().getMychainHashType()
                    );
                }

                msgList.add(CrossChainMessage.createCrossChainMessage(
                        CrossChainMessage.CrossChainMessageType.AUTH_MSG,
                        block.getBlockHeader().getNumber().longValue(),
                        block.getBlockHeader().getTimestamp(),
                        block.getBlockHeader().getHash().getValue(),
                        // evm 和 wasm 合约的不同之处
                        new EVMOutput(Hex.toHexString(logEntry.getLogData())).getBytes(),
                        CrossChainMsgLedgerData.builder()
                                .receipt(receipt)
                                .logIndex(j)
                                .receiptIndex(i)
                                .logTopicHex(logSign)
                                .amContractIdHex(amContractId.hexStrValue())
                                .build()
                                .encode(),
                        receiptProof,
                        block.getBlockBody().getTransactionList().get(i).getHash().getValue()
                ));
            }
        }

        return msgList;
    }
}
