/*
 * Copyright 2023 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.bridge.plugins.citacloud;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageFactory;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.commons.core.sdp.ISDPMessage;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageFactory;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import com.cita.cloud.response.TransactionReceipt;
import com.citahub.cita.abi.TypeReference;
import com.citahub.cita.abi.datatypes.Address;
import com.citahub.cita.abi.datatypes.DynamicBytes;
import com.citahub.cita.abi.datatypes.Type;
import com.citahub.cita.abi.datatypes.Utf8String;
import com.citahub.cita.abi.datatypes.generated.Bytes32;
import com.citahub.cita.abi.datatypes.generated.Uint256;
import com.citahub.cita.utils.Numeric;
import lombok.Getter;
import lombok.Setter;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CITACloudBBCTest {

    private static CITACloudBBCService citaCloudBBCService;

    private static String appContractAddress;

    @BeforeClass
    public static void setUp() throws Exception {

        DefaultBBCContext bbcContext = new DefaultBBCContext();
        bbcContext.setConfForBlockchainClient(FileUtil.readBytes("wjs.json"));

        citaCloudBBCService = new CITACloudBBCService();
        citaCloudBBCService.startup(bbcContext);
    }

    private static String deployAppContract() {
        String address = citaCloudBBCService.getCitaCloudClient().deployContractWithoutConstructor(
                FileUtil.readString("app.bin", StandardCharsets.UTF_8),
                true
        );

        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Address(citaCloudBBCService.getContext().getSdpContract().getContractAddress()));
        citaCloudBBCService.getCitaCloudClient().callContract(
                address,
                "setProtocol",
                inputParameters,
                true
        );

        return address;
    }

    private static byte[] getLatestMsgFromApp(String authorHex, int idx) {
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Bytes32(Numeric.hexStringToByteArray(authorHex)));
        inputParameters.add(new Uint256(idx));

        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(TypeReference.create(DynamicBytes.class));

        List<Type> res = citaCloudBBCService.getCitaCloudClient().localCallContract(
                appContractAddress,
                "recvMsg",
                inputParameters,
                outputParameters
        );
        if (ObjectUtil.isEmpty(res)) {
            return null;
        }
        return ((DynamicBytes) res.get(0)).getValue();
    }

    @Test
    public void test_0_QueryLatestHeight() throws Exception {
        Assert.assertTrue(citaCloudBBCService.queryLatestHeight() > 0);
        System.out.println(citaCloudBBCService.queryLatestHeight());
    }

    @Test
    public void test_1_SetupContracts() {
        citaCloudBBCService.setupAuthMessageContract();

        Assert.assertNotNull(citaCloudBBCService.getContext().getAuthMessageContract());
        Assert.assertEquals(citaCloudBBCService.getContext().getAuthMessageContract().getStatus(), ContractStatusEnum.CONTRACT_DEPLOYED);
        System.out.println(citaCloudBBCService.getContext().getAuthMessageContract().getContractAddress());

        citaCloudBBCService.setupSDPMessageContract();

        Assert.assertNotNull(citaCloudBBCService.getContext().getSdpContract());
        Assert.assertEquals(citaCloudBBCService.getContext().getSdpContract().getStatus(), ContractStatusEnum.CONTRACT_DEPLOYED);
        System.out.println(citaCloudBBCService.getContext().getSdpContract().getContractAddress());
    }

    @Test
    public void test_2_GetContractsReady() {
        if (ObjectUtil.isNull(citaCloudBBCService.getContext().getAuthMessageContract())) {
            test_1_SetupContracts();
        }
        citaCloudBBCService.setAmContract(citaCloudBBCService.getContext().getAuthMessageContract().getContractAddress());
        citaCloudBBCService.setProtocol(citaCloudBBCService.getContext().getSdpContract().getContractAddress(), "0");
        citaCloudBBCService.setLocalDomain("citaCloud");

        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, citaCloudBBCService.getContext().getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, citaCloudBBCService.getContext().getSdpContract().getStatus());

        appContractAddress = deployAppContract();
    }

    @Test
    public void test_3_ReadCCMsg() {
        prepareBBC();
        TransactionReceipt receipt = sendUnorderedSDP();

        List<CrossChainMessage> receipts = citaCloudBBCService.readCrossChainMessagesByHeight(receipt.getBlockNumber().longValue());
        Assert.assertEquals(1, receipts.size());
        IAuthMessage am = AuthMessageFactory.createAuthMessage(receipts.get(0).getMessage());
        Assert.assertEquals(
                "000000000000000000000000" + Numeric.cleanHexPrefix(citaCloudBBCService.getConfig().getAddress()),
                am.getIdentity().toHex()
        );

        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(am.getPayload());
        Assert.assertEquals("awesome antchain bridge", new String(sdpMessage.getPayload()));
        Assert.assertEquals(-1, sdpMessage.getSequence());
        Assert.assertEquals("receiver", sdpMessage.getTargetDomain().getDomain());
        Assert.assertEquals(DigestUtil.sha256Hex("receiver"), sdpMessage.getTargetIdentity().toHex());
    }

    @Test
    public void test_4_RelayAuthMessage() throws IOException, InterruptedException {
        prepareBBC();

        String senderDomain = UUID.randomUUID().toString();
        String senderHex = DigestUtil.sha256Hex(senderDomain);

        byte[] rawMsg = getRawMsgFromRelayer(senderDomain, senderHex);

        CrossChainMessageReceipt receipt = citaCloudBBCService.relayAuthMessage(rawMsg);
        Assert.assertTrue(receipt.isSuccessful());

        CrossChainMessageReceipt receipt1 = citaCloudBBCService.readCrossChainMessageReceipt(receipt.getTxhash());
        Assert.assertTrue(receipt1.isSuccessful());
        Assert.assertTrue(receipt1.getErrorMsg().isEmpty());

        int count = 30;
        while (--count >= 0) {
            receipt1 = citaCloudBBCService.readCrossChainMessageReceipt(receipt.getTxhash());
            if (receipt1.isConfirmed()) {
                Assert.assertTrue(receipt1.isSuccessful());
                break;
            }
            Thread.sleep(1000);
        }

        byte[] rawLatestMsg = getLatestMsgFromApp(senderHex, 0);
        Assert.assertTrue(ObjectUtil.isNotEmpty(rawLatestMsg));
        Assert.assertEquals("awesome antchain bridge", new String(rawLatestMsg));
    }

    @Test
    public void test_5_QuerySeq() throws IOException, InterruptedException {
        prepareBBC();

        String senderDomain = UUID.randomUUID().toString();
        String senderHex = DigestUtil.sha256Hex(senderDomain);

        Assert.assertEquals(
                0,
                citaCloudBBCService.querySDPMessageSeq(
                        senderDomain,
                        senderHex,
                        "citaCloud",
                        String.format("000000000000000000000000%s", Numeric.cleanHexPrefix(appContractAddress))
                )
        );

        byte[] rawMsg = getRawMsgFromRelayer(senderDomain, senderHex);
        CrossChainMessageReceipt receipt = citaCloudBBCService.relayAuthMessage(rawMsg);
        Assert.assertTrue(receipt.isSuccessful());

        int count = 30;
        while (--count >= 0) {
            CrossChainMessageReceipt receipt1 = citaCloudBBCService.readCrossChainMessageReceipt(receipt.getTxhash());
            if (receipt1.isConfirmed()) {
                Assert.assertTrue(receipt1.isSuccessful());
                break;
            }
            Thread.sleep(1000);
        }

        Assert.assertEquals(
                1,
                citaCloudBBCService.querySDPMessageSeq(
                        senderDomain,
                        senderHex,
                        "citaCloud",
                        String.format("000000000000000000000000%s", Numeric.cleanHexPrefix(appContractAddress))
                )
        );
    }

    private void prepareBBC() {
        if (ObjectUtil.isNull(citaCloudBBCService.getContext().getAuthMessageContract())) {
            test_1_SetupContracts();
        }
        if (citaCloudBBCService.getContext().getAuthMessageContract().getStatus() != ContractStatusEnum.CONTRACT_READY) {
            test_2_GetContractsReady();
        }
    }

    private TransactionReceipt sendUnorderedSDP() {
        return sendSDP("sendUnorderedMessage", DigestUtil.sha256Hex("receiver"), "receiver");
    }

    private TransactionReceipt sendOrderedSDPRandomly() {
        return sendSDP("sendMessage", DigestUtil.sha256Hex(UUID.randomUUID().toString()), UUID.randomUUID().toString());
    }

    private TransactionReceipt sendSDP(String method, String receiverHex, String receiverDomain) {
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Utf8String(receiverDomain));
        inputParameters.add(new Bytes32(HexUtil.decodeHex(receiverHex)));
        inputParameters.add(new DynamicBytes("awesome antchain bridge".getBytes()));

        TransactionReceipt receipt = citaCloudBBCService.getCitaCloudClient().callContract(
                citaCloudBBCService.getContext().getSdpContract().getContractAddress(),
                method,
                inputParameters,
                true
        );
        System.out.println(JSON.toJSONString(receipt));
        Assert.assertTrue(citaCloudBBCService.getCitaCloudClient().isTxExecuteSuccess(receipt) && receipt.getBlockNumber().longValue() > 0);

        return receipt;
    }

    private byte[] getRawMsgFromRelayer(String senderDomain, String authorHex) throws IOException {
        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(
                1,
                "citaCloud",
                HexUtil.decodeHex(
                        String.format("000000000000000000000000%s", Numeric.cleanHexPrefix(appContractAddress))
                ),
                0,
                "awesome antchain bridge".getBytes()
        );

        IAuthMessage am = AuthMessageFactory.createAuthMessage(
                1,
                HexUtil.decodeHex(authorHex),
                0,
                sdpMessage.encode()
        );

        MockResp resp = new MockResp();
        resp.setRawResponse(am.encode());

        MockProof proof = new MockProof();
        proof.setResp(resp);
        proof.setDomain(senderDomain);

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
