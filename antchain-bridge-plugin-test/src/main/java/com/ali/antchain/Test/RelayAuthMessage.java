package com.ali.antchain.Test;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.ali.antchain.abi.AppContract;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageFactory;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.commons.core.sdp.ISDPMessage;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageFactory;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class RelayAuthMessage {

    private static final Logger log = LoggerFactory.getLogger(RelayAuthMessage.class);
    AbstractBBCService service;
    AppContract appContract;
    String product;


    public RelayAuthMessage(AbstractBBCService service) {
        this.service = service;
    }
    public static void run(AbstractBBCContext context, AbstractBBCService service, String product) throws Exception {
        if(product.equals("eth")){
            ETHRelayAuthMessage.run(context,service);
        }
    }

    public void relayauthmessage(AbstractBBCContext context) throws Exception {
        RelayAmPrepare.run(context,service);

        // relay am msg
//        CrossChainMessageReceipt receipt = service.relayAuthMessage(getRawMsgFromRelayer());
//        Assert.assertTrue(receipt.isSuccessful());

//        waitForTxConfirmed(receipt.getTxhash(), ethereumBBCService.getWeb3j());

//        EthGetTransactionReceipt ethGetTransactionReceipt = service.getWeb3j().ethGetTransactionReceipt(receipt.getTxhash()).send();
//        TransactionReceipt transactionReceipt = ethGetTransactionReceipt.getTransactionReceipt().get();
//        Assert.assertNotNull(transactionReceipt);
//        Assert.assertTrue(transactionReceipt.isStatusOK());
    }

}
