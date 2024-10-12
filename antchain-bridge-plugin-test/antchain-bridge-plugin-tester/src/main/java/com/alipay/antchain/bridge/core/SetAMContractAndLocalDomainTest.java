package com.alipay.antchain.bridge.core;

import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;

public class SetAMContractAndLocalDomainTest {

    AbstractBBCService bbcService;

    public SetAMContractAndLocalDomainTest(AbstractBBCService _bbcService) {
        bbcService = _bbcService;
    }

    public static void run(AbstractBBCService _bbcService) {
        SetAMContractAndLocalDomainTest setAMContractAndLocaldomainTest = new SetAMContractAndLocalDomainTest(_bbcService);

        setAMContractAndLocaldomainTest.setAMContractAndLocalDomain_success();
    }

    public void setAMContractAndLocalDomain_success() {
        // before
        bbcService.setupAuthMessageContract();
        bbcService.setupSDPMessageContract();
        AbstractBBCContext curCtx = bbcService.getContext();

        // set am to sdp
        bbcService.setAmContract(curCtx.getAuthMessageContract().getContractAddress());

        // set local domain to sdp
        bbcService.setLocalDomain("receiverDomain");
    }

}
