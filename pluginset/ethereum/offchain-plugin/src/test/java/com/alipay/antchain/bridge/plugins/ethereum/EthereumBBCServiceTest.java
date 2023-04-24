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

package com.alipay.antchain.bridge.plugins.ethereum;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import com.alipay.antchain.bridge.plugins.ethereum.abi.AppContract;
import com.alipay.antchain.bridge.plugins.ethereum.abi.SDPMsg;
import lombok.Getter;
import lombok.Setter;
import com.alipay.antchain.bridge.plugins.ethereum.abi.AuthMsg;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

public class EthereumBBCServiceTest {

    private static final String VALID_URL = "http://localhost:7545";

    private static final String INVALID_URL = "http://localhost:6545";

    // !!! replace to your test key
    private static final String APP_USER_ETH_PRIVATE_KEY = "YourPrivateKey";

    private static final String REMOTE_APP_CONTRACT = "0xdd11AA371492B94AB8CDEdf076F84ECCa72820e1";

    private static final long WAIT_TIME = 15000;

    private static EthereumBBCService ethereumBBCService;

    private static AppContract appContract;

    @Before
    public void init() throws Exception {
        ethereumBBCService = new EthereumBBCService();

        Web3j web3j = Web3j.build(new HttpService(VALID_URL));
        Credentials credentials = Credentials.create(APP_USER_ETH_PRIVATE_KEY);

        RawTransactionManager rawTransactionManager = new RawTransactionManager(
                web3j, credentials, web3j.ethChainId().send().getChainId().longValue());

        appContract = AppContract.deploy(
                web3j,
                rawTransactionManager,
                new DefaultGasProvider()
        ).send();
    }

    @Test
    public void testStartup(){
        // start up success
        AbstractBBCContext mockValidCtx = mockValidCtx();
        ethereumBBCService.startup(mockValidCtx);
        Assert.assertEquals(null, ethereumBBCService.getBbcContext().getAuthMessageContract());
        Assert.assertEquals(null, ethereumBBCService.getBbcContext().getSdpContract());

        // start up failed
        AbstractBBCContext mockInvalidCtx = mockInvalidCtx();
        try {
            ethereumBBCService.startup(mockInvalidCtx);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testStartupWithDeployedContract(){
        // start up a tmp
        AbstractBBCContext mockValidCtx = mockValidCtx();
        EthereumBBCService ethereumBBCServiceTmp = new EthereumBBCService();
        ethereumBBCServiceTmp.startup(mockValidCtx);

        // set up am and sdp
        ethereumBBCServiceTmp.setupAuthMessageContract();
        ethereumBBCServiceTmp.setupSDPMessageContract();
        String amAddr = ethereumBBCServiceTmp.getContext().getAuthMessageContract().getContractAddress();
        String sdpAddr = ethereumBBCServiceTmp.getContext().getSdpContract().getContractAddress();

        // start up success
        AbstractBBCContext ctx = mockValidCtxWithPreDeployedContracts(amAddr, sdpAddr);
        ethereumBBCService.startup(ctx);
        Assert.assertEquals(amAddr, ethereumBBCService.getBbcContext().getAuthMessageContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ethereumBBCService.getBbcContext().getAuthMessageContract().getStatus());
        Assert.assertEquals(sdpAddr, ethereumBBCService.getBbcContext().getSdpContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ethereumBBCService.getBbcContext().getSdpContract().getStatus());
    }

    @Test
    public void testStartupWithReadyContract(){
        // start up a tmp ethereumBBCService to set up contract
        AbstractBBCContext mockValidCtx = mockValidCtx();
        EthereumBBCService ethereumBBCServiceTmp = new EthereumBBCService();
        ethereumBBCServiceTmp.startup(mockValidCtx);

        // set up am and sdp
        ethereumBBCServiceTmp.setupAuthMessageContract();
        ethereumBBCServiceTmp.setupSDPMessageContract();
        String amAddr = ethereumBBCServiceTmp.getContext().getAuthMessageContract().getContractAddress();
        String sdpAddr = ethereumBBCServiceTmp.getContext().getSdpContract().getContractAddress();

        // start up success
        AbstractBBCContext ctx = mockValidCtxWithPreReadyContracts(amAddr, sdpAddr);
        ethereumBBCService.startup(ctx);
        Assert.assertEquals(amAddr, ethereumBBCService.getBbcContext().getAuthMessageContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ethereumBBCService.getBbcContext().getAuthMessageContract().getStatus());
        Assert.assertEquals(sdpAddr, ethereumBBCService.getBbcContext().getSdpContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ethereumBBCService.getBbcContext().getSdpContract().getStatus());
    }

    @Test
    public void testShutdown(){
        AbstractBBCContext mockValidCtx = mockValidCtx();
        ethereumBBCService.startup(mockValidCtx);
        ethereumBBCService.shutdown();
    }

    @Test
    public void testGetContext(){
        AbstractBBCContext mockValidCtx = mockValidCtx();
        ethereumBBCService.startup(mockValidCtx);
        AbstractBBCContext ctx = ethereumBBCService.getContext();
        Assert.assertNotNull(ctx);
        Assert.assertEquals(null, ctx.getAuthMessageContract());
    }

    @Test
    public void testSetupAuthMessageContract(){
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        ethereumBBCService.startup(mockValidCtx);

        // set up am
        ethereumBBCService.setupAuthMessageContract();

        // get context
        AbstractBBCContext ctx = ethereumBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
    }

    @Test
    public void testSetupSDPMessageContract(){
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        ethereumBBCService.startup(mockValidCtx);

        // set up sdp
        ethereumBBCService.setupSDPMessageContract();

        // get context
        AbstractBBCContext ctx = ethereumBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());
    }

    @Test
    public void testQuerySDPMessageSeq(){
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        ethereumBBCService.startup(mockValidCtx);

        // set up sdp
        ethereumBBCService.setupSDPMessageContract();

        // set the domain
        ethereumBBCService.setLocalDomain("receiverDomain");

        // query seq
        long seq = ethereumBBCService.querySDPMessageSeq(
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
        ethereumBBCService.startup(mockValidCtx);

        // set up am
        ethereumBBCService.setupAuthMessageContract();

        // set up sdp
        ethereumBBCService.setupSDPMessageContract();

        // get context
        AbstractBBCContext ctx = ethereumBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        // set protocol to am (sdp type: 0)
        ethereumBBCService.setProtocol(
                ctx.getSdpContract().getContractAddress(),
                "0");

        String addr = AuthMsg.load(
                ethereumBBCService.getBbcContext().getAuthMessageContract().getContractAddress(),
                ethereumBBCService.getWeb3j(),
                ethereumBBCService.getCredentials(),
                new DefaultGasProvider()
        ).getProtocol(BigInteger.ZERO).send();
        System.out.printf("protocol: %s\n", addr);

        // check am status
        ctx = ethereumBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getAuthMessageContract().getStatus());
    }

    @Test
    public void testSetAmContractAndLocalDomain() throws Exception {
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        ethereumBBCService.startup(mockValidCtx);

        // set up am
        ethereumBBCService.setupAuthMessageContract();

        // set up sdp
        ethereumBBCService.setupSDPMessageContract();

        // get context
        AbstractBBCContext ctx = ethereumBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        // set am to sdp
        ethereumBBCService.setAmContract(ctx.getAuthMessageContract().getContractAddress());

        String amAddr = SDPMsg.load(
                ethereumBBCService.getBbcContext().getSdpContract().getContractAddress(),
                ethereumBBCService.getWeb3j(),
                ethereumBBCService.getCredentials(),
                new DefaultGasProvider()
        ).getAmAddress().send();
        System.out.printf("amAddr: %s\n", amAddr);

        // check contract status
        ctx = ethereumBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        // set the domain
        ethereumBBCService.setLocalDomain("receiverDomain");

        byte[] rawDomain = SDPMsg.load(
                ethereumBBCService.getBbcContext().getSdpContract().getContractAddress(),
                ethereumBBCService.getWeb3j(),
                ethereumBBCService.getCredentials(),
                new DefaultGasProvider()
        ).getLocalDomain().send();
        System.out.printf("domain: %s\n", HexUtil.encodeHexStr(rawDomain));

        // check contract status
        ctx = ethereumBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getSdpContract().getStatus());
    }

    @Test
    public void testRelayAuthMessage() throws Exception {
        relayAmPrepare();

        // relay am msg
        CrossChainMessageReceipt receipt = ethereumBBCService.relayAuthMessage(getRawMsgFromRelayer());
        Assert.assertTrue(receipt.isSuccessful());

        System.out.println("sleep 15s for tx to be packaged...");
        Thread.sleep(WAIT_TIME);

        EthGetTransactionReceipt ethGetTransactionReceipt = ethereumBBCService.getWeb3j().ethGetTransactionReceipt(receipt.getTxhash()).send();
        TransactionReceipt transactionReceipt = ethGetTransactionReceipt.getTransactionReceipt().get();
        Assert.assertNotNull(transactionReceipt);
        Assert.assertTrue(transactionReceipt.isStatusOK());
    }

    @Test
    public void testReadCrossChainMessageReceipt() throws IOException, InterruptedException {
        relayAmPrepare();

        // relay am msg
        CrossChainMessageReceipt crossChainMessageReceipt = ethereumBBCService.relayAuthMessage(getRawMsgFromRelayer());

        System.out.println("sleep 15s for tx to be packaged...");
        Thread.sleep(WAIT_TIME);

        // read receipt by txHash
        CrossChainMessageReceipt crossChainMessageReceipt1 = ethereumBBCService.readCrossChainMessageReceipt(crossChainMessageReceipt.getTxhash());
        Assert.assertTrue(crossChainMessageReceipt1.isConfirmed());
        Assert.assertEquals(crossChainMessageReceipt.isSuccessful(), crossChainMessageReceipt1.isSuccessful());
    }

    @Test
    public void testReadCrossChainMessagesByHeight_sendUnordered() throws Exception {
        relayAmPrepare();

        // 1. set sdp addr
        TransactionReceipt receipt = appContract.setProtocol(ethereumBBCService.getBbcContext().getSdpContract().getContractAddress()).send();
        if (receipt.isStatusOK()){
            System.out.printf("set protocol(%s) to app contract(%s) \n",
                    appContract.getContractAddress(),
                    ethereumBBCService.getBbcContext().getSdpContract().getContractAddress());
        } else {
            throw new Exception(String.format("failed to set protocol(%s) to app contract(%s)",
                    appContract.getContractAddress(),
                    ethereumBBCService.getBbcContext().getSdpContract().getContractAddress()));
        }

        // 2. send msg
        try {
            // 2.1 create function
            List<Type> inputParameters = new ArrayList<>();
            inputParameters.add(new Utf8String("remoteDomain"));
            inputParameters.add(new Bytes32(DigestUtil.sha256(REMOTE_APP_CONTRACT)));
            inputParameters.add(new DynamicBytes("UnorderedCrossChainMessage".getBytes()));
            Function function = new Function(
                    AppContract.FUNC_SENDUNORDEREDMESSAGE, // function name
                    inputParameters, // inputs
                    Collections.emptyList() // outputs
            );
            String encodedFunc = FunctionEncoder.encode(function);

            // 2.2 pre-execute before commit tx
            EthCall call = ethereumBBCService.getWeb3j().ethCall(
                    Transaction.createEthCallTransaction(
                            ethereumBBCService.getCredentials().getAddress(),
                            appContract.getContractAddress(),
                            encodedFunc
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();

            // 2.3 async send tx
            EthSendTransaction ethSendTransaction = ethereumBBCService.getRawTransactionManager().sendTransaction(
                    BigInteger.valueOf(ethereumBBCService.getConfig().getGasPrice()),
                    BigInteger.valueOf(ethereumBBCService.getConfig().getGasLimit()),
                    appContract.getContractAddress(),
                    encodedFunc,
                    BigInteger.ZERO
            );

            System.out.printf("send unordered msg tx %s\n", ethSendTransaction.getTransactionHash());
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("failed to send unordered msg"), e
            );
        }

        // 3. query latest height
        long height1 = ethereumBBCService.queryLatestHeight();

        System.out.println("sleep 15s for tx to be packaged...");
        Thread.sleep(WAIT_TIME);

        long height2 = ethereumBBCService.queryLatestHeight();

        // 4. read cc msg
        List<CrossChainMessage> messageList = ListUtil.toList();
        for(long i = height1; i <= height2; i++){
            messageList.addAll(ethereumBBCService.readCrossChainMessagesByHeight(i));
        }
        Assert.assertEquals(1, messageList.size());
        Assert.assertEquals(CrossChainMessage.CrossChainMessageType.AUTH_MSG, messageList.get(0).getType());
    }

    @Test
    public void testReadCrossChainMessagesByHeight_sendOrdered() throws Exception {
        relayAmPrepare();

        // 1. set sdp addr
        TransactionReceipt receipt = appContract.setProtocol(ethereumBBCService.getBbcContext().getSdpContract().getContractAddress()).send();
        if (receipt.isStatusOK()){
            System.out.printf("set protocol(%s) to app contract(%s) \n",
                    appContract.getContractAddress(),
                    ethereumBBCService.getBbcContext().getSdpContract().getContractAddress());
        } else {
            throw new Exception(String.format("failed to set protocol(%s) to app contract(%s)",
                    appContract.getContractAddress(),
                    ethereumBBCService.getBbcContext().getSdpContract().getContractAddress()));
        }

        // 2. send msg
        try {
            // 2.1 create function
            List<Type> inputParameters = new ArrayList<>();
            inputParameters.add(new Utf8String("remoteDomain"));
            inputParameters.add(new Bytes32(DigestUtil.sha256(REMOTE_APP_CONTRACT)));
            inputParameters.add(new DynamicBytes("CrossChainMessage".getBytes()));
            Function function = new Function(
                    AppContract.FUNC_SENDMESSAGE, // function name
                    inputParameters, // inputs
                    Collections.emptyList() // outputs
            );
            String encodedFunc = FunctionEncoder.encode(function);

            // 2.2 pre-execute before commit tx
            EthCall call = ethereumBBCService.getWeb3j().ethCall(
                    Transaction.createEthCallTransaction(
                            ethereumBBCService.getCredentials().getAddress(),
                            appContract.getContractAddress(),
                            encodedFunc
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();

            // 2.3 async send tx
            EthSendTransaction ethSendTransaction = ethereumBBCService.getRawTransactionManager().sendTransaction(
                    BigInteger.valueOf(ethereumBBCService.getConfig().getGasPrice()),
                    BigInteger.valueOf(ethereumBBCService.getConfig().getGasLimit()),
                    appContract.getContractAddress(),
                    encodedFunc,
                    BigInteger.ZERO
            );

            System.out.printf("send ordered msg tx %s\n", ethSendTransaction.getTransactionHash());
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("failed to send ordered msg"), e
            );
        }

        // 3. query latest height
        long height1 = ethereumBBCService.queryLatestHeight();

        System.out.println("sleep 15s for tx to be packaged...");
        Thread.sleep(WAIT_TIME);

        long height2 = ethereumBBCService.queryLatestHeight();

        // 4. read cc msg
        List<CrossChainMessage> messageList = ListUtil.toList();
        for(long i = height1; i <= height2; i++){
            messageList.addAll(ethereumBBCService.readCrossChainMessagesByHeight(i));
        }
        Assert.assertEquals(1, messageList.size());
        Assert.assertEquals(CrossChainMessage.CrossChainMessageType.AUTH_MSG, messageList.get(0).getType());
    }

    private void relayAmPrepare(){
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        ethereumBBCService.startup(mockValidCtx);

        // set up am
        ethereumBBCService.setupAuthMessageContract();

        // set up sdp
        ethereumBBCService.setupSDPMessageContract();

        // set protocol to am (sdp type: 0)
        ethereumBBCService.setProtocol(
                mockValidCtx.getSdpContract().getContractAddress(),
                "0");

        // set am to sdp
        ethereumBBCService.setAmContract(mockValidCtx.getAuthMessageContract().getContractAddress());

        // set local domain to sdp
        ethereumBBCService.setLocalDomain("receiverDomain");

        // check contract ready
        AbstractBBCContext ctxCheck = ethereumBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctxCheck.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctxCheck.getSdpContract().getStatus());
    }

    private AbstractBBCContext mockValidCtx(){
        EthereumConfig mockConf = new EthereumConfig();
        mockConf.setUrl(VALID_URL);
        mockConf.setPrivateKey(APP_USER_ETH_PRIVATE_KEY);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    private AbstractBBCContext mockValidCtxWithPreDeployedContracts(String amAddr, String sdpAddr){
        EthereumConfig mockConf = new EthereumConfig();
        mockConf.setUrl(VALID_URL);
        mockConf.setPrivateKey(APP_USER_ETH_PRIVATE_KEY);
        mockConf.setAmContractAddressDeployed(amAddr);
        mockConf.setSdpContractAddressDeployed(sdpAddr);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    private AbstractBBCContext mockValidCtxWithPreReadyContracts(String amAddr, String sdpAddr){
        EthereumConfig mockConf = new EthereumConfig();
        mockConf.setUrl(VALID_URL);
        mockConf.setPrivateKey(APP_USER_ETH_PRIVATE_KEY);
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

    private AbstractBBCContext mockInvalidCtx(){
        EthereumConfig mockConf = new EthereumConfig();
        mockConf.setUrl(INVALID_URL);
        mockConf.setPrivateKey(APP_USER_ETH_PRIVATE_KEY);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    private byte[] getRawMsgFromRelayer() throws IOException {
        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(
                1,
                "receiverDomain",
                HexUtil.decodeHex(
                        String.format("000000000000000000000000%s", appContract.getContractAddress().replaceAll("0x", ""))
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
