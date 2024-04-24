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

package com.alipay.antchain.bridge.plugins.fiscobcos;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ByteUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
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
import com.alipay.antchain.bridge.plugins.fiscobcos.abi.AppContract;
import com.alipay.antchain.bridge.plugins.fiscobcos.abi.AuthMsg;
import com.alipay.antchain.bridge.plugins.fiscobcos.abi.SDPMsg;
import lombok.Getter;
import lombok.Setter;
import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes;
import org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.model.callback.TransactionCallback;
import org.fisco.bcos.sdk.v3.transaction.manager.AssembleTransactionProcessor;
import org.fisco.bcos.sdk.v3.transaction.manager.TransactionProcessorFactory;
import org.fisco.bcos.sdk.v3.transaction.model.dto.TransactionResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;


public class FISCOBCOSBBCServiceTest {
    private static final String VALID_FILENAME = "config.toml";

    private static final String INVALID_FILENAME = "config-example.toml";

    private static final String VALID_GROUPID = "group0";

    private static final long WAIT_TIME = 5000;

    private static FISCOBCOSBBCService fiscobcosBBCService;

    private static AppContract appContract;

    private static final String REMOTE_APP_CONTRACT = "0xdd11AA371492B94AB8CDEdf076F84ECCa72820e1";

    @Before
    public void init() throws Exception{
        fiscobcosBBCService = new FISCOBCOSBBCService();

        BcosSDK sdk = BcosSDK.build(FISCOBCOSBBCService.class.getClassLoader().getResource(VALID_FILENAME).getPath());
        Client client = sdk.getClient(VALID_GROUPID);

        appContract = AppContract.deploy(client, client.getCryptoSuite().getCryptoKeyPair());
    }

    @Test
    public void testStart(){
        fiscobcosBBCService.start();
    }

    @Test
    public void testStartup(){
        // start up success
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);
        Assert.assertEquals(null, fiscobcosBBCService.getBbcContext().getAuthMessageContract());
        Assert.assertEquals(null, fiscobcosBBCService.getBbcContext().getSdpContract());
        // start up failed
        AbstractBBCContext mockInvalidCtx = mockInvalidCtx();
        try {
            fiscobcosBBCService.startup(mockInvalidCtx);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testStartupWithDeployedContract(){
        // start up a tmp
        AbstractBBCContext mockValidCtx = mockValidCtx();
        FISCOBCOSBBCService fiscobcosBBCServiceTmp = new FISCOBCOSBBCService();
        fiscobcosBBCServiceTmp.startup(mockValidCtx);

        // set up am and sdp
        fiscobcosBBCServiceTmp.setupAuthMessageContract();
        fiscobcosBBCServiceTmp.setupSDPMessageContract();
        String amAddr = fiscobcosBBCServiceTmp.getContext().getAuthMessageContract().getContractAddress();
        String sdpAddr = fiscobcosBBCServiceTmp.getContext().getSdpContract().getContractAddress();

        // start up success
        AbstractBBCContext ctx = mockValidCtxWithPreDeployedContracts(amAddr, sdpAddr);
        fiscobcosBBCService.startup(ctx);
        Assert.assertEquals(amAddr, fiscobcosBBCService.getBbcContext().getAuthMessageContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, fiscobcosBBCService.getBbcContext().getAuthMessageContract().getStatus());
        Assert.assertEquals(sdpAddr, fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, fiscobcosBBCService.getBbcContext().getSdpContract().getStatus());
    }

    @Test
    public void testStartupWithReadyContract(){
        // start up a tmp fiscobcosBBCService to set up contract
        AbstractBBCContext mockValidCtx = mockValidCtx();
        FISCOBCOSBBCService fiscobcosBBCServiceTmp = new FISCOBCOSBBCService();
        fiscobcosBBCServiceTmp.startup(mockValidCtx);

        // set up am and sdp
        fiscobcosBBCServiceTmp.setupAuthMessageContract();
        fiscobcosBBCServiceTmp.setupSDPMessageContract();
        String amAddr = fiscobcosBBCServiceTmp.getContext().getAuthMessageContract().getContractAddress();
        String sdpAddr = fiscobcosBBCServiceTmp.getContext().getSdpContract().getContractAddress();

        // start up success
        AbstractBBCContext ctx = mockValidCtxWithPreReadyContracts(amAddr, sdpAddr);
        fiscobcosBBCService.startup(ctx);
        Assert.assertEquals(amAddr, fiscobcosBBCService.getBbcContext().getAuthMessageContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, fiscobcosBBCService.getBbcContext().getAuthMessageContract().getStatus());
        Assert.assertEquals(sdpAddr, fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, fiscobcosBBCService.getBbcContext().getSdpContract().getStatus());
    }

    @Test
    public void testShutdown(){
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);
        fiscobcosBBCService.shutdown();
    }

    @Test
    public void testGetContext(){
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);
        AbstractBBCContext ctx = fiscobcosBBCService.getContext();
        Assert.assertNotNull(ctx);
        Assert.assertEquals(null, ctx.getAuthMessageContract());
    }
    @Test
    public void testQueryLatestHeight(){
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);
        Assert.assertNotNull(fiscobcosBBCService.queryLatestHeight());
    }
    @Test
    public void testSetupAuthMessageContract(){
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);

        // set up am
        fiscobcosBBCService.setupAuthMessageContract();

        // get context
        AbstractBBCContext ctx = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
    }

    @Test
    public void testSetupSDPMessageContract(){
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);

        // set up sdp
        fiscobcosBBCService.setupSDPMessageContract();

        // get context
        AbstractBBCContext ctx = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());
    }

    @Test
    public void testQuerySDPMessageSeq(){
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);

        // set up sdp
        fiscobcosBBCService.setupSDPMessageContract();

        // set the domain
        fiscobcosBBCService.setLocalDomain("receiverDomain");

        // query seq
        long seq = fiscobcosBBCService.querySDPMessageSeq(
                "senderDomain",
                DigestUtil.sha256Hex("senderID"),
                "receiverDomain",
                DigestUtil.sha256Hex("receiverID")
        );
        Assert.assertEquals(0L, seq);
    }

    @Test
    public void testSetProtocol() throws Exception {
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);
        AbstractBBCContext ctx;

        // set up am
        fiscobcosBBCService.setupAuthMessageContract();

        // set up sdp
        fiscobcosBBCService.setupSDPMessageContract();

        // get context
        ctx = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        // set protocol to am (sdp type: 0)
        fiscobcosBBCService.setProtocol(
                ctx.getSdpContract().getContractAddress(),
                "0");

        String addr = AuthMsg.load(
                fiscobcosBBCService.getBbcContext().getAuthMessageContract().getContractAddress(),
                fiscobcosBBCService.getClient(),
                fiscobcosBBCService.getKeyPair()
        ).getProtocol(BigInteger.ZERO);
        System.out.printf("protocol: %s\n", addr);

        // check am status
        ctx = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getAuthMessageContract().getStatus());
    }

    @Test
    public void testSetAmContractAndLocalDomain() throws Exception {
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);

        // set up am
        fiscobcosBBCService.setupAuthMessageContract();

        // set up sdp
        fiscobcosBBCService.setupSDPMessageContract();

        // get context
        AbstractBBCContext ctx = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        // set am to sdp
        fiscobcosBBCService.setAmContract(ctx.getAuthMessageContract().getContractAddress());

        String amAddr = SDPMsg.load(
                fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress(),
                fiscobcosBBCService.getClient(),
                fiscobcosBBCService.getKeyPair()
        ).getAmAddress();
        System.out.printf("amAddr: %s\n", amAddr);

        // check contract status
        ctx = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        // set the domain
        fiscobcosBBCService.setLocalDomain("receiverDomain");

        byte[] rawDomain = SDPMsg.load(
                fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress(),
                fiscobcosBBCService.getClient(),
                fiscobcosBBCService.getKeyPair()
        ).getLocalDomain();
        System.out.printf("domain: %s\n", HexUtil.encodeHexStr(rawDomain));

        // check contract status
        ctx = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getSdpContract().getStatus());
    }

    @Test
    public void testRelayAuthMessage() throws Exception {
        relayAmPrepare();

        // relay am msg
        CrossChainMessageReceipt receipt = fiscobcosBBCService.relayAuthMessage(getRawMsgFromRelayer());
        System.out.println(receipt.getErrorMsg());
        System.out.println(receipt.isSuccessful());

        System.out.println("sleep 15s for tx to be packaged...");
        Thread.sleep(WAIT_TIME);

        TransactionReceipt transactionReceipt = fiscobcosBBCService.getClient().getTransactionReceipt(receipt.getTxhash(), false).getTransactionReceipt();
        Assert.assertNotNull(transactionReceipt);
        Assert.assertTrue(transactionReceipt.isStatusOK());
    }

    @Test
    public void testReadCrossChainMessageReceipt() throws IOException, InterruptedException {
        relayAmPrepare();

        // relay am msg
        CrossChainMessageReceipt crossChainMessageReceipt = fiscobcosBBCService.relayAuthMessage(getRawMsgFromRelayer());

        System.out.println("sleep 15s for tx to be packaged...");
        Thread.sleep(WAIT_TIME);

        // read receipt by txHash
        CrossChainMessageReceipt crossChainMessageReceipt1 = fiscobcosBBCService.readCrossChainMessageReceipt(crossChainMessageReceipt.getTxhash());
        Assert.assertTrue(crossChainMessageReceipt1.isConfirmed());
    }

    @Test
    public void testReadCrossChainMessagesByHeight_sendUnordered() throws Exception {
        relayAmPrepare();

        // 1. set sdp addr
        TransactionReceipt receipt = appContract.setProtocol(fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress());
        if (receipt.isStatusOK()){
            System.out.printf("set protocol(%s) to app contract(%s) \n",
                    appContract.getContractAddress(),
                    fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress());
        } else {
            throw new Exception(String.format("failed to set protocol(%s) to app contract(%s)",
                    appContract.getContractAddress(),
                    fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress()));
        }

        // 2. send msg
        try {
            // 2.1 create inputParameters
            List<Object> inputParameters = new ArrayList<>();
            inputParameters.add(new Utf8String("remoteDomain"));
            inputParameters.add(new Bytes32(DigestUtil.sha256(REMOTE_APP_CONTRACT)));
            inputParameters.add(new DynamicBytes("UnorderedCrossChainMessage".getBytes()));

            // 2.2 async send tx
            AssembleTransactionProcessor transactionProcessor = TransactionProcessorFactory.createAssembleTransactionProcessor(
                    fiscobcosBBCService.getClient(),
                    fiscobcosBBCService.getKeyPair(),
                    fiscobcosBBCService.abiFile,
                    fiscobcosBBCService.binFile
            );
            transactionProcessor.sendTransactionAndGetReceiptByContractLoaderAsync(
                    "AppContract", // contract name
                    appContract.getContractAddress(),  // contract address
                    AppContract.FUNC_SENDUNORDEREDMESSAGE, // function name
                    inputParameters, // input
                    new TransactionCallback() { // callback
                        @Override
                        public void onResponse(TransactionReceipt receipt) {
                            System.out.printf("send unordered msg tx %s\n", receipt.getTransactionHash());
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException(
                    "failed to send unordered msg", e
            );
        }

        // 3. query latest height
        long height1 = fiscobcosBBCService.queryLatestHeight();

        System.out.printf("sleep %ds for tx to be packaged...%n", WAIT_TIME / 100);
        Thread.sleep(WAIT_TIME);

        long height2 = fiscobcosBBCService.queryLatestHeight();

        // 4. read cc msg
        List<CrossChainMessage> messageList = ListUtil.toList();
        for(long i = height1; i <= height2; i++){
            messageList.addAll(fiscobcosBBCService.readCrossChainMessagesByHeight(i));
        }
        Assert.assertEquals(1, messageList.size());
        Assert.assertEquals(CrossChainMessage.CrossChainMessageType.AUTH_MSG, messageList.get(0).getType());
    }

    @Test
    public void testReadCrossChainMessagesByHeight_sendOrdered() throws Exception {
        relayAmPrepare();

        // 1. set sdp addr
        TransactionReceipt receipt = appContract.setProtocol(fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress());
        if (receipt.isStatusOK()){
            System.out.printf("set protocol(%s) to app contract(%s) \n",
                    appContract.getContractAddress(),
                    fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress());
        } else {
            throw new Exception(String.format("failed to set protocol(%s) to app contract(%s)",
                    appContract.getContractAddress(),
                    fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress()));
        }

        // 2. send msg
        try {
            // 2.1 create inputParameters
            List<Object> inputParameters = new ArrayList<>();
            inputParameters.add(new Utf8String("remoteDomain"));
            inputParameters.add(new Bytes32(DigestUtil.sha256(REMOTE_APP_CONTRACT)));
            inputParameters.add(new DynamicBytes("UnorderedCrossChainMessage".getBytes()));

            // 2.2 async send tx
            AssembleTransactionProcessor transactionProcessor = TransactionProcessorFactory.createAssembleTransactionProcessor(
                    fiscobcosBBCService.getClient(),
                    fiscobcosBBCService.getKeyPair(),
                    fiscobcosBBCService.abiFile,
                    fiscobcosBBCService.binFile
            );
            transactionProcessor.sendTransactionAndGetReceiptByContractLoaderAsync(
                    "AppContract", // contract name
                    appContract.getContractAddress(),  // contract address
                    AppContract.FUNC_SENDMESSAGE, // function name
                    inputParameters, // input
                    new TransactionCallback() { // callback
                        @Override
                        public void onResponse(TransactionReceipt receipt) {
                            System.out.printf("send ordered msg tx %s\n", receipt.getTransactionHash());
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException(
                    "failed to send ordered msg", e
            );
        }

        // 3. query latest height
        long height1 = fiscobcosBBCService.queryLatestHeight();

        System.out.printf("sleep %ds for tx to be packaged...%n", WAIT_TIME / 100);
        Thread.sleep(WAIT_TIME);

        long height2 = fiscobcosBBCService.queryLatestHeight();

        // 4. read cc msg
        List<CrossChainMessage> messageList = ListUtil.toList();
        for(long i = height1; i <= height2; i++){
            messageList.addAll(fiscobcosBBCService.readCrossChainMessagesByHeight(i));
        }
        Assert.assertEquals(1, messageList.size());
        Assert.assertEquals(CrossChainMessage.CrossChainMessageType.AUTH_MSG, messageList.get(0).getType());
    }

    private void relayAmPrepare(){
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);

        // set up am
        fiscobcosBBCService.setupAuthMessageContract();

        // set up sdp
        fiscobcosBBCService.setupSDPMessageContract();

        // set protocol to am (sdp type: 0)
        fiscobcosBBCService.setProtocol(
                mockValidCtx.getSdpContract().getContractAddress(),
                "0");
        System.out.println("sdp address:"+mockValidCtx.getSdpContract().getContractAddress());
        System.out.println("am address:"+mockValidCtx.getAuthMessageContract().getContractAddress());

        // set am to sdp
        fiscobcosBBCService.setAmContract(mockValidCtx.getAuthMessageContract().getContractAddress());

        // set local domain to sdp
        fiscobcosBBCService.setLocalDomain("receiverDomain");

        // check contract ready
        AbstractBBCContext ctxCheck = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctxCheck.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctxCheck.getSdpContract().getStatus());
    }



    private AbstractBBCContext mockValidCtx(){
        FISCOBCOSConfig mockConf = new FISCOBCOSConfig();
        mockConf.setFileName(VALID_FILENAME);
        mockConf.setGroupID(VALID_GROUPID);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }
    private AbstractBBCContext mockInvalidCtx(){
        FISCOBCOSConfig mockConf = new FISCOBCOSConfig();
        mockConf.setFileName(INVALID_FILENAME);
        mockConf.setGroupID(VALID_GROUPID);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    private AbstractBBCContext mockValidCtxWithPreDeployedContracts(String amAddr, String sdpAddr){
        FISCOBCOSConfig mockConf = new FISCOBCOSConfig();
        mockConf.setFileName(VALID_FILENAME);
        mockConf.setGroupID(VALID_GROUPID);
        mockConf.setAmContractAddressDeployed(amAddr);
        mockConf.setSdpContractAddressDeployed(sdpAddr);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    private AbstractBBCContext mockValidCtxWithPreReadyContracts(String amAddr, String sdpAddr){
        FISCOBCOSConfig mockConf = new FISCOBCOSConfig();
        mockConf.setFileName(VALID_FILENAME);
        mockConf.setGroupID(VALID_GROUPID);
        mockConf.setAmContractAddressDeployed(amAddr);
        mockConf.setSdpContractAddressDeployed(sdpAddr);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
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
    }

    private byte[] getRawMsgFromRelayer() throws IOException {
        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(
                1,
                "receiverDomain",
                HexUtil.decodeHex(
                        String.format("000000000000000000000000%s", HexUtil.encodeHexStr(RandomUtil.randomBytes(20)))
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

    /**
     * Get the sdp message payload from the raw bytes
     * which is the input for {@link com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService#relayAuthMessage(byte[])}
     *
     * @param raw the input for {@link com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService#relayAuthMessage(byte[])}
     * @return {@code byte[]} sdp payload
     */
    private static byte[] getSDPPayloadFromRawMsg(byte[] raw) {
        ByteArrayInputStream stream = new ByteArrayInputStream(raw);

        byte[] zeros = new byte[4];
        stream.read(zeros, 0, 4);

        byte[] rawLen = new byte[4];
        stream.read(rawLen, 0, 4);

        int len = ByteUtil.bytesToInt(rawLen, ByteOrder.BIG_ENDIAN);

        byte[] rawProof = new byte[len];
        stream.read(rawProof, 0, len);

        MockProof proof = TLVUtils.decode(rawProof, MockProof.class);
        IAuthMessage authMessage = AuthMessageFactory.createAuthMessage(proof.getResp().getRawResponse());
        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(authMessage.getPayload());

        return sdpMessage.getPayload();
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
