package com.ali.antchain.Test;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.ali.antchain.abi.AppContract;
import com.ali.antchain.service.EthereumBBCService;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.ServiceConfigurationError;

public class ReadCrossChainMessageReceipt {

    private static final Logger log = LoggerFactory.getLogger(ReadCrossChainMessageReceipt.class);
    AbstractBBCService service;
    AppContract appContract;

    public ReadCrossChainMessageReceipt(AbstractBBCService service) {
        this.service = service;
    }
    public static void run(AbstractBBCContext context, AbstractBBCService service){
        RelayAmPrepare relayam = new RelayAmPrepare(service);
        relayam.relayamprepare(context);
    }

    public void readcrosschainmessagereceipt(AbstractBBCContext context) throws IOException {
        RelayAmPrepare.run(context,service);
        CrossChainMessageReceipt crossChainMessageReceipt = service.relayAuthMessage(getRawMsgFromRelayer());

//        waitForTxConfirmed(crossChainMessageReceipt.getTxhash(), ethereumBBCService.getWeb3j());

        // read receipt by txHash
        CrossChainMessageReceipt crossChainMessageReceipt1 = service.readCrossChainMessageReceipt(crossChainMessageReceipt.getTxhash());
        if (!crossChainMessageReceipt1.isConfirmed()) {
            // 记录调试信息
            log.warn("Transaction is not confirmed: " + crossChainMessageReceipt1.getTxhash());
        }
        if (!Objects.equals(crossChainMessageReceipt.isSuccessful(), crossChainMessageReceipt1.isSuccessful())) {
            // 记录调试信息
            log.warn("Success status mismatch: " + crossChainMessageReceipt.getTxhash());
        }
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
        private MockResp resp;

        @TLVField(tag = 9, type = TLVTypeEnum.STRING)
        private String domain;
    }
}
