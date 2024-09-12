package com.ali.antchain.Test;

import com.ali.antchain.abi.AuthMsg;
import com.ali.antchain.service.EthereumBBCService;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;

public class ETHSetProtocolTest {

    public static void run(AbstractBBCContext context) throws Exception {
        ETHSetProtocolTest ETHSetProtocolTest = new ETHSetProtocolTest();
        ETHSetProtocolTest.setprotocol(context);
    }
    public void setprotocol(AbstractBBCContext context) throws Exception {
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

        // set protocol to am (sdp type: 0)
        ethereumBBCService.setProtocol(
                ctx.getSdpContract().getContractAddress(),
                "0");

        String addr = AuthMsg.load(
                ethereumBBCService.getBbcContext().getAuthMessageContract().getContractAddress(),
                ethereumBBCService.getWeb3j(),
                ethereumBBCService.getCredentials(),
                new DefaultGasProvider()
        ).getProtocol(BigInteger.ZERO).send();
        System.out.println("protocol: {" + addr + "}");

        // check am status
        ctx = ethereumBBCService.getContext();
        System.out.println(ctx.getAuthMessageContract().getStatus());

    }
}
