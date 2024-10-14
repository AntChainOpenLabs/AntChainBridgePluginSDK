package com.alipay.antchain.bridge.core;

import cn.hutool.crypto.digest.DigestUtil;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import com.alipay.antchain.bridge.exception.PluginTestToolException;
import com.alipay.antchain.bridge.exception.PluginTestToolException.*;

public class QuerySDPMessageSeqTest {

    AbstractBBCService bbcService;

    public QuerySDPMessageSeqTest(AbstractBBCService _bbcService) {
        this.bbcService = _bbcService;
    }


    public static void run(AbstractBBCService _bbcService) throws PluginTestToolException {
        QuerySDPMessageSeqTest querySDPMessageSeqTest = new QuerySDPMessageSeqTest(_bbcService);
        querySDPMessageSeqTest.querySdpMessageSeq_success();
    }

    public void querySdpMessageSeq_success() throws PluginTestToolException {
        try {
            // 部署AM、SDP合约
            prepare();

            // query seq
            long seq = bbcService.querySDPMessageSeq(
                    "senderDomain",
                    DigestUtil.sha256Hex("senderID"),
                    "receiverDomain",
                    DigestUtil.sha256Hex("receiverID")
            );
//            Assert.assertEquals(0L, seq);
            if (seq != 0L) {
                throw new QuerySDPMessageSeqTestException("QuerySDPMessageSeqTest failed, seq is not 0");
            }
        } catch (Exception e) {
            throw new QuerySDPMessageSeqTestException("QuerySDPMessageSeqTest failed", e);
        }
    }

    private void prepare() {
        bbcService.setupAuthMessageContract();

        bbcService.setupSDPMessageContract();

        AbstractBBCContext curCtx = bbcService.getContext();

        bbcService.setProtocol(curCtx.getSdpContract().getContractAddress(), "0");

        bbcService.setAmContract(curCtx.getAuthMessageContract().getContractAddress());

        bbcService.setLocalDomain("receiverDomain");
    }
}
