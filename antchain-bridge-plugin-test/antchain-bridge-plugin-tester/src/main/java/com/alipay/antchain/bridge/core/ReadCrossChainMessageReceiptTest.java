//package com.ali.antchain.core;
//
//import cn.hutool.core.util.HexUtil;
//import cn.hutool.core.util.StrUtil;
//import cn.hutool.crypto.digest.DigestUtil;
//import com.ali.antchain.Test.RelayAmPrepare;
//import com.ali.antchain.abi.AppContract;
//import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
//import com.alipay.antchain.bridge.commons.core.am.AuthMessageFactory;
//import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
//import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
//import com.alipay.antchain.bridge.commons.core.sdp.ISDPMessage;
//import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageFactory;
//import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
//import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
//import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
//import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
//import lombok.Getter;
//import lombok.Setter;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.util.Objects;
//
//public class ReadCrossChainMessageReceiptTest {
//
//    private static final Logger log = LoggerFactory.getLogger(ReadCrossChainMessageReceiptTest.class);
//    AbstractBBCService service;
//    AppContract appContract;
//
//    public ReadCrossChainMessageReceiptTest(AbstractBBCService service) {
//        this.service = service;
//    }
//    public static void run(AbstractBBCContext context, AbstractBBCService service){
//        RelayAmPrepare relayam = new RelayAmPrepare(service);
//        relayam.relayamprepare(context);
//    }
//
//    public void readcrosschainmessagereceipt(AbstractBBCContext context) throws IOException {
//        RelayAmPrepare.run(context,service);
//        CrossChainMessageReceipt crossChainMessageReceipt = service.relayAuthMessage(getRawMsgFromRelayer());
//
////        waitForTxConfirmed(crossChainMessageReceipt.getTxhash(), ethereumBBCService.getWeb3j());
//
//        // read receipt by txHash
//        CrossChainMessageReceipt crossChainMessageReceipt1 = service.readCrossChainMessageReceipt(crossChainMessageReceipt.getTxhash());
//        if (!crossChainMessageReceipt1.isConfirmed()) {
//            // 记录调试信息
//            log.warn("Transaction is not confirmed: " + crossChainMessageReceipt1.getTxhash());
//        }
//        if (!Objects.equals(crossChainMessageReceipt.isSuccessful(), crossChainMessageReceipt1.isSuccessful())) {
//            // 记录调试信息
//            log.warn("Success status mismatch: " + crossChainMessageReceipt.getTxhash());
//        }
//    }
//
//    private byte[] getRawMsgFromRelayer() throws IOException {
//        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(
//                1,
//                new byte[32],
//                "receiverDomain",
//                HexUtil.decodeHex(
//                        String.format("000000000000000000000000%s", StrUtil.removePrefix(appContract.getContractAddress(), "0x"))
//                ),
//                -1,
//                "awesome antchain-bridge".getBytes()
//        );
//
//        IAuthMessage am = AuthMessageFactory.createAuthMessage(
//                1,
//                DigestUtil.sha256("senderID"),
//                0,
//                sdpMessage.encode()
//        );
//
//        MockResp resp = new MockResp();
//        resp.setRawResponse(am.encode());
//
//        MockProof proof = new MockProof();
//        proof.setResp(resp);
//        proof.setDomain("senderDomain");
//
//        byte[] rawProof = TLVUtils.encode(proof);
//
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        stream.write(new byte[]{0, 0, 0, 0});
//
//        int len = rawProof.length;
//        stream.write((len >>> 24) & 0xFF);
//        stream.write((len >>> 16) & 0xFF);
//        stream.write((len >>> 8) & 0xFF);
//        stream.write((len) & 0xFF);
//
//        stream.write(rawProof);
//
//        return stream.toByteArray();
//    }
//
//    @Getter
//    @Setter
//    public static class MockResp {
//
//        @TLVField(tag = 0, type = TLVTypeEnum.BYTES)
//        private byte[] rawResponse;
//    }
//
//    @Getter
//    @Setter
//    public static class MockProof {
//
//        @TLVField(tag = 5, type = TLVTypeEnum.BYTES)
//        private MockResp resp;
//
//        @TLVField(tag = 9, type = TLVTypeEnum.STRING)
//        private String domain;
//    }
//}
package com.alipay.antchain.bridge.core;

import cn.hutool.crypto.digest.DigestUtil;
import com.alipay.antchain.bridge.abstarct.AbstractTester;
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
import com.alipay.antchain.bridge.exception.PluginTestToolException;
import com.alipay.antchain.bridge.exception.PluginTestToolException.*;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class ReadCrossChainMessageReceiptTest {

    private static final Logger log = LoggerFactory.getLogger(ReadCrossChainMessageReceiptTest.class);

    AbstractTester tester;
    AbstractBBCService service;

    public ReadCrossChainMessageReceiptTest(AbstractBBCService service, AbstractTester tester) {
        this.service = service;
        this.tester = tester;
    }

    public static void run(AbstractBBCService service, AbstractTester tester) throws PluginTestToolException {
        ReadCrossChainMessageReceiptTest test = new ReadCrossChainMessageReceiptTest(service, tester);
        test.relayAuthMessageReceipt_success();
    }

    public void relayAuthMessageReceipt_success() throws PluginTestToolException{

        // 部署AM、SDP合约
        prepare();

        try {
            // relay am msg
            AbstractBBCContext curCtx = service.getContext();

            byte[] targetIdentity = tester.deployApp(curCtx.getSdpContract().getContractAddress());

            CrossChainMessageReceipt receipt = service.relayAuthMessage(getRawMsgFromRelayer(targetIdentity));

            tester.waitForTxConfirmed(receipt.getTxhash());

            // read receipt by txHash
            CrossChainMessageReceipt receipt1 = service.readCrossChainMessageReceipt(receipt.getTxhash());

            if (!receipt1.isConfirmed()) {
                throw new CrossChainMessageReceiptNotConfirmedException("ReadCrossChainMessageReceiptTest failed, not confirmed");
            }
            if (receipt.isSuccessful() != receipt1.isSuccessful()) {
                throw new CrossChainMessageReceiptSuccessStatusMismatchException("ReadCrossChainMessageReceiptTest failed, success status mismatch");
            }

        } catch (Exception e) {
            throw new ReadCrossChainMessageReceiptTestException("ReadCrossChainMessageReceiptTest failed", e);
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

    /**
     * 伪造中继上的跨链消息
     *
     * @param targetIdentity
     * @return
     * @throws IOException
     */
    private byte[] getRawMsgFromRelayer(byte[] targetIdentity) {
        try {
            ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(
                    1,
                    new byte[32],
                    "receiverDomain",
                    targetIdentity,
                    -1,
                    "awesome antchain-bridge".getBytes()
            );

            IAuthMessage am = AuthMessageFactory.createAuthMessage(
                    1,
                    DigestUtil.sha256("senderID"),
                    0,
                    sdpMessage.encode()
            );

            MockResp resp = new MockResp();
            resp.setRawResponse(am.encode());

            MockProof proof = new MockProof();
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
        } catch (Exception e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
    }


    @Getter
    @Setter
    public static class MockProof {

        @TLVField(tag = 5, type = TLVTypeEnum.BYTES)
        private MockResp resp;

        @TLVField(tag = 9, type = TLVTypeEnum.STRING)
        private String domain;
    }

    @Getter
    @Setter
    public static class MockResp {

        @TLVField(tag = 0, type = TLVTypeEnum.BYTES)
        private byte[] rawResponse;
    }
}
