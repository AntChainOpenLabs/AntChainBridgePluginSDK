package com.ali.antchain.Test;

import cn.hutool.crypto.digest.DigestUtil;
import com.ali.antchain.service.EthereumBBCService;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;

public class ETHQuerySDPMessageSeqTest {

    boolean setupBBC;


    public static void run(AbstractBBCContext context) throws Exception {
        ETHQuerySDPMessageSeqTest ETHQuerySDPMessageSeqTest = new ETHQuerySDPMessageSeqTest();
        ETHQuerySDPMessageSeqTest.querysdpmessageseq(context);
    }

    public void querysdpmessageseq(AbstractBBCContext  context) throws Exception {
        EthereumBBCService ethereumBBCService = new EthereumBBCService();
        ethereumBBCService.startup(context);

        if (setupBBC) {
            System.out.println("The BBC has already been set up.");
        }else {
            RelayAmPrepare.relayamprepare(context);
        }
//            //set up am
//            ethereumBBCService.setupAuthMessageContract();
//
//            // set up sdp
//            ethereumBBCService.setupSDPMessageContract();
//
//            ethereumBBCService.setProtocol(
//                    context.getSdpContract().getContractAddress(),
//                    "0");
//
//            // set am to sdp
//            ethereumBBCService.setAmContract(context.getAuthMessageContract().getContractAddress());
//
//            // set local domain to sdp
//            ethereumBBCService.setLocalDomain("receiverDomain");
//
//            // check contract ready
//            AbstractBBCContext ctxCheck = ethereumBBCService.getContext();
//
//            System.out.println(ctxCheck.getAuthMessageContract().getStatus());
//            System.out.println(ctxCheck.getSdpContract().getStatus());
//
//            Web3j web3j = Web3j.build(new HttpService("http://127.0.0.1:7545"));
//            Credentials credentials = Credentials.create("0x45cfa3b6addf98274bfd9e3482a695406c7dcc99e1588bd6e0127b8fc06ae916");
//
//            RawTransactionManager rawTransactionManager = new RawTransactionManager(
//                    web3j, credentials, web3j.ethChainId().send().getChainId().longValue());
//
//            AppContract appContract = AppContract.deploy(
//                    web3j,
//                    rawTransactionManager,
//                    new DefaultGasProvider()
//            ).send();
//
//            TransactionReceipt receipt = appContract.setProtocol(ethereumBBCService.getBbcContext().getSdpContract().getContractAddress()).send();
//            if (receipt.isStatusOK()) {
//                System.out.println("set protocol(" + appContract.getContractAddress()+ ") to app contract(" +ethereumBBCService.getBbcContext().getSdpContract().getContractAddress()+ ")");
//            } else {
//                throw new Exception(String.format("failed to set protocol(%s) to app contract(%s)",
//                        appContract.getContractAddress(),
//                        ethereumBBCService.getBbcContext().getSdpContract().getContractAddress()));
//            }
//            System.out.println(receipt);
//        }

        // query seq
        long seq = ethereumBBCService.querySDPMessageSeq(
                "senderDomain",
                DigestUtil.sha256Hex("senderID"),
                "receiverDomain",
                DigestUtil.sha256Hex("receiverID")
        );
        System.out.println(seq);

    }

}
