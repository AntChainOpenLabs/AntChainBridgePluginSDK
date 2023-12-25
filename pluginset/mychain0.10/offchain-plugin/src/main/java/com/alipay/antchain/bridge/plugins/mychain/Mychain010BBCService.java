package com.alipay.antchain.bridge.plugins.mychain;

import java.math.BigInteger;
import java.util.List;

import cn.hutool.core.codec.Base64;
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
import com.alipay.antchain.bridge.plugins.mychain.contract.AMContractClientEVM;
import com.alipay.antchain.bridge.plugins.mychain.contract.AMContractClientWASM;
import com.alipay.antchain.bridge.plugins.mychain.sdk.Mychain010Client;
import com.alipay.antchain.bridge.plugins.mychain.utils.ContractUtils;
import com.alipay.antchain.bridge.plugins.mychain.utils.MychainUtils;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import com.alipay.mychain.sdk.api.utils.Utils;
import com.alipay.mychain.sdk.common.VMTypeEnum;
import com.alipay.mychain.sdk.domain.block.Block;
import com.alipay.mychain.sdk.domain.transaction.LogEntry;
import com.alipay.mychain.sdk.domain.transaction.TransactionReceipt;
import com.alipay.mychain.sdk.errorcode.ErrorCode;
import com.alipay.mychain.sdk.message.transaction.TransactionReceiptResponse;
import com.alipay.mychain.sdk.vm.EVMOutput;
import com.alipay.mychain.sdk.vm.WASMOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@BBCService(products = "mychain010", pluginId = "plugin-mychain010")
public class Mychain010BBCService implements IBBCService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mychain010BBCService.class);

    private Mychain010BBCContext context;

    private Mychain010Client mychain010Client;

    /**
     * 入参 bbcContext 可能是一个已经存在的 bbcService 的 context，即 MychainBBCContext，携带一定的插件服务信息，
     * 也可能是一个新建的 DefaultContext，携带信息存储在 confForBlockchainClient 中
     *
     * @param bbcContext the context object.
     *                   please check the comments of interface {@link AbstractBBCContext}.
     */
    @Override
    public void startup(AbstractBBCContext bbcContext) {
        LOGGER.info("[Mychain010BBCService] start up mychain0.10 bbc service, context: {}",
                JSON.toJSONString(bbcContext));

        if (ObjectUtil.isNull(bbcContext)) {
            throw new RuntimeException("[Mychain010BBCService] null bbc context");
        }

        try {
            // 1. 根据 config 构造 sdk
            mychain010Client = new Mychain010Client(bbcContext.getConfForBlockchainClient());
            if (!mychain010Client.startup()) {
                throw new RuntimeException(
                        StrUtil.format("[Mychain010BBCService] start up mychain0.10 bbc service fail for {}, isSM:{}",
                                mychain010Client.getPrimary(),
                                mychain010Client.isSMChain()));
            }

            // 2. 根据 config 初始化 context
            context = new Mychain010BBCContext(bbcContext);
            context.initContractClient(mychain010Client);

            LOGGER.info("[Mychain010BBCService] start up mychain0.10 bbc service success for {}, isSM:{}",
                    mychain010Client.getPrimary(),
                    mychain010Client.isSMChain());

        } catch (Exception e) {
            LOGGER.error("[Mychain010BBCService] start up mychain0.10 bbc service error for {}",
                    mychain010Client.getPrimary(),
                    e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] start up mychain0.10 bbc service error for {}, ",
                            mychain010Client.getPrimary()),
                    e);
        }
    }

    @Override
    public void shutdown() {
        LOGGER.info("[Mychain010BBCService] shut down mychain0.10 bbc service for {}",
                mychain010Client.getPrimary());

        mychain010Client.shutdown();
    }

    @Override
    public AbstractBBCContext getContext() {
        if (ObjectUtil.isNull(this.context)) {
            throw new RuntimeException("empty bbc context!");
        }

        LOGGER.info(StrUtil.format("[Mychain010BBCService] Mychain0.10 BBCService context: \n" +
                        " amAddr: {}, amStatus: {}, sdpAddr: {}, sdpStatus: {}",
                this.context.getAuthMessageContract() != null ? this.context.getAuthMessageContract().getContractAddress() : "",
                this.context.getAuthMessageContract() != null ? this.context.getAuthMessageContract().getStatus() : "",
                this.context.getSdpContract() != null ? this.context.getSdpContract().getContractAddress() : "",
                this.context.getSdpContract() != null ? this.context.getSdpContract().getStatus() : ""
        ));

        return this.context;
    }

    @Override
    public void setupAuthMessageContract() {
        LOGGER.info("[Mychain010BBCService] set up auth message contract for {}",
                mychain010Client.getPrimary());

        // 1. check context
        if (ObjectUtil.isNull(this.context)) {
            throw new RuntimeException("empty bbc context");
        }

        boolean isDeploy = false;
        if (mychain010Client.isTeeChain()) {
            // 2. tee链只需要部署teewasm合约
            LOGGER.info("[Mychain010BBCService] deploy am with tee wasm contract for {}",
                    mychain010Client.getPrimary());

            if (!context.getAmContractClientTeeWASM().deployContract()) {
                LOGGER.error("[Mychain010BBCService] deploy am with tee wasm contract for {} failed",
                        mychain010Client.getPrimary());
                throw new RuntimeException(StrUtil.format("[Mychain010BBCService] deploy am with tee wasm contract for {} failed",
                        mychain010Client.getPrimary()));
            } else {
                if (context.getAmContractClientTeeWASM().addRelayers(
                        mychain010Client.getConfig().getMychainAnchorAccount())) {
                    LOGGER.info("[Mychain010BBCService] deploy am with tee wasm contract for {} success",
                            mychain010Client.getPrimary());
                    isDeploy = true;
                } else {
                    LOGGER.error("[Mychain010BBCService] add relayers {} to am tee wasm for {} failed",
                            mychain010Client.getConfig().getMychainAnchorAccount(),
                            mychain010Client.getPrimary());
                    throw new RuntimeException(StrUtil.format("[Mychain010BBCService] add relayers {} to am tee wasm for {} failed",
                            mychain010Client.getConfig().getMychainAnchorAccount(),
                            mychain010Client.getPrimary()));
                }
            }
        } else {
            // 3. 部署evm合约
            LOGGER.info("[Mychain010BBCService] deploy am with evm contract for {}",
                    mychain010Client.getPrimary());
            if (!context.getAmContractClientEVM().deployContract()) {
                LOGGER.error("[Mychain010BBCService] deploy am with evm contract for {} failed",
                        mychain010Client.getPrimary());
                throw new RuntimeException(StrUtil.format("[Mychain010BBCService] deploy am with evm contract for {} failed",
                        mychain010Client.getPrimary()));
            } else {
                if (context.getAmContractClientEVM().addRelayers(
                        mychain010Client.getConfig().getMychainAnchorAccount())) {
                    LOGGER.info("[Mychain010BBCService] deploy am with evm contract for {} success",
                            mychain010Client.getPrimary());
                    isDeploy = true;
                } else {
                    LOGGER.error("[Mychain010BBCService] add relayers {} to am evm for {} failed",
                            mychain010Client.getConfig().getMychainAnchorAccount(),
                            mychain010Client.getPrimary());
                    throw new RuntimeException(StrUtil.format("[Mychain010BBCService] add relayers {} to am evm for {} failed",
                            mychain010Client.getConfig().getMychainAnchorAccount(),
                            mychain010Client.getPrimary()));
                }
            }

            // 4. 需要支持wasm，部署wasm合约
            if (mychain010Client.getConfig().isWasmSupported()) {
                LOGGER.info("[Mychain010BBCService] deploy am with wasm contract for {}",
                        mychain010Client.getPrimary());
                if (!context.getAmContractClientWASM().deployContract()) {
                    LOGGER.error("[Mychain010BBCService] deploy am with wasm contract for {} failed",
                            mychain010Client.getPrimary());
                    throw new RuntimeException(StrUtil.format("[Mychain010BBCService] deploy am with wasm contract for {} failed",
                            mychain010Client.getPrimary()));
                } else {
                    if (context.getAmContractClientWASM().addRelayers(mychain010Client.getConfig().getMychainAnchorAccount())) {
                        LOGGER.info("[Mychain010BBCService] deploy am with wasm contract for {} success",
                                mychain010Client.getPrimary());
                        isDeploy = true;
                    } else {
                        LOGGER.error("[Mychain010BBCService] add relayers {} to am wasm for {} failed",
                                mychain010Client.getConfig().getMychainAnchorAccount(),
                                mychain010Client.getPrimary());
                        throw new RuntimeException(StrUtil.format("[Mychain010BBCService] add relayers {} to am wasm for {} failed",
                                mychain010Client.getConfig().getMychainAnchorAccount(),
                                mychain010Client.getPrimary()));
                    }
                }
            }
        }

        // 设置context里面的总状态
        if (isDeploy) {
            context.setAuthMessageContract(new AuthMessageContract(
                    MychainUtils.contractAddrFormat(
                            ObjectUtil.isNotEmpty(context.getAmContractClientEVM()) ?
                                    context.getAmContractClientEVM().getContractAddress() : ""
                            ,
                            mychain010Client.isTeeChain() ?
                                    (ObjectUtil.isNotEmpty(context.getAmContractClientTeeWASM()) ?
                                            context.getAmContractClientTeeWASM().getContractAddress() : "") :
                                    (ObjectUtil.isNotEmpty(context.getAmContractClientWASM()) ?
                                            context.getAmContractClientWASM().getContractAddress() : ""))
                    ,
                    ContractStatusEnum.CONTRACT_DEPLOYED)
            );
        }
    }

    @Override
    public void setupSDPMessageContract() {
        LOGGER.info("[Mychain010BBCService] set up sdp contract for {}",
                mychain010Client.getPrimary());

        // 1. check context
        if (ObjectUtil.isNull(this.context)) {
            throw new RuntimeException("empty bbc context");
        }

        boolean isDeploy = false;
        if (mychain010Client.isTeeChain()) {
            // 2. tee链只需要部署teewasm合约
            LOGGER.info("[Mychain010BBCService] deploy sdp with tee wasm contract for {}",
                    mychain010Client.getPrimary());

            if (!context.getSdpContractClientTeeWASM().deployContract()) {
                LOGGER.error("[Mychain010BBCService] deploy sdp with tee wasm contract for {} failed",
                        mychain010Client.getPrimary());
                throw new RuntimeException(StrUtil.format("[Mychain010BBCService] deploy sdp with tee wasm contract for {} failed",
                        mychain010Client.getPrimary()));
            } else {
                LOGGER.info("[Mychain010BBCService] deploy sdp with tee wasm contract for {} success",
                        mychain010Client.getPrimary());
                isDeploy = true;
            }
        } else {
            // 3. 部署evm合约
            LOGGER.info("[Mychain010BBCService] deploy sdp with evm contract for {}",
                    mychain010Client.getPrimary());
            if (!context.getSdpContractClientEVM().deployContract()) {
                LOGGER.error("[Mychain010BBCService] deploy sdp with evm contract for {} failed",
                        mychain010Client.getPrimary());
                throw new RuntimeException(StrUtil.format("[Mychain010BBCService] deploy sdp with evm contract for {} failed",
                        mychain010Client.getPrimary()));
            } else {
                LOGGER.info("[Mychain010BBCService] deploy sdp with evm contract for {} success",
                        mychain010Client.getPrimary());
                isDeploy = true;
            }

            // 4. 需要支持wasm，部署wasm合约
            if (mychain010Client.getConfig().isWasmSupported()) {
                LOGGER.info("[Mychain010BBCService] deploy sdp with wasm contract for {}",
                        mychain010Client.getPrimary());
                if (!context.getSdpContractClientWASM().deployContract()) {
                    LOGGER.error("[Mychain010BBCService] deploy sdp with wasm contract for {} failed",
                            mychain010Client.getPrimary());
                    throw new RuntimeException(StrUtil.format("[Mychain010BBCService] deploy sdp with wasm contract for {} failed",
                            mychain010Client.getPrimary()));
                } else {
                    LOGGER.info("[Mychain010BBCService] deploy sdp with wasm contract for {} success",
                            mychain010Client.getPrimary());
                    isDeploy = true;
                }
            }
        }

        // 设置context里面的总状态
        if (isDeploy) {
            context.setSdpContract(new SDPContract(
                    MychainUtils.contractAddrFormat(
                            ObjectUtil.isNotEmpty(context.getSdpContractClientEVM()) ?
                                    context.getSdpContractClientEVM().getContractAddress() : ""
                            ,
                            mychain010Client.isTeeChain() ?
                                    (ObjectUtil.isNotEmpty(context.getSdpContractClientTeeWASM()) ?
                                            context.getSdpContractClientTeeWASM().getContractAddress() : "") :
                                    (ObjectUtil.isNotEmpty(context.getSdpContractClientWASM()) ?
                                            context.getSdpContractClientWASM().getContractAddress() : ""))
                    ,
                    ContractStatusEnum.CONTRACT_DEPLOYED)
            );
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
        LOGGER.info("[Mychain010BBCService] call am contract to set sdp for {}",
                mychain010Client.getPrimary());

        try {
            // 1. 判断 am 合约是否 ready，已 ready 不需要重复set am
            if (context.isAMReady(mychain010Client.isTeeChain())) {
                LOGGER.info("[Mychain010BBCService] am contract is ready (protocol: {}) for {}, do not need to set protocol",
                        context.getSdpContract().getContractAddress(),
                        mychain010Client.getPrimary());
                return;
            }

            // 2. 设置 protocol 信息
            doSetProtocol(protocolType);

            LOGGER.info("[Mychain010BBCService] call am to set protocol {}-{} success, sdp is ready for {}",
                    protocolAddress,
                    protocolType,
                    mychain010Client.getPrimary());

        } catch (Exception e) {
            LOGGER.error("[Mychain010BBCService] call am to set protocol {}-{} for {} failed",
                    protocolAddress,
                    protocolType,
                    mychain010Client.getPrimary(),
                    e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] call am to set protocol {}-{} for {} failed",
                            protocolAddress,
                            protocolType,
                            mychain010Client.getPrimary()),
                    e);
        }
    }

    /**
     * 由于mychain合约种类较多，上层合约地址直接从上下文中获取，不使用传入参数
     * - evm 及 teewasm 合约名称均存储在上下文的 AuthMessageContract 或 SDPContract 结构中
     * - wasm 合约名称存储上下文在 AuthMessageWasmContract 或 SDPWasmContract 结构中
     *
     * @param protocolType
     */
    private void doSetProtocol(String protocolType) {

        // 1. tee wasm
        if (mychain010Client.isTeeChain()) {
            if (context.getAmContractClientTeeWASM().setProtocol(
                    context.getSdpContractClientTeeWASM().getContractAddress(),
                    protocolType)) {
                LOGGER.info("[Mychain010BBCService] call am contract to set sdp with teewasm contract for {} success",
                        mychain010Client.getPrimary());
            } else {
                LOGGER.error("[Mychain010BBCService] call am contract to set sdp with teewasm  contract for {} failed",
                        mychain010Client.getPrimary());
                throw new RuntimeException(StrUtil.format("[Mychain010BBCService] call am contract to set sdp with teewasm  contract for {} failed",
                                mychain010Client.getPrimary()));
            }
        } else {
            // 2. evm
            if (context.getAmContractClientEVM().setProtocol(
                    context.getSdpContractClientEVM().getContractAddress(),
                    protocolType)) {
                LOGGER.info("[Mychain010BBCService] call am contract to set sdp with evm contract for {} success",
                        mychain010Client.getPrimary());
            } else {
                LOGGER.error("[Mychain010BBCService] call am contract to set sdp with evm contract for {} failed",
                        mychain010Client.getPrimary());
                throw new RuntimeException(StrUtil.format("[Mychain010BBCService] call am contract to set sdp with evm contract for {} failed",
                        mychain010Client.getPrimary()));
            }

            // 3. wasm
            if (mychain010Client.getConfig().isWasmSupported()) {
                if (context.getAmContractClientWASM().setProtocol(
                        context.getSdpContractClientWASM().getContractAddress(),
                        protocolType)) {
                    LOGGER.info("[Mychain010BBCService] call am contract to set sdp with wasm contract for {} success",
                            mychain010Client.getPrimary());
                } else {
                    LOGGER.error("[Mychain010BBCService] call am contract to set sdp with wasm contract for {} failed",
                            mychain010Client.getPrimary());
                    throw new RuntimeException(StrUtil.format("[Mychain010BBCService] call am contract to set sdp with wasm contract for {} failed",
                            mychain010Client.getPrimary()));
                }
            }
        }

        // 4. 更新总状态
        if (context.isAMReady(mychain010Client.isTeeChain())) {
            context.getAuthMessageContract().setStatus(ContractStatusEnum.CONTRACT_READY);
        }
    }


    /**
     * 调用 sdp 合约设置 am
     * 1. 检查 sdp 合约是否 ready ，已 ready 则不需要再 set am
     * 2. 检查 domain 是否已设置，未设置则结束
     * 由于 mychain 可能存在多语言合约，实际 set am 的时候根据上下文中的 am 进行 set ，
     * 故不需要记录入参 contractAddress
     * 3. domain 已设置，调用 sdp 合约设置 am 及 domain，同时更新合约状态
     *
     * @param contractAddress am contract address
     */
    @Override
    public void setAmContract(String contractAddress) {
        LOGGER.info("[Mychain010BBCService] call sdp contract to set am {} for {}",
                contractAddress,
                mychain010Client.getPrimary());

        try {
            String localDomain = mychain010Client.isTeeChain() ?
                    context.getSdpContractClientTeeWASM().getLocalDomain() :
                    context.getSdpContractClientEVM().getLocalDomain();

            // 1. 判断 sdp 合约是否ready，已ready不需要重复set am
            if (context.isSDPReady(mychain010Client.isTeeChain())) {
                LOGGER.info("[Mychain010BBCService] sdp contract is ready (am: {}, domain: {}) for {}, do not need to set am contract",
                        context.getAuthMessageContract().getContractAddress(),
                        localDomain,
                        mychain010Client.getPrimary());
                return;
            }

            // 2. 根据domain是否准备好，判断是否进行合约调用及合约状态更新
            if (StrUtil.isNotEmpty(localDomain)) {

                // 3. domain准备好，调用sdp合约执行 setAmContractAndDomain
                doSetAmAndDomain();

                LOGGER.info("[Mychain010BBCService] sdp set domain {} and am {} success, sdp is ready for {}",
                        localDomain,
                        contractAddress,
                        mychain010Client.getPrimary());
            } else {
                // domain未准备好，直接返回
                LOGGER.info("[Mychain010BBCService] domain is not set, set am contract name into context success for {}",
                        mychain010Client.getPrimary());
            }

        } catch (Exception e) {
            LOGGER.error("[Mychain010BBCService] call sdp contract to set am {} for {}",
                    contractAddress,
                    mychain010Client.getPrimary(),
                    e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] call sdp contract to set am {} for {}",
                            contractAddress,
                            mychain010Client.getPrimary()),
                    e);
        }

    }

    /**
     * 调用 sdp 合约设置 domain
     * 1. 检查 sdp 合约是否 ready ，已 ready 则不需要再 set domain
     * 2. 更新 domain 到 context
     * 3. 调用 sdp 合约设置 am 及 domain，同时更新合约状态
     * 由于 mychain 可能存在多语言合约，实际 set am 的时候根据上下文中的 am 进行 set ，
     * 故不需要检查 am 是否已 set
     *
     * @param domain the domain value
     */
    @Override
    public void setLocalDomain(String domain) {
        LOGGER.info("[Mychain010BBCService] call sdp contract to set domain {} for {}",
                domain,
                mychain010Client.getPrimary());

        try {
            String localDomain = mychain010Client.isTeeChain() ?
                    context.getSdpContractClientTeeWASM().getLocalDomain() :
                    context.getSdpContractClientEVM().getLocalDomain();
            // 1. 判断 sdp 合约是否 ready ，已 ready 不需要重复 set domain
            if (context.isSDPReady(mychain010Client.isTeeChain())) {
                LOGGER.info("[Mychain010BBCService] sdp contract is ready (am: {}, domain: {}) for {}, do not need to set domain",
                        context.getAuthMessageContract().getContractAddress(),
                        localDomain,
                        mychain010Client.getPrimary());
                return;
            }

            // 2. 更新 domain 到 context
            if (StrUtil.isNotEmpty(localDomain) &&
                    !StrUtil.equals(
                            domain,
                            localDomain)) {
                LOGGER.warn("[Mychain010BBCService] domain to set ({}) is not equal domain in context ({}) for {}. " +
                                "The new contract name will overwrite the old value in the context",
                        domain,
                        localDomain,
                        mychain010Client.getPrimary());
            }
            if (mychain010Client.isTeeChain()) {
                context.getSdpContractClientTeeWASM().setLocalDomain(domain);
            } else {
                context.getSdpContractClientEVM().setLocalDomain(domain);
                context.getSdpContractClientWASM().setLocalDomain(domain);
            }

            // 3. 调用合约执行setAmContractAndDomain
            doSetAmAndDomain();

            LOGGER.info("[Mychain010BBCService] sdp set domain {} and am {} success, sdp is ready for {}",
                    mychain010Client.isTeeChain() ?
                            context.getSdpContractClientTeeWASM().getLocalDomain() :
                            context.getSdpContractClientEVM().getLocalDomain(),
                    context.getAuthMessageContract().getContractAddress(),
                    mychain010Client.getPrimary());
        } catch (Exception e) {
            LOGGER.error("[Mychain010BBCService] fail to call sdp contract to set domain {} for {}, ",
                    domain,
                    mychain010Client.getPrimary(),
                    e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] fail to call sdp contract to set domain {} for {}, ",
                            domain,
                            mychain010Client.getPrimary()), e);
        }

    }

    private void doSetAmAndDomain() {

        // 1. teeWasm
        if (mychain010Client.isTeeChain()) {
            if (context.getSdpContractClientTeeWASM().setAmContractAndDomain(
                    context.getAmContractClientTeeWASM().getContractAddress()
            )) {
                LOGGER.info("[Mychain010BBCService] call sdp contract to set am and domain with teewasm contract for {} success",
                        mychain010Client.getPrimary());
            } else {
                LOGGER.error("[Mychain010BBCService] call sdp contract to set am and domain with teewasm contract for {} failed",
                        mychain010Client.getPrimary());
                throw new RuntimeException(StrUtil.format("[Mychain010BBCService] call sdp contract to set am and domain with teewasm contract for {} failed",
                        mychain010Client.getPrimary()));
            }
        } else {
            // 2. evm
            if (context.getSdpContractClientEVM().setAmContractAndDomain(
                    context.getAmContractClientEVM().getContractAddress())) {
                LOGGER.info("[Mychain010BBCService] call sdp contract to set am and domain with evm contract for {} success",
                        mychain010Client.getPrimary());
            } else {
                LOGGER.error("[Mychain010BBCService] call sdp contract to set am and domain with evm contract for {} failed",
                        mychain010Client.getPrimary());
                throw new RuntimeException(StrUtil.format("[Mychain010BBCService] call sdp contract to set am and domain with evm contract for {} failed",
                        mychain010Client.getPrimary()));
            }

            // 3. wasm
            if (mychain010Client.getConfig().isWasmSupported()) {
                if (context.getSdpContractClientWASM().setAmContractAndDomain(
                        context.getAmContractClientWASM().getContractAddress())) {
                    LOGGER.info("[Mychain010BBCService] call sdp contract to set am and domain with wasm contract for {} success",
                            mychain010Client.getPrimary());
                } else {
                    LOGGER.error("[Mychain010BBCService] call sdp contract to set am and domain with wasm contract for {} failed",
                            mychain010Client.getPrimary());
                    throw new RuntimeException(StrUtil.format("[Mychain010BBCService] call sdp contract to set am and domain with wasm contract for {} failed",
                            mychain010Client.getPrimary()));
                }
            }
        }

        // 4. 更新总状态
        if (context.isSDPReady(mychain010Client.isTeeChain())) {
            context.getSdpContract().setStatus(ContractStatusEnum.CONTRACT_READY);
        }
    }

    /**
     * 中继调用该接口是为了验证消息在接收链上顺序是否合法，故当前链应当是receiverDomain对应的链，to合约为mychain合约
     * 故该接口中根据to合约名称判断合约类型
     *
     * @param senderDomain   blockchain domain where sender from
     * @param fromName       sender contract name
     * @param receiverDomain blockchain domain where receiver from
     * @param toName         receiver contract name
     * @return
     */
    @Override
    public long querySDPMessageSeq(String senderDomain, String fromName, String receiverDomain, String toName) {
        LOGGER.info("[Mychain010BBCService] query SDP message seq for {}, " +
                        "senderDomain: {}, fromName: {}, receiverDomain: {}, toName: {}",
                mychain010Client.getPrimary(),
                senderDomain,
                fromName,
                receiverDomain,
                toName);

        try {
            if (mychain010Client.isTeeChain()) {
                return context.getSdpContractClientTeeWASM().queryP2PMsgSeqOnChain(
                        senderDomain,
                        fromName,
                        receiverDomain,
                        toName);
            }

            VMTypeEnum vmType = mychain010Client.getContractType(toName);
            if (ObjectUtil.isNull(vmType)) {
                throw new RuntimeException(
                        StrUtil.format("no vm type found for receiver blockchain, " +
                                        "senderDomain: {}, fromName: {}, receiverDomain: {}, toName: {}",
                                senderDomain,
                                fromName,
                                receiverDomain,
                                toName));
            }

            if (vmType == VMTypeEnum.EVM) {
                return context.getSdpContractClientEVM().queryP2PMsgSeqOnChain(
                        senderDomain,
                        fromName,
                        receiverDomain,
                        toName);
            } else if (vmType == VMTypeEnum.WASM) {
                return context.getSdpContractClientWASM().queryP2PMsgSeqOnChain(
                        senderDomain,
                        fromName,
                        receiverDomain,
                        toName);
            }
        } catch (Exception e) {
            LOGGER.error("[Mychain010BBCService] fail to query SDP message seq for {}, " +
                            "senderDomain: {}, fromName: {}, receiverDomain: {}, toName: {}",
                    mychain010Client.getPrimary(),
                    senderDomain,
                    fromName,
                    receiverDomain,
                    toName,
                    e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] fail to query SDP message seq for {}, " +
                                    "senderDomain: {}, fromName: {}, receiverDomain: {}, toName: {}",
                            mychain010Client.getPrimary(),
                            senderDomain,
                            fromName,
                            receiverDomain,
                            toName),
                    e);
        }

        return 0L;
    }

    @Override
    public CrossChainMessageReceipt readCrossChainMessageReceipt(String txhash) {
        LOGGER.info("[Mychain010BBCService] read cross chain message receipt with txHash {} for {}",
                txhash,
                mychain010Client.getPrimary());

        CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();

        try {
            TransactionReceipt receipt = mychain010Client.getTxReceiptByTxhash(txhash);

            int cnt = 10;
            while ((ObjectUtil.isNull(receipt) || receipt.getGasUsed().equals(BigInteger.ZERO)) && cnt-- > 0) {
                Thread.sleep(200);
                receipt = mychain010Client.getTxReceiptByTxhash(txhash);
            }

            crossChainMessageReceipt.setTxhash(txhash);

            // 查到交易回执即可证明出块成功，交易被确认
            crossChainMessageReceipt.setConfirmed(ObjectUtil.isNotEmpty(receipt));

            if (crossChainMessageReceipt.isConfirmed()) {
                crossChainMessageReceipt.setSuccessful(
                        ErrorCode.valueOf((int) receipt.getResult()).isSuccess());

                crossChainMessageReceipt.setErrorMsg(
                        StrUtil.format("{}: {}",
                                ErrorCode.valueOf((int) receipt.getResult()).getErrorDesc(),
                                getErrorMsgFromReceipt(receipt)));
            }
        } catch (Exception e) {
            LOGGER.error("[Mychain010BBCService] fail to read cross chain message receipt with txHash {} for {}, ",
                    txhash,
                    mychain010Client.getPrimary(),
                    e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] fail to read cross chain message receipt with txHash {} for {}, ",
                            txhash,
                            mychain010Client.getPrimary()), e);
        }

        LOGGER.info("[Mychain010BBCService] read cross chain message receipt with txHash {} for {} success, " +
                        "isConfirmed: {}, isSuccessful: {}, errorMsg: {}",
                txhash,
                mychain010Client.getPrimary(),
                crossChainMessageReceipt.isConfirmed(),
                crossChainMessageReceipt.isSuccessful(),
                crossChainMessageReceipt.getErrorMsg()
        );

        System.out.printf("read crosschain message receipt [txhash: %s, isConfirmed: %b, isSuccessful: %b, ErrorMsg: %s", crossChainMessageReceipt.getTxhash(), crossChainMessageReceipt.isConfirmed(), crossChainMessageReceipt.isSuccessful(), crossChainMessageReceipt.getErrorMsg());

        return crossChainMessageReceipt;
    }

    private String getErrorMsgFromReceipt(TransactionReceipt receipt) {
        if (ObjectUtil.isEmpty(receipt.getOutput())) {
            return "";
        }
        for (LogEntry log : receipt.getLogs()) {
            if (log.getTopics().isEmpty()) {
                continue;
            }
            if (log.getTopics().get(0).equals(AMContractClientWASM.AM_MSG_RECV_SIGN_HEX)
                    && (log.getTo().equals(
                    Utils.getIdentityByName(
                            context.getAmContractClientWASM().getContractAddress(),
                            mychain010Client.getConfig().getMychainHashType()))
                    || log.getTo().equals(
                    Utils.getIdentityByName(
                            context.getAmContractClientTeeWASM().getContractAddress(),
                            mychain010Client.getConfig().getMychainHashType()))
            )) {
                return new WASMOutput(HexUtil.encodeHexStr(receipt.getOutput())).getString();
            }
            if (log.getTopics().get(0).equals(AMContractClientEVM.AM_MSG_RECV_SIGN_HEX)
                    && log.getTo().equals(
                    Utils.getIdentityByName(
                            context.getAmContractClientEVM().getContractAddress(),
                            mychain010Client.getConfig().getMychainHashType()
                    )
            )) {
                return new EVMOutput(HexUtil.encodeHexStr(receipt.getOutput()).substring(8)).getString();
            }
        }
        return Base64.encode(receipt.getOutput());
    }

    @Override
    public List<CrossChainMessage> readCrossChainMessagesByHeight(long l) {
        LOGGER.info("[Mychain010BBCService] read cross chain message by height {} for {}",
                l,
                mychain010Client.getPrimary());

        List<CrossChainMessage> messageList = ListUtil.toList();

        try {
            Block block = mychain010Client.getBlockByHeight(l);
            if (mychain010Client.isTeeChain()) {
                messageList.addAll(context.getAmContractClientTeeWASM().parseCrossChainMessage(block));
            } else {
                messageList.addAll(context.getAmContractClientEVM().parseCrossChainMessage(block));
                if (mychain010Client.getConfig().isWasmSupported()) {
                    messageList.addAll(context.getAmContractClientWASM().parseCrossChainMessage(block));
                }
            }
        } catch (
                Exception e) {
            LOGGER.error("[Mychain010BBCService] fail to read cross chain message by height {} for {}, ",
                    l,
                    mychain010Client.getPrimary(),
                    e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] fail to read cross chain message by height {} for {}, ",
                            l,
                            mychain010Client.getPrimary()),
                    e);
        }

        LOGGER.info("[Mychain010BBCService] read cross chain message by height {} for {} success",
                l,
                mychain010Client.getPrimary());
        return messageList;
    }

    @Override
    public CrossChainMessageReceipt relayAuthMessage(byte[] pkg) {
        LOGGER.info("[Mychain010BBCService] relay AuthMessage for {}, ",
                mychain010Client.getPrimary());

        try {
            TransactionReceiptResponse response;

            if (mychain010Client.isTeeChain()) {
                response = context.getAmContractClientTeeWASM().recvPkgFromRelayer(pkg);
            } else {
                String receiverIdentity = ContractUtils.extractReceiverIdentity(pkg);

                VMTypeEnum vmType = mychain010Client.getContractType(receiverIdentity);
                if (ObjectUtil.isNull(vmType)) {
                    throw new RuntimeException(
                            StrUtil.format("no vm type found for receiver identity, chain: {}, receiver: {}",
                                    mychain010Client.getPrimary(),
                                    receiverIdentity.toString()));
                }

                if (vmType == VMTypeEnum.WASM) {
                    response = context.getAmContractClientWASM().recvPkgFromRelayer(pkg);
                } else {
                    // evm 及其他类型默认均提交给evm合约
                    response = context.getAmContractClientEVM().recvPkgFromRelayer(pkg);
                }
            }

            CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();
            crossChainMessageReceipt.setTxhash(
                    response.getTxHash().toString());
            crossChainMessageReceipt.setSuccessful(
                    response.isSuccess()
                            && ErrorCode.SUCCESS.getErrorCode() == response.getTransactionReceipt().getResult());
            crossChainMessageReceipt.setConfirmed(false);
            crossChainMessageReceipt.setErrorMsg(
                    ErrorCode.valueOf((int) response.getTransactionReceipt().getResult()).getErrorDesc());

            return crossChainMessageReceipt;
        } catch (Exception e) {
            LOGGER.error("[Mychain010BBCService] fail to relay AuthMessage for {} ",
                    mychain010Client.getPrimary(),
                    e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] fail to relay AuthMessage for {}",
                            mychain010Client.getPrimary()),
                    e);
        }
    }

    @Override
    public Long queryLatestHeight() {
        LOGGER.info("[Mychain010BBCService] query latest height for {}",
                mychain010Client.getPrimary());

        Long ret = mychain010Client.queryLatestHeight();

        LOGGER.info("[Mychain010BBCService] latest height is {} for {}",
                ret,
                mychain010Client.getPrimary());

        return ret;
    }
}