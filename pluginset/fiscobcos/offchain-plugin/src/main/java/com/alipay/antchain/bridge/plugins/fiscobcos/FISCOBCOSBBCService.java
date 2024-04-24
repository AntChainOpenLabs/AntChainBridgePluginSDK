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
package com.alipay.antchain.bridge.plugins.fiscobcos;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.AuthMessageContract;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.bbc.syscontract.SDPContract;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.plugins.lib.BBCService;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import lombok.Getter;
import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock;
import org.fisco.bcos.sdk.v3.codec.ContractCodecException;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import com.alipay.antchain.bridge.plugins.fiscobcos.abi.AuthMsg;
import com.alipay.antchain.bridge.plugins.fiscobcos.abi.SDPMsg;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.model.callback.TransactionCallback;
import org.fisco.bcos.sdk.v3.transaction.codec.decode.TransactionDecoderInterface;
import org.fisco.bcos.sdk.v3.transaction.codec.decode.TransactionDecoderService;
import org.fisco.bcos.sdk.v3.transaction.manager.AssembleTransactionProcessor;
import org.fisco.bcos.sdk.v3.transaction.manager.TransactionProcessorFactory;
import org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes;
import org.fisco.bcos.sdk.v3.transaction.tools.ContractLoader;

import static com.alipay.antchain.bridge.plugins.fiscobcos.abi.AuthMsg.SENDAUTHMESSAGE_EVENT;

@BBCService(products = "fiscobcos", pluginId = "plugin-fiscobcos")
@Getter
public class FISCOBCOSBBCService extends AbstractBBCService{
    private FISCOBCOSConfig config;

    private BcosSDK sdk;
    private Client client;
    private CryptoKeyPair keyPair;
    private AssembleTransactionProcessor transactionProcessor;

    private AbstractBBCContext bbcContext;
    public static final String abiFile = FISCOBCOSBBCService.class.getClassLoader().getResource("abi").getPath();
    public static final String binFile = FISCOBCOSBBCService.class.getClassLoader().getResource("bin").getPath();

    public void start() {
        System.out.println(FISCOBCOSBBCService.class.getClassLoader().getResource("config.toml").getPath());
    }
    @Override
    public void startup(AbstractBBCContext abstractBBCContext) {
        getBBCLogger().info("FISCO-BCOS BBCService startup with context: {}", new String(abstractBBCContext.getConfForBlockchainClient()));

        if (ObjectUtil.isNull(abstractBBCContext)) {
            throw new RuntimeException("null bbc context");
        }
        if (ObjectUtil.isEmpty(abstractBBCContext.getConfForBlockchainClient())) {
            throw new RuntimeException("empty blockchain client conf");
        }

        // 1. obtain the configuration information
        try {
            config = FISCOBCOSConfig.fromJsonString(new String(abstractBBCContext.getConfForBlockchainClient()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(StrUtil.isEmpty(config.getFileName())){
            throw new RuntimeException("filename for configuration file is empty");
        }

        if(StrUtil.isEmpty(config.getGroupID())){
            throw new RuntimeException("groupID to which the connected node belongs is empty");
        }

        // 2. connect to the FISCO-BCOS network
        try{
            // Initialize BcosSDK
            sdk = BcosSDK.build(FISCOBCOSBBCService.class.getClassLoader().getResource(config.getFileName()).getPath());
            // Initialize the client for the group
            client = sdk.getClient(config.getGroupID());

        }catch (Exception e){
            throw new RuntimeException(String.format("failed to connect fisco-bcos with %s to %s", config.getFileName(), config.getGroupID()), e);
        }

        // 3. initialize keypair and create transaction processor
        this.keyPair = client.getCryptoSuite().getCryptoKeyPair();
        try {
            this.transactionProcessor =
                    TransactionProcessorFactory.createAssembleTransactionProcessor(
                            client, keyPair,
                            abiFile,
                            binFile
                    );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 4. set context
        this.bbcContext = abstractBBCContext;

        // 5. set the pre-deployed contracts into context
        if (ObjectUtil.isNull(abstractBBCContext.getAuthMessageContract())
                && StrUtil.isNotEmpty(this.config.getAmContractAddressDeployed())) {
            AuthMessageContract authMessageContract = new AuthMessageContract();
            authMessageContract.setContractAddress(this.config.getAmContractAddressDeployed());
            authMessageContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
            this.bbcContext.setAuthMessageContract(authMessageContract);
        }

        if (ObjectUtil.isNull(abstractBBCContext.getSdpContract())
                && StrUtil.isNotEmpty(this.config.getSdpContractAddressDeployed())) {
            SDPContract sdpContract = new SDPContract();
            sdpContract.setContractAddress(this.config.getSdpContractAddressDeployed());
            sdpContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
            this.bbcContext.setSdpContract(sdpContract);
        }
    }

    @Override
    public void shutdown() {
        getBBCLogger().info("shut down FISCO-BCOS BBCService!");
        this.client.stop();

    }

    @Override
    public AbstractBBCContext getContext() {
        if (ObjectUtil.isNull(this.bbcContext)){
            throw new RuntimeException("empty bbc context");
        }

        getBBCLogger().debug("FISCO-BCOS BBCService context (amAddr: {}, amStatus: {}, sdpAddr: {}, sdpStatus: {})",
                this.bbcContext.getAuthMessageContract() != null ? this.bbcContext.getAuthMessageContract().getContractAddress() : "",
                this.bbcContext.getAuthMessageContract() != null ? this.bbcContext.getAuthMessageContract().getStatus() : "",
                this.bbcContext.getSdpContract() != null ? this.bbcContext.getSdpContract().getContractAddress() : "",
                this.bbcContext.getSdpContract() != null ? this.bbcContext.getSdpContract().getStatus() : ""
        );

        return this.bbcContext;
    }

    @Override
    public CrossChainMessageReceipt readCrossChainMessageReceipt(String txHash) {
        // 1. Obtain FISCO-BCOS receipt according to transaction hash
        TransactionReceipt transactionReceipt;

        try {
            transactionReceipt = client.getTransactionReceipt(txHash,false).getTransactionReceipt();
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to read cross chain message receipt (txHash: %s)", txHash
                    ), e
            );
        }

        // 2. Construct cross-chain message receipt
        CrossChainMessageReceipt crossChainMessageReceipt = getCrossChainMessageReceipt(transactionReceipt);
        getBBCLogger().info("cross chain message receipt for txhash {} : {}", txHash, JSON.toJSONString(crossChainMessageReceipt));

        return crossChainMessageReceipt;
    }

    private CrossChainMessageReceipt getCrossChainMessageReceipt(TransactionReceipt transactionReceipt) {
        CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();
        if (transactionReceipt == null) {
            // If the transaction is not packaged, the return receipt is empty
            crossChainMessageReceipt.setConfirmed(false);
            crossChainMessageReceipt.setSuccessful(false);
            crossChainMessageReceipt.setTxhash("");
            crossChainMessageReceipt.setErrorMsg("");
            return crossChainMessageReceipt;
        }

        SDPMsg sdpMsg = SDPMsg.load(config.getSdpContractAddressDeployed(), client, keyPair);
        List<SDPMsg.ReceiveMessageEventResponse> receiveMessageEventResponses = sdpMsg.getReceiveMessageEvents(transactionReceipt);
        if (ObjectUtil.isNotEmpty(receiveMessageEventResponses)) {
            SDPMsg.ReceiveMessageEventResponse response = receiveMessageEventResponses.get(0);
            crossChainMessageReceipt.setConfirmed(true);
            crossChainMessageReceipt.setSuccessful(transactionReceipt.isStatusOK() && response.result);
            crossChainMessageReceipt.setTxhash(transactionReceipt.getTransactionHash());
            crossChainMessageReceipt.setErrorMsg(
                    transactionReceipt.isStatusOK() ? StrUtil.format(
                            "SDP calls biz contract: {}", response.result ? "SUCCESS" : response.errMsg
                    ) : StrUtil.emptyToDefault(transactionReceipt.getMessage(), "")
            );
            getBBCLogger().info(
                    "event receiveMessage from SDP contract is found in tx {} of block {} : " +
                            "( send_domain: {}, sender: {}, receiver: {}, biz_call: {}, err_msg: {} )",
                    transactionReceipt.getTransactionHash(), transactionReceipt.getBlockNumber(),
                    response.senderDomain, HexUtil.encodeHexStr(response.senderID), response.receiverID, response.result.toString(),
                    response.errMsg
            );
            return crossChainMessageReceipt;
        }

        crossChainMessageReceipt.setConfirmed(true);
        crossChainMessageReceipt.setSuccessful(transactionReceipt.isStatusOK());
        crossChainMessageReceipt.setTxhash(transactionReceipt.getTransactionHash());
        crossChainMessageReceipt.setErrorMsg(StrUtil.emptyToDefault(transactionReceipt.getMessage(), ""));

        return crossChainMessageReceipt;
    }

    @Override
    public List<CrossChainMessage> readCrossChainMessagesByHeight(long height) {
        if (ObjectUtil.isNull(this.bbcContext)){
            throw new RuntimeException("empty bbc context");
        }

        if (ObjectUtil.isNull(this.bbcContext.getAuthMessageContract())){
            throw new RuntimeException("empty am contract in bbc context");
        }

        List<CrossChainMessage> messageList = ListUtil.toList();
        try {

            // 1. get block
            BcosBlock.Block block = client.getBlockByNumber(BigInteger.valueOf(height), false,true).getBlock();
            TransactionDecoderInterface decoder =
                    new TransactionDecoderService(client.getCryptoSuite(), false);
            ContractLoader contractLoader = new ContractLoader(abiFile, binFile);
            String abi = contractLoader.getABIByContractName("AuthMsg");

            // 2. get crosschain msgs
            messageList.addAll(
                    // 2.1 get txHashes in block
                    block.getTransactionHashes().stream()
                            .map(txHash -> {
                                // 2.2 get transaction receipt
                                TransactionReceipt receipt = client.getTransactionReceipt(txHash.get(), false).getTransactionReceipt();
                                // 2.3 decode events from transaction receipt
                                Map<String, List<List<Object>>> events = null;
                                try {
                                    events = decoder.decodeEvents(abi, receipt.getLogEntries());
                                } catch (ContractCodecException e) {
                                    throw new RuntimeException(e);
                                }
                                return events.getOrDefault("SendAuthMessage", Collections.emptyList()).stream()
                                        .map(event -> {
                                            // 2.4 create crosschain msg
                                            return CrossChainMessage.createCrossChainMessage(
                                                    CrossChainMessage.CrossChainMessageType.AUTH_MSG,
                                                    receipt.getBlockNumber().longValue(),
                                                    block.getTimestamp(),
                                                    HexUtil.decodeHex(StrUtil.removePrefix(block.getHash().trim(), "0x")),
                                                    (byte[]) event.get(0),
                                                    // todo: put ledger data, for SPV or other attestations
                                                    // this time we need no verify. it's ok to set it with empty bytes
                                                    "".getBytes(),
                                                    // todo: put ledger data, for SPV or other attestations
                                                    // this time we need no verify. it's ok to set it with empty bytes
                                                    "".getBytes(),
                                                    HexUtil.decodeHex(txHash.get().replaceFirst("^0x", ""))
                                            );
                                        }).collect(Collectors.toList());
                            })
                            .flatMap(List::stream) // flatten from List<List<CrossChainMessage>> to List<CrossChainMessage>
                            .collect(Collectors.toList())
            );

            if (!messageList.isEmpty()) {
                getBBCLogger().info("read cross chain messages (height: {}, msg_size: {})", height, messageList.size());
                getBBCLogger().debug("read cross chain messages (height: {}, msgs: {})",
                        height,
                        messageList.stream().map(JSON::toJSONString).collect(Collectors.joining(","))
                );
            }

            return messageList;
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to readCrossChainMessagesByHeight (Height: %d, contractAddr: %s, topic: %s)",
                            height,
                            this.bbcContext.getAuthMessageContract().getContractAddress(),
                            SENDAUTHMESSAGE_EVENT
                    ), e
            );
        }
    }

    @Override
    public Long queryLatestHeight() {
        Long l;
        try {
            l = client.getBlockNumber().getBlockNumber().longValue();
        } catch (Exception e) {
            throw new RuntimeException("failed to query latest height", e);
        }
        getBBCLogger().debug("latest height: {}", l);
        return l;
    }

    @Override
    public long querySDPMessageSeq(String senderDomain, String senderID, String receiverDomain, String receiverID) {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)){
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getSdpContract())){
            throw new RuntimeException("empty sdp contract in bbc context");
        }

        // 2. load sdpMsg
        SDPMsg sdpMsg = SDPMsg.load(
                bbcContext.getSdpContract().getContractAddress(),
                this.client,
                this.keyPair
        );

        // 3. query sequence
        long seq;
        try {
            seq = sdpMsg.querySDPMessageSeq(
                    senderDomain,
                    HexUtil.decodeHex(senderID),
                    receiverDomain,
                    HexUtil.decodeHex(receiverID)
            ).longValue();

            getBBCLogger().info("sdpMsg seq: {} (senderDomain: {}, senderID: {}, receiverDomain: {}, receiverID: {})",
                    seq,
                    senderDomain,
                    senderID,
                    receiverDomain,
                    receiverID
            );
        } catch (Exception e) {
            throw new RuntimeException(String.format(
                    "failed to query sdpMsg seq (senderDomain: %s, senderID: %s, receiverDomain: %s, receiverID: %s)",
                    senderDomain,
                    senderID,
                    receiverDomain,
                    receiverID
            ), e);
        }

        return seq;
    }

    @Override
    public void setProtocol(String protocolAddress, String protocolType) {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getAuthMessageContract())){
            throw new RuntimeException("empty am contract in bbc context");
        }

        // 2. load am contract
        AuthMsg am = AuthMsg.load(
                this.bbcContext.getAuthMessageContract().getContractAddress(),
                this.client,
                this.keyPair
        );

        // 3. set protocol to am
        try {
            TransactionReceipt receipt = am.setProtocol(protocolAddress, BigInteger.valueOf(Long.parseLong(protocolType)));
            if(receipt.getStatus() == 0){
                getBBCLogger().info(
                        "set protocol (address: {}, type: {}) to AM {} by tx {} ",
                        protocolAddress, protocolType,
                        this.bbcContext.getAuthMessageContract().getContractAddress(),
                        receipt.getTransactionHash()
                );
            }else{
                getBBCLogger().info(
                        "set protocol failed, receipt status code: {}",
                        receipt.getStatus()
                );
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to set protocol (address: %s, type: %s) to AM %s",
                            protocolAddress, protocolType, this.bbcContext.getAuthMessageContract().getContractAddress()
                    ), e
            );
        }

        // 4. update am contract status
        try {
            if (!StrUtil.isEmpty(am.getProtocol(BigInteger.ZERO))){
                this.bbcContext.getAuthMessageContract().setStatus(ContractStatusEnum.CONTRACT_READY);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to update am contract status (address: %s)",
                            this.bbcContext.getAuthMessageContract().getContractAddress()
                    ), e);
        }
    }

    @Override
    public CrossChainMessageReceipt relayAuthMessage(byte[] rawMessage) {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getAuthMessageContract())){
            throw new RuntimeException("empty am contract in bbc context");
        }

        getBBCLogger().info("relay AM {} to {} ",
                HexUtil.encodeHexStr(rawMessage), this.bbcContext.getAuthMessageContract().getContractAddress());

        // 2. creat Transaction
        CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();
        try{
            // 2.1 async send tx
            transactionProcessor.sendTransactionAndGetReceiptByContractLoaderAsync(
                    "AuthMsg", // contract name
                    this.bbcContext.getAuthMessageContract().getContractAddress(),  // contract address
                    AuthMsg.FUNC_RECVPKGFROMRELAYER, // function name
                    Collections.singletonList(new DynamicBytes(rawMessage)), // input
                    new TransactionCallback() { // callback
                        @Override
                        public void onResponse(TransactionReceipt receipt) {
                            // set `confirmed` to false and `successful` to true if succeeded
                            crossChainMessageReceipt.setConfirmed(false);
                            crossChainMessageReceipt.setSuccessful(true);
                            crossChainMessageReceipt.setTxhash(receipt.getTransactionHash());
                            crossChainMessageReceipt.setErrorMsg("");

                            getBBCLogger().info("relay tx {}", receipt.getTransactionHash());
                        }
                    });
            // 2.2 return crossChainMessageReceipt
            return crossChainMessageReceipt;
        }catch (Exception e){
            throw new RuntimeException(
                    String.format("failed to relay AM %s to %s",
                            HexUtil.encodeHexStr(rawMessage), this.bbcContext.getAuthMessageContract().getContractAddress()
                    ), e
            );
        }
    }

    @Override
    public void setupAuthMessageContract() {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)){
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNotNull(this.bbcContext.getAuthMessageContract())
                && StrUtil.isNotEmpty(this.bbcContext.getAuthMessageContract().getContractAddress())) {
            // If the contract has been pre-deployed and the contract address is configured in the configuration file,
            // there is no need to redeploy.
            return;
        }

        // 2. deploy contract
        AuthMsg authMsg;
        try {
            authMsg = AuthMsg.deploy(client,keyPair);
        } catch (Exception e) {
            throw new RuntimeException("failed to deploy authMsg", e);
        }

        // 3. get tx receipt
        TransactionReceipt transactionReceipt = authMsg.getDeployReceipt();

        // 4. check whether the deployment is successful
        if (!ObjectUtil.isNull(transactionReceipt) && transactionReceipt.getStatus() == 0) {
            AuthMessageContract authMessageContract = new AuthMessageContract();
            authMessageContract.setContractAddress(authMsg.getContractAddress());
            authMessageContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
            bbcContext.setAuthMessageContract(authMessageContract);
            getBBCLogger().info("setup am contract successful: {}", authMsg.getContractAddress());
        } else {
            throw new RuntimeException("failed to get deploy authMsg tx receipt");
        }
    }

    @Override
    public void setupSDPMessageContract() {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)){
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNotNull(this.bbcContext.getSdpContract())
                && StrUtil.isNotEmpty(this.bbcContext.getSdpContract().getContractAddress())) {
            // If the contract has been pre-deployed and the contract address is configured in the configuration file,
            // there is no need to redeploy.
            return;
        }

        // 2. deploy contract
        SDPMsg sdpMsg;
        try {
            sdpMsg = SDPMsg.deploy(client,keyPair);
        } catch (Exception e) {
            throw new RuntimeException("failed to deploy sdpMsg", e);
        }

        // 3. get tx receipt
        TransactionReceipt transactionReceipt = sdpMsg.getDeployReceipt();

        // 4. check whether the deployment is successful
        if (!ObjectUtil.isNull(transactionReceipt) && transactionReceipt.getStatus() == 0) {
            SDPContract sdpContract = new SDPContract();
            sdpContract.setContractAddress(sdpMsg.getContractAddress());
            sdpContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
            bbcContext.setSdpContract(sdpContract);
            getBBCLogger().info("setup sdp contract successful: {}", sdpMsg.getContractAddress());
        } else {
            throw new RuntimeException("failed to get deploy sdpMsg tx receipt");
        }
    }

    @Override
    public void setAmContract(String contractAddress) {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getSdpContract())){
            throw new RuntimeException("empty sdp contract in bbc context");
        }

        // 2. load sdp contract
        SDPMsg sdp = SDPMsg.load(
                this.bbcContext.getSdpContract().getContractAddress(),
                this.client,
                this.keyPair
        );

        // 3. set am to sdp
        try {
            TransactionReceipt receipt = sdp.setAmContract(contractAddress);
            if(receipt.getStatus() == 0){
                getBBCLogger().info(
                        "set am contract (address: {}) to SDP {} by tx {}",
                        contractAddress,
                        this.bbcContext.getSdpContract().getContractAddress(),
                        receipt.getTransactionHash()
                );
            }else{
                getBBCLogger().info(
                        "set am contract failed, receipt status code: {}",
                        receipt.getStatus()
                );
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to set am contract (address: %s) to SDP %s",
                            contractAddress,
                            this.bbcContext.getSdpContract().getContractAddress()
                    ), e
            );
        }

        // 4. update sdp contract status
        try {
            if (!StrUtil.isEmpty(sdp.getAmAddress()) && !isByteArrayZero(sdp.getLocalDomain())){
                this.bbcContext.getSdpContract().setStatus(ContractStatusEnum.CONTRACT_READY);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to update sdp contract status (address: %s)",
                            this.bbcContext.getSdpContract().getContractAddress()
                    ), e);
        }
    }

    private boolean isByteArrayZero(byte[] bytes) {
        for (byte b : bytes) {
            if (b != 0x00) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setLocalDomain(String domain) {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (StrUtil.isEmpty(this.bbcContext.getSdpContract().getContractAddress())) {
            throw new RuntimeException("none sdp contract address");
        }

        // 2. load sdp contract
        SDPMsg sdp = SDPMsg.load(
                this.bbcContext.getSdpContract().getContractAddress(),
                this.client,
                this.keyPair
        );

        // 3. set domain to sdp
        try {
            TransactionReceipt receipt = sdp.setLocalDomain(domain);
            if(receipt.getStatus() == 0){
                getBBCLogger().info(
                        "set domain ({}) to SDP {} by tx {}",
                        domain,
                        this.bbcContext.getSdpContract().getContractAddress(),
                        receipt.getTransactionHash()
                );
            } else{
                getBBCLogger().info(
                        "set domain failed, receipt status code: {}",
                        receipt.getStatus()
                );
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to set domain (%s) to SDP %s",
                            domain,
                            this.bbcContext.getSdpContract().getContractAddress()
                    ), e
            );
        }

        // 4. update sdp contract status
        try {
            if (!StrUtil.isEmpty(sdp.getAmAddress()) && !ObjectUtil.isEmpty(sdp.getLocalDomain())){
                this.bbcContext.getSdpContract().setStatus(ContractStatusEnum.CONTRACT_READY);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to update sdp contract status (address: %s)",
                            this.bbcContext.getSdpContract().getContractAddress()
                    ), e);
        }
    }
}