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
import com.alipay.antchain.bridge.plugins.ethereum.abi.ProxyAdmin;
import com.alipay.antchain.bridge.plugins.ethereum.abi.SDPMsg;
import com.alipay.antchain.bridge.plugins.ethereum.abi.TransparentUpgradeableProxy;
import com.alipay.antchain.bridge.plugins.ethereum.helper.*;
import com.alipay.antchain.bridge.plugins.ethereum.helper.model.GasPriceProviderConfig;
import com.alipay.antchain.bridge.plugins.ethereum.kms.service.TxKMSSignService;
import com.alipay.antchain.bridge.plugins.lib.BBCService;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import com.aliyun.kms20160120.Client;
import lombok.Getter;
import lombok.SneakyThrows;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import static com.alipay.antchain.bridge.plugins.ethereum.abi.AuthMsg.SENDAUTHMESSAGE_EVENT;

@BBCService(products = "simple-ethereum", pluginId = "plugin-simple-ethereum")
@Getter
public class EthereumBBCService extends AbstractBBCService {

    private static final String SDP_NONCE_HAS_BEEN_PROCESSED_REVERT_REASON = "SDPMsg: nonce has been processed";

    private EthereumConfig config;

    private Web3j web3j;

    private Credentials credentials;

    private AbstractBBCContext bbcContext;

    private RawTransactionManager rawTransactionManager;

    private IGasPriceProvider contractGasProvider;

    private TxKMSSignService txKMSSignService;

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

        if (!config.isKmsService() && StrUtil.isEmpty(config.getPrivateKey())) {
            throw new RuntimeException("private key is empty");
        }

        if (StrUtil.isEmpty(config.getUrl())) {
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

        // 3. Connect to the specified gas price supplier
        try {
            GasPriceProviderConfig gasPriceProviderConfig = new GasPriceProviderConfig();
            gasPriceProviderConfig.setGasPriceProviderSupplier(config.getGasPriceProviderSupplier());
            gasPriceProviderConfig.setGasProviderUrl(config.getGasProviderUrl());
            gasPriceProviderConfig.setApiKey(config.getGasProviderApiKey());
            gasPriceProviderConfig.setGasUpdateInterval(config.getGasUpdateInterval());
            this.contractGasProvider = createGasPriceProvider(gasPriceProviderConfig);
        } catch (Exception e) {
            throw new RuntimeException(String.format("failed to create gas price provider"), e);
        }

        // 4. Connect to the specified wallet account
        if (!config.isKmsService()) {
            this.credentials = Credentials.create(config.getPrivateKey());
        }

        // 5. Create tx manager with web3j and credentials or kmsService
        try {
            this.rawTransactionManager = createTransactionManager(chainId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 6. set context
        this.bbcContext = abstractBBCContext;

        // 7. set the pre-deployed contracts into context
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

        if (ObjectUtil.isEmpty(this.config.getProxyAdmin()) && this.config.isUpgradableContracts()) {
            getBBCLogger().info("deploy proxy admin contract now!");
            try {
                ProxyAdmin proxyAdmin = ProxyAdmin.deploy(
                        web3j,
                        rawTransactionManager,
                        new AcbGasProvider(
                                this.contractGasProvider,
                                createDeployGasLimitProvider(ProxyAdmin.BINARY)
                        )
                ).send();
                this.config.setProxyAdmin(proxyAdmin.getContractAddress());
                this.bbcContext.setConfForBlockchainClient(this.config.toJsonString().getBytes());
                getBBCLogger().info("deploy proxy admin contract success! address: {}", proxyAdmin.getContractAddress());
            } catch (Exception e) {
                throw new RuntimeException("failed to deploy proxy admin", e);
            }
        }
    }

    @Override
    public void shutdown() {
        getBBCLogger().info("shut down ETH BBCService!");
        this.web3j.shutdown();
    }

    @Override
    public AbstractBBCContext getContext() {
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }

        getBBCLogger().debug("ETH BBCService context (amAddr: {}, amStatus: {}, sdpAddr: {}, sdpStatus: {})",
                this.bbcContext.getAuthMessageContract() != null ? this.bbcContext.getAuthMessageContract().getContractAddress() : "",
                this.bbcContext.getAuthMessageContract() != null ? this.bbcContext.getAuthMessageContract().getStatus() : "",
                this.bbcContext.getSdpContract() != null ? this.bbcContext.getSdpContract().getContractAddress() : "",
                this.bbcContext.getSdpContract() != null ? this.bbcContext.getSdpContract().getStatus() : ""
        );

        this.bbcContext.setConfForBlockchainClient(this.config.toJsonString().getBytes());
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

        BigInteger currHeight = queryLatestBlockHeight();
        if (transactionReceipt.getBlockNumber().compareTo(currHeight) > 0) {
            crossChainMessageReceipt.setConfirmed(false);
            crossChainMessageReceipt.setSuccessful(true);
            crossChainMessageReceipt.setTxhash(transactionReceipt.getTransactionHash());
            crossChainMessageReceipt.setErrorMsg("");
            return crossChainMessageReceipt;
        }

        List<SDPMsg.ReceiveMessageEventResponse> receiveMessageEventResponses = SDPMsg.getReceiveMessageEvents(transactionReceipt);
        if (ObjectUtil.isNotEmpty(receiveMessageEventResponses)) {
            SDPMsg.ReceiveMessageEventResponse response = receiveMessageEventResponses.get(0);
            crossChainMessageReceipt.setConfirmed(true);
            crossChainMessageReceipt.setSuccessful(transactionReceipt.isStatusOK() && response.result);
            crossChainMessageReceipt.setTxhash(transactionReceipt.getTransactionHash());
            crossChainMessageReceipt.setErrorMsg(
                    transactionReceipt.isStatusOK() ? StrUtil.format(
                            "SDP calls biz contract: {}", response.result ? "SUCCESS" : response.errMsg
                    ) : StrUtil.emptyToDefault(transactionReceipt.getRevertReason(), "")
            );
            getBBCLogger().info(
                    "event receiveMessage from SDP contract is found in no.{} tx {} of block {} : " +
                            "( send_domain: {}, sender: {}, receiver: {}, biz_call: {}, err_msg: {} )",
                    transactionReceipt.getTransactionIndex().toString(), transactionReceipt.getTransactionHash(), transactionReceipt.getBlockHash(),
                    response.senderDomain, HexUtil.encodeHexStr(response.senderID), response.receiverID, response.result.toString(),
                    response.errMsg
            );
            return crossChainMessageReceipt;
        }

        crossChainMessageReceipt.setConfirmed(true);
        crossChainMessageReceipt.setSuccessful(transactionReceipt.isStatusOK());
        crossChainMessageReceipt.setTxhash(transactionReceipt.getTransactionHash());
        crossChainMessageReceipt.setErrorMsg(StrUtil.emptyToDefault(transactionReceipt.getRevertReason(), ""));

        return crossChainMessageReceipt;
    }

    @Override
    public List<CrossChainMessage> readCrossChainMessagesByHeight(long height) {
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }

        if (ObjectUtil.isNull(this.bbcContext.getAuthMessageContract())) {
            throw new RuntimeException("empty am contract in bbc context");
        }

        try {
            switch (this.config.getMsgScanPolicy()) {
                case BLOCK_SCAN:
                    return readMessagesFromEntireBlock(height);
                case LOG_FILTER:
                default:
                    return readMessagesByFilter(height);
            }
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

    @SneakyThrows
    private List<CrossChainMessage> readMessagesFromEntireBlock(long height) {
        EthBlock.Block block = web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(height), true).send().getBlock();

        List<CrossChainMessage> messageList = ListUtil.toList();
        for (EthBlock.TransactionResult transactionResult : block.getTransactions()) {
            Transaction transaction = (Transaction) transactionResult.get();
            TransactionReceipt receipt = web3j.ethGetTransactionReceipt(transaction.getHash())
                    .send()
                    .getTransactionReceipt()
                    .orElseThrow(() -> new RuntimeException("failed to get receipt for tx " + transaction.getHash()));
            if (ObjectUtil.isNull(receipt)) {
                throw new RuntimeException("empty receipt for tx " + transaction.getHash());
            }
            messageList.addAll(AuthMsg.getSendAuthMessageEvents(receipt).stream()
                    .filter(x -> StrUtil.equals(x.log.getAddress(), this.bbcContext.getAuthMessageContract().getContractAddress()))
                    .map(
                            response -> CrossChainMessage.createCrossChainMessage(
                                    CrossChainMessage.CrossChainMessageType.AUTH_MSG,
                                    height,
                                    block.getTimestamp().longValue() * 1000,
                                    HexUtil.decodeHex(StrUtil.removePrefix(block.getHash().trim(), "0x")),
                                    response.pkg,
                                    // todo: put ledger data, for SPV or other attestations
                                    // this time we need no verify. it's ok to set it with empty bytes
                                    "".getBytes(),
                                    // todo: put proof data
                                    // this time we need no proof data. it's ok to set it with empty bytes
                                    "".getBytes(),
                                    HexUtil.decodeHex(transaction.getHash().replaceFirst("^0x", ""))
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
    }

    @SneakyThrows
    private List<CrossChainMessage> readMessagesByFilter(long height) {
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
                            block.getTimestamp().longValue() * 1000,
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
    }

    @Override
    public Long queryLatestHeight() {
        Long l = queryLatestBlockHeight().longValue();
        getBBCLogger().debug("latest height: {}", l);
        return l;
    }

    private BigInteger queryLatestBlockHeight() {
        BigInteger l;
        try {
            l = web3j.ethGetBlockByNumber(config.getBlockHeightPolicy().getDefaultBlockParameterName(), false)
                    .send()
                    .getBlock()
                    .getNumber();
        } catch (IOException e) {
            throw new RuntimeException("failed to query latest height", e);
        }
        return l;
    }

    @Override
    public void setupAuthMessageContract() {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (this.config.isUpgradableContracts() && StrUtil.isEmpty(this.config.getProxyAdmin())) {
            throw new RuntimeException("empty proxy admin");
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
                    new AcbGasProvider(
                            this.contractGasProvider,
                            createDeployGasLimitProvider(AuthMsg.BINARY)
                    )
            ).send();
        } catch (Exception e) {
            throw new RuntimeException("failed to deploy authMsg", e);
        }

        String amContractAddr = authMsg.getContractAddress();
        if (this.config.isUpgradableContracts()) {
            TransparentUpgradeableProxy proxy;
            try {
                proxy = TransparentUpgradeableProxy.deploy(
                        web3j,
                        rawTransactionManager,
                        new AcbGasProvider(
                                this.contractGasProvider,
                                createDeployGasLimitProvider(
                                        TransparentUpgradeableProxy.BINARY +
                                                FunctionEncoder.encodeConstructor(ListUtil.toList(
                                                        new Address(authMsg.getContractAddress()),
                                                        new Address(this.config.getProxyAdmin()),
                                                        new DynamicBytes(HexUtil.decodeHex("e1c7392a"))
                                                ))
                                )
                        ),
                        BigInteger.ZERO,
                        authMsg.getContractAddress(),
                        this.config.getProxyAdmin(),
                        HexUtil.decodeHex("e1c7392a")
                ).send();
            } catch (Exception e) {
                throw new RuntimeException("failed to deploy authMsg", e);
            }
            amContractAddr = proxy.getContractAddress();
            getBBCLogger().info("deploy proxy contract for am: {}", proxy.getContractAddress());
        }

        AuthMessageContract authMessageContract = new AuthMessageContract();
        authMessageContract.setContractAddress(amContractAddr);
        authMessageContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
        bbcContext.setAuthMessageContract(authMessageContract);

        getBBCLogger().info("setup am contract successful: {}", amContractAddr);
    }

    @Override
    public void setupSDPMessageContract() {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (this.config.isUpgradableContracts() && StrUtil.isEmpty(this.config.getProxyAdmin())) {
            throw new RuntimeException("empty proxy admin");
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
                    new AcbGasProvider(
                            this.contractGasProvider,
                            createDeployGasLimitProvider(SDPMsg.BINARY)
                    )
            ).send();
        } catch (Exception e) {
            throw new RuntimeException("failed to deploy sdpMsg", e);
        }

        String sdpContractAddr = sdpMsg.getContractAddress();
        if (this.config.isUpgradableContracts()) {
            TransparentUpgradeableProxy proxy;
            try {
                proxy = TransparentUpgradeableProxy.deploy(
                        web3j,
                        rawTransactionManager,
                        new AcbGasProvider(
                                this.contractGasProvider,
                                createDeployGasLimitProvider(
                                        TransparentUpgradeableProxy.BINARY +
                                                FunctionEncoder.encodeConstructor(ListUtil.toList(
                                                        new Address(sdpMsg.getContractAddress()),
                                                        new Address(this.config.getProxyAdmin()),
                                                        new DynamicBytes(HexUtil.decodeHex("e1c7392a"))
                                                ))
                                )
                        ),
                        BigInteger.ZERO,
                        sdpMsg.getContractAddress(),
                        this.config.getProxyAdmin(),
                        HexUtil.decodeHex("e1c7392a")
                ).send();
            } catch (Exception e) {
                throw new RuntimeException("failed to deploy sdp contract", e);
            }
            sdpContractAddr = proxy.getContractAddress();
            getBBCLogger().info("deploy proxy contract for sdp: {}", proxy.getContractAddress());
        }

        SDPContract sdpContract = new SDPContract();
        sdpContract.setContractAddress(sdpContractAddr);
        sdpContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
        bbcContext.setSdpContract(sdpContract);
        getBBCLogger().info("setup sdp contract successful: {}", sdpContractAddr);
    }

    @Override
    public long querySDPMessageSeq(String senderDomain, String senderID, String receiverDomain, String receiverID) {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getSdpContract())) {
            throw new RuntimeException("empty sdp contract in bbc context");
        }

        // 2. load sdpMsg
        SDPMsg sdpMsg = SDPMsg.load(
                bbcContext.getSdpContract().getContractAddress(),
                web3j,
                rawTransactionManager,
                new DefaultGasProvider()
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
        if (ObjectUtil.isNull(this.bbcContext.getAuthMessageContract())) {
            throw new RuntimeException("empty am contract in bbc context");
        }

        // 2. load am contract
        AuthMsg am = AuthMsg.load(
                this.bbcContext.getAuthMessageContract().getContractAddress(),
                this.web3j,
                this.rawTransactionManager,
                new AcbGasProvider(
                        this.contractGasProvider,
                        createEthCallGasLimitProvider(
                                this.bbcContext.getAuthMessageContract().getContractAddress(),
                                new Function(
                                        AuthMsg.FUNC_SETPROTOCOL,
                                        ListUtil.toList(new Address(protocolAddress), new Uint32(Long.parseLong(protocolType))), // inputs
                                        Collections.emptyList()
                                )
                        )
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
            if (!StrUtil.isEmpty(am.getProtocol(BigInteger.ZERO).send())) {
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
        if (ObjectUtil.isNull(this.bbcContext.getSdpContract())) {
            throw new RuntimeException("empty sdp contract in bbc context");
        }

        // 2. load sdp contract
        SDPMsg sdp = SDPMsg.load(
                this.bbcContext.getSdpContract().getContractAddress(),
                this.web3j,
                this.rawTransactionManager,
                new AcbGasProvider(
                        this.contractGasProvider,
                        createEthCallGasLimitProvider(
                                this.bbcContext.getSdpContract().getContractAddress(),
                                new Function(
                                        SDPMsg.FUNC_SETAMCONTRACT,
                                        ListUtil.toList(new Address(contractAddress)), // inputs
                                        Collections.emptyList()
                                )
                        )
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
            if (!StrUtil.isEmpty(sdp.getAmAddress().send()) && !isByteArrayZero(sdp.getLocalDomain().send())) {
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

    private RawTransactionManager createTransactionManager(BigInteger chainId) throws Exception {
        if (config.isKmsService()) {
            com.aliyun.teaopenapi.models.Config kmsConfig = new com.aliyun.teaopenapi.models.Config()
                    .setAccessKeyId(config.getKmsAccessKeyId())
                    .setAccessKeySecret(config.getKmsAccessKeySecret())
                    .setEndpoint(config.getKmsEndpoint());
            Client kmsClient = new Client(kmsConfig);
            txKMSSignService = new TxKMSSignService(kmsClient, config.getKmsPrivateKeyId());

            return config.getEthNoncePolicy() == EthNoncePolicyEnum.FAST ?
                    new AcbFastRawTransactionManager(this.web3j, txKMSSignService, chainId.longValue())
                    : new AcbRawTransactionManager(this.web3j, txKMSSignService, chainId.longValue());
        } else {
            return config.getEthNoncePolicy() == EthNoncePolicyEnum.FAST ?
                    new AcbFastRawTransactionManager(this.web3j, this.credentials, chainId.longValue())
                    : new AcbRawTransactionManager(this.web3j, this.credentials, chainId.longValue());
        }
    }

    private IGasLimitProvider createEthCallGasLimitProvider(String toAddr, Function function) {
        switch (config.getGasLimitPolicy()) {
            case ESTIMATE:
                if (config.isKmsService()) {
                    return new EstimateGasLimitProvider(web3j, txKMSSignService.getAddress(), toAddr, FunctionEncoder.encode(function), config.getExtraGasLimit());
                }
                return new EstimateGasLimitProvider(web3j, credentials.getAddress(), toAddr, FunctionEncoder.encode(function), config.getExtraGasLimit());
            case STATIC:
            default:
                return new StaticGasLimitProvider(BigInteger.valueOf(config.getGasLimit()));
        }
    }

    private IGasLimitProvider createDeployGasLimitProvider(String data) {
        switch (config.getGasLimitPolicy()) {
            case ESTIMATE:
                if (config.isKmsService()) {
                    return new EstimateGasLimitProvider(web3j, txKMSSignService.getAddress(), null, data, config.getExtraGasLimit());
                }
                return new EstimateGasLimitProvider(web3j, credentials.getAddress(), null, data, config.getExtraGasLimit());
            case STATIC:
            default:
                return new StaticGasLimitProvider(BigInteger.valueOf(config.getGasLimit()));
        }
    }

    private IGasPriceProvider createGasPriceProvider(GasPriceProviderConfig gasPriceProviderConfig) {
        switch (config.getGasPricePolicy()) {
            case FROM_API:
                return GasPriceProvider.create(this.web3j, gasPriceProviderConfig, getBBCLogger());
            case STATIC:
            default:
                return new StaticGasPriceProvider(BigInteger.valueOf(config.getGasPrice()));
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
                new AcbGasProvider(
                        this.contractGasProvider,
                        createEthCallGasLimitProvider(
                                this.bbcContext.getSdpContract().getContractAddress(),
                                new Function(
                                        SDPMsg.FUNC_SETLOCALDOMAIN,
                                        ListUtil.toList(new Utf8String(domain)),
                                        Collections.emptyList()
                                )
                        )
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
            if (!StrUtil.isEmpty(sdp.getAmAddress().send()) && !ObjectUtil.isEmpty(sdp.getLocalDomain().send())) {
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
        if (ObjectUtil.isNull(this.bbcContext.getAuthMessageContract())) {
            throw new RuntimeException("empty am contract in bbc context");
        }

        getBBCLogger().debug("relay AM {} to {} ",
                HexUtil.encodeHexStr(rawMessage), this.bbcContext.getAuthMessageContract().getContractAddress());

        // 2. create Transaction
        try {
            // 2.1 create function
            Function function = new Function(
                    AuthMsg.FUNC_RECVPKGFROMRELAYER, // function name
                    Collections.singletonList(new DynamicBytes(rawMessage)), // inputs
                    Collections.emptyList() // outputs
            );
            String encodedFunc = FunctionEncoder.encode(function);

            // 2.2 pre-execute before commit tx
            EthCall call = this.web3j.ethCall(
                    org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                            config.isKmsService() ? this.txKMSSignService.getAddress() : this.credentials.getAddress(),
                            this.bbcContext.getAuthMessageContract().getContractAddress(),
                            encodedFunc
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();

            // 2.3 set `confirmed` and `successful` to false if reverted
            CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();
            if (call.isReverted()) {
                crossChainMessageReceipt.setSuccessful(false);
                crossChainMessageReceipt.setConfirmed(StrUtil.contains(call.getRevertReason(), SDP_NONCE_HAS_BEEN_PROCESSED_REVERT_REASON));
                crossChainMessageReceipt.setErrorMsg(call.getRevertReason());
                return crossChainMessageReceipt;
            }

            // 2.4 async send tx
            EthSendTransaction ethSendTransaction = rawTransactionManager.sendTransaction(
                    this.contractGasProvider.getGasPrice(encodedFunc),
                    createEthCallGasLimitProvider(this.bbcContext.getAuthMessageContract().getContractAddress(), function).getGasLimit(encodedFunc),
                    this.bbcContext.getAuthMessageContract().getContractAddress(),
                    encodedFunc,
                    BigInteger.ZERO
            );
            if (ObjectUtil.isNull(ethSendTransaction)) {
                throw new RuntimeException("send tx with null result");
            }
            if (ethSendTransaction.hasError()) {
                throw new RuntimeException(StrUtil.format("tx error: {} - {}",
                        ethSendTransaction.getError().getCode(), ethSendTransaction.getError().getMessage()));
            }
            if (StrUtil.isEmpty(ethSendTransaction.getTransactionHash())) {
                throw new RuntimeException("tx hash is empty");
            }

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
