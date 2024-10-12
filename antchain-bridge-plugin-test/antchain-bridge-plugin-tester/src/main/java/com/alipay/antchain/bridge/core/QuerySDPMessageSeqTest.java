package com.alipay.antchain.bridge.core;

import cn.hutool.crypto.digest.DigestUtil;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;

public class QuerySDPMessageSeqTest {

    AbstractBBCService bbcService;

    public QuerySDPMessageSeqTest(AbstractBBCService _bbcService) {
        this.bbcService = _bbcService;
    }


    public static void run(AbstractBBCService _bbcService) {
        QuerySDPMessageSeqTest querySDPMessageSeqTest = new QuerySDPMessageSeqTest(_bbcService);

        querySDPMessageSeqTest.querySdpMessageSeq_success();
    }

    public void querySdpMessageSeq_success()  {
        // 部署AM、SDP合约
        prepare();

        // query seq
        long seq = bbcService.querySDPMessageSeq(
                "senderDomain",
                DigestUtil.sha256Hex("senderID"),
                "receiverDomain",
                DigestUtil.sha256Hex("receiverID")
        );
    }


    private void prepare() {
        // set up am
        bbcService.setupAuthMessageContract();

        // set up sdp
        bbcService.setupSDPMessageContract();

        AbstractBBCContext curCtx = bbcService.getContext();

        // set protocol to am (sdp type: 0)
        bbcService.setProtocol(curCtx.getSdpContract().getContractAddress(), "0");

        // set am to sdp
        bbcService.setAmContract(curCtx.getAuthMessageContract().getContractAddress());

        // set local domain to sdp
        bbcService.setLocalDomain("receiverDomain");
    }

}
