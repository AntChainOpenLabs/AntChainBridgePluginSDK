package com.ali.antchain.Test;

import com.ali.antchain.abi.AuthMsg;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.DefaultGasProvider;

import java.lang.reflect.Method;
import java.math.BigInteger;

public class SetProtocolTest {

    private static final Logger log = LoggerFactory.getLogger(SetProtocolTest.class);
    static AbstractBBCService service;
    String product;

    public SetProtocolTest(AbstractBBCService service,String product) {
        this.service = service;
        this.product = product;
    }
    public static void run(AbstractBBCContext context, AbstractBBCService service, String product) throws Exception {
        SetProtocolTest setProtocolTest = new SetProtocolTest(service,product);
        setProtocolTest.setprotocol_success(context);
    }

    public void setprotocol_success(AbstractBBCContext context) throws Exception {
        System.out.println("eth setprotocol test ...");
        // EthereumBBCService service = new EthereumBBCService();
        // start up
        service.startup(context);
        service.setupAuthMessageContract();
        service.setupSDPMessageContract();


        //set protocol
        AbstractBBCContext ctx = service.getContext();
        service.setProtocol(ctx.getSdpContract().getContractAddress(),"0");

        if(product.equals("simple-ethereum")){

            System.out.println("eth get protocol test ...");
            //get protocol
            Tester tester = new EthTester(service);
            tester.getProtocol();

            System.out.println("eth check am test ...");
            // check am
            tester.checkAm();
        }

    }

}
