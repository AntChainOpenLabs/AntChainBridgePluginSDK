package com.ali.antchain.Test;

import com.ali.antchain.abi.AppContract;
import com.ali.antchain.service.EthereumBBCService;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

public class RelayAmPrepareTest {

    static boolean setupBBC;
    static AppContract appContract;

    public static void relayamprepare(AbstractBBCContext context) throws Exception {
        if (setupBBC) {
            return;
        }

        // start up
        EthereumBBCService ethereumBBCService = new EthereumBBCService();
        ethereumBBCService.startup(context);

        // set up am
        ethereumBBCService.setupAuthMessageContract();

        // set up sdp
        ethereumBBCService.setupSDPMessageContract();

        // set protocol to am (sdp type: 0)
        ethereumBBCService.setProtocol(
                context.getSdpContract().getContractAddress(),
                "0");

        // set am to sdp
        ethereumBBCService.setAmContract(context.getAuthMessageContract().getContractAddress());

        // set local domain to sdp
        ethereumBBCService.setLocalDomain("receiverDomain");

        // check contract ready
        AbstractBBCContext ctxCheck = ethereumBBCService.getContext();

        System.out.println(ctxCheck.getAuthMessageContract().getStatus());
        System.out.println(ctxCheck.getSdpContract().getStatus());

        Web3j web3j = Web3j.build(new HttpService("http://127.0.0.1:7545"));
        Credentials credentials = Credentials.create("0x45cfa3b6addf98274bfd9e3482a695406c7dcc99e1588bd6e0127b8fc06ae916");

        RawTransactionManager rawTransactionManager = new RawTransactionManager(
                web3j, credentials, web3j.ethChainId().send().getChainId().longValue());

        appContract = AppContract.deploy(
                web3j,
                rawTransactionManager,
                new DefaultGasProvider()
        ).send();

        TransactionReceipt receipt = appContract.setProtocol(ethereumBBCService.getBbcContext().getSdpContract().getContractAddress()).send();
        if (receipt.isStatusOK()) {
            System.out.println("set protocol(" + appContract.getContractAddress()+ ") to app contract(" +ethereumBBCService.getBbcContext().getSdpContract().getContractAddress()+ ")");
        } else {
            throw new Exception(String.format("failed to set protocol(%s) to app contract(%s)",
                    appContract.getContractAddress(),
                    ethereumBBCService.getBbcContext().getSdpContract().getContractAddress()));
        }
        System.out.println(receipt);

        setupBBC = true;
    }
}
