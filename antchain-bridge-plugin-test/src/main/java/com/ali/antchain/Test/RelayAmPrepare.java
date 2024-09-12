package com.ali.antchain.Test;

import com.ali.antchain.abi.AppContract;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

public class RelayAmPrepare {

    private static final Logger log = LoggerFactory.getLogger(RelayAmPrepare.class);
    boolean setupBBC;
    AbstractBBCService service;
    AppContract appContract;

    public RelayAmPrepare(AbstractBBCService service) {
        this.service = service;
    }
    public static void run(AbstractBBCContext context, AbstractBBCService service){
        RelayAmPrepare amPrepare = new RelayAmPrepare(service);
        amPrepare.relayamprepare(context);
    }
    public void relayamprepare(AbstractBBCContext context){
        if (service == null) {
            throw new IllegalStateException("Service is not initialized.");
        }
        try {
            // 启动服务
            service.startup(context);
            // 获取上下文
            service.setupAuthMessageContract();

            service.setupSDPMessageContract();

            service.setProtocol( context.getSdpContract().getContractAddress(),"0");

            service.setAmContract(context.getAuthMessageContract().getContractAddress());
            service.setLocalDomain("receiverDomain");

            AbstractBBCContext ctx =service.getContext();
            System.out.println(ctx.getAuthMessageContract().getStatus());
            System.out.println(ctx.getSdpContract().getStatus());

            TransactionReceipt receipt = appContract.setProtocol(service.getContext().getSdpContract().getContractAddress()).send();

            if (receipt.isStatusOK()) {
                log.info("set protocol({}) to app contract({})",
                        appContract.getContractAddress(),
                        service.getContext().getSdpContract().getContractAddress());
            } else {
                throw new Exception(String.format("failed to set protocol(%s) to app contract(%s)",
                        appContract.getContractAddress(),
                        service.getContext().getSdpContract().getContractAddress()));
            }
            setupBBC = true;
        } catch (Exception e) {
            // 处理异常
            log.error("An error occurred: ", e);
        }
    }
}
