package com.alipay.antchain.bridge.plugins.mychain;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.AuthMessageContract;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.bbc.syscontract.PTCContract;
import com.alipay.antchain.bridge.commons.bbc.syscontract.SDPContract;
import com.alipay.antchain.bridge.commons.core.base.*;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTypeEnum;
import com.alipay.antchain.bridge.commons.core.ptc.PTCVerifyAnchor;
import com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyBlockchainTrustAnchor;
import com.alipay.antchain.bridge.commons.core.rcc.ReliableCrossChainMessage;
import com.alipay.antchain.bridge.plugins.lib.BBCService;
import com.alipay.antchain.bridge.plugins.mychain.contract.AMContractClientEVM;
import com.alipay.antchain.bridge.plugins.mychain.contract.AMContractClientWASM;
import com.alipay.antchain.bridge.plugins.mychain.sdk.Mychain010Client;
import com.alipay.antchain.bridge.plugins.mychain.utils.ContractUtils;
import com.alipay.antchain.bridge.plugins.mychain.utils.MychainUtils;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import com.alipay.mychain.sdk.api.utils.Utils;
import com.alipay.mychain.sdk.common.VMTypeEnum;
import com.alipay.mychain.sdk.domain.block.Block;
import com.alipay.mychain.sdk.domain.spv.BlockHeaderInfo;
import com.alipay.mychain.sdk.domain.transaction.LogEntry;
import com.alipay.mychain.sdk.domain.transaction.Transaction;
import com.alipay.mychain.sdk.domain.transaction.TransactionReceipt;
import com.alipay.mychain.sdk.errorcode.ErrorCode;
import com.alipay.mychain.sdk.message.query.QueryTransactionReceiptResponse;
import com.alipay.mychain.sdk.vm.EVMOutput;
import com.alipay.mychain.sdk.vm.WASMOutput;

@BBCService(products = "mychain", pluginId = "plugin-mychain")
public class Mychain010BBCService extends AbstractBBCService {

    private static final int RCC_RETRY_LIMIT = 10;

    private static final String UINT64_MAX_STR = "18446744073709551615";

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

        getBBCLogger().info("[Mychain010BBCService] start up mychain0.10 bbc service, context: {}",
                JSON.toJSONString(bbcContext));

        if (ObjectUtil.isNull(bbcContext)) {
            throw new RuntimeException("[Mychain010BBCService] null bbc context");
        }

        try {
            // 1. 根据 config 构造 sdk
            mychain010Client = new Mychain010Client(bbcContext.getConfForBlockchainClient(), getBBCLogger());
            if (!mychain010Client.startup()) {
                throw new RuntimeException(
                        StrUtil.format("[Mychain010BBCService] start up mychain0.10 bbc service fail for {}, isSM:{}",
                                mychain010Client.getPrimary(),
                                mychain010Client.isSMChain()));
            }

            // 2. 根据 config 初始化 context
            context = new Mychain010BBCContext(bbcContext, getBBCLogger());
            context.initContractClient(mychain010Client);

            getBBCLogger().info("[Mychain010BBCService] start up mychain0.10 bbc service success for {}, isSM:{}",
                    mychain010Client.getPrimary(),
                    mychain010Client.isSMChain());

        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] start up mychain0.10 bbc service error for {}",
                    ObjectUtil.isNull(mychain010Client) ? "" : mychain010Client.getPrimary(),
                    e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] start up mychain0.10 bbc service error for {}, ",
                            ObjectUtil.isNull(mychain010Client) ? "" : mychain010Client.getPrimary()),
                    e);
        }
    }

    @Override
    public void shutdown() {
        getBBCLogger().info("[Mychain010BBCService] shut down mychain0.10 bbc service for {}",
                mychain010Client.getPrimary());

        mychain010Client.shutdown();
    }

    @Override
    public AbstractBBCContext getContext() {
        if (ObjectUtil.isNull(this.context)) {
            throw new RuntimeException("empty bbc context!");
        }

        getBBCLogger().info(StrUtil.format("[Mychain010BBCService] Mychain0.10 BBCService context: \n" +
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
        getBBCLogger().info("[Mychain010BBCService] set up auth message contract for {}",
                mychain010Client.getPrimary());

        // 1. check context
        if (ObjectUtil.isNull(this.context)) {
            throw new RuntimeException("[Mychain010BBCService] empty bbc context");
        }

        try {
            if (mychain010Client.isTeeChain()) {
                // 2. tee链只需要部署teewasm合约
                getBBCLogger().info("[Mychain010BBCService] deploy am with tee wasm contract for {}",
                        mychain010Client.getPrimary());

                if (!context.getAmContractClientTeeWASM().deployContract()) {
                    throw new RuntimeException("deploy am with tee wasm contract failed");
                } else {
                    if (context.getAmContractClientTeeWASM().addRelayers(
                            mychain010Client.getConfig().getMychainAnchorAccount())) {
                        getBBCLogger().info("[Mychain010BBCService] deploy am with tee wasm contract for {} success",
                                mychain010Client.getPrimary());
                    } else {
                        throw new RuntimeException(StrUtil.format("add relayers {} to am tee wasm for {} failed",
                                mychain010Client.getConfig().getMychainAnchorAccount(),
                                mychain010Client.getPrimary()));
                    }
                }

                // 设置context里面的总状态
                if (StrUtil.isEmpty(context.getAmContractClientTeeWASM().getContractAddress())) {
                    throw new RuntimeException("empty am tee wasm contract");
                } else {
                    context.setAuthMessageContract(new AuthMessageContract(
                            MychainUtils.contractAddrFormat("", context.getAmContractClientTeeWASM().getContractAddress()),
                            ContractStatusEnum.CONTRACT_DEPLOYED)
                    );
                }
            } else {
                // 3. 部署evm合约
                getBBCLogger().info("[Mychain010BBCService] deploy am with evm contract for {}",
                        mychain010Client.getPrimary());
                if (!context.getAmContractClientEVM().deployContract()) {
                    throw new RuntimeException("deploy am with evm contract failed");
                } else {
                    if (context.getAmContractClientEVM().addRelayers(
                            mychain010Client.getConfig().getMychainAnchorAccount())) {
                        getBBCLogger().info("[Mychain010BBCService] deploy am with evm contract for {} success",
                                mychain010Client.getPrimary());
                    } else {
                        throw new RuntimeException(StrUtil.format("add relayers {} to am evm ailed",
                                mychain010Client.getConfig().getMychainAnchorAccount()));
                    }
                }

                // 4. 需要支持wasm，部署wasm合约
                if (mychain010Client.getConfig().isWasmSupported()) {
                    getBBCLogger().info("[Mychain010BBCService] deploy am with wasm contract for {}",
                            mychain010Client.getPrimary());
                    if (!context.getAmContractClientWASM().deployContract()) {
                        throw new RuntimeException("deploy am with wasm contract failed");
                    } else {
                        if (context.getAmContractClientWASM().addRelayers(mychain010Client.getConfig().getMychainAnchorAccount())) {
                            getBBCLogger().info("[Mychain010BBCService] deploy am with wasm contract for {} success",
                                    mychain010Client.getPrimary());
                        } else {
                            throw new RuntimeException(StrUtil.format("add relayers {} to am wasm failed",
                                    mychain010Client.getConfig().getMychainAnchorAccount()));
                        }
                    }
                }

                // 设置context里面的总状态
                if (StrUtil.isEmpty(context.getAmContractClientEVM().getContractAddress()) || (
                        mychain010Client.getConfig().isWasmSupported()
                                && StrUtil.isEmpty(context.getAmContractClientWASM().getContractAddress()))) {
                    throw new RuntimeException(StrUtil.format("empty am evm({}) or wasm({}) contract",
                            context.getAmContractClientEVM().getContractAddress(),
                            context.getAmContractClientWASM().getContractAddress()
                    ));
                } else {
                    context.setAuthMessageContract(new AuthMessageContract(
                            MychainUtils.contractAddrFormat(
                                    context.getAmContractClientEVM().getContractAddress(),
                                    context.getAmContractClientWASM().getContractAddress()),
                            ContractStatusEnum.CONTRACT_DEPLOYED)
                    );
                }
            }
        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] setup AuthMessageContract for {} failed",
                    mychain010Client.getPrimary(), e);
            throw new RuntimeException(StrUtil.format("[Mychain010BBCService] setup AuthMessageContract for {} failed",
                    mychain010Client.getPrimary()), e);
        }

    }

    @Override
    public void setupSDPMessageContract() {
        getBBCLogger().info("[Mychain010BBCService] set up sdp contract for {}",
                mychain010Client.getPrimary());

        // 1. check context
        if (ObjectUtil.isNull(this.context)) {
            throw new RuntimeException("[Mychain010BBCService] empty bbc context");
        }

        try {

            if (mychain010Client.isTeeChain()) {
                // 2. tee链只需要部署teewasm合约
                getBBCLogger().info("[Mychain010BBCService] deploy sdp with tee wasm contract for {}",
                        mychain010Client.getPrimary());

                if (!context.getSdpContractClientTeeWASM().deployContract()) {
                    throw new RuntimeException("deploy sdp with tee wasm contract failed");
                } else {
                    getBBCLogger().info("[Mychain010BBCService] deploy sdp with tee wasm contract for {} success",
                            mychain010Client.getPrimary());
                }

                // 设置context里面的总状态
                if (StrUtil.isEmpty(context.getSdpContractClientTeeWASM().getContractAddress())) {
                    throw new RuntimeException("empty sdp tee wasm contract");
                } else {
                    context.setSdpContract(new SDPContract(
                            MychainUtils.contractAddrFormat("", context.getSdpContractClientTeeWASM().getContractAddress()),
                            ContractStatusEnum.CONTRACT_DEPLOYED)
                    );
                }
            } else {
                // 3. 部署evm合约
                getBBCLogger().info("[Mychain010BBCService] deploy sdp with evm contract for {}",
                        mychain010Client.getPrimary());
                if (!context.getSdpContractClientEVM().deployContract()) {
                    throw new RuntimeException("deploy sdp with evm contract failed");
                } else {
                    getBBCLogger().info("[Mychain010BBCService] deploy sdp with evm contract for {} success",
                            mychain010Client.getPrimary());
                }

                // 4. 需要支持wasm，部署wasm合约
                if (mychain010Client.getConfig().isWasmSupported()) {
                    getBBCLogger().info("[Mychain010BBCService] deploy sdp with wasm contract for {}",
                            mychain010Client.getPrimary());
                    if (!context.getSdpContractClientWASM().deployContract()) {
                        throw new RuntimeException("deploy sdp with wasm contract failed");
                    } else {
                        getBBCLogger().info("[Mychain010BBCService] deploy sdp with wasm contract for {} success",
                                mychain010Client.getPrimary());
                    }
                }

                // 设置context里面的总状态
                if (StrUtil.isEmpty(context.getSdpContractClientEVM().getContractAddress()) || (
                        mychain010Client.getConfig().isWasmSupported()
                                && StrUtil.isEmpty(context.getSdpContractClientWASM().getContractAddress()))) {
                    throw new RuntimeException(StrUtil.format("empty sdp evm({}) or wasm({}) contract",
                            context.getSdpContractClientEVM().getContractAddress(),
                            context.getSdpContractClientWASM().getContractAddress()
                    ));
                } else {
                    context.setSdpContract(new SDPContract(
                            MychainUtils.contractAddrFormat(
                                    context.getSdpContractClientEVM().getContractAddress(),
                                    context.getSdpContractClientWASM().getContractAddress()),
                            ContractStatusEnum.CONTRACT_DEPLOYED)
                    );
                }
            }
        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] setup SDPMessageContract for {} failed",
                    mychain010Client.getPrimary(), e);
            throw new RuntimeException(StrUtil.format("[Mychain010BBCService] setup SDPMessageContract for {} failed",
                    mychain010Client.getPrimary()), e);
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
        getBBCLogger().info("[Mychain010BBCService] call am contract to set sdp for {}",
                mychain010Client.getPrimary());

        try {
            // 1. 判断 am 合约是否 ready，已 ready 不需要重复set am
            if (context.isAMReady(mychain010Client.isTeeChain())) {
                getBBCLogger().info("[Mychain010BBCService] am contract is ready (protocol: {}) for {}, do not need to set protocol",
                        context.getSdpContract().getContractAddress(),
                        mychain010Client.getPrimary());
                return;
            }

            // 2. 设置 protocol 信息
            doSetProtocol(protocolType);

            getBBCLogger().info("[Mychain010BBCService] call am to set protocol {}-{} success, sdp is ready for {}",
                    protocolAddress,
                    protocolType,
                    mychain010Client.getPrimary());

        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] call am to set protocol {}-{} for {} failed",
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
                getBBCLogger().info("[Mychain010BBCService] call am contract to set sdp with teewasm contract for {} success",
                        mychain010Client.getPrimary());
            } else {
                getBBCLogger().error("[Mychain010BBCService] call am contract to set sdp with teewasm  contract for {} failed",
                        mychain010Client.getPrimary());
                throw new RuntimeException(StrUtil.format("[Mychain010BBCService] call am contract to set sdp with teewasm  contract for {} failed",
                        mychain010Client.getPrimary()));
            }
        } else {
            // 2. evm
            if (context.getAmContractClientEVM().setProtocol(
                    context.getSdpContractClientEVM().getContractAddress(),
                    protocolType)) {
                getBBCLogger().info("[Mychain010BBCService] call am contract to set sdp with evm contract for {} success",
                        mychain010Client.getPrimary());
            } else {
                getBBCLogger().error("[Mychain010BBCService] call am contract to set sdp with evm contract for {} failed",
                        mychain010Client.getPrimary());
                throw new RuntimeException(StrUtil.format("[Mychain010BBCService] call am contract to set sdp with evm contract for {} failed",
                        mychain010Client.getPrimary()));
            }

            // 3. wasm
            if (mychain010Client.getConfig().isWasmSupported()) {
                if (context.getAmContractClientWASM().setProtocol(
                        context.getSdpContractClientWASM().getContractAddress(),
                        protocolType)) {
                    getBBCLogger().info("[Mychain010BBCService] call am contract to set sdp with wasm contract for {} success",
                            mychain010Client.getPrimary());
                } else {
                    getBBCLogger().error("[Mychain010BBCService] call am contract to set sdp with wasm contract for {} failed",
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
        getBBCLogger().info("[Mychain010BBCService] call sdp contract to set am {} for {}",
                contractAddress,
                mychain010Client.getPrimary());

        try {
            String localDomain = mychain010Client.isTeeChain() ?
                    context.getSdpContractClientTeeWASM().getLocalDomain() :
                    context.getSdpContractClientEVM().getLocalDomain();

            // 1. 判断 sdp 合约是否ready，已ready不需要重复set am
            if (context.isSDPReady(mychain010Client.isTeeChain())) {
                getBBCLogger().info("[Mychain010BBCService] sdp contract is ready (am: {}, domain: {}) for {}, do not need to set am contract",
                        context.getAuthMessageContract().getContractAddress(),
                        localDomain,
                        mychain010Client.getPrimary());
                return;
            }

            // 2. 根据domain是否准备好，判断是否进行合约调用及合约状态更新
            if (StrUtil.isNotEmpty(localDomain)) {

                // 3. domain准备好，调用sdp合约执行 setAmContractAndDomain
                doSetAmAndDomain();

                getBBCLogger().info("[Mychain010BBCService] sdp set domain {} and am {} success, sdp is ready for {}",
                        localDomain,
                        contractAddress,
                        mychain010Client.getPrimary());
            } else {
                // domain未准备好，直接返回
                getBBCLogger().info("[Mychain010BBCService] domain is not set, set am contract name into context success for {}",
                        mychain010Client.getPrimary());
            }

        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] call sdp contract to set am {} for {}",
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
        getBBCLogger().info("[Mychain010BBCService] call sdp contract to set domain {} for {}",
                domain,
                mychain010Client.getPrimary());

        try {
            String localDomain = mychain010Client.isTeeChain() ?
                    context.getSdpContractClientTeeWASM().getLocalDomain() :
                    context.getSdpContractClientEVM().getLocalDomain();
            // 1. 判断 sdp 合约是否 ready ，已 ready 不需要重复 set domain
            if (context.isSDPReady(mychain010Client.isTeeChain())) {
                getBBCLogger().info("[Mychain010BBCService] sdp contract is ready (am: {}, domain: {}) for {}, do not need to set domain",
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
                getBBCLogger().warn("[Mychain010BBCService] domain to set ({}) is not equal domain in context ({}) for {}. " +
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

            getBBCLogger().info("[Mychain010BBCService] sdp set domain {} and am {} success, sdp is ready for {}",
                    mychain010Client.isTeeChain() ?
                            context.getSdpContractClientTeeWASM().getLocalDomain() :
                            context.getSdpContractClientEVM().getLocalDomain(),
                    context.getAuthMessageContract().getContractAddress(),
                    mychain010Client.getPrimary());
        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] fail to call sdp contract to set domain {} for {}, ",
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
                getBBCLogger().info("[Mychain010BBCService] call sdp contract to set am and domain with teewasm contract for {} success",
                        mychain010Client.getPrimary());
            } else {
                getBBCLogger().error("[Mychain010BBCService] call sdp contract to set am and domain with teewasm contract for {} failed",
                        mychain010Client.getPrimary());
                throw new RuntimeException(StrUtil.format("[Mychain010BBCService] call sdp contract to set am and domain with teewasm contract for {} failed",
                        mychain010Client.getPrimary()));
            }
        } else {
            // 2. evm
            if (context.getSdpContractClientEVM().setAmContractAndDomain(
                    context.getAmContractClientEVM().getContractAddress())) {
                getBBCLogger().info("[Mychain010BBCService] call sdp contract to set am and domain with evm contract for {} success",
                        mychain010Client.getPrimary());
            } else {
                getBBCLogger().error("[Mychain010BBCService] call sdp contract to set am and domain with evm contract for {} failed",
                        mychain010Client.getPrimary());
                throw new RuntimeException(StrUtil.format("[Mychain010BBCService] call sdp contract to set am and domain with evm contract for {} failed",
                        mychain010Client.getPrimary()));
            }

            // 3. wasm
            if (mychain010Client.getConfig().isWasmSupported()) {
                if (context.getSdpContractClientWASM().setAmContractAndDomain(
                        context.getAmContractClientWASM().getContractAddress())) {
                    getBBCLogger().info("[Mychain010BBCService] call sdp contract to set am and domain with wasm contract for {} success",
                            mychain010Client.getPrimary());
                } else {
                    getBBCLogger().error("[Mychain010BBCService] call sdp contract to set am and domain with wasm contract for {} failed",
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
        getBBCLogger().info("[Mychain010BBCService] query SDP message seq for {}, " +
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
            getBBCLogger().error("[Mychain010BBCService] fail to query SDP message seq for {}, " +
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
    public CrossChainMessageReceipt readCrossChainMessageReceipt(String txHash) {
        getBBCLogger().info("[Mychain010BBCService] read cross chain message receipt with txHash {} for {}",
                txHash,
                mychain010Client.getPrimary());

        CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();

        try {
            /***
             ATTENTION: 2.0/3.0 交易查询严格来说有三种情况
             1. 确定上链: response为success，并且有不为0的区块高度
             2. 确定没上链：
             - response的错误码为 SERVICE_QUERY_NO_RESULT 且 区块高度不为`UINT64MAX`（v3中若查询有问题会将区块高度设置为`UINT64MAX`返回SERVICE_QUERY_NO_RESULT）
             - response的错误码为 SERVICE_TX_VERIFY_FAILED
             3. 不确定上没上链： 其他，需要重试查询
             */

            QueryTransactionReceiptResponse response = mychain010Client.getTxReceiptByTxhash(txHash);
            if (response != null && response.isSuccess()) {
                // 1. 确定上链，区块高度大于0认为收录进区块
                if (response.getBlockNumber().longValue() > 0) {
                    crossChainMessageReceipt.setConfirmed(true);
                    // 落块的时候更新交易hash
                    crossChainMessageReceipt.setTxhash(txHash);
                    // 交易是否成功根据result判断
                    crossChainMessageReceipt.setSuccessful(response.getTransactionReceipt().getResult() == 0);
                    crossChainMessageReceipt.setErrorMsg(getErrorMsgFromReceipt(response.getTransactionReceipt()));
                } else {
                    // 理论上不存在这种情况
                    crossChainMessageReceipt.setConfirmed(false);
                    crossChainMessageReceipt.setSuccessful(false);
                }
            } else if (
                    response != null &&
                            (response.getErrorCode() == ErrorCode.SERVICE_TX_VERIFY_FAILED ||
                                    (response.getErrorCode() == ErrorCode.SERVICE_QUERY_NO_RESULT &&
                                            !response.getBlockNumber().equals(new BigInteger(UINT64_MAX_STR))))) {
                // 2. 确定没上链，重发交易（如交易过期会换号重发）
                crossChainMessageReceipt.setConfirmed(false);
                crossChainMessageReceipt.setSuccessful(false);
            } else {
                // 3. 不确定是否上链，重试查询
                getBBCLogger().warn("can not find tx {}'s result, try later", txHash);
                crossChainMessageReceipt.setConfirmed(false);
                crossChainMessageReceipt.setSuccessful(true);
            }
        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] fail to read cross chain message receipt with txHash {} for {}, ",
                    txHash,
                    mychain010Client.getPrimary(),
                    e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] fail to read cross chain message receipt with txHash {} for {}, ",
                            txHash,
                            mychain010Client.getPrimary()), e);
        }

        getBBCLogger().info("[Mychain010BBCService] read cross chain message receipt with txHash {} for {} success, " +
                        "isConfirmed: {}, isSuccessful: {}, errorMsg: {}",
                txHash,
                mychain010Client.getPrimary(),
                crossChainMessageReceipt.isConfirmed(),
                crossChainMessageReceipt.isSuccessful(),
                crossChainMessageReceipt.getErrorMsg()
        );

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
    public List<CrossChainMessage> readCrossChainMessagesByHeight(long height) {
        getBBCLogger().info("[Mychain010BBCService] read cross chain message by height {} for {}",
                height,
                mychain010Client.getPrimary());

        List<CrossChainMessage> messageList = ListUtil.toList();

        try {
            Block block = mychain010Client.getBlockByHeight(height);
            if (mychain010Client.isTeeChain()) {
                messageList.addAll(context.getAmContractClientTeeWASM().parseCrossChainMessage(block));
            } else {
                messageList.addAll(context.getAmContractClientEVM().parseCrossChainMessage(block));
                if (mychain010Client.getConfig().isWasmSupported()) {
                    messageList.addAll(context.getAmContractClientWASM().parseCrossChainMessage(block));
                }
            }
        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] fail to read cross chain message by height {} for {}, ",
                    height,
                    mychain010Client.getPrimary(),
                    e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] fail to read cross chain message by height {} for {}, ",
                            height,
                            mychain010Client.getPrimary()),
                    e);
        }

        getBBCLogger().info("[Mychain010BBCService] read cross chain message by height {} for {} success",
                height,
                mychain010Client.getPrimary());
        return messageList;
    }

    @Override
    public CrossChainMessageReceipt relayAuthMessage(byte[] pkg) {
        getBBCLogger().info("[Mychain010BBCService] relay AuthMessage for {}, ",
                mychain010Client.getPrimary());

        try {
            SendResponseResult response;

            if (mychain010Client.isTeeChain()) {
                response = context.getAmContractClientTeeWASM().recvPkgFromRelayer(pkg);
            } else {
                String receiverIdentity = ContractUtils.extractReceiverIdentity(pkg);
                VMTypeEnum vmType = VMTypeEnum.EVM;
                // Zero receiver means that this msg is for block state sync, which is not supported by wasm now.
                // Remember to make it on WASM contracts.
                if (!StrUtil.equals(CrossChainIdentity.ZERO_ID.toHex(), receiverIdentity)) {
                    vmType = mychain010Client.getContractType(receiverIdentity);
                    if (ObjectUtil.isNull(vmType)) {
                        throw new RuntimeException(
                                StrUtil.format("no vm type found for receiver identity, chain: {}, receiver: {}",
                                        mychain010Client.getPrimary(),
                                        receiverIdentity.toString()));
                    }
                }

                if (vmType == VMTypeEnum.WASM) {
                    response = context.getAmContractClientWASM().recvPkgFromRelayer(pkg);
                } else {
                    // evm 及其他类型默认均提交给evm合约
                    response = context.getAmContractClientEVM().recvPkgFromRelayer(pkg);
                }
            }

            CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();
            crossChainMessageReceipt.setTxhash(response.getTxId());
            crossChainMessageReceipt.setSuccessful(response.isSuccess());
            crossChainMessageReceipt.setConfirmed(false);
            crossChainMessageReceipt.setErrorMsg(response.getErrorMessage());
            crossChainMessageReceipt.setTxTimestamp(response.getTxTimestamp());
            crossChainMessageReceipt.setRawTx(response.getRawTx());

            getBBCLogger().info(
                    StrUtil.format("[Mychain010BBCService] relay AuthMessage result, " +
                                    "hash: {}, successful: {}, confirmed: {}, errMsg: {}, txTimestamp: {}",
                            crossChainMessageReceipt.getTxhash(),
                            crossChainMessageReceipt.isSuccessful(),
                            crossChainMessageReceipt.isConfirmed(),
                            crossChainMessageReceipt.getErrorMsg(),
                            crossChainMessageReceipt.getTxTimestamp()));

            return crossChainMessageReceipt;
        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] fail to relay AuthMessage for {} ",
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
        getBBCLogger().info("[Mychain010BBCService] query latest height for {}",
                mychain010Client.getPrimary());

        Long ret = mychain010Client.queryLatestHeight();

        getBBCLogger().info("[Mychain010BBCService] latest height is {} for {}",
                ret,
                mychain010Client.getPrimary());

        return ret;
    }

    @Override
    public ConsensusState readConsensusState(BigInteger height) {
        getBBCLogger().info("[Mychain010BBCService] read consensus state with height {}", height);
        if (ObjectUtil.isNull(context.getAuthMessageContract())
                || context.getAuthMessageContract().getStatus().ordinal() < ContractStatusEnum.CONTRACT_DEPLOYED.ordinal()) {
            throw new RuntimeException("auth message contract is not deployed");
        }

        try {
            BlockHeaderInfo blockHeaderInfo = mychain010Client.getRawBlockHeaderInfoByHeight(height);
            if (ObjectUtil.isNull(blockHeaderInfo)) {
                throw new RuntimeException(StrUtil.format("none header found for height {}", height.toString()));
            }
            return new ConsensusState(
                    height,
                    blockHeaderInfo.getBlockHeader().getHash().getData(),
                    blockHeaderInfo.getBlockHeader().getParentHash().getData(),
                    blockHeaderInfo.getBlockHeader().getTimestamp(),
                    blockHeaderInfo.getBlockHeader().toString().getBytes(),
                    mychain010Client.generateConsensusNodeInfo(
                            context.getAuthMessageContract().getContractAddress(),
                            blockHeaderInfo.getBlockHeader()
                    ).encode(),
                    blockHeaderInfo.getProof().toString().getBytes()
            );
        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] failed to read consensus state {}", height.toString(), e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] failed to read consensus state {}", height.toString()), e
            );
        }
    }

    @Override
    public boolean hasTpBta(CrossChainLane tpbtaLane, int tpBtaVersion) {
        if (context.getPtcContract().getStatus() != ContractStatusEnum.CONTRACT_READY) {
            throw new RuntimeException("ptc contract is not ready");
        }

        getBBCLogger().info("[Mychain010BBCService] call ptc hub to check if tpbta {}:{} exist", tpbtaLane.getLaneKey(), tpBtaVersion);
        try {
            return context.getPtcContractEvm().hasTpBta(tpbtaLane, tpBtaVersion);
        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] call ptc hub to check if tpbta {}:{} exist failed",
                    tpbtaLane.getLaneKey(), tpBtaVersion, e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] call ptc hub to check if tpbta {}:{} exist failed", tpbtaLane.getLaneKey(), tpBtaVersion), e
            );
        }
    }

    @Override
    public ThirdPartyBlockchainTrustAnchor getTpBta(CrossChainLane tpbtaLane, int tpBtaVersion) {
        if (context.getPtcContract().getStatus() != ContractStatusEnum.CONTRACT_READY) {
            throw new RuntimeException("ptc contract is not ready");
        }

        getBBCLogger().info("[Mychain010BBCService] call ptc hub to get tpbta {}:{}", tpbtaLane.getLaneKey(), tpBtaVersion);
        try {
            return context.getPtcContractEvm().getTpBta(tpbtaLane, tpBtaVersion);
        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] call ptc hub to get tpbta {}:{} failed",
                    tpbtaLane.getLaneKey(), tpBtaVersion, e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] call ptc hub to get tpbta {}:{} failed", tpbtaLane.getLaneKey(), tpBtaVersion), e
            );
        }
    }

    @Override
    public Set<PTCTypeEnum> getSupportedPTCType() {
        if (context.getPtcContract().getStatus() != ContractStatusEnum.CONTRACT_READY) {
            throw new RuntimeException("ptc contract is not ready");
        }

        try {
            return context.getPtcContractEvm().getSupportedPTCTypes();
        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] call ptc hub to get supported ptc types failed", e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] call ptc hub to get supported ptc types failed"), e
            );
        }
    }

    @Override
    public PTCTrustRoot getPTCTrustRoot(ObjectIdentity ptcOwnerOid) {
        if (context.getPtcContract().getStatus() != ContractStatusEnum.CONTRACT_READY) {
            throw new RuntimeException("ptc contract is not ready");
        }

        getBBCLogger().info("[Mychain010BBCService] call ptc hub to get ptc trust root for ptc owner oid {}", ptcOwnerOid.toHex());
        try {
            return context.getPtcContractEvm().getPTCTrustRoot(ptcOwnerOid);
        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] call ptc hub to get ptc trust root for ptc owner oid {} failed",
                    ptcOwnerOid.toHex(), e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] call ptc hub to get ptc verify anchor for ptc owner oid {} failed",
                            ptcOwnerOid.toHex()), e
            );
        }
    }

    @Override
    public boolean hasPTCTrustRoot(ObjectIdentity ptcOwnerOid) {
        if (context.getPtcContract().getStatus() != ContractStatusEnum.CONTRACT_READY) {
            throw new RuntimeException("ptc contract is not ready");
        }

        getBBCLogger().info("[Mychain010BBCService] call ptc hub to check if ptc trust root for ptc owner oid {} exist", ptcOwnerOid.toHex());
        try {
            return context.getPtcContractEvm().hasPTCTrustRoot(ptcOwnerOid);
        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] call ptc hub to check if ptc trust root for ptc owner oid {} exist failed",
                    ptcOwnerOid.toHex(), e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] call ptc hub to check if ptc verify anchor for ptc owner oid {} exist failed",
                            ptcOwnerOid.toHex()), e
            );
        }
    }

    @Override
    public PTCVerifyAnchor getPTCVerifyAnchor(ObjectIdentity ptcOwnerOid, BigInteger version) {
        if (context.getPtcContract().getStatus() != ContractStatusEnum.CONTRACT_READY) {
            throw new RuntimeException("ptc contract is not ready");
        }

        getBBCLogger().info("[Mychain010BBCService] call ptc hub to get ptc verify anchor for ptc owner oid {} and version {} exist",
                ptcOwnerOid.toHex(), version.toString());
        try {
            return context.getPtcContractEvm().getPTCVerifyAnchor(ptcOwnerOid, version);
        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] call ptc hub to get ptc verify anchor for ptc owner oid {} and version {} failed",
                    ptcOwnerOid.toHex(), version, e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] call ptc hub to get ptc verify anchor for ptc owner oid {} and version {} failed",
                            ptcOwnerOid.toHex(), version), e
            );
        }
    }

    @Override
    public boolean hasPTCVerifyAnchor(ObjectIdentity ptcOwnerOid, BigInteger version) {
        if (context.getPtcContract().getStatus() != ContractStatusEnum.CONTRACT_READY) {
            throw new RuntimeException("ptc contract is not ready");
        }

        getBBCLogger().info("[Mychain010BBCService] call ptc hub to check if ptc verify anchor for ptc owner oid {} and version {} exist",
                ptcOwnerOid.toHex(), version.toString());
        try {
            return context.getPtcContractEvm().hasPTCVerifyAnchor(ptcOwnerOid, version);
        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] call ptc hub to check if ptc verify anchor for ptc owner oid {} and version {} exist failed",
                    ptcOwnerOid.toHex(), version, e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] call ptc hub to check if ptc verify anchor for ptc owner oid {} and version {} exist failed",
                            ptcOwnerOid.toHex(), version), e
            );
        }
    }

    @Override
    public void setupPTCContract() {
        getBBCLogger().info("[Mychain010BBCService] set up ptc hub contract for {}", mychain010Client.getPrimary());

        if (ObjectUtil.isNull(this.context)) {
            throw new RuntimeException("[Mychain010BBCService] empty bbc context");
        }

        try {
            getBBCLogger().info("[Mychain010BBCService] deploy ptc with evm contract for {}", mychain010Client.getPrimary());
            if (!context.getPtcContractEvm().deployContract(this.mychain010Client.getConfig().getBcdnsRootCertPem())) {
                throw new RuntimeException("deploy ptc hub with evm contract failed");
            }

            if (StrUtil.isEmpty(context.getPtcContractEvm().getContractAddress())) {
                throw new RuntimeException("empty ptc hub contract in context after deployment");
            }

            PTCContract ptcContract = new PTCContract();
            ptcContract.setContractAddress(MychainUtils.contractAddrFormat(context.getPtcContractEvm().getContractAddress(), ""));
            ptcContract.setStatus(context.getPtcContractEvm().getStatus());
            context.setPtcContract(ptcContract);

        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] setup ptc hub for {} failed", mychain010Client.getPrimary(), e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] setup ptc hub for {} failed", mychain010Client.getPrimary()),
                    e
            );
        }
    }

    @Override
    public void setPtcContract(String ptcContractAddress) {
        getBBCLogger().info("[Mychain010BBCService] call am contract to set ptc hub for {}", mychain010Client.getPrimary());

        try {
            // 1. 判断 am 合约是否 ready，已 ready 不需要重复set am
            if (context.isAMReady(mychain010Client.isTeeChain())) {
                getBBCLogger().info("[Mychain010BBCService] am contract is ready that no need to set ptc hub");
                return;
            }

            // 1. tee wasm
            if (mychain010Client.isTeeChain()) {
                context.getAmContractClientTeeWASM().setPtcHub(context.getPtcContractEvm().getContractAddress());
            } else {
                context.getAmContractClientEVM().setPtcHub(context.getPtcContractEvm().getContractAddress());
                // 3. wasm
                if (mychain010Client.getConfig().isWasmSupported()) {
                    context.getAmContractClientWASM().setPtcHub(context.getPtcContractEvm().getContractAddress());
                }
            }

            // 4. 更新总状态
            if (context.isAMReady(mychain010Client.isTeeChain())) {
                context.getAuthMessageContract().setStatus(ContractStatusEnum.CONTRACT_READY);
            }

            getBBCLogger().info("[Mychain010BBCService] call am to set ptc hub {} success, AM status is {} for {}",
                    context.getPtcContractEvm().getContractAddress(),
                    context.getAuthMessageContract().getStatus(),
                    mychain010Client.getPrimary()
            );
        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] call am to set ptc hub {} for {} failed",
                    context.getPtcContractEvm().getContractAddress(),
                    mychain010Client.getPrimary(),
                    e
            );
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] call am to set ptc hub {} for {} failed",
                            context.getPtcContractEvm().getContractAddress(),
                            mychain010Client.getPrimary()
                    ), e
            );
        }
    }

    @Override
    public void updatePTCTrustRoot(PTCTrustRoot ptcTrustRoot) {
        if (context.getPtcContract().getStatus() != ContractStatusEnum.CONTRACT_READY) {
            throw new RuntimeException("ptc contract is not ready");
        }

        getBBCLogger().info("[Mychain010BBCService] call ptc hub to update trust root for ptc owner oid {}",
                ptcTrustRoot.getPtcCredentialSubject().getApplicant().toHex());
        try {
            context.getPtcContractEvm().updatePTCTrustRoot(ptcTrustRoot);
            getBBCLogger().info("[Mychain010BBCService] call ptc hub to update trust root for ptc owner oid {} success",
                    ptcTrustRoot.getPtcCredentialSubject().getApplicant().toHex());
        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] call ptc hub to update trust root for ptc owner oid {} failed",
                    ptcTrustRoot.getPtcCredentialSubject().getApplicant().toHex(), e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] call ptc hub to update trust root for ptc owner oid {} failed",
                            ptcTrustRoot.getPtcCredentialSubject().getApplicant().toHex()), e
            );
        }
    }

    @Override
    public void addTpBta(ThirdPartyBlockchainTrustAnchor tpbta) {
        if (context.getPtcContract().getStatus() != ContractStatusEnum.CONTRACT_READY) {
            throw new RuntimeException("ptc contract is not ready");
        }

        getBBCLogger().info("[Mychain010BBCService] call ptc hub to add TpBta {}:{}",
                tpbta.getCrossChainLane().getLaneKey(), tpbta.getTpbtaVersion());
        try {
            context.getPtcContractEvm().addTpBta(tpbta);
            getBBCLogger().info("[Mychain010BBCService] call ptc hub to add TpBta {}:{} success",
                    tpbta.getCrossChainLane().getLaneKey(), tpbta.getTpbtaVersion());
        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] call ptc hub to add TpBta {}:{} failed",
                    tpbta.getCrossChainLane().getLaneKey(), tpbta.getTpbtaVersion(), e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] call ptc hub to add TpBta {}:{} failed",
                            tpbta.getCrossChainLane().getLaneKey(), tpbta.getTpbtaVersion()), e
            );
        }
    }

    /**
     * 查询SDP合约中记录的最新已验证区块高度
     *
     * @param recvDomain 接收链域名
     * @return
     */
    @Override
    public BlockState queryValidatedBlockStateByDomain(CrossChainDomain recvDomain) {
        getBBCLogger().info("[Mychain010BBCService] query block state for {} by domain {}",
                mychain010Client.getPrimary(),
                recvDomain);

        try {
            if (mychain010Client.isTeeChain()) {
                return context.getSdpContractClientTeeWASM().queryValidatedBlockStateByDomain(recvDomain.getDomain());
            } else {
                return context.getSdpContractClientEVM().queryValidatedBlockStateByDomain(recvDomain.getDomain());
            }
        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] fail to query block state for {} by domain {}",
                    mychain010Client.getPrimary(),
                    recvDomain,
                    e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] fail to query block state for {} by domain {}",
                            mychain010Client.getPrimary(),
                            recvDomain),
                    e);
        }
    }

    /**
     * 提交链下异常消息
     *
     * @param exceptionMsgAuthor 异常跨链消息原发送合约
     * @param exceptionMsgPkg    异常跨链消息原文（SDP消息序列化）
     * @return
     */
    @Override
    public CrossChainMessageReceipt recvOffChainException(String exceptionMsgAuthor, byte[] exceptionMsgPkg) {
        getBBCLogger().info("[Mychain010BBCService] recv off-chain exception for {} with exception msg pkg {} and exception msg author {}",
                mychain010Client.getPrimary(),
                exceptionMsgAuthor,
                exceptionMsgPkg);

        try {
            SendResponseResult response;

            if (mychain010Client.isTeeChain()) {
                response = context.getSdpContractClientTeeWASM().recvOffChainException(exceptionMsgAuthor, exceptionMsgPkg);
            } else {
                VMTypeEnum vmType = mychain010Client.getContractType(exceptionMsgAuthor);
                if (ObjectUtil.isNull(vmType)) {
                    throw new RuntimeException(
                            StrUtil.format("no vm type found for exception msg author, chain: {}, author: {}",
                                    mychain010Client.getPrimary(),
                                    exceptionMsgAuthor));
                }

                if (vmType == VMTypeEnum.WASM) {
                    response = context.getSdpContractClientWASM().recvOffChainException(exceptionMsgAuthor, exceptionMsgPkg);
                } else {
                    // evm 及其他类型默认均提交给evm合约
                    response = context.getSdpContractClientEVM().recvOffChainException(exceptionMsgAuthor, exceptionMsgPkg);
                }
            }

            CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();
            crossChainMessageReceipt.setTxhash(response.getTxId());
            crossChainMessageReceipt.setSuccessful(response.isSuccess());
            crossChainMessageReceipt.setConfirmed(response.isConfirmed());
            crossChainMessageReceipt.setErrorMsg(response.getErrorMessage());
            crossChainMessageReceipt.setTxTimestamp(response.getTxTimestamp());
            crossChainMessageReceipt.setRawTx(response.getRawTx());

            return crossChainMessageReceipt;
        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] fail to recv off-chain exception for {} with exception msg pkg {} and exception msg author {}",
                    mychain010Client.getPrimary(),
                    exceptionMsgAuthor,
                    exceptionMsgPkg,
                    e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] fail to relay AuthMessage {} with exception msg pkg {} and exception msg author {}",
                            mychain010Client.getPrimary(),
                            exceptionMsgAuthor,
                            exceptionMsgPkg),
                    e);
        }
    }

    /**
     * 可靠上链重试
     *
     * @param rccMsg
     * @return
     */
    @Override
    public CrossChainMessageReceipt reliableRetry(ReliableCrossChainMessage rccMsg) {
        try {
            getBBCLogger().info("[Mychain010BBCService] reliable retry cross chain message: {} for {}",
                    rccMsg.getInfo(),
                    mychain010Client.getPrimary());

            CrossChainMessageReceipt receipt = new CrossChainMessageReceipt();

            // 判断是否达到重试上限
            if (RCC_RETRY_LIMIT > 0 && rccMsg.getRetryTime() > RCC_RETRY_LIMIT) {
                receipt.setSuccessful(false);
                receipt.setErrorMsg(StrUtil.format("retry times reach the limit {}", RCC_RETRY_LIMIT));
                return receipt;
            }

            long currentTimestamp = mychain010Client.getLatestBlockTimestamp();
            // 防止变小
            if (context.getLatestBlockTimestamp() < currentTimestamp) {
                context.setLatestBlockTimestamp(currentTimestamp);
            }

            CrossChainMessageReceipt oldReceipt = this.readCrossChainMessageReceipt(rccMsg.getCurrentHash());

            if (rccMsg.getTxTimestamp() != 0 &&
                    rccMsg.getTxTimestamp() + context.getNonceBefore() < context.getLatestBlockTimestamp()) {
                // 过期
                if (!oldReceipt.isConfirmed()) {
                    // 过期且无确定回执，换号重试
                    Transaction newTx = mychain010Client.refreshTransaction(rccMsg.getRawTx());

                    getBBCLogger().error("cross chain message {} is expired, retry submit with new hash {}",
                            rccMsg.getIdempotentInfo().getInfo(),
                            newTx.getHash().hexStrValue());

                    return mychain010Client.submitSyncTransactionByRawTx(newTx.toRlp());
                } else {
                    // 过期且有确定回执，更新可靠上链记录消息到终态（返回消息后，中继直接更新可靠上链记录和SDP表记录）
                    getBBCLogger().info("cross chain message {} is expired but find result {}-{}-{} with hash {}, return it",
                            rccMsg.getIdempotentInfo().getInfo(),
                            oldReceipt.isSuccessful(),
                            oldReceipt.isConfirmed(),
                            oldReceipt.getErrorMsg(),
                            rccMsg.getCurrentHash());

                    // 确定交易上链时需要更新交易时间戳
                    oldReceipt.setTxTimestamp(rccMsg.getTxTimestamp());
                    return oldReceipt;
                }
            } else {
                // 未过期
                if (oldReceipt.isConfirmed() || oldReceipt.isSuccessful()) {
                    // 上链且有确定结果 || 上链但处理中
                    getBBCLogger().info("cross chain message {} is not expired and find result {}-{}-{} with hash {}, return it",
                            rccMsg.getIdempotentInfo().getInfo(),
                            oldReceipt.isSuccessful(),
                            oldReceipt.isConfirmed(),
                            oldReceipt.getErrorMsg(),
                            rccMsg.getCurrentHash());

                    // 确定交易上链时需要更新交易时间戳
                    if (oldReceipt.isConfirmed()) {
                        oldReceipt.setTxTimestamp(rccMsg.getTxTimestamp());
                    }

                    return oldReceipt;
                } else {
                    // 确定未上链，不换号重试
                    getBBCLogger().info("cross chain message {} is not expired but find no result, retry submit with hash {}",
                            rccMsg.getIdempotentInfo().getInfo(),
                            rccMsg.getCurrentHash());

                    return mychain010Client.submitSyncTransactionByRawTx(rccMsg.getRawTx());
                }
            }
        } catch (Exception e) {
            getBBCLogger().error("[Mychain010BBCService] fail to retry relay AuthMessage for {} with rccMsg {}",
                    mychain010Client.getPrimary(),
                    rccMsg.getIdempotentInfo(),
                    e);
            throw new RuntimeException(
                    StrUtil.format("[Mychain010BBCService] fail to retry relay AuthMessage for {} with rccMsg {}",
                            mychain010Client.getPrimary(),
                            rccMsg.getIdempotentInfo()),
                    e);
        }
    }
}