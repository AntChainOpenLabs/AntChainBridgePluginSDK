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

package com.alipay.antchain.bridge.plugins.chainmaker;

import java.io.*;
import java.util.*;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.digest.DigestUtil;

import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.AuthMessageContract;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.bbc.syscontract.SDPContract;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageFactory;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.commons.core.sdp.ISDPMessage;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageFactory;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;

import org.chainmaker.pb.common.*;
import org.chainmaker.sdk.*;
import org.chainmaker.sdk.crypto.ChainMakerCryptoSuiteException;
import org.chainmaker.sdk.utils.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes32;

public class ChainMakerBBCServiceTest {

    private static final String SDK_CONFIG = "sdk_config.yml";
    private static final String SDK_CONFIG_JSON = "chainmaker.json";
    private static final String APP_PATH = "AppContract.bin";
    private static final String AM_PATH = "AuthMsg.bin";
    private static final String SDP_PATH = "SDPMsg.bin";
    private static final String APP_NAME = "AppContract";
    private static final String AM_NAME = "AuthMsg";
    private static final String SDP_NAME = "SDPMsg";

    private static final long rpcCallTimeout = 10000;
    private static final long syncResultTimeout = 10000;
    public static final String amAddr = "46c6ef0df655e2d1e0051ece3201908c64c57989";
    public static final String sdpAddr = "df3638f1fa346af36f2125698dd4232355067ee2";
    public static final String appAddr = "9977770745623cb06b7b48b7e9f509f829b2f1b2";
    private static final String REMOTE_APP_CONTRACT = "0xdd11AA371492B94AB8CDEdf076F84ECCa72820e1";

    private static ChainMakerBBCService chainMakerBBCService;
    private ChainManager chainManager;
    private ChainClient chainClient;
    private String clientAddress;

    @Before
    public void init() throws Exception {
        chainMakerBBCService = new ChainMakerBBCService();
    }

    @Test
    public void testStartup() {
        // start up success
        AbstractBBCContext mockValidCtx = mockValidCtx();
        chainMakerBBCService.startup(mockValidCtx);
        Assert.assertNull(chainMakerBBCService.getBbcContext().getAuthMessageContract());
        Assert.assertNull(chainMakerBBCService.getBbcContext().getSdpContract());

//        Assert.assertTrue(chainMakerBBCService.myTest);

        // start up failed
//        AbstractBBCContext mockInvalidCtx = mockInvalidCtx();
//        try {
//            chainMakerBBCService.startup(mockInvalidCtx);
//        } catch (Exception e){
//            e.printStackTrace();
//        }
    }

    @Test
    public void testStartupWithDeployedContract(){
        AbstractBBCContext mockValidCtx = mockValidCtxWithPreDeployedContracts();
        chainMakerBBCService.startup(mockValidCtx);
        Assert.assertEquals(amAddr, chainMakerBBCService.getBbcContext().getAuthMessageContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, chainMakerBBCService.getBbcContext().getAuthMessageContract().getStatus());
        Assert.assertEquals(sdpAddr, chainMakerBBCService.getBbcContext().getSdpContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, chainMakerBBCService.getBbcContext().getSdpContract().getStatus());
    }

    @Test
    public void testStartupWithReadyContract(){
        AbstractBBCContext mockValidCtx = mockValidCtxWithPreReadyContracts();
        chainMakerBBCService.startup(mockValidCtx);
        Assert.assertEquals(amAddr, chainMakerBBCService.getBbcContext().getAuthMessageContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, chainMakerBBCService.getBbcContext().getAuthMessageContract().getStatus());
        Assert.assertEquals(sdpAddr, chainMakerBBCService.getBbcContext().getSdpContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, chainMakerBBCService.getBbcContext().getSdpContract().getStatus());
    }

    @Test
    public void testShutdown(){
        AbstractBBCContext mockValidCtx = mockValidCtx();
        chainMakerBBCService.startup(mockValidCtx);
        chainMakerBBCService.shutdown();
    }

    @Test
    public void testGetContext(){
        AbstractBBCContext mockValidCtx = mockValidCtx();
        chainMakerBBCService.startup(mockValidCtx);
        AbstractBBCContext ctx = chainMakerBBCService.getContext();
        Assert.assertNotNull(ctx);
        Assert.assertNull(ctx.getAuthMessageContract());
    }

    @Test
    public void testSetupAuthMessageContract(){
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        chainMakerBBCService.startup(mockValidCtx);

        // set up am
        chainMakerBBCService.setupAuthMessageContract();

        // get context
        AbstractBBCContext ctx = chainMakerBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());

    }

    @Test
    public void testSetupSDPMessageContract(){
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        chainMakerBBCService.startup(mockValidCtx);

        // set up sdp
        chainMakerBBCService.setupSDPMessageContract();

        // get context
        AbstractBBCContext ctx = chainMakerBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());
    }

    @Test
    public void testQuerySDPMessageSeq(){
        AbstractBBCContext mockValidCtx = mockValidCtxWithPreReadyContracts();
        chainMakerBBCService.startup(mockValidCtx);
        long seq = chainMakerBBCService.querySDPMessageSeq(
                "senderDomain",
                DigestUtil.sha256Hex("senderID"),
                "receiverDomain",
                DigestUtil.sha256Hex("receiverID")
        );
        Assert.assertEquals(0L, seq);
    }

    @Test
    public void testSetProtocol() throws Exception {
        AbstractBBCContext mockValidCtx = mockValidCtxWithPreDeployedContracts();
        chainMakerBBCService.startup(mockValidCtx);
        AbstractBBCContext ctx = chainMakerBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        chainMakerBBCService.setProtocol(ctx.getSdpContract().getContractAddress(), "0");

        ctx = chainMakerBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getAuthMessageContract().getStatus());
    }

    @Test
    public void testSetAmContractAndLocalDomain() throws Exception {
        AbstractBBCContext mockValidCtx = mockValidCtxWithPreDeployedContracts();
        chainMakerBBCService.startup(mockValidCtx);
        AbstractBBCContext ctx = chainMakerBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        chainMakerBBCService.setAmContract(ctx.getAuthMessageContract().getContractAddress());
        ctx = chainMakerBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        chainMakerBBCService.setLocalDomain("receiverDomain");
        ctx = chainMakerBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getSdpContract().getStatus());
    }

    @Test
    public void testRelayAuthMessage() throws Exception {
        AbstractBBCContext mockValidCtx = mockValidCtxWithPreReadyContracts();
        chainMakerBBCService.startup(mockValidCtx);

        // relay am msg
        CrossChainMessageReceipt receipt = chainMakerBBCService.relayAuthMessage(getRawMsgFromRelayer());
        Assert.assertTrue(receipt.isSuccessful());

        System.out.println("sleep 10s for tx to be packaged...");
        Thread.sleep(10000);

        ChainmakerTransaction.TransactionInfo transactionInfo =
                chainMakerBBCService.getChainClient().getTxByTxId(receipt.getTxhash(), rpcCallTimeout);
        System.out.println(receipt.getTxhash());
        System.out.println(transactionInfo.toString());
        Assert.assertNotNull(transactionInfo);
    }

    @Test
    public void testReadCrossChainMessageReceipt() throws IOException, InterruptedException {
        AbstractBBCContext mockValidCtx = mockValidCtxWithPreReadyContracts();
        chainMakerBBCService.startup(mockValidCtx);

        // relay am msg
        CrossChainMessageReceipt receipt = chainMakerBBCService.relayAuthMessage(getRawMsgFromRelayer());

        System.out.println("sleep 10s for tx to be packaged...");
        Thread.sleep(10000);

        // read receipt by txHash
        CrossChainMessageReceipt receipt1 = chainMakerBBCService.readCrossChainMessageReceipt(receipt.getTxhash());
        System.out.println(receipt.getTxhash());
        Assert.assertTrue(receipt1.isConfirmed());
        Assert.assertEquals(receipt.isSuccessful(), receipt1.isSuccessful());
    }

    @Test
    public void testReadCrossChainMessagesByHeight_sendUnordered() throws Exception {
        AbstractBBCContext mockValidCtx = mockValidCtxWithPreReadyContracts();
        chainMakerBBCService.startup(mockValidCtx);

        // 1. send msg
        try {
            Map<String, byte[]> params = new HashMap<>();
            List<Type> inputParameters = new ArrayList<>();
            inputParameters.add(new Utf8String("remoteDomain"));
            inputParameters.add(new Bytes32(DigestUtil.sha256(REMOTE_APP_CONTRACT)));
            inputParameters.add(new DynamicBytes("UnorderedCrossChainMessage".getBytes()));
            Function function = new Function(
                    "sendUnorderedMessage", // function name
                    inputParameters,
                    Collections.<TypeReference<?>>emptyList() // outputs
            );
            String methodDataStr = FunctionEncoder.encode(function);
            String method = methodDataStr.substring(0, 10);
            params.put("data", methodDataStr.getBytes());

            ResultOuterClass.TxResponse responseInfo = null;
            responseInfo = chainMakerBBCService.getChainClient().invokeContract(Utils.calcContractName(APP_NAME),
                    method, null, params, rpcCallTimeout, syncResultTimeout);

            System.out.println("responseInfo\n" + responseInfo.toString());
            Assert.assertEquals(ResultOuterClass.TxStatusCode.SUCCESS, responseInfo.getCode());

        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("failed to send unordered msg"), e
            );
        }

        // 2. query latest height
        long height1 = chainMakerBBCService.queryLatestHeight();

        System.out.println("sleep 10s for tx to be packaged...");
        Thread.sleep(10000);

        long height2 = chainMakerBBCService.queryLatestHeight();

        // 3. read cc msg
        List<CrossChainMessage> messageList = ListUtil.toList();
        for(long i = height1; i <= height2; i++){
            messageList.addAll(chainMakerBBCService.readCrossChainMessagesByHeight(i));
        }
        Assert.assertEquals(1, messageList.size());
        Assert.assertEquals(CrossChainMessage.CrossChainMessageType.AUTH_MSG, messageList.get(0).getType());
    }

    @Test
    public void testReadCrossChainMessagesByHeight_sendOrdered() throws Exception {
        AbstractBBCContext mockValidCtx = mockValidCtxWithPreReadyContracts();
        chainMakerBBCService.startup(mockValidCtx);

        // 1. send msg
        try {
            Map<String, byte[]> params = new HashMap<>();
            List<Type> inputParameters = new ArrayList<>();
            inputParameters.add(new Utf8String("remoteDomain"));
            inputParameters.add(new Bytes32(DigestUtil.sha256(REMOTE_APP_CONTRACT)));
            inputParameters.add(new DynamicBytes("CrossChainMessage".getBytes()));
            Function function = new Function(
                    "sendMessage", // function name
                    inputParameters,
                    Collections.<TypeReference<?>>emptyList() // outputs
            );
            String methodDataStr = FunctionEncoder.encode(function);
            String method = methodDataStr.substring(0, 10);
            params.put("data", methodDataStr.getBytes());

            ResultOuterClass.TxResponse responseInfo = null;
            responseInfo = chainMakerBBCService.getChainClient().invokeContract(Utils.calcContractName(APP_NAME),
                    method, null, params, rpcCallTimeout, syncResultTimeout);

            System.out.println("responseInfo\n" + responseInfo.toString());
            Assert.assertEquals(ResultOuterClass.TxStatusCode.SUCCESS, responseInfo.getCode());

        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("failed to send msg"), e
            );
        }

        // 2. query latest height
        long height1 = chainMakerBBCService.queryLatestHeight();

        System.out.println("sleep 10s for tx to be packaged...");
        Thread.sleep(10000);

        long height2 = chainMakerBBCService.queryLatestHeight();

        // 3. read cc msg
        List<CrossChainMessage> messageList = ListUtil.toList();
        for(long i = height1; i <= height2; i++){
            messageList.addAll(chainMakerBBCService.readCrossChainMessagesByHeight(i));
        }
        Assert.assertEquals(1, messageList.size());
        Assert.assertEquals(CrossChainMessage.CrossChainMessageType.AUTH_MSG, messageList.get(0).getType());

    }

    private AbstractBBCContext mockValidCtx() {
        ChainMakerConfig mockConf = new ChainMakerConfig();
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(SDK_CONFIG_JSON));
            StringBuilder jsonStringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }
            String jsonString = jsonStringBuilder.toString();
            mockConf = ChainMakerConfig.fromJsonString(jsonString);

            mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
            return mockCtx;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mockCtx;
    }

    private AbstractBBCContext mockValidCtxWithPreDeployedContracts(){
        ChainMakerConfig mockConf = new ChainMakerConfig();
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(SDK_CONFIG_JSON));
            StringBuilder jsonStringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }
            String jsonString = jsonStringBuilder.toString();
            mockConf = ChainMakerConfig.fromJsonString(jsonString);
            mockConf.setAmContractAddressDeployed(amAddr);
            mockConf.setSdpContractAddressDeployed(sdpAddr);
            mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
            return mockCtx;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mockCtx;
    }

    private AbstractBBCContext mockValidCtxWithPreReadyContracts(){
        ChainMakerConfig mockConf = new ChainMakerConfig();
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(SDK_CONFIG_JSON));
            StringBuilder jsonStringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }
            String jsonString = jsonStringBuilder.toString();
            mockConf = ChainMakerConfig.fromJsonString(jsonString);
            mockConf.setAmContractAddressDeployed(amAddr);
            mockConf.setSdpContractAddressDeployed(sdpAddr);

            mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());

            AuthMessageContract authMessageContract = new AuthMessageContract();
            authMessageContract.setContractAddress(amAddr);
            authMessageContract.setStatus(ContractStatusEnum.CONTRACT_READY);
            mockCtx.setAuthMessageContract(authMessageContract);

            SDPContract sdpContract = new SDPContract();
            sdpContract.setContractAddress(sdpAddr);
            sdpContract.setStatus(ContractStatusEnum.CONTRACT_READY);
            mockCtx.setSdpContract(sdpContract);

            return mockCtx;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mockCtx;
    }

    private AbstractBBCContext mockInvalidCtx() {
        ChainMakerConfig mockConf = new ChainMakerConfig();
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    // return a byte[] containing rawProof and its length, used to send msg in relayers
    private byte[] getRawMsgFromRelayer() throws IOException {
        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(
                1,
                "SDPMessageV2Used".getBytes(),
                "receiverDomain",
                HexUtil.decodeHex(
                        String.format("000000000000000000000000%s", appAddr)
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