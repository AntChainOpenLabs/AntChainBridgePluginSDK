package com.alipay.antchain.bridge.core;

import com.alipay.antchain.bridge.abstarct.AbstractTester;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import com.alipay.antchain.bridge.exception.PluginTestToolException;
import com.alipay.antchain.bridge.exception.PluginTestToolException.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadCrossChainMessageByHeightTest {
    private static final Logger log = LoggerFactory.getLogger(ReadCrossChainMessageReceiptTest.class);

    AbstractTester tester;
    AbstractBBCService service;

    public ReadCrossChainMessageByHeightTest(AbstractBBCService service, AbstractTester tester) {
        this.service = service;
        this.tester = tester;
    }

    public static void run(AbstractBBCService service, AbstractTester tester) throws PluginTestToolException {
        ReadCrossChainMessageByHeightTest test = new ReadCrossChainMessageByHeightTest(service, tester);
        test.readCrossChainMessageByHeight_success();
    }

    private void readCrossChainMessageByHeight_success() throws PluginTestToolException {
        // 对应 relayAmPrepare
        prepare();

        try {
            tester.sendMsgUnordered(service);
        } catch (Exception e) {
            throw new ReadCrossChainMessageByHeightTestException("ReadCrossChainMessageByHeightTest failed", e);
        }
    }



    private void prepare() {
        // set up am
        service.setupAuthMessageContract();

        // set up sdp
        service.setupSDPMessageContract();

        AbstractBBCContext curCtx = service.getContext();

        // set protocol to am (sdp type: 0)
        service.setProtocol(curCtx.getSdpContract().getContractAddress(), "0");

        // set am to sdp
        service.setAmContract(curCtx.getAuthMessageContract().getContractAddress());

        // set local domain to sdp
        service.setLocalDomain("receiverDomain");
    }

}
