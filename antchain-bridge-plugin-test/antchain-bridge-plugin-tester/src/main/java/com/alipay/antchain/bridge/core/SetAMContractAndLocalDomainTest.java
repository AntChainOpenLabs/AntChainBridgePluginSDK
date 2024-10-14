package com.alipay.antchain.bridge.core;

import com.alipay.antchain.bridge.abstarct.AbstractTester;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import com.alipay.antchain.bridge.exception.PluginTestToolException;
import com.alipay.antchain.bridge.exception.PluginTestToolException.*;

public class SetAMContractAndLocalDomainTest {

    AbstractBBCService bbcService;
    AbstractTester tester;

    public SetAMContractAndLocalDomainTest(AbstractBBCService _bbcService, AbstractTester _tester) {
        bbcService = _bbcService;
        tester = _tester;
    }

    public static void run(AbstractBBCService _bbcService, AbstractTester _tester) throws PluginTestToolException {
        SetAMContractAndLocalDomainTest setAMContractAndLocaldomainTest = new SetAMContractAndLocalDomainTest(_bbcService, _tester);
        setAMContractAndLocaldomainTest.setAMContractAndLocalDomain_success();
    }

    public void setAMContractAndLocalDomain_success() throws PluginTestToolException {

        setUpContractsAndCheckStatus();

        AbstractBBCContext ctx = bbcService.getContext();

        // set am to sdp
        bbcService.setAmContract(ctx.getAuthMessageContract().getContractAddress());

        String amAddress = null;
        try {
            amAddress = tester.getAmAddress(bbcService);
        } catch (Exception e) {
            throw new GetAuthMessageContractAddressException("SetAMContractAndLocalDomainTest failed, getAmAddress failed", e);
        }

        if (!ContractStatusEnum.CONTRACT_DEPLOYED.equals(bbcService.getContext().getSdpContract().getStatus())) {
            throw new SDPContractStatusException("SetAMContractAndLocalDomainTest failed, SDPContract status should be CONTRACT_DEPLOYED after setup.");
        }

        // set local domain
        bbcService.setLocalDomain("receiverDomain");

        byte[] rawDomain = null;
        try {
            rawDomain = tester.getLocalDomain(bbcService);
        } catch (Exception e) {
            throw new GetLocalDomainException("SetAMContractAndLocalDomainTest failed, getLocalDomain failed", e);
        }

        if (!ContractStatusEnum.CONTRACT_READY.equals(bbcService.getContext().getSdpContract().getStatus())) {
            throw new SDPContractStatusException("SetAMContractAndLocalDomainTest failed, SDPContract status should be CONTRACT_READY after setup.");
        }

    }

    private void setUpContractsAndCheckStatus() throws PluginTestToolException {
        try {
            bbcService.setupAuthMessageContract();
            bbcService.setupSDPMessageContract();

            AbstractBBCContext ctx = bbcService.getContext();
            if (!ContractStatusEnum.CONTRACT_DEPLOYED.equals(ctx.getAuthMessageContract().getStatus())) {
                throw new AuthMessageContractStatusException("SetAMContractAndLocalDomainTest failed, AuthMessageContract status should be CONTRACT_DEPLOYED after setup.");
            }
            if (!ContractStatusEnum.CONTRACT_DEPLOYED.equals(ctx.getSdpContract().getStatus())) {
                throw new SDPContractStatusException("SetAMContractAndLocalDomainTest failed, SDPContract status should be CONTRACT_DEPLOYED after setup.");
            }
        } catch (Exception e) {
            throw new SetProtocolTestException("SetAMContractAndLocalDomainTest failed, setup contracts failed", e);
        }
    }
}
