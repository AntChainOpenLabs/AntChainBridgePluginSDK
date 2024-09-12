package com.ali.antchain.core;

import cn.hutool.crypto.digest.DigestUtil;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import org.junit.Assert;

public class QuerySDPMessageSeqTest {

    AbstractBBCService bbcService;
    
    public QuerySDPMessageSeqTest(AbstractBBCService _bbcService) {
        this.bbcService = _bbcService;
    }


    public static void run(AbstractBBCService _bbcService) {
        QuerySDPMessageSeqTest querySDPMessageSeqTest = new QuerySDPMessageSeqTest(_bbcService);
        
        querySDPMessageSeqTest.querysdpmessageseq_success();
    }

    public void querysdpmessageseq_success()  {
        // 部署AM、SDP合约
        prepare();

        // query seq
        long seq = bbcService.querySDPMessageSeq(
                "senderDomain",
                DigestUtil.sha256Hex("senderID"),
                "receiverDomain",
                DigestUtil.sha256Hex("receiverID")
        );
        Assert.assertEquals(0, seq);
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

        // check contract ready
        curCtx = bbcService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, curCtx.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, curCtx.getSdpContract().getStatus());
    }

}
