package com.ali.antchain.Test;

import cn.hutool.core.util.HexUtil;
import com.ali.antchain.abi.SDPMsg;
import com.ali.antchain.service.EthereumBBCService;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import org.web3j.tx.gas.DefaultGasProvider;

public class ETHSetAmContractAndLocalDomainTest {

    public static void run(AbstractBBCContext context) throws Exception {
        ETHSetAmContractAndLocalDomainTest ETHSetAmContractAndLocalDomainTest = new ETHSetAmContractAndLocalDomainTest();
        ETHSetAmContractAndLocalDomainTest.setamcontractandlocaldomain(context);
    }
    public void setamcontractandlocaldomain(AbstractBBCContext  context) throws Exception {
        EthereumBBCService ethereumBBCService = new EthereumBBCService();
        // start up
        ethereumBBCService.startup(context);

        // set up am
        ethereumBBCService.setupAuthMessageContract();

        // set up sdp
        ethereumBBCService.setupSDPMessageContract();

        // get context
        AbstractBBCContext ctx = ethereumBBCService.getContext();

        System.out.println(ctx.getAuthMessageContract().getStatus());
        System.out.println(ctx.getSdpContract().getStatus());

        // set am to sdp
        ethereumBBCService.setAmContract(ctx.getAuthMessageContract().getContractAddress());

        String amAddr = SDPMsg.load(
                ethereumBBCService.getBbcContext().getSdpContract().getContractAddress(),
                ethereumBBCService.getWeb3j(),
                ethereumBBCService.getCredentials(),
                new DefaultGasProvider()
        ).getAmAddress().send();
        System.out.println("amAddr: {"+amAddr+"}");

        // check contract status
        ctx = ethereumBBCService.getContext();
        System.out.println(ctx.getSdpContract().getStatus());

        // set the domain
        ethereumBBCService.setLocalDomain("receiverDomain");

        byte[] rawDomain = SDPMsg.load(
                ethereumBBCService.getBbcContext().getSdpContract().getContractAddress(),
                ethereumBBCService.getWeb3j(),
                ethereumBBCService.getCredentials(),
                new DefaultGasProvider()
        ).getLocalDomain().send();
        System.out.println("domain: {"+HexUtil.encodeHexStr(rawDomain)+ "}");

        // check contract status
        ctx = ethereumBBCService.getContext();
        System.out.println(ctx.getSdpContract().getStatus());

    }
}
