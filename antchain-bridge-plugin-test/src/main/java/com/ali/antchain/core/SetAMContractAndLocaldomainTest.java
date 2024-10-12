package com.ali.antchain.core;

import com.ali.antchain.abstarct.AbstractTester;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import org.junit.Assert;

public class SetAMContractAndLocaldomainTest {

    AbstractBBCService bbcService;

    public SetAMContractAndLocaldomainTest(AbstractBBCService _bbcService) {
        bbcService = _bbcService;
    }

    public static void run(AbstractBBCService _bbcService) {
        SetAMContractAndLocaldomainTest setAMContractAndLocaldomainTest = new SetAMContractAndLocaldomainTest(_bbcService);

        setAMContractAndLocaldomainTest.setAMContractAndLocaldomain_success();
    }

    public void setAMContractAndLocaldomain_success() {
        // before
        bbcService.setupAuthMessageContract();
        bbcService.setupSDPMessageContract();
        AbstractBBCContext curCtx = bbcService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, curCtx.getSdpContract().getStatus());

        // set am to sdp
        bbcService.setAmContract(curCtx.getAuthMessageContract().getContractAddress());

        // set local domain to sdp
        bbcService.setLocalDomain("receiverDomain");

        // check after
        curCtx = bbcService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, curCtx.getSdpContract().getStatus());
    }

}
