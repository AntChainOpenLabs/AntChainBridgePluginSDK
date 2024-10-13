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

public class RelayAuthMessageTest {

    private static final Logger log = LoggerFactory.getLogger(RelayAuthMessageTest.class);

    AbstractTester tester;
    AbstractBBCService service;

    public RelayAuthMessageTest(AbstractBBCService service, AbstractTester tester) {
        this.service = service;
        this.tester = tester;
    }

    public static void run(AbstractBBCService service, AbstractTester tester) throws PluginTestToolException {
        RelayAuthMessageTest relayAuthMessageTest = new RelayAuthMessageTest(service, tester);
        relayAuthMessageTest.relayAuthMessage_success();
    }

    public void relayAuthMessage_success() throws PluginTestToolException {
        // 部署AM、SDP合约
        prepare();

        // 部署APP合约
        AbstractBBCContext curCtx = service.getContext();

        byte[] targetIdentity = tester.deployApp(curCtx.getSdpContract().getContractAddress());

        // relay am msg
        CrossChainMessageReceipt receipt = service.relayAuthMessage(getRawMsgFromRelayer(targetIdentity));

        if (!receipt.isSuccessful()) {
            throw new CrossChainMessageReceiptFailedException("RelayAuthMessageTest failed, receipt is not successful");
        }

        tester.waitForTxConfirmed(receipt.getTxhash());

        try {
            tester.checkTransactionReceipt(service, receipt.getTxhash());
        } catch (Exception e) {
            throw new CrossChainMessageReceiptFailedException("RelayAuthMessageTest failed, checkTransactionReceipt failed", e);
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
