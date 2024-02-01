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

package com.alipay.antchain.bridge.plugins.ethereum;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
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
import com.alipay.antchain.bridge.plugins.ethereum.abi.AuthMsg;
import com.alipay.antchain.bridge.plugins.ethereum.abi.SDPMsg;
import com.alipay.antchain.bridge.plugins.lib.BBCService;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import lombok.Getter;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.StaticGasProvider;

import static com.alipay.antchain.bridge.plugins.ethereum.abi.AuthMsg.SENDAUTHMESSAGE_EVENT;

@BBCService(products = "simple-ethereum", pluginId = "plugin-simple-ethereum")
@Getter
public class EthereumBBCService extends AbstractBBCService {

    private EthereumConfig config;

    private Web3j web3j;

    private Credentials credentials;

    private AbstractBBCContext bbcContext;

    private RawTransactionManager rawTransactionManager;

    @Override
    public void startup(AbstractBBCContext abstractBBCContext) {
        getBBCLogger().info("ETH BBCService startup with context: {}", new String(abstractBBCContext.getConfForBlockchainClient()));

        if (ObjectUtil.isNull(abstractBBCContext)) {
            throw new RuntimeException("null bbc context");
        }
        if (ObjectUtil.isEmpty(abstractBBCContext.getConfForBlockchainClient())) {
            throw new RuntimeException("empty blockchain client conf");
        }

        // 1. Obtain the configuration information
        try {
            config = EthereumConfig.fromJsonString(new String(abstractBBCContext.getConfForBlockchainClient()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(StrUtil.isEmpty(config.getPrivateKey())){
            throw new RuntimeException("private key is empty");
        }

        if(StrUtil.isEmpty(config.getUrl())){
            throw new RuntimeException("ethereum url is empty");
        }

        // 2. Connect to the Ethereum network
        BigInteger chainId;
        try {
            web3j = Web3j.build(new HttpService(config.getUrl()));
            chainId = web3j.ethChainId().send().getChainId();
        } catch (Exception e) {
            throw new RuntimeException(String.format("failed to connect ethereum (url: %s)", config.getUrl()), e);
        }

        // 3. Connect to the specified wallet account
        this.credentials = Credentials.create(config.getPrivateKey());

        // 4. Create tx manager with web3j and credentials
        this.rawTransactionManager = new RawTransactionManager(this.web3j, this.credentials, chainId.longValue());

        // 5. set context
        this.bbcContext = abstractBBCContext;

        // 6. set the pre-deployed contracts into context
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
        getBBCLogger().info("shut down ETH BBCService!");
        this.web3j.shutdown();
    }

    @Override
    public AbstractBBCContext getContext() {
        if (ObjectUtil.isNull(this.bbcContext)){
            throw new RuntimeException("empty bbc context");
        }

        getBBCLogger().debug("ETH BBCService context (amAddr: {}, amStatus: {}, sdpAddr: {}, sdpStatus: {})",
                this.bbcContext.getAuthMessageContract() != null ? this.bbcContext.getAuthMessageContract().getContractAddress() : "",
                this.bbcContext.getAuthMessageContract() != null ? this.bbcContext.getAuthMessageContract().getStatus() : "",
                this.bbcContext.getSdpContract() != null ? this.bbcContext.getSdpContract().getContractAddress() : "",
                this.bbcContext.getSdpContract() != null ? this.bbcContext.getSdpContract().getStatus() : ""
        );

        return this.bbcContext;
    }

    @Override
    public CrossChainMessageReceipt readCrossChainMessageReceipt(String txHash) {
        // 1. Obtain Ethereum receipt according to transaction hash
        TransactionReceipt transactionReceipt;

        try {
            transactionReceipt = web3j.ethGetTransactionReceipt(txHash)
                    .send().getTransactionReceipt().orElse(null);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format(
                            "failed to read cross chain message receipt (txHash: %s)", txHash
                    ), e
            );
        }

        // 2. Construct cross-chain message receipt
        CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();
        if (transactionReceipt == null){
            // If the transaction is not packaged, the return receipt is empty
            crossChainMessageReceipt.setConfirmed(false);
            crossChainMessageReceipt.setSuccessful(false);
            crossChainMessageReceipt.setTxhash("");
            crossChainMessageReceipt.setErrorMsg("");
        } else {
            crossChainMessageReceipt.setConfirmed(true);
            crossChainMessageReceipt.setSuccessful(transactionReceipt.isStatusOK());
            crossChainMessageReceipt.setTxhash(transactionReceipt.getTransactionHash());
            crossChainMessageReceipt.setErrorMsg(StrUtil.emptyToDefault(transactionReceipt.getRevertReason(), ""));
        }

        getBBCLogger().info("cross chain message receipt for txhash {} : {}", txHash, JSON.toJSONString(crossChainMessageReceipt));
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

        try {
            // 1. get eth logs
            List<EthLog.LogResult> logs = web3j.ethGetLogs(
                    new EthFilter(
                            new DefaultBlockParameterNumber(BigInteger.valueOf(height)),
                            new DefaultBlockParameterNumber(BigInteger.valueOf(height)),
                            this.bbcContext.getAuthMessageContract().getContractAddress()
                    ).addSingleTopic(EventEncoder.encode(SENDAUTHMESSAGE_EVENT))
            ).send().getLogs();

            // 2. get block
            EthBlock.Block block = web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(height), false).send().getBlock();

            // 3. get crosschain msgs
            List<CrossChainMessage> messageList = ListUtil.toList();
            for (EthLog.LogResult logResult : logs) {
                // 3.1 get log obj
                EthLog.LogObject logObject = (EthLog.LogObject) logResult.get();

                // 3.2 get receipt
                TransactionReceipt transactionReceipt;
                transactionReceipt = web3j.ethGetTransactionReceipt(logObject.getTransactionHash()).send().getResult();

                // 3.3 create crosschain msg
                messageList.addAll(AuthMsg.getSendAuthMessageEvents(transactionReceipt).stream().map(
                        response -> CrossChainMessage.createCrossChainMessage(
                                CrossChainMessage.CrossChainMessageType.AUTH_MSG,
                                logObject.getBlockNumber().longValue(),
                                block.getTimestamp().longValue(),
                                HexUtil.decodeHex(StrUtil.removePrefix(logObject.getBlockHash().trim(), "0x")),
                                response.pkg,
                                // todo: put ledger data, for SPV or other attestations
                                // this time we need no verify. it's ok to set it with empty bytes
                                "".getBytes(),
                                // todo: put proof data
                                // this time we need no proof data. it's ok to set it with empty bytes
                                "".getBytes(),
                                HexUtil.decodeHex(logObject.getTransactionHash().replaceFirst("^0x", ""))
                        )
                ).collect(Collectors.toList()));
            }

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
            l = web3j.ethBlockNumber().send().getBlockNumber().longValue();
        } catch (IOException e) {
            throw new RuntimeException("failed to query latest height", e);
        }

        getBBCLogger().info("latest height: {}", l);
        return l;
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
            authMsg = AuthMsg.deploy(
                    web3j,
                    rawTransactionManager,
                    new StaticGasProvider(
                            BigInteger.valueOf(this.config.getGasPrice()),
                            BigInteger.valueOf(this.config.getGasLimit())
                    )
            ).send();
        } catch (Exception e) {
            throw new RuntimeException("failed to deploy authMsg", e);
        }

        // 3. get tx receipt
        TransactionReceipt transactionReceipt = authMsg.getTransactionReceipt().orElse(null);

        // 4. check whether the deployment is successful
        if (!ObjectUtil.isNull(transactionReceipt) && transactionReceipt.getStatus().equals("0x1")) {
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
            sdpMsg = SDPMsg.deploy(
                    web3j,
                    rawTransactionManager,
                    new StaticGasProvider(
                            BigInteger.valueOf(this.config.getGasPrice()),
                            BigInteger.valueOf(this.config.getGasLimit())
                    )
            ).send();
        } catch (Exception e) {
            throw new RuntimeException("failed to deploy sdpMsg", e);
        }

        // 3. get tx receipt
        TransactionReceipt transactionReceipt = sdpMsg.getTransactionReceipt().orElse(null);

        // 4. check whether the deployment is successful
        if (!ObjectUtil.isNull(transactionReceipt) && transactionReceipt.getStatus().equals("0x1")) {
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
                web3j,
                rawTransactionManager,
                new StaticGasProvider(
                        BigInteger.valueOf(this.config.getGasPrice()),
                        BigInteger.valueOf(this.config.getGasLimit())
                )
        );

        // 3. query sequence
        long seq;
        try {
            seq = sdpMsg.querySDPMessageSeq(
                    senderDomain,
                    HexUtil.decodeHex(senderID),
                    receiverDomain,
                    HexUtil.decodeHex(receiverID)
            ).send().longValue();

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
                this.web3j,
                this.rawTransactionManager,
                new StaticGasProvider(
                        BigInteger.valueOf(this.config.getGasPrice()),
                        BigInteger.valueOf(this.config.getGasLimit())
                )
        );

        // 3. set protocol to am
        try {
            TransactionReceipt receipt = am.setProtocol(protocolAddress, BigInteger.valueOf(Long.parseLong(protocolType))).send();
            getBBCLogger().info(
                    "set protocol (address: {}, type: {}) to AM {} by tx {} ",
                    protocolAddress, protocolType,
                    this.bbcContext.getAuthMessageContract().getContractAddress(),
                    receipt.getTransactionHash()
            );
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
            if (!StrUtil.isEmpty(am.getProtocol(BigInteger.ZERO).send())){
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
                this.web3j,
                this.rawTransactionManager,
                new StaticGasProvider(
                        BigInteger.valueOf(this.config.getGasPrice()),
                        BigInteger.valueOf(this.config.getGasLimit())
                )
        );

        // 3. set am to sdp
        try {
            TransactionReceipt receipt = sdp.setAmContract(contractAddress).send();
            getBBCLogger().info(
                    "set am contract (address: {}) to SDP {} by tx {}",
                    contractAddress,
                    this.bbcContext.getSdpContract().getContractAddress(),
                    receipt.getTransactionHash()
            );
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
            if (!StrUtil.isEmpty(sdp.getAmAddress().send()) && !isByteArrayZero(sdp.getLocalDomain().send())){
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
                this.web3j,
                this.rawTransactionManager,
                new StaticGasProvider(
                        BigInteger.valueOf(this.config.getGasPrice()),
                        BigInteger.valueOf(this.config.getGasLimit())
                )
        );

        // 3. set domain to sdp
        try {
            TransactionReceipt receipt = sdp.setLocalDomain(domain).send();
            getBBCLogger().info(
                    "set domain ({}) to SDP {} by tx {}",
                    domain,
                    this.bbcContext.getSdpContract().getContractAddress(),
                    receipt.getTransactionHash()
            );
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
            if (!StrUtil.isEmpty(sdp.getAmAddress().send()) && !ObjectUtil.isEmpty(sdp.getLocalDomain().send())){
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
        try {
            // 2.1 create function
            Function function = new Function(
                    AuthMsg.FUNC_RECVPKGFROMRELAYER, // funtion name
                    Collections.singletonList(new DynamicBytes(rawMessage)), // inputs
                    Collections.emptyList() // outputs
            );
            String encodedFunc = FunctionEncoder.encode(function);

            // 2.2 pre-execute before commit tx
            EthCall call = this.web3j.ethCall(
                    Transaction.createEthCallTransaction(
                            this.credentials.getAddress(),
                            this.bbcContext.getAuthMessageContract().getContractAddress(),
                            encodedFunc
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();

            // 2.3 set `confirmed` and `successful` to false if reverted
            CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();
            if (call.isReverted()) {
                crossChainMessageReceipt.setSuccessful(false);
                crossChainMessageReceipt.setConfirmed(false);
                crossChainMessageReceipt.setErrorMsg(call.getRevertReason());
                return crossChainMessageReceipt;
            }

            // 2.4 async send tx
            EthSendTransaction ethSendTransaction = rawTransactionManager.sendTransaction(
                    BigInteger.valueOf(this.config.getGasPrice()),
                    BigInteger.valueOf(this.config.getGasLimit()),
                    this.bbcContext.getAuthMessageContract().getContractAddress(),
                    encodedFunc,
                    BigInteger.ZERO
            );

            // 2.5 return crossChainMessageReceipt
            crossChainMessageReceipt.setConfirmed(false);
            crossChainMessageReceipt.setSuccessful(true);
            crossChainMessageReceipt.setTxhash(ethSendTransaction.getTransactionHash());
            crossChainMessageReceipt.setErrorMsg("");

            getBBCLogger().info("relay tx {}", ethSendTransaction.getTransactionHash());

            return crossChainMessageReceipt;
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("failed to relay AM %s to %s",
                            HexUtil.encodeHexStr(rawMessage), this.bbcContext.getAuthMessageContract().getContractAddress()
                    ), e
            );
        }
    }
}
