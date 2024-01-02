package com.alipay.antchain.bridge.plugins.mychain;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.HexUtil;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.plugins.mychain.sdk.Mychain010Client;
import com.alipay.antchain.bridge.plugins.mychain.sdk.Mychain010Config;
import com.alipay.antchain.bridge.plugins.mychain.utils.ContractUtils;
import com.alipay.mychain.sdk.common.VMTypeEnum;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class Mychain010BBCServiceTest {

    private static Mychain010BBCService mychain010BBCService;
    @Before
    public void init() throws Exception {
        mychain010BBCService = new Mychain010BBCService();

        AbstractBBCContext mockCtx = mychainNoContractCtx();
        mychain010BBCService.startup(mockCtx);
    }

    @After
    public void clear() {
        mychain010BBCService.shutdown();
    }

    @Test
    public void setupAuthMessageContractTest() {
        AbstractBBCContext contextBefore = mychain010BBCService.getContext();
        Assert.isNull(contextBefore.getAuthMessageContract());

        mychain010BBCService.setupAuthMessageContract();

        AbstractBBCContext contextAfter = mychain010BBCService.getContext();
        Assert.notNull(contextAfter.getAuthMessageContract());
    }

    @Test
    public void setupSDPMessageContractTest() {
        AbstractBBCContext contextBefore = mychain010BBCService.getContext();
        Assert.isNull(contextBefore.getSdpContract());

        mychain010BBCService.setupSDPMessageContract();

        AbstractBBCContext contextAfter = mychain010BBCService.getContext();
        Assert.notNull(contextAfter.getSdpContract());
    }

    @Test
    public void setProtocolTest() {
        AbstractBBCContext context1 = mychain010BBCService.getContext();
        Assert.isNull(context1.getAuthMessageContract());
        Assert.isNull(context1.getSdpContract());

        mychain010BBCService.setupAuthMessageContract();
        mychain010BBCService.setupSDPMessageContract();

        AbstractBBCContext context2 = mychain010BBCService.getContext();
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context2.getAuthMessageContract().getStatus());
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context2.getSdpContract().getStatus());

        // 第一个参数不重要，根据上下文中的合约地址进行 set 操作
        mychain010BBCService.setProtocol("", "0");

        AbstractBBCContext context3 = mychain010BBCService.getContext();
        Assert.equals(ContractStatusEnum.CONTRACT_READY, context3.getAuthMessageContract().getStatus());
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context3.getSdpContract().getStatus());
    }

    @Test
    public void setAmContractAndDomainTest() {
        AbstractBBCContext context1 = mychain010BBCService.getContext();
        Assert.isNull(context1.getAuthMessageContract());
        Assert.isNull(context1.getSdpContract());

        mychain010BBCService.setupAuthMessageContract();
        mychain010BBCService.setupSDPMessageContract();

        AbstractBBCContext context2 = mychain010BBCService.getContext();
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context2.getAuthMessageContract().getStatus());
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context2.getSdpContract().getStatus());

        // 参数不重要，根据上下文中的合约地址进行 set 操作
        mychain010BBCService.setAmContract("");

        AbstractBBCContext context3 = mychain010BBCService.getContext();
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context3.getAuthMessageContract().getStatus());
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context3.getSdpContract().getStatus());

        String testDomain = "test.domain";
        mychain010BBCService.setLocalDomain(testDomain);

        AbstractBBCContext context4 = mychain010BBCService.getContext();
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context4.getAuthMessageContract().getStatus());
        Assert.equals(ContractStatusEnum.CONTRACT_READY, context4.getSdpContract().getStatus());
    }

    @Test
    public void readCrossChainMessagesByHeightTest() {
        AbstractBBCContext context1 = mychain010BBCService.getContext();
        Assert.isNull(context1.getAuthMessageContract());

        mychain010BBCService.setupAuthMessageContract();

        AbstractBBCContext context2 = mychain010BBCService.getContext();
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context2.getAuthMessageContract().getStatus());

        List<CrossChainMessage> messageList = mychain010BBCService.readCrossChainMessagesByHeight(0);
        Assert.equals(0, messageList.size());
    }

    @Test
    public void queryLatestHeightTest() {
        Assert.notEquals(0, mychain010BBCService.queryLatestHeight());
    }

    private AbstractBBCContext mychainNoContractCtx() throws IOException {
        String jsonStr = Mychain010Config.readFileJson("mychain010_template.json");
        Mychain010Config mockConf = Mychain010Config.fromJsonString(jsonStr);

        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    @Test
    public void extractReceiverVmTypeFromAmPkgTest() throws Exception {
        String proofsHex = "000000000000014200003c01000005001401000000000e0100000000080100000000001c0000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000007be8324c6d797465737431342e32303233303931332e746573742e636861696effffffff10ad6f177909939380fe92d11ac827d59cb47d5d04ca578f6091a53e000000000000000000000000000000000000000000000000000000000000000268696e64206d65737361676520746f000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000a40000000028353428dd9f052694da89f991c90ed69907c138f7ab96bc0b240f711a0f604e0000000109001c0000006d797465737431342e32303233303931332e746573742e636861696e";
        byte[] proofs = HexUtil.decodeHex(proofsHex);

        String receiver = ContractUtils.extractReceiverIdentity(proofs);

        Mychain010Client mychain010Client = new Mychain010Client(mychain010BBCService.getContext().getConfForBlockchainClient());
        mychain010Client.startup();
        Assert.equals(VMTypeEnum.EVM, mychain010Client.getContractType(receiver));
    }
}