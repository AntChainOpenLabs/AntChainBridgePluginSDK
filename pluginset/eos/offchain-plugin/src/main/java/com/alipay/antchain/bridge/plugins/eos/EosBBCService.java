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

package com.alipay.antchain.bridge.plugins.eos;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONPath;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.AuthMessageContract;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.bbc.syscontract.SDPContract;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.plugins.eos.types.EosBlockInfo;
import com.alipay.antchain.bridge.plugins.eos.types.EosTransactionStatusEnum;
import com.alipay.antchain.bridge.plugins.eos.types.EosTxActions;
import com.alipay.antchain.bridge.plugins.eos.types.EosTxInfo;
import com.alipay.antchain.bridge.plugins.eos.utils.Utils;
import com.alipay.antchain.bridge.plugins.lib.BBCService;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import lombok.Getter;
import okhttp3.RequestBody;
import one.block.eosiojava.error.rpcProvider.RpcProviderError;
import one.block.eosiojava.error.serializationProvider.SerializationProviderError;
import one.block.eosiojava.error.session.TransactionPrepareError;
import one.block.eosiojava.error.session.TransactionSignAndBroadCastError;
import one.block.eosiojava.implementations.ABIProviderImpl;
import one.block.eosiojava.interfaces.ISerializationProvider;
import one.block.eosiojava.models.rpcProvider.Action;
import one.block.eosiojava.models.rpcProvider.Authorization;
import one.block.eosiojava.models.rpcProvider.TransactionConfig;
import one.block.eosiojava.models.rpcProvider.request.GetBlockRequest;
import one.block.eosiojava.models.rpcProvider.response.GetBlockResponse;
import one.block.eosiojava.models.rpcProvider.response.SendTransactionResponse;
import one.block.eosiojava.session.TransactionProcessor;
import one.block.eosiojava.session.TransactionSession;
import one.block.eosiojavaabieosserializationprovider.AbiEosSerializationProviderImpl;
import one.block.eosiojavarpcprovider.error.EosioJavaRpcProviderInitializerError;
import one.block.eosiojavarpcprovider.implementations.EosioJavaRpcProviderImpl;
import one.block.eosiosoftkeysignatureprovider.SoftKeySignatureProviderImpl;
import one.block.eosiosoftkeysignatureprovider.error.ImportKeyError;

@BBCService(products = "eos", pluginId = "plugin-eos")
@Getter
public class EosBBCService implements IBBCService {

    /**
     * 跨链事件标识
     */
    private static final String CROSSCHAIN_ACTION = "crossing";

    // ============================== SDP合约信息常量 ==============================
    /**
     * sdp合约「有序消息seq表」：该表记录跨链四元组对应的有序消息的seq
     * - 表名
     */
    private static final String SDP_MSG_SEQ_TABLE = "sdpmsgseq";

    /**
     * - 主键的value名称
     */
    private static final String SDP_MSG_SEQ_TABLE_VALUE_NAME = "sdp_msg_seq";

    /**
     * sdp合约「初始化信息表」：该表记录am合约账户和链的localdomain
     * - 表名
     */
    private static final String SDP_INIT_INFO_TABLE = "sdpinitinfo";

    /**
     * - 主键的key值：固定为1
     */
    private static final long SDP_INIT_INFO_TABLE_KEY = 1;

    /**
     * - 主键的value名称
     */
    private static final String SDP_INIT_INFO_TABLE_VALUE_AM_NAME = "am_contract_account";
    private static final String SDP_INIT_INFO_TABLE_VALUE_DOMAIN_NAME = "local_domain";

    /**
     * sdp合约中设置am合约账户的action
     * - 名称
     */
    private static final String SDP_SET_AM_CONTRACT_ACTION = "setam";

    /**
     * - 参数格式
     */
    private static final String SDP_SET_AM_CONTRACT_PARAMETER_FORMAT = "{\"invoker\": \"%s\", \"am_contract_account\": \"%s\"}";

    /**
     * sdp合约中设置链localdomain的action
     * - 名称
     */
    private static final String SDP_SET_LOCALDOMAIN_ACTION = "setdomain";

    /**
     * - 参数格式
     */
    private static final String SDP_SET_LOCALDOMAIN_PARAMETER_FORMAT = "{\"invoker\": \"%s\", \"local_domain\": \"%s\"}";

    // ============================== AM合约信息常量 ==============================
    /**
     * am 中继信息表
     * - 表名
     */
    private static final String AM_RELAYER_INFO_TABLE = "relayerinfo";

    private static final String AM_RELAYER_INFO_VALUE_NAME = "relayer";

    /**
     * am protocol信息表 account -> type
     * - 表名
     */
    private static final String AM_PROTOCOL_ACCOUNT_TABLE = "protaccount";

    /**
     * - 主键的value名称
     */
    private static final String AM_PROTOCOL_ACCOUNT_VALUE_NAME = "protocol_type";

    /**
     * am protocol信息表 type -> account
     * - 表名
     */
    private static final String AM_PROTOCOL_TYPE_TABLE = "prottype";

    /**
     * - 主键的key值格式：protocol账户名称
     */
    private static final String AM_PROTOCOL_TYPE_KEY_FORMAT = "%s";

    /**
     * - 主键的value名称
     */
    private static final String AM_PROTOCOL_TYPE_VALUE_NAME = "protocol_account";

    /**
     * am合约中设置上层协议合约账户的action
     * - 名称
     */
    private static final String AM_SET_PROTOCOL_ACTION = "setprotocol";

    /**
     * - 参数格式
     */
    private static final String AM_SET_PROTOCOL_PARAMETER_FORMAT = "{\"invoker\": \"%s\", \"protocol_account\": \"%s\", \"protocol_type\": %s}";

    /**
     * am合约中接收中继消息的action
     * - 名称
     */
    private static final String AM_RECV_PKG_FROM_RELAYER_ACTION = "recvrelayerx";

    /**
     * - 参数格式
     */
    private static final String AM_RECV_PKG_FROM_RELAYER_PARAMETER_FORMAT = "{\"invoker\": \"%s\", \"pkg_hex\": \"%s\"}";

    // ============================== 插件基本变量 ==============================
    private EosConfig config;

    private AbstractBBCContext bbcContext;

    // ============================== EOS-SDK相关组件 ==============================

    /**
     * rpc服务组件
     */
    private EosioJavaRpcProviderImpl rpcProvider;

    /**
     * 序列化管理组件
     */
    private ISerializationProvider serializationProvider;

    /**
     * 合约abi处理组件
     */
    private ABIProviderImpl abiProvider;

    /**
     * 签名管理组件
     */
    private SoftKeySignatureProviderImpl signatureProvider;

    /**
     * 交易处理器
     */
    private TransactionSession session;

    /**
     * 启动插件
     * <pre>
     *     1. 插件连接eos
     *     2. 检查context中是否携带已部署合约信息（需要携带）
     * </pre>
     *
     * @param abstractBBCContext
     */
    @Override
    public void startup(AbstractBBCContext abstractBBCContext) {
        System.out.printf("EOS BBCService startup with context: %s \n",
                new String(abstractBBCContext.getConfForBlockchainClient()));

        if (ObjectUtil.isNull(abstractBBCContext)) {
            throw new RuntimeException("null bbc context");
        }
        if (ObjectUtil.isEmpty(abstractBBCContext.getConfForBlockchainClient())) {
            throw new RuntimeException("empty blockchain client conf");
        }

        // 1. Obtain the configuration information
        try {
            config = EosConfig.fromJsonString(new String(abstractBBCContext.getConfForBlockchainClient()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!Utils.isEosBase32Name(config.getUserName())) {
            throw new RuntimeException("require EOS Base32 name: " + config.getUserName());
        }

        if (StrUtil.isEmpty(config.getUserPriKey())) {
            throw new RuntimeException("private key is empty");
        }

        if (StrUtil.isEmpty(config.getUrl())) {
            throw new RuntimeException("eos url is empty");
        }

        // 2. Connect to the Eos network
        try {
            // 2.1 Initialize the various service components
            rpcProvider = new EosioJavaRpcProviderImpl(config.getUrl());

            serializationProvider = new AbiEosSerializationProviderImpl();

            abiProvider = new ABIProviderImpl(rpcProvider, serializationProvider);

            signatureProvider = new SoftKeySignatureProviderImpl();
            signatureProvider.importKey(config.getUserPriKey());

            // 2.2 Initializes the transaction processing component
            session = new TransactionSession(
                    serializationProvider,
                    rpcProvider,
                    abiProvider,
                    signatureProvider);

        } catch (EosioJavaRpcProviderInitializerError | SerializationProviderError | ImportKeyError e) {
            throw new RuntimeException(String.format("failed to connect eos (url: %s)", config.getUrl()), e);
        }

        // 3. set context
        this.bbcContext = abstractBBCContext;

        // 4. check the pre-deployed contracts into context
        if (ObjectUtil.isNull(abstractBBCContext.getAuthMessageContract())) {
            if (StrUtil.isEmpty(this.config.getAmContractAddressDeployed())) {
                throw new RuntimeException("The am contract is not deployed");
            } else {
                AuthMessageContract authMessageContract = new AuthMessageContract();
                authMessageContract.setContractAddress(this.config.getAmContractAddressDeployed());
                authMessageContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
                this.bbcContext.setAuthMessageContract(authMessageContract);
            }
        }

        if (ObjectUtil.isNull(abstractBBCContext.getSdpContract())) {
            if (StrUtil.isEmpty(this.config.getSdpContractAddressDeployed())) {
                throw new RuntimeException("The sdp contract is not deployed");
            } else {
                SDPContract sdpContract = new SDPContract();
                sdpContract.setContractAddress(this.config.getSdpContractAddressDeployed());
                sdpContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
                this.bbcContext.setSdpContract(sdpContract);
            }
        }
    }

    /**
     * 关闭插件（当前没有什么需要操作）
     */
    @Override
    public void shutdown() {
        System.out.println("shut down EOS BBCService!");
    }

    /**
     * 返回上下文
     *
     * @return
     */
    @Override
    public AbstractBBCContext getContext() {
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }

        System.out.printf("EOS BBCService context (amAddr: %s, amStatus: %s, sdpAddr: %s, sdpStatus: %s) \n",
                this.bbcContext.getAuthMessageContract() != null ? this.bbcContext.getAuthMessageContract().getContractAddress() : "",
                this.bbcContext.getAuthMessageContract() != null ? this.bbcContext.getAuthMessageContract().getStatus() : "",
                this.bbcContext.getSdpContract() != null ? this.bbcContext.getSdpContract().getContractAddress() : "",
                this.bbcContext.getSdpContract() != null ? this.bbcContext.getSdpContract().getStatus() : ""
        );

        return this.bbcContext;
    }

    /**
     * EOS不支持插件部署合约，这里直接根据`bbcContext`判断`AM`合约是否已经部署好
     */
    @Override
    public void setupAuthMessageContract() {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getAuthMessageContract())
                || StrUtil.isEmpty(this.bbcContext.getAuthMessageContract().getContractAddress())) {
            throw new RuntimeException("Please contact EOS Chain operations personnel to pre-deploy AM contract " +
                    "and add the contract information to the plugin configuration file");
        }
        // If the contract has been pre-deployed and the contract address is configured in the configuration file,
        // there is no need to redeploy.
    }

    /**
     * EOS不支持插件部署合约，这里直接根据`bbcContext`判断`SDP`合约是否已经部署好
     */
    @Override
    public void setupSDPMessageContract() {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (!ObjectUtil.isNotNull(this.bbcContext.getSdpContract())
                || !StrUtil.isNotEmpty(this.bbcContext.getSdpContract().getContractAddress())) {
            throw new RuntimeException("Please contact EOS Chain operations personnel to pre-deploy SDP contract " +
                    "and add the contract information to the plugin configuration file");
        }
        // If the contract has been pre-deployed and the contract address is configured in the configuration file,
        // there is no need to redeploy.
    }

    /**
     * 根据交易哈希获取跨链交易结果信息
     *
     * @param txHash
     * @return
     */
    @Override
    public CrossChainMessageReceipt readCrossChainMessageReceipt(String txHash) {

        EosTxInfo txInfo = bbcGetTxInfoByTransactionHashOnRpc(txHash);
        if (ObjectUtil.isNull(txInfo)) {
            throw new RuntimeException(String.format("failed to get transaction info %s", txHash));
        }

        CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();
        crossChainMessageReceipt.setSuccessful(txInfo.isSuccess());
        crossChainMessageReceipt.setConfirmed(txInfo.isConfirmed());
        crossChainMessageReceipt.setTxhash(txHash);
        crossChainMessageReceipt.setErrorMsg(crossChainMessageReceipt.isSuccessful() ? "SUCCESS" : txInfo.getStatus().getStatus());

        System.out.printf("cross chain message receipt for tx %s : %s\n", crossChainMessageReceipt.getTxhash(), crossChainMessageReceipt.getErrorMsg());

        return crossChainMessageReceipt;
    }

    /**
     * 根据区块高度获取相应区块中所有的跨链信息
     * <pre>
     *     1. 根据区块高度获取指定区块
     *     2. 获取区块中所有交易
     *     3. 获取每个交易中的action
     *     4. 如果action中包含跨链信息则取出
     * </pre>
     *
     * @param height
     * @return
     */
    @Override
    public List<CrossChainMessage> readCrossChainMessagesByHeight(long height) {
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }

        if (ObjectUtil.isNull(this.bbcContext.getAuthMessageContract())) {
            throw new RuntimeException("empty am contract in bbc context");
        }

        try {
            EosBlockInfo blockInfo = getIrreversibleBlockInfoByHeight(height);
            List<EosTxActions> eosTxActionsList = EosTxActions.parseTxActionsListByNameAndAcc(
                    blockInfo.getEosTxActionsList(),
                    CROSSCHAIN_ACTION,
                    this.config.getAmContractAddressDeployed()
            );

            List<CrossChainMessage> messageList = ListUtil.toList();
            for (EosTxActions eosTxActions : eosTxActionsList) {
                if (ObjectUtil.isNotEmpty(eosTxActions.getActions())) {
                    eosTxActions.getActions().forEach(
                            action -> messageList.add(
                                    CrossChainMessage.createCrossChainMessage(
                                            CrossChainMessage.CrossChainMessageType.AUTH_MSG,
                                            height,
                                            blockInfo.getTimestamp(),
                                            HexUtil.decodeHex(blockInfo.getHash()),
                                            HexUtil.decodeHex(JSON.parseObject(action.getData()).getString("msg_hex")),
                                            // this time we need no verify. it's ok to set it with empty bytes
                                            new byte[]{},
                                            // this time we need no proof data. it's ok to set it with empty bytes
                                            new byte[]{},
                                            HexUtil.decodeHex(eosTxActions.getTxId().replaceFirst("^0x", ""))
                                    )
                            )
                    );
                }
            }

            System.out.printf("read cross chain messages (height: %d, msg_size: %s)\n", height, messageList.size());

            return messageList;
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to readCrossChainMessagesByHeight (Height: %d, contractAddr: %s, topic: %s)",
                            height,
                            this.bbcContext.getAuthMessageContract().getContractAddress(),
                            CROSSCHAIN_ACTION
                    ), e
            );
        }
    }

    /**
     * 获取最新区块高度
     *
     * @return
     */
    @Override
    public Long queryLatestHeight() {
        try {
            return rpcProvider.getInfo().getLastIrreversibleBlockNum().longValue();
        } catch (Exception e) {
            throw new RuntimeException("failed to query latest irreversible height", e);
        }
    }

    /**
     * 获取SDP合约中有序消息的seq
     * <pre>
     *     1. 检查sdp合约已部署
     *     2. 从sdp合约的`SDP_MSG_SEQ_TABLE`表中读取seq
     *
     *     todo:补充单测
     * </pre>
     *
     * @param senderDomain
     * @param senderID
     * @param receiverDomain
     * @param receiverID
     * @return
     */
    @Override
    public long querySDPMessageSeq(String senderDomain, String senderID, String receiverDomain, String receiverID) {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getSdpContract())) {
            throw new RuntimeException("empty sdp contract in bbc context");
        }

        // 2. 读合约数据
        return (Integer) bbcGetValueFromTableByIndexOnRpc(
                config.getSdpContractAddressDeployed(),
                config.getSdpContractAddressDeployed(),
                SDP_MSG_SEQ_TABLE,
                "sha256",
                DigestUtil.sha256Hex(
                        ArrayUtil.addAll(
                                senderDomain.getBytes(), "-".getBytes(),
                                HexUtil.decodeHex(senderID), "-".getBytes(),
                                HexUtil.decodeHex(receiverID)
                        )
                ),
                SDP_MSG_SEQ_TABLE_VALUE_NAME,
                0
        );
    }

    /**
     * 设置AM合约中上层协议地址
     * <pre>
     *     1. 检查am合约已部署
     *     2. 调用am合约的`setprotocol`action
     *     3. 检查交易是否执行成功
     *     4. 判断合约是否ready
     *
     *     todo:补充单测
     * </pre>
     *
     * @param protocolAddress
     * @param protocolType
     */
    @Override
    public void setProtocol(String protocolAddress, String protocolType) {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getAuthMessageContract())) {
            throw new RuntimeException("empty am contract in bbc context");
        }

        // 2. invoke am contract
        SendTransactionResponse sendTransactionResponse = bbcInvokeContractsOnRpc(
                new String[][]{
                        {
                                this.bbcContext.getAuthMessageContract().getContractAddress(),
                                AM_SET_PROTOCOL_ACTION,
                                String.format(AM_SET_PROTOCOL_PARAMETER_FORMAT,
                                        this.config.getUserName(), protocolAddress, protocolType)
                        },
                }
        );
        EosTxInfo txInfo = bbcGetTxInfoByTransactionHashOnRpc(sendTransactionResponse.getTransactionId());
        if (ObjectUtil.isNull(txInfo)) {
            throw new RuntimeException(String.format("failed to get transaction info %s", sendTransactionResponse.getTransactionId()));
        }

        // 3. check transaction
        if (txInfo.isSuccess()) {
            waitUntilTxIrreversible(txInfo);
            System.out.printf(
                    "set protocol (address: %s, type: %s) to AM %s by tx %s \n",
                    protocolAddress,
                    protocolType,
                    this.bbcContext.getAuthMessageContract().getContractAddress(),
                    txInfo.getTxId()
            );
        } else {
            throw new RuntimeException(String.format("fail to invoke setprotocol by send transaction %s", txInfo.getTxId()));
        }

        // 4. check if am is ready
        if (isAmReady()) {
            this.bbcContext.getAuthMessageContract().setStatus(ContractStatusEnum.CONTRACT_READY);
        }
    }

    public void waitUntilTxIrreversible(EosTxInfo txInfo) {
        int cnt = this.config.getMaxIrreversibleWaitCount();
        while (this.config.isWaitUtilTxIrreversible() && !txInfo.isConfirmed() && cnt-- != 0) {
            try {
                Thread.sleep(this.config.getWaitTimeOnce());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            txInfo = bbcGetTxInfoByTransactionHashOnRpc(txInfo.getTxId());
        }
    }

    /**
     * 判断am合约是否ready
     * <pre>
     *     1. 中继账户名称已初始化
     *     2. 指定类型的上层协议合约名称已初始化
     * </pre>
     *
     * @return
     */
    public boolean isAmReady() {
        try {
            return StrUtil.equals((String) bbcGetValueFromTableByKeyOnRpc(
                    this.bbcContext.getAuthMessageContract().getContractAddress(),
                    this.bbcContext.getAuthMessageContract().getContractAddress(),
                    AM_RELAYER_INFO_TABLE,
                    Utils.convertEosBase32NameToNum(this.config.getUserName()),
                    AM_RELAYER_INFO_VALUE_NAME
            ), this.config.getUserName()) && ObjectUtil.isNotEmpty(bbcGetValueFromTableByKeyOnRpc(
                    this.bbcContext.getAuthMessageContract().getContractAddress(),
                    this.bbcContext.getAuthMessageContract().getContractAddress(),
                    AM_PROTOCOL_TYPE_TABLE,
                    BigInteger.valueOf(0),
                    AM_PROTOCOL_TYPE_VALUE_NAME
            ));
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to read am contract status (address: %s)",
                            this.bbcContext.getAuthMessageContract().getContractAddress()
                    ), e);
        }
    }

    /**
     * 设置SDP合约中AM合约账户
     * <pre>
     *     1. 检查sdp合约已部署
     *     2. 调用sdp合约的`setamcontract`action
     *     3. 检查交易是否执行成功
     *     4. 判断合约是否ready
     *
     *     todo:补充单测
     * </pre>
     *
     * @param contractAddress
     */
    @Override
    public void setAmContract(String contractAddress) {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getSdpContract())) {
            throw new RuntimeException("empty sdp contract in bbc context");
        }

        // 2. invoke sdp contract
        SendTransactionResponse sendTransactionResponse = bbcInvokeContractsOnRpc(
                new String[][]{
                        {
                                this.bbcContext.getSdpContract().getContractAddress(),
                                SDP_SET_AM_CONTRACT_ACTION,
                                String.format(SDP_SET_AM_CONTRACT_PARAMETER_FORMAT,
                                        this.config.getUserName(), contractAddress)
                        },
                }
        );
        EosTxInfo txInfo = bbcGetTxInfoByTransactionHashOnRpc(sendTransactionResponse.getTransactionId());
        if (ObjectUtil.isNull(txInfo)) {
            throw new RuntimeException(String.format("failed to get transaction info %s", sendTransactionResponse.getTransactionId()));
        }

        // 3. check transaction
        if (!txInfo.isSuccess()) {
            throw new RuntimeException(String.format("fail to invoke setamcontract by send transaction %s", txInfo.getTxId()));
        }
        waitUntilTxIrreversible(txInfo);
        System.out.printf(
                "set AM contract (%s) to SDP (%s) by tx %s \n",
                contractAddress,
                this.bbcContext.getSdpContract().getContractAddress(),
                txInfo.getTxId()
        );

        // 4. check if sdp is ready
        if (isSdpReady()) {
            this.bbcContext.getSdpContract().setStatus(ContractStatusEnum.CONTRACT_READY);
        }
    }

    /**
     * 设置SDP合约中本地域名
     * <pre>
     *     1. 检查sdp合约已部署
     *     2. 调用sdp合约的`setlocaldoamin`action
     *     3. 检查交易是否执行成功
     *     4. 判断合约是否ready
     *
     *     todo:补充单测
     * </pre>
     *
     * @param domain
     */
    @Override
    public void setLocalDomain(String domain) {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (StrUtil.isEmpty(this.bbcContext.getSdpContract().getContractAddress())) {
            throw new RuntimeException("none sdp contract address");
        }

        // 2. invoke sdp contract
        SendTransactionResponse sendTransactionResponse = bbcInvokeContractsOnRpc(
                new String[][]{
                        {
                                this.bbcContext.getSdpContract().getContractAddress(),
                                SDP_SET_LOCALDOMAIN_ACTION,
                                String.format(SDP_SET_LOCALDOMAIN_PARAMETER_FORMAT,
                                        this.config.getUserName(), domain)
                        },
                }
        );
        EosTxInfo txInfo = bbcGetTxInfoByTransactionHashOnRpc(sendTransactionResponse.getTransactionId());
        if (ObjectUtil.isNull(txInfo)) {
            throw new RuntimeException(String.format("failed to get transaction info %s", sendTransactionResponse.getTransactionId()));
        }

        // 3. check transaction
        if (!txInfo.isSuccess()) {
            throw new RuntimeException(String.format("fail to invoke setlocaldomain by send transaction %s", txInfo.getTxId()));
        }
        waitUntilTxIrreversible(txInfo);
        System.out.printf(
                "set localdomain (%s) to SDP (%s) by tx %s \n",
                domain,
                this.bbcContext.getSdpContract().getContractAddress(),
                txInfo.getTxId()
        );

        // 4. update sdp contract status
        if (isSdpReady()) {
            this.bbcContext.getSdpContract().setStatus(ContractStatusEnum.CONTRACT_READY);
        }
    }

    /**
     * 判断sdp合约是否ready
     * <pre>
     *     1. am信息已经初始化
     *     2. localdomain信息已经初始化
     * </pre>
     *
     * @return
     */
    private boolean isSdpReady() {
        try {
            return ObjectUtil.isNotEmpty(bbcGetValueFromTableByKeyOnRpc(
                    this.bbcContext.getSdpContract().getContractAddress(),
                    this.bbcContext.getSdpContract().getContractAddress(),
                    SDP_INIT_INFO_TABLE,
                    BigInteger.valueOf(SDP_INIT_INFO_TABLE_KEY),
                    SDP_INIT_INFO_TABLE_VALUE_AM_NAME
            )) && ObjectUtil.isNotEmpty(bbcGetValueFromTableByKeyOnRpc(
                    this.bbcContext.getSdpContract().getContractAddress(),
                    this.bbcContext.getSdpContract().getContractAddress(),
                    SDP_INIT_INFO_TABLE,
                    BigInteger.valueOf(SDP_INIT_INFO_TABLE_KEY),
                    SDP_INIT_INFO_TABLE_VALUE_DOMAIN_NAME
            ));
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to read sdp contract status (address: %s)",
                            this.bbcContext.getSdpContract().getContractAddress()
                    ), e);
        }
    }

    /**
     * 调用AM合约方法将中继消息转发到接收链
     *
     * @param rawMessage
     * @return
     */
    @Override
    public CrossChainMessageReceipt relayAuthMessage(byte[] rawMessage) {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getAuthMessageContract())) {
            throw new RuntimeException("empty am contract in bbc context");
        }

        System.out.printf("relay AM %s to %s \n",
                HexUtil.encodeHexStr(rawMessage), this.bbcContext.getAuthMessageContract().getContractAddress());

        // 2. invoke am contract
        SendTransactionResponse sendTransactionResponse = bbcInvokeContractsOnRpc(
                new String[][]{
                        {
                                this.bbcContext.getAuthMessageContract().getContractAddress(),
                                AM_RECV_PKG_FROM_RELAYER_ACTION,
                                String.format(
                                        AM_RECV_PKG_FROM_RELAYER_PARAMETER_FORMAT,
                                        this.config.getUserName(),
                                        generateRandomPrefix() + HexUtil.encodeHexStr(rawMessage)
                                )
                        },
                }
        );
        EosTxInfo txInfo = bbcGetTxInfoByTransactionHashOnRpc(sendTransactionResponse.getTransactionId());
        if (ObjectUtil.isNull(txInfo)) {
            throw new RuntimeException(String.format("failed to get transaction info %s", sendTransactionResponse.getTransactionId()));
        }

        waitUntilTxIrreversible(txInfo);

        // 3. check transaction
        CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();

        crossChainMessageReceipt.setSuccessful(txInfo.isSuccess());
        crossChainMessageReceipt.setConfirmed(txInfo.isConfirmed());
        crossChainMessageReceipt.setTxhash(txInfo.getTxId());
        crossChainMessageReceipt.setErrorMsg(crossChainMessageReceipt.isSuccessful() ? "" : txInfo.getStatus().getStatus());

        System.out.printf("relay auth message by tx %s \n", txInfo.getTxId());

        return crossChainMessageReceipt;
    }

    private String generateRandomPrefix() {
        return StrUtil.sub(DigestUtil.sha256Hex(UUID.randomUUID().toString()), 0, 8);
    }

    // ============================== EOS合约工具方法 ==============================

    /**
     * 合约工具方法：发送异步交易调用合约方法，可以一次调用多个合约
     *
     * @param invokeParams
     * @return
     */
    private SendTransactionResponse bbcInvokeContractsOnRpc(String[][] invokeParams) {
        List<Action> actionList = new ArrayList<>();

        for (String[] infos : invokeParams) {
            if (infos.length != 3) {
                throw new RuntimeException(String.format(
                        "the parameters length should be 3 but %s", infos.length));
            }
            Action action = new Action(
                    // 合约账户名
                    infos[0].trim(),
                    // action名
                    infos[1].trim(),
                    // 调用者权限
                    Collections.singletonList(new Authorization(config.getUserName(), "active")),
                    // 合约参数
                    infos[2].trim()
            );
            actionList.add(action);
        }

        try {
            TransactionProcessor processor = buildDefaultTransactionProcessor();
            processor.prepare(actionList);
            return processor.signAndBroadcast();
        } catch (TransactionPrepareError e) {
            throw new RuntimeException("failed to prepare invoke contract action", e);
        } catch (TransactionSignAndBroadCastError e) {
            throw new RuntimeException("failed to sign and broadcast invoke contract action", e);
        }
    }

    private TransactionProcessor buildDefaultTransactionProcessor() {
        TransactionProcessor processor = session.getTransactionProcessor();

        // 2.3 Now the TransactionConfig can be altered, if desired
        TransactionConfig transactionConfig = processor.getTransactionConfig();

        // Use blocksBehind (default 3) the current head block to calculate TAPOS
        transactionConfig.setUseLastIrreversible(false);
        // Set the expiration time of transactions 600(default 300) seconds later than the timestamp
        // of the block used to calculate TAPOS
        transactionConfig.setExpiresSeconds(600);

        // Update the TransactionProcessor with the config changes
        processor.setTransactionConfig(transactionConfig);

        return processor;
    }

    /**
     * 合约工具方法：根据交易哈希查询交易回执状态
     *
     * @param txHash
     * @return
     */
    private EosTxInfo bbcGetTxInfoByTransactionHashOnRpc(String txHash) {

        String response = getTxFromEos(txHash);

        EosTxInfo txInfo = JSON.parseObject(response, EosTxInfo.class);
        txInfo.setStatus(
                EosTransactionStatusEnum.parse(
                        (String) JSONPath.extract(response, "$.trx.receipt.status")
                )
        );
        return txInfo;
    }

    private String getTxFromEos(String txHash) {
        String getTransactionRequest = String.format("{\"id\":\"%s\"}", txHash);
        RequestBody requestBody = RequestBody.create(
                getTransactionRequest,
                okhttp3.MediaType.parse("application/json; charset=utf-8")
        );

        String response;
        try {
            response = rpcProvider.getTransaction(requestBody);
        } catch (RpcProviderError e) {
            throw new RuntimeException(
                    String.format(
                            "failed to invoke getTransaction rpc (req: %s)", getTransactionRequest
                    ), e
            );
        }
        if (ObjectUtil.isEmpty(response)) {
            throw new RuntimeException(String.format("empty response for querying tx %s", txHash));
        }

        return response;
    }

    private EosBlockInfo getIrreversibleBlockInfoByHeight(Long height) {
        try {
            GetBlockResponse getBlockResponse = rpcProvider.getBlock(new GetBlockRequest(String.valueOf(height)));
            if (ObjectUtil.isNull(getBlockResponse)) {
                throw new RuntimeException(String.format("unexpected null response for querying block %d", height));
            }

            return new EosBlockInfo(
                    getBlockResponse.getTransactions().stream()
                            .map(txMap -> (String) ObjectUtil.defaultIfNull((Map) txMap.get("trx"), new HashMap()).getOrDefault("id", ""))
                            .filter(StrUtil::isNotEmpty)
                            .map(txId -> EosTxActions.convertFrom(getTxFromEos(txId)))
                            .filter(actions -> EosTransactionStatusEnum.EXECUTED == actions.getStatus())
                            .collect(Collectors.toList()),
                    DateUtil.parse(getBlockResponse.getTimestamp()).getTime(),
                    height,
                    getBlockResponse.getId()
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to getIrreversibleActionsByHeight (Height: %d, contractAddr: %s, topic: %s)",
                            height,
                            this.bbcContext.getAuthMessageContract().getContractAddress(),
                            CROSSCHAIN_ACTION
                    ), e
            );
        }
    }

    /**
     * 合约工具方法：读取合约存储表格数据
     *
     * @param contractAcc
     * @param tableScope
     * @param tableName
     * @param tableKey    主键值必须为uint64
     * @param valueName
     * @return
     */
    private Object bbcGetValueFromTableByKeyOnRpc(String contractAcc, String tableScope, String tableName, BigInteger tableKey, String valueName) {
        // 1. 构造rpc请求
        String getTableRowsRequest = String.format("{\"code\":\"%s\",\"scope\": \"%s\",\"table\":\"%s\",\"lower_bound\":\"%s\",\"upper_bound\":\"%s\",\"json\":true}",
                contractAcc,
                tableScope,
                tableName,
                tableKey.toString(),
                tableKey.toString()
        );
        RequestBody requestBody = RequestBody.create(
                getTableRowsRequest,
                okhttp3.MediaType.parse("application/json; charset=utf-8")
        );

        // 2. 发送rpc请求
        String response;
        try {
            response = rpcProvider.getTableRows(requestBody);
        } catch (RpcProviderError | RuntimeException e) {
            throw new RuntimeException(
                    String.format(
                            "failed to invoke getTableRows rpc (req: %s)", getTableRowsRequest
                    ), e
            );
        }
        if (ObjectUtil.isEmpty(response)) {
            throw new RuntimeException(String.format("empty response for getTableRows: %s", getTableRowsRequest));
        }

        // 3. 解析rpc结果，返回结果应当只有0或1行的数据，根据value名称返回value值
        return JSON.parseObject(response)
                .getJSONArray("rows")
                .getJSONObject(0)
                .get(valueName);
    }

    private Object bbcGetValueFromTableByIndexOnRpc(String contractAcc, String tableScope, String tableName, String keyType, String tableKey, String valueName, Object defaultValue) {
        String getTableRowsRequest = String.format(
                "{\"lower_bound\":\"%s\",\"upper_bound\":\"%s\",\"code\":\"%s\",\"scope\":\"%s\",\"json\":true,\"table\":\"%s\",\"index_position\":\"2\", \"key_type\":\"%s\"}",
                tableKey,
                tableKey,
                contractAcc,
                tableScope,
                tableName,
                keyType
        );
        RequestBody requestBody = RequestBody.create(
                getTableRowsRequest,
                okhttp3.MediaType.parse("application/json; charset=utf-8")
        );

        // 2. 发送rpc请求
        String response = null;
        try {
            response = rpcProvider.getTableRows(requestBody);
        } catch (RpcProviderError | RuntimeException e) {
            throw new RuntimeException(
                    String.format(
                            "failed to invoke getTableRows rpc (req: %s)", getTableRowsRequest
                    ), e
            );
        }
        if (ObjectUtil.isEmpty(response)) {
            throw new RuntimeException(String.format("empty response for getTableRows: %s", getTableRowsRequest));
        }

        // 3. 解析rpc结果，返回结果应当只有0或1行的数据
        JSONArray resArray = JSON.parseObject(response)
                .getJSONArray("rows");
        if (resArray.size() == 0 && ObjectUtil.isNotNull(defaultValue)) {
            return defaultValue;
        }
        // 根据value名称返回value值
        return resArray.getJSONObject(0).get(valueName);
    }
}
