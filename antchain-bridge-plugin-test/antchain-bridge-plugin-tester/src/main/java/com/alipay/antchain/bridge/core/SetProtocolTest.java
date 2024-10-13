package com.alipay.antchain.bridge.core;

import com.alipay.antchain.bridge.abstarct.AbstractTester;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import com.alipay.antchain.bridge.exception.PluginTestToolException;
import com.alipay.antchain.bridge.exception.PluginTestToolException.*;
import org.pf4j.Plugin;

public class SetProtocolTest {

    AbstractTester tester;
    AbstractBBCService bbcService;

    public SetProtocolTest(AbstractBBCService _bbcService, AbstractTester _tester) {
        bbcService = _bbcService;
        tester = _tester;
    }

    public static void run(AbstractBBCService _bbcService, AbstractTester _tester) throws PluginTestToolException {
        SetProtocolTest setProtocolTest = new SetProtocolTest(_bbcService, _tester);
        setProtocolTest.setProtocol_success();
    }

    public void setProtocol_success() throws PluginTestToolException {
        tester.getBbcLogger().info("start setProtocol_success test ...");

        setUpContractsAndCheckStatus();

        // 2. before set protocol
        AbstractBBCContext curCtx = bbcService.getContext();

        // 3. execute set_protocol
        try {
            bbcService.setProtocol(curCtx.getSdpContract().getContractAddress(), "0");
        } catch (Exception e) {
            throw new SetProtocolTestException("SetProtocolTest failed, set protocol failed", e);
        }


        try {
            tester.getProtocol(curCtx.getAuthMessageContract().getContractAddress());
        } catch (Exception e) {
            throw new SetProtocolTestException("SetProtocolTest failed, get protocol failed", e);
        }

        // 4.2 Check whether the contract status in the context is ready
        AbstractBBCContext ctx = bbcService.getContext();
        if (!ContractStatusEnum.CONTRACT_READY.equals(ctx.getAuthMessageContract().getStatus())) {
            throw new AuthMessageContractStatusException("SetProtocolTest failed, AuthMessageContract status should be CONTRACT_READY after setup.");
        }
    }

    private void setUpContractsAndCheckStatus() throws PluginTestToolException {
        try {
            bbcService.setupAuthMessageContract();
            bbcService.setupSDPMessageContract();

            AbstractBBCContext ctx = bbcService.getContext();
            if (!ContractStatusEnum.CONTRACT_DEPLOYED.equals(ctx.getAuthMessageContract().getStatus())) {
                throw new AuthMessageContractStatusException("SetProtocolTest failed, AuthMessageContract status should be CONTRACT_DEPLOYED after setup.");
            }
            if (!ContractStatusEnum.CONTRACT_DEPLOYED.equals(ctx.getSdpContract().getStatus())) {
                throw new SDPContractStatusException("SetProtocolTest failed, SDPContract status should be CONTRACT_DEPLOYED after setup.");
            }
        } catch (Exception e) {
            throw new SetProtocolTestException("SetProtocolTest failed, setup contracts failed", e);
        }
    }
}
