package com.alipay.antchain.bridge.plugins.hyperchain;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hyperchain.sdk.account.Account;
import cn.hyperchain.sdk.account.Algo;
import cn.hyperchain.sdk.common.solidity.Abi;
import cn.hyperchain.sdk.common.utils.ByteUtil;
import cn.hyperchain.sdk.common.utils.Encoder;
import cn.hyperchain.sdk.common.utils.FileUtil;
import cn.hyperchain.sdk.common.utils.FuncParams;
import cn.hyperchain.sdk.exception.RequestException;
import cn.hyperchain.sdk.exception.RequestExceptionCode;
import cn.hyperchain.sdk.provider.DefaultHttpProvider;
import cn.hyperchain.sdk.provider.ProviderManager;
import cn.hyperchain.sdk.request.Request;
import cn.hyperchain.sdk.response.EventLog;
import cn.hyperchain.sdk.response.ReceiptResponse;
import cn.hyperchain.sdk.response.TxHashResponse;
import cn.hyperchain.sdk.response.block.BlockNumberResponse;
import cn.hyperchain.sdk.response.block.BlockResponse;
import cn.hyperchain.sdk.response.tx.TxResponse;
import cn.hyperchain.sdk.service.*;
import cn.hyperchain.sdk.transaction.Transaction;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.AuthMessageContract;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.bbc.syscontract.SDPContract;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.plugins.lib.BBCService;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@BBCService(products = "hyperchain2", pluginId = "plugin-hyperchain2")
public class HyperchainBBCService extends AbstractBBCService {

    public static final String AM_ABI_PATH = "/contract/am/AuthMsg.abi";
    public static final String AM_BIN_PATH = "/contract/am/AuthMsg.bin";
    public static final String SDP_ABI_PATH = "/contract/sdp/SDPMsg.abi";
    public static final String SDP_BIN_PATH = "/contract/sdp/SDPMsg.bin";
    public static final String AM_METHOD_SETPROTOCOL = "setProtocol(address,uint32)";
    public static final String AM_METHOD_GETPROTOCOL = "getProtocol(uint32)";
    public static final String AM_METHOD_RECVPKGFROMRELAYER = "recvPkgFromRelayer(bytes)";
    public static final String SDP_METHOD_SETAMCONTRACT = "setAmContract(address)";
    public static final String SDP_METHOD_GETAMCONTRACT = "getAmAddress()";
    public static final String SDP_METHOD_SETLOCALDOMAIN = "setLocalDomain(string)";
    public static final String SDP_METHOD_GETLOCALDOMAIN = "getLocalDomain()";
    public static final String SDP_METHOD_QUERYSDPMESSAGESEQ = "querySDPMessageSeq(string,bytes32,string,bytes32)";

    public static final String AM_EVENT_SENDAUTHMESSAGE = "SendAuthMessage";
    public static final String AM_EVENT_SENDAUTHMESSAGE_ABI = "SendAuthMessage(bytes)";
    public static final String AM_EVENT_RECVAUTHMESSAGE = "recvAuthMessage";
    public static final String AM_EVENT_RECVAUTHMESSAGE_ABI = "recvAuthMessage(string,bytes)";

    public static final String SUCCESS = "success";
    public static final String EMPTY_MATCHER = "0x0*";

    private AbstractBBCContext bbcContext;

    private HyperchainConfig config;

    DefaultHttpProvider defaultHttpProvider;
    ProviderManager providerManager;
    ContractService contractService;
    AccountService accountService;
    TxService txService;
    BlockService blockService;
    MQService mqService;
    Account account;

    /**
     * 入参 bbcContext 可能是一个已经存在的 bbcService 的 bbcContext，即 HyperchainBBCContext，携带一定的插件服务信息，
     * 也可能是一个新建的 DefaultContext，携带信息存储在 confForBlockchainClient 中
     *
     * @param bbcContext the bbcContext object.
     *                   please check the comments of interface {@link AbstractBBCContext}.
     */
    @Override
    public void startup(AbstractBBCContext bbcContext) {
        getBBCLogger().info("[HyperchainBBCService.startup] start up hyperchain2.0 bbc service, bbcContext: {}",
                JSON.toJSONString(bbcContext));

        if (ObjectUtil.isNull(bbcContext)) {
            throw new RuntimeException("[HyperchainBBCService.startup] null bbc bbcContext");
        }

        try {
            // 1. obtain the configuration information
            config = HyperchainConfig.fromJsonString(new String(bbcContext.getConfForBlockchainClient()));

            // 2. build provider manager
            defaultHttpProvider = new DefaultHttpProvider.Builder().setUrl(config.getUrl()).build();
            providerManager = ProviderManager.createManager(defaultHttpProvider);

            // 3. build service
            contractService = ServiceManager.getContractService(providerManager);
            accountService = ServiceManager.getAccountService(providerManager);
            txService = ServiceManager.getTxService(providerManager);
            blockService = ServiceManager.getBlockService(providerManager);
            mqService = ServiceManager.getMQService(providerManager);

            // 4. create account
            if (StrUtil.isNotEmpty(config.getAccountJson()) && config.getPassword() != null) {
                account = Account.fromAccountJson(config.getAccountJson(), config.getPassword());
            } else {
                account = accountService.genAccount(Algo.SMRAW);
            }
            getBBCLogger().info("[HyperchainBBCService.startup] account addr: {}",
                    account.getAddress());

            // 5. set context
            this.bbcContext = bbcContext;

            // 6. set the pre-deployed contracts into context
            if (ObjectUtil.isNull(bbcContext.getAuthMessageContract())
                    && StrUtil.isNotEmpty(this.config.getAmContractAddressDeployed())) {
                AuthMessageContract authMessageContract = new AuthMessageContract();
                authMessageContract.setContractAddress(this.config.getAmContractAddressDeployed());
                authMessageContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
                this.bbcContext.setAuthMessageContract(authMessageContract);
            }

            if (ObjectUtil.isNull(bbcContext.getSdpContract())
                    && StrUtil.isNotEmpty(this.config.getSdpContractAddressDeployed())) {
                SDPContract sdpContract = new SDPContract();
                sdpContract.setContractAddress(this.config.getSdpContractAddressDeployed());
                sdpContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
                this.bbcContext.setSdpContract(sdpContract);
            }

        } catch (Exception e) {
            getBBCLogger().error("[HyperchainBBCService.startup] start up hyperchain2.0 bbc service error for {}",
                    config.getUrl(),
                    e);
            throw new RuntimeException(
                    StrUtil.format("[HyperchainBBCService.startup] start up hyperchain2.0 bbc service error for {}, ",
                            config.getUrl()),
                    e);
        }
    }

    @Override
    public void shutdown() {
        getBBCLogger().info("[HyperchainBBCService.shutdown] shut down hyperchain2.0 bbc service for {}",
                config.getUrl());
    }

    @Override
    public AbstractBBCContext getContext() {
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("[HyperchainBBCService.getContext] empty bbc bbcContext!");
        }

        getBBCLogger().info(StrUtil.format("[HyperchainBBCService.getContext] Hyperchain2.0 BBCService bbcContext: \n" +
                        " amAddr: {}, amStatus: {}, sdpAddr: {}, sdpStatus: {}",
                this.bbcContext.getAuthMessageContract() != null ? this.bbcContext.getAuthMessageContract().getContractAddress() : "",
                this.bbcContext.getAuthMessageContract() != null ? this.bbcContext.getAuthMessageContract().getStatus() : "",
                this.bbcContext.getSdpContract() != null ? this.bbcContext.getSdpContract().getContractAddress() : "",
                this.bbcContext.getSdpContract() != null ? this.bbcContext.getSdpContract().getStatus() : ""
        ));

        return this.bbcContext;
    }

    @Override
    public void setupAuthMessageContract() {
        getBBCLogger().info("[HyperchainBBCService.setupAuthMessageContract] set up auth message contract for {}",
                config.getUrl());

        if (ObjectUtil.isEmpty(this.bbcContext)) {
            throw new RuntimeException("[HyperchainBBCService.setupAuthMessageContract] empty bbc bbcContext");
        }

        try {
            String contractAddr = deployContract(AM_BIN_PATH, AM_ABI_PATH);

            AuthMessageContract authMessageContract = new AuthMessageContract();
            authMessageContract.setContractAddress(contractAddr);
            authMessageContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
            bbcContext.setAuthMessageContract(authMessageContract);

            getBBCLogger().info("[HyperchainBBCService.setupAuthMessageContract] setup AuthMessageContract for {} success, contractAddr: {}",
                    config.getUrl(),
                    contractAddr);
        } catch (Exception e) {
            getBBCLogger().error("[HyperchainBBCService.setupAuthMessageContract] setup AuthMessageContract for {} failed",
                    config.getUrl(), e);
            throw new RuntimeException(StrUtil.format("[HyperchainBBCService.setupAuthMessageContract] setup AuthMessageContract for {} failed",
                    config.getUrl()), e);
        }
    }

    @Override
    public void setupSDPMessageContract() {
        getBBCLogger().info("[HyperchainBBCService.setupSDPMessageContract] set up sdp message contract for {}",
                config.getUrl());

        if (ObjectUtil.isEmpty(this.bbcContext)) {
            throw new RuntimeException("[HyperchainBBCService.setupSDPMessageContract] empty bbc bbcContext");
        }

        try {
            String contractAddr = deployContract(SDP_BIN_PATH, SDP_ABI_PATH);

            SDPContract sdpContract = new SDPContract();
            sdpContract.setContractAddress(contractAddr);
            sdpContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
            bbcContext.setSdpContract(sdpContract);

            getBBCLogger().info("[HyperchainBBCService.setupSDPMessageContract] setup SDPContract for {} success, contractAddr: {}",
                    config.getUrl(),
                    contractAddr);
        } catch (Exception e) {
            getBBCLogger().error("[HyperchainBBCService.setupSDPMessageContract] setup SDPContract for {} failed",
                    config.getUrl(), e);
            throw new RuntimeException(StrUtil.format("[HyperchainBBCService.setupSDPMessageContract] setup SDPContract for {} failed",
                    config.getUrl()), e);
        }
    }

    /**
     * 调用AM合约设置上层协议的地址和类型
     *
     * @param protocolAddress protocol contract address 实际使用的时候也是从bbc里面拿的地址
     * @param protocolType    type of the protocol. sdp protocol is zero.
     */
    @Override
    public void setProtocol(String protocolAddress, String protocolType) {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("[HyperchainBBCService.setProtocol] empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getAuthMessageContract())) {
            throw new RuntimeException("[HyperchainBBCService.setProtocol] empty am contract in bbc context");
        }

        // 2. invoke am contract
        try {
            // setProtocol
            FuncParams params = new FuncParams();
            params.addParams(protocolAddress);
            params.addParams(protocolType);
            invokeContract(
                    this.bbcContext.getAuthMessageContract().getContractAddress(),
                    AM_ABI_PATH,
                    AM_METHOD_SETPROTOCOL,
                    params);

            // getProtocol
            params = new FuncParams();
            params.addParams(protocolType);
            String ret = invokeContract(
                    this.bbcContext.getAuthMessageContract().getContractAddress(),
                    AM_ABI_PATH,
                    AM_METHOD_GETPROTOCOL,
                    params).split(",")[1];
            if (!ret.matches(
                    StrUtil.format("{}{}", EMPTY_MATCHER, protocolAddress.substring(2)))) {
                throw new RuntimeException(
                        StrUtil.format("[HyperchainBBCService.setProtocol] can not get right protocol after set protocol, ret: {}", ret));
            }
        } catch (Exception e) {
            getBBCLogger().error(
                    "[HyperchainBBCService.setProtocol] failed to set protocol (address: {}, type: {}) to AM {}",
                    protocolAddress,
                    protocolType,
                    this.bbcContext.getAuthMessageContract().getContractAddress(),
                    e
            );
            throw new RuntimeException(
                    StrUtil.format(
                            "[HyperchainBBCService.setProtocol] failed to set protocol (address: {}, type: {}) to AM {}",
                            protocolAddress,
                            protocolType,
                            this.bbcContext.getAuthMessageContract().getContractAddress()
                    ), e
            );
        }

        // 3. update am contract status
        try {
            this.bbcContext.getAuthMessageContract().setStatus(ContractStatusEnum.CONTRACT_READY);
            getBBCLogger().info(
                    "[HyperchainBBCService.setProtocol] update am contract status to ready status (address: {})",
                    this.bbcContext.getAuthMessageContract().getContractAddress()
            );
        } catch (Exception e) {
            getBBCLogger().error(
                    "[HyperchainBBCService.setProtocol] failed to update am contract status (address: {})",
                    this.bbcContext.getAuthMessageContract().getContractAddress(),
                    e
            );
            throw new RuntimeException(
                    StrUtil.format(
                            "[HyperchainBBCService.setProtocol] failed to update am contract status (address: {})",
                            this.bbcContext.getAuthMessageContract().getContractAddress()
                    ), e);
        }

        getBBCLogger().info(
                "[HyperchainBBCService.setProtocol] set protocol (address: {}, type: {}) to AM {} success",
                protocolAddress,
                protocolType,
                this.bbcContext.getAuthMessageContract().getContractAddress()
        );
    }


    /**
     * set am contract info to sdp contract
     * 1. set am contract
     * 2. get am contract to check if success
     * 3. check if local domain is set
     * - if local doamin has set, update sdp status to ready, end
     * - else, end
     *
     * @param contractAddress am contract address
     */
    @Override
    public void setAmContract(String contractAddress) {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("[HyperchainBBCService.setAmContract] empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getSdpContract())) {
            throw new RuntimeException("[HyperchainBBCService.setAmContract]empty sdp contract in bbc context");
        }

        // 2. invoke sdp contract
        try {
            // set am contract
            FuncParams params = new FuncParams();
            params.addParams(contractAddress);
            invokeContract(
                    this.bbcContext.getSdpContract().getContractAddress(),
                    SDP_ABI_PATH,
                    SDP_METHOD_SETAMCONTRACT,
                    params);

            // get am contract
            params = new FuncParams();
            String ret = invokeContract(
                    this.bbcContext.getSdpContract().getContractAddress(),
                    SDP_ABI_PATH,
                    SDP_METHOD_GETAMCONTRACT,
                    params).split(",")[1];
            if (!ret.matches(
                    StrUtil.format("{}{}", EMPTY_MATCHER, contractAddress.substring(2)))) {
                throw new RuntimeException(
                        StrUtil.format("[HyperchainBBCService.setAmContract] can not get right am after set am contract, ret: {}", ret));
            }
        } catch (Exception e) {
            getBBCLogger().info(
                    "[HyperchainBBCService.setAmContract] failed to set am contract (address: {}) to SDP {}",
                    contractAddress,
                    this.bbcContext.getSdpContract().getContractAddress(),
                    e
            );

            throw new RuntimeException(
                    StrUtil.format(
                            "[HyperchainBBCService.setAmContract] failed to set am contract (address: {}) to SDP {}",
                            contractAddress,
                            this.bbcContext.getSdpContract().getContractAddress()
                    ), e
            );
        }

        // 3. update am contract status
        try {
            String ret = invokeContract(
                    this.bbcContext.getSdpContract().getContractAddress(),
                    SDP_ABI_PATH,
                    SDP_METHOD_GETLOCALDOMAIN,
                    new FuncParams()).split(",")[1];
            if (!ret.matches(EMPTY_MATCHER)) {
                this.bbcContext.getSdpContract().setStatus(ContractStatusEnum.CONTRACT_READY);
                getBBCLogger().info(
                        "[HyperchainBBCService.setAmContract] update sdp contract status to ready status (address: {}, am: {}, localDomain: {})",
                        this.bbcContext.getSdpContract().getContractAddress(),
                        contractAddress,
                        ret
                );
            } else {
                getBBCLogger().info(
                        "[HyperchainBBCService.setAmContract] wait for local domain to update sdp status (address: {}, am: {})",
                        this.bbcContext.getSdpContract().getContractAddress(),
                        contractAddress
                );
            }
        } catch (Exception e) {
            getBBCLogger().info(
                    "[HyperchainBBCService.setAmContract] fail to update sdp contract status(address: {})",
                    this.bbcContext.getSdpContract().getContractAddress()
            );
            throw new RuntimeException(
                    StrUtil.format(
                            "[HyperchainBBCService.setAmContract] failed to update sdp contract status (address:{})",
                            this.bbcContext.getSdpContract().getContractAddress()
                    ), e);
        }

        getBBCLogger().info(
                "[HyperchainBBCService.setAmContract] set am contract (address: {}) to SDP {} success",
                contractAddress,
                this.bbcContext.getSdpContract().getContractAddress()
        );
    }

    /**
     * set local domain to sdp contract
     * 1. set local domain
     * 2. get local domain to check if success
     * 3. check if am contract is set
     * - if am contract has set, update sdp status to ready, end
     * - else, end
     *
     * @param domain the domain value
     */
    @Override
    public void setLocalDomain(String domain) {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("[HyperchainBBCService.setLocalDomain] empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getSdpContract())) {
            throw new RuntimeException("[HyperchainBBCService.setLocalDomain]empty sdp contract in bbc context");
        }

        // 2. invoke sdp contract
        try {
            // set local domain
            FuncParams params = new FuncParams();
            params.addParams(domain);
            invokeContract(
                    this.bbcContext.getSdpContract().getContractAddress(),
                    SDP_ABI_PATH,
                    SDP_METHOD_SETLOCALDOMAIN,
                    params);

            // get local domain
            String ret = invokeContract(
                    this.bbcContext.getSdpContract().getContractAddress(),
                    SDP_ABI_PATH,
                    SDP_METHOD_GETLOCALDOMAIN,
                    new FuncParams()).split(",")[1];
            // 返回的是local domain的哈希 keccak256(abi.encodePacked(domain))
            if (ret.matches(EMPTY_MATCHER)) {
                throw new RuntimeException(
                        StrUtil.format("[HyperchainBBCService.setLocalDomain] can not get domain after set  k ad  contract, ret: {}", ret));
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "[HyperchainBBCService.setLocalDomain] failed to set local domain(%s) to SDP %s",
                            domain,
                            this.bbcContext.getSdpContract().getContractAddress()
                    ), e
            );
        }

        // 3. update sdp contract status
        try {
            String ret = invokeContract(
                    this.bbcContext.getSdpContract().getContractAddress(),
                    SDP_ABI_PATH,
                    SDP_METHOD_GETAMCONTRACT,
                    new FuncParams()).split(",")[1];
            if (!ret.matches(EMPTY_MATCHER)) {
                this.bbcContext.getSdpContract().setStatus(ContractStatusEnum.CONTRACT_READY);
                getBBCLogger().info(
                        "[HyperchainBBCService.setLocalDomain] update sdp contract status to ready status (address: {}, am: {}, localDomain: {})",
                        this.bbcContext.getSdpContract().getContractAddress(),
                        ret,
                        domain
                );
            } else {
                getBBCLogger().info(
                        "[HyperchainBBCService.setLocalDomain] wait for am contract info to update sdp status (address: {}, localDomain: {})",
                        this.bbcContext.getSdpContract().getContractAddress(),
                        domain
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "[HyperchainBBCService.setLocalDomain] failed to update sdp contract status (address: %s)",
                            this.bbcContext.getSdpContract().getContractAddress()
                    ), e);
        }

        getBBCLogger().info(
                "[HyperchainBBCService.setLocalDomain] set local domain ({}) to SDP {} success",
                domain,
                this.bbcContext.getSdpContract().getContractAddress()
        );
    }

    /**
     * 中继调用该接口是为了验证消息在接收链上顺序是否合法，故当前链应当是receiverDomain对应的链
     *
     * @param senderDomain   blockchain domain where sender from
     * @param fromName       sender contract name
     * @param receiverDomain blockchain domain where receiver from
     * @param toName         receiver contract name
     * @return
     */
    @Override
    public long querySDPMessageSeq(String senderDomain, String fromName, String receiverDomain, String toName) {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("[HyperchainBBCService.querySDPMessageSeq] empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getSdpContract())) {
            throw new RuntimeException("[HyperchainBBCService.querySDPMessageSeq] empty sdp contract in bbc context");
        }

        // 2. invoke sdp contract
        try {
            FuncParams params = new FuncParams();
            params.addParams(senderDomain);
            params.addParams(fromName);
            params.addParams(receiverDomain);
            params.addParams(toName);
            String ret = invokeContract(
                    this.bbcContext.getSdpContract().getContractAddress(),
                    SDP_ABI_PATH,
                    SDP_METHOD_QUERYSDPMESSAGESEQ,
                    params).split(",")[1];
            getBBCLogger().info(
                    "[HyperchainBBCService.querySDPMessageSeq] get sdp msg seq from SDP {} success",
                    this.bbcContext.getSdpContract().getContractAddress()
            );

            return Long.parseLong(ret.substring(2), 16);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "[HyperchainBBCService.querySDPMessageSeq] failed to get sdp msg seq from SDP %s",
                            this.bbcContext.getSdpContract().getContractAddress()
                    ), e
            );
        }
    }

    @Override
    public CrossChainMessageReceipt readCrossChainMessageReceipt(String txHash) {
        getBBCLogger().info("[HyperchainBBCService.readCrossChainMessageReceipt] read cross chain message receipt with txHash {} for {}",
                txHash,
                config.getUrl());

        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("[HyperchainBBCService.readCrossChainMessageReceipt] empty bbc context");
        }

        if (ObjectUtil.isNull(this.bbcContext.getAuthMessageContract())) {
            throw new RuntimeException("[HyperchainBBCService.readCrossChainMessageReceipt] empty am contract in bbc context");
        }

        CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();

        try {
            // 1. get receipt by tx hash
            Request<ReceiptResponse> transactionReceipt = txService.getTransactionReceipt(txHash);
            ReceiptResponse receiptResponse = transactionReceipt.send();

            // 2. construct cross-chain msg receipt
            if (receiptResponse.getCode() == 0) {
                crossChainMessageReceipt.setConfirmed(true);
                crossChainMessageReceipt.setSuccessful(receiptResponse.getMessage().toLowerCase().equals(SUCCESS));

            } else if (receiptResponse.getCode() == RequestExceptionCode.RECEIPT_NOT_FOUND.getCode()) {
                // tx not confirmed
                crossChainMessageReceipt.setConfirmed(false);
                crossChainMessageReceipt.setSuccessful(true);
            } else {
                // tx failed
                crossChainMessageReceipt.setConfirmed(false);
                crossChainMessageReceipt.setSuccessful(false);
            }

            crossChainMessageReceipt.setTxhash(receiptResponse.getTxHash());
            crossChainMessageReceipt.setErrorMsg(receiptResponse.getMessage());
        } catch (Exception e) {
            getBBCLogger().error("[HyperchainBBCService.readCrossChainMessageReceipt] fail to read cross chain message receipt with txHash {} for {}, ",
                    txHash,
                    config.getUrl(),
                    e);
            throw new RuntimeException(
                    StrUtil.format("[HyperchainBBCService.readCrossChainMessageReceipt] fail to read cross chain message receipt with txHash {} for {}, ",
                            txHash,
                            config.getUrl()
                    ), e);
        }

        getBBCLogger().info("[HyperchainBBCService.readCrossChainMessageReceipt] read cross chain message receipt with txHash {} for {} success, " +
                        "isConfirmed: {}, isSuccessful: {}, errorMsg: {}",
                txHash,
                config.getUrl(),
                crossChainMessageReceipt.isConfirmed(),
                crossChainMessageReceipt.isSuccessful(),
                crossChainMessageReceipt.getErrorMsg()
        );

        return crossChainMessageReceipt;
    }

    @Override
    public List<CrossChainMessage> readCrossChainMessagesByHeight(long blockNum) {
        getBBCLogger().info("[HyperchainBBCService.readCrossChainMessagesByHeight] read cross chain message by height {} for {}",
                blockNum,
                config.getUrl());

        List<CrossChainMessage> messageList = ListUtil.toList();

        try {
            Request<BlockResponse> blockResponseRequest = blockService.getBlockByNum(BigInteger.valueOf(blockNum));
            BlockResponse blockResponse = blockResponseRequest.send();

            if (!blockResponse.getMessage().toLowerCase().equals(SUCCESS)) {
                throw new RuntimeException(
                        StrUtil.format("send block request failed, code:{}, msg: {}",
                                blockResponse.getCode(),
                                blockResponse.getMessage())
                );
            }

            String amAbiJson = FileUtil.readFile(this.getClass().getResourceAsStream(AM_ABI_PATH));
            Abi amAbi = Abi.fromJson(amAbiJson);

            Map<String, String> amEventMap = Encoder.encodeEVMEvent(amAbiJson);
            String crosschainEventTopic = amEventMap.get(AM_EVENT_SENDAUTHMESSAGE);

            if (Long.parseLong(blockResponse.getResult().get(0).getTxcounts().replace("0x", ""), 16) > 0) {
                for (TxResponse.Transaction tx : blockResponse.getResult().get(0).getTransactions()) {
                    String txHash = tx.getHash();
                    Request<ReceiptResponse> transactionReceipt = txService.getTransactionReceipt(txHash);
                    ReceiptResponse receiptResponse = transactionReceipt.send();
                    if (!receiptResponse.getMessage().toLowerCase().equals(SUCCESS)) {
                        continue;
                    }

                    for (EventLog elog : receiptResponse.getLog()) {
                        if (StrUtil.equals(elog.getAddress(), bbcContext.getAuthMessageContract().getContractAddress())
                                && Arrays.stream(elog.getTopics()).anyMatch(crosschainEventTopic::equals)) {
                            getBBCLogger().info("[HyperchainBBCService.readCrossChainMessagesByHeight] " +
                                            "read cross chain message by height {} for {}, log block number: {}, contract addr: {}",
                                    blockNum,
                                    config.getUrl(),
                                    (elog.getBlockNumber()),
                                    elog.getAddress());

                            String[] topics = elog.getTopics();
                            byte[][] topicsData = new byte[topics.length][];
                            for (int i = 0; i < topics.length; i++) {
                                topicsData[i] = ByteUtil.fromHex(topics[i]);
                            }

                            getBBCLogger().info("[HyperchainBBCService.readCrossChainMessagesByHeight] read topic {}-{} by height {} for {}",
                                    StrUtil.join(",", topics),
                                    amAbi.getEvent(AM_EVENT_SENDAUTHMESSAGE_ABI).decode(
                                                    ByteUtil.fromHex(elog.getData()),
                                                    topicsData)
                                            .get(0),
                                    blockNum,
                                    config.getUrl());

                            messageList.add(CrossChainMessage.createCrossChainMessage(
                                            CrossChainMessage.CrossChainMessageType.AUTH_MSG,
                                            elog.getBlockNumber(),
                                            Long.parseLong(tx.getTimestamp()) / 1000000,
                                            HexUtil.decodeHex(StrUtil.removePrefix(
                                                    blockResponse.getResult().get(0).getHash().trim(),
                                                    "0x")),
                                            (byte[]) amAbi.getEvent(AM_EVENT_SENDAUTHMESSAGE_ABI).decode(
                                                            ByteUtil.fromHex(elog.getData()),
                                                            topicsData)
                                                    .get(0),
                                            // todo: put ledger data, for SPV or other attestations
                                            // this time we need no verify. it's ok to set it with empty bytes
                                            "".getBytes(),
                                            // todo: put proof data
                                            // this time we need no proof data. it's ok to set it with empty bytes
                                            "".getBytes(),
                                            HexUtil.decodeHex(tx.getHash().replaceFirst("^0x", ""))
                                    )
                            );
                        }
                    }
                }
            }
        } catch (
                Exception e) {
            getBBCLogger().error("[HyperchainBBCService.readCrossChainMessagesByHeight] fail to read cross chain message by height {} for {}, ",
                    blockNum,
                    config.getUrl(),
                    e);
            throw new RuntimeException(
                    StrUtil.format("[HyperchainBBCService.readCrossChainMessagesByHeight] fail to read cross chain message by height {} for {}, ",
                            blockNum,
                            config.getUrl()),
                    e);
        }

        getBBCLogger().info("[HyperchainBBCService.readCrossChainMessagesByHeight] read cross chain message by height {} for {} success",
                blockNum,
                config.getUrl());
        return messageList;
    }

    @Override
    public CrossChainMessageReceipt relayAuthMessage(byte[] pkg) {
        getBBCLogger().info("[HyperchainBBCService.relayAuthMessage] relay AuthMessage for {}",
                config.getUrl());

        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("[HyperchainBBCService.relayAuthMessage] empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getAuthMessageContract())) {
            throw new RuntimeException("[HyperchainBBCService.relayAuthMessage] empty am contract in bbc context");
        }

        // 2. invoke sdp contract
        try {
            FuncParams params = new FuncParams();
            params.addParams(pkg);
            String ret = invokeContractAsync(
                    this.bbcContext.getAuthMessageContract().getContractAddress(),
                    AM_ABI_PATH,
                    AM_METHOD_RECVPKGFROMRELAYER,
                    params
            );
            getBBCLogger().info(
                    "[HyperchainBBCService.relayAuthMessage] recv pkg from relayer for AM {} success",
                    this.bbcContext.getAuthMessageContract().getContractAddress()
            );

            CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();

            crossChainMessageReceipt.setConfirmed(false);
            // 若异步提交失败会抛出异常
            crossChainMessageReceipt.setSuccessful(true);
            crossChainMessageReceipt.setTxhash(ret);
            crossChainMessageReceipt.setErrorMsg("");

            getBBCLogger().info("relay tx {}", ret);

            return crossChainMessageReceipt;
        } catch (Exception e) {
            getBBCLogger().error("[HyperchainBBCService.relayAuthMessage] fail to relay AuthMessage for {} ",
                    config.getUrl(),
                    e);
            throw new RuntimeException(
                    StrUtil.format("[HyperchainBBCService.relayAuthMessage] fail to relay AuthMessage for {}",
                            config.getUrl()),
                    e);
        }
    }

    @Override
    public Long queryLatestHeight() {
        getBBCLogger().info("[HyperchainBBCService.queryLatestHeight] query latest height for {}",
                config.getUrl());

        Long ret;
        try {
            Request<BlockNumberResponse> blockRequest = blockService.getChainHeight();
            BlockNumberResponse blockNumberResponse = blockRequest.send();
            if (!blockNumberResponse.getMessage().toLowerCase().equals(SUCCESS)) {
                throw new RuntimeException(
                        StrUtil.format("send query latest height request failed, code:{}, msg: {}",
                                blockNumberResponse.getCode(),
                                blockNumberResponse.getMessage())
                );
            }

            ret = Long.parseLong(blockNumberResponse.getResult().substring(2), 16);
        } catch (RequestException e) {
            getBBCLogger().error("[HyperchainBBCService.queryLatestHeight] fail to get latest height for {}",
                    config.getUrl(), e);
            throw new RuntimeException(StrUtil.format(
                    "[HyperchainBBCService.queryLatestHeight] fail to get latest height for {}",
                    config.getUrl()
            ), e);
        }

        getBBCLogger().info("[HyperchainBBCService.queryLatestHeight] latest height is {} for {}",
                ret,
                config.getUrl());
        return ret;
    }

    public String deployContract(String binPath, String abiPath) {
        try {
            InputStream inputStream1 = this.getClass().getResourceAsStream(binPath);
            InputStream inputStream2 = this.getClass().getResourceAsStream(abiPath);
            String bin = FileUtil.readFile(inputStream1);
            String abiStr = FileUtil.readFile(inputStream2);
            Abi abi = Abi.fromJson(abiStr);

            Transaction transaction = new Transaction.EVMBuilder(account.getAddress()).deploy(bin).build();

            transaction.sign(account);
            ReceiptResponse receiptResponse = contractService.deploy(transaction).send().polling();

            if (!receiptResponse.getMessage().toLowerCase().equals(SUCCESS)) {
                throw new RuntimeException(
                        StrUtil.format("deploy contract failed, code:{}, msg: {}",
                                receiptResponse.getCode(),
                                receiptResponse.getMessage())
                );
            }

            getBBCLogger().info("[HyperchainBBCService] deploy contract for {} success, contractAddr: {}",
                    config.getUrl(),
                    receiptResponse.getContractAddress());
            return receiptResponse.getContractAddress();
        } catch (Exception e) {
            getBBCLogger().error("[HyperchainBBCService] deploy contract for {} failed",
                    config.getUrl(),
                    e);
            throw new RuntimeException(StrUtil.format("[HyperchainBBCService] deploy contract for {} failed",
                    config.getUrl()), e);
        }
    }

    public String deployContractWithParams(String binPath, String abiPath, String paramsStr) {
        try {
            InputStream inputStream1 = this.getClass().getResourceAsStream(binPath);
            InputStream inputStream2 = this.getClass().getResourceAsStream(abiPath);
            String bin = FileUtil.readFile(inputStream1);
            String abiStr = FileUtil.readFile(inputStream2);
            Abi abi = Abi.fromJson(abiStr);

            FuncParams params = new FuncParams();
            for (String p : StrUtil.split(paramsStr, ',')) {
                params.addParams(p);
            }
            Transaction transaction = new Transaction.EVMBuilder(account.getAddress()).deploy(bin, abi, params).build();
            transaction.sign(account);
            ReceiptResponse receiptResponse = contractService.deploy(transaction).send().polling();
            if (!receiptResponse.getMessage().toLowerCase().equals(SUCCESS)) {
                throw new RuntimeException(
                        StrUtil.format("deploy contract failed, code:{}, msg: {}",
                                receiptResponse.getCode(),
                                receiptResponse.getMessage())
                );
            }

            getBBCLogger().info("[HyperchainBBCService] deploy contract with params {} for {} success, contractAddr: {}",
                    paramsStr,
                    config.getUrl(),
                    receiptResponse.getContractAddress());
            return receiptResponse.getContractAddress();
        } catch (Exception e) {
            getBBCLogger().error("[HyperchainBBCService] deploy contract with params {} for {} failed",
                    paramsStr,
                    config.getUrl(),
                    e);
            throw new RuntimeException(StrUtil.format("[HyperchainBBCService] deploy contract with params {} for {} failed",
                    paramsStr,
                    config.getUrl()),
                    e);
        }
    }

    public String invokeContract(String contractAddress, String abiPath, String methodName, FuncParams params) {
        try {
            InputStream inputStream2 = this.getClass().getResourceAsStream(abiPath);
            String abiStr = FileUtil.readFile(inputStream2);
            Abi abi = Abi.fromJson(abiStr);

            Transaction transaction = new Transaction.EVMBuilder(
                    account.getAddress()).invoke(
                    contractAddress,
                    methodName,
                    abi,
                    params
            ).build();

            transaction.sign(account);
            ReceiptResponse receiptResponse = contractService.invoke(transaction).send().polling();
            if (!receiptResponse.getMessage().toLowerCase().equals(SUCCESS)) {
                throw new RuntimeException(
                        StrUtil.format("invoke contract failed, case:{}, msg: {}",
                                receiptResponse.getCode(),
                                receiptResponse.getMessage())
                );
            }

            getBBCLogger().info("[HyperchainBBCService] invoke contract {} with {} for {} by tx {} success",
                    contractAddress,
                    methodName,
                    config.getUrl(),
                    receiptResponse.getTxHash());

            return StrUtil.format("{},{}",
                    receiptResponse.getTxHash(),
                    receiptResponse.getRet());

        } catch (Exception e) {
            getBBCLogger().error("[HyperchainBBCService] invoke contract {} with {} for {} failed",
                    contractAddress,
                    methodName,
                    config.getUrl(),
                    e);
            throw new RuntimeException(StrUtil.format("[HyperchainBBCService] invoke contract {} with {} for {} failed",
                    contractAddress,
                    methodName,
                    config.getUrl()),
                    e);
        }
    }

    public String invokeContractAsync(String contractAddress, String abiPath, String methodName, FuncParams params) {
        try {
            InputStream inputStream2 = this.getClass().getResourceAsStream(abiPath);
            String abiStr = FileUtil.readFile(inputStream2);
            Abi abi = Abi.fromJson(abiStr);

            Transaction transaction = new Transaction.EVMBuilder(
                    account.getAddress()).invoke(
                    contractAddress,
                    methodName,
                    abi,
                    params
            ).build();

            transaction.sign(account);
            TxHashResponse txHashResponse = contractService.invoke(transaction).sendAsync().get();

            if (!txHashResponse.getMessage().toLowerCase().equals(SUCCESS)) {
                throw new RuntimeException(StrUtil.format("[HyperchainBBCService] send invoke contract tx failed, code:{}, msg: {}",
                        txHashResponse.getCode(),
                        txHashResponse.getMessage()));
            }

            getBBCLogger().info("[HyperchainBBCService] invoke contract {} async with {} for {} by tx {} success",
                    contractAddress,
                    methodName,
                    config.getUrl(),
                    txHashResponse.getTxHash());

            return txHashResponse.getTxHash();

        } catch (Exception e) {
            getBBCLogger().error("[HyperchainBBCService] invoke contract {} async with {} for {} failed",
                    contractAddress,
                    methodName,
                    config.getUrl(),
                    e);
            throw new RuntimeException(StrUtil.format("[HyperchainBBCService] invoke contract {} async with {} for {} failed",
                    contractAddress,
                    methodName,
                    config.getUrl()),
                    e);
        }
    }
}