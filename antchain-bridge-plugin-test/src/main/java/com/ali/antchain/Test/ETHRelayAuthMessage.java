package com.ali.antchain.Test;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
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
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ETHRelayAuthMessage extends RelayAuthMessage {
    public ETHRelayAuthMessage(AbstractBBCService service) {
        super(service);
    }

    public static void run(AbstractBBCContext context, AbstractBBCService service) throws Exception {
        ETHRelayAuthMessage.run(context,service);
    }

    @Override
    public void relayauthmessage(AbstractBBCContext context) throws Exception {
        RelayAmPrepare.run(context,service);

        // relay am msg
        CrossChainMessageReceipt receipt = service.relayAuthMessage(getRawMsgFromRelayer());
//        Assert.assertTrue(receipt.isSuccessful());

//        waitForTxConfirmed(receipt.getTxhash(), ethereumBBCService.getWeb3j());

//        EthGetTransactionReceipt ethGetTransactionReceipt = service.getWeb3j().ethGetTransactionReceipt(receipt.getTxhash()).send();
//        TransactionReceipt transactionReceipt = ethGetTransactionReceipt.getTransactionReceipt().get();
//        Assert.assertNotNull(transactionReceipt);
//        Assert.assertTrue(transactionReceipt.isStatusOK());
    }
    private byte[] getRawMsgFromRelayer() throws IOException {
        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(
                1,
                new byte[32],
                "receiverDomain",
                HexUtil.decodeHex(
                        String.format("000000000000000000000000%s", StrUtil.removePrefix(appContract.getContractAddress(), "0x"))
                ),
                -1,
                "awesome antchain-bridge".getBytes()
        );

        IAuthMessage am = AuthMessageFactory.createAuthMessage(
                1,
                DigestUtil.sha256("senderID"),
                0,
                sdpMessage.encode()
        );

        ReadCrossChainMessageReceipt.MockResp resp = new ReadCrossChainMessageReceipt.MockResp();
        resp.setRawResponse(am.encode());

        ReadCrossChainMessageReceipt.MockProof proof = new ReadCrossChainMessageReceipt.MockProof();
        proof.setResp(resp);
        proof.setDomain("senderDomain");

        byte[] rawProof = TLVUtils.encode(proof);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(new byte[]{0, 0, 0, 0});

        int len = rawProof.length;
        stream.write((len >>> 24) & 0xFF);
        stream.write((len >>> 16) & 0xFF);
        stream.write((len >>> 8) & 0xFF);
        stream.write((len) & 0xFF);

        stream.write(rawProof);

        return stream.toByteArray();
    }

    @Getter
    @Setter
    public static class MockResp {

        @TLVField(tag = 0, type = TLVTypeEnum.BYTES)
        private byte[] rawResponse;
    }

    @Getter
    @Setter
    public static class MockProof {

        @TLVField(tag = 5, type = TLVTypeEnum.BYTES)
        private ReadCrossChainMessageReceipt.MockResp resp;

        @TLVField(tag = 9, type = TLVTypeEnum.STRING)
        private String domain;
    }
}
