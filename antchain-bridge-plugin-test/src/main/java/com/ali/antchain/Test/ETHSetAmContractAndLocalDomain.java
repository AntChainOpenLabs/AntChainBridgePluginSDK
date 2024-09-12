package com.ali.antchain.Test;

import cn.hutool.core.util.HexUtil;
import com.ali.antchain.abi.SDPMsg;
import com.ali.antchain.service.EthereumBBCService;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.tx.gas.DefaultGasProvider;

public class ETHSetAmContractAndLocalDomain extends SetAmContractAndLocalDomain {

    private static final Logger log = LoggerFactory.getLogger(ETHSetAmContractAndLocalDomain.class);

    public ETHSetAmContractAndLocalDomain(AbstractBBCService service) {
        super(service);
    }

    @Override
    public void setamcontractandlocaldomain(AbstractBBCContext context) throws Exception {
        // start up

        service.startup(context);

        // set up am
        service.setupAuthMessageContract();

        // set up sdp
        service.setupSDPMessageContract();

        // get context
        AbstractBBCContext ctx = service.getContext();
//        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
//        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        // set am to sdp
        service.setAmContract(ctx.getAuthMessageContract().getContractAddress());

//        String amAddr = SDPMsg.load(
//                service.getContext().getSdpContract().getContractAddress(),
//                service.getWeb3j(),
//                service.getCredentials(),
//                new DefaultGasProvider()
//        ).getAmAddress().send();
//        log.info("amAddr: {}", amAddr);

        // check contract status
        ctx = service.getContext();
//        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());


        // set the domain
        service.setLocalDomain("receiverDomain");

//        byte[] rawDomain = SDPMsg.load(
//                service.getContext().getSdpContract().getContractAddress(),
//                service.getWeb3j(),
//                service.getCredentials(),
//                new DefaultGasProvider()
//        ).getLocalDomain().send();
//        log.info("domain: {}", HexUtil.encodeHexStr(rawDomain));

        // check contract status
        ctx = service.getContext();
//        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getSdpContract().getStatus());

    }
}
