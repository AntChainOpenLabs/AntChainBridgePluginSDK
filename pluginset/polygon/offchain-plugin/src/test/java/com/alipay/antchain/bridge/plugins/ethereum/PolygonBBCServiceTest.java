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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ByteUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
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
import com.alipay.antchain.bridge.plugins.ethereum.abi.AuthMsg;
import com.alipay.antchain.bridge.plugins.ethereum.abi.SDPMsg;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
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
import org.web3j.tx.gas.StaticGasProvider;

@Slf4j
public class PolygonBBCServiceTest {

    /*
     * The polygon pos official rpc service has limitation about calling frequency,
     *  please using some solid rpc service.
     */
    private static final String VALID_URL = "https://rpc-amoy.polygon.technology/";

    private static final String INVALID_URL = "http://localhost:6545";

    // !!! replace to your test key
    // using https://faucet.polygon.technology/ to get the Amoy MATIC
    private static final String APP_USER_ETH_PRIVATE_KEY = "YourPrivateKey";

    private static final String REMOTE_APP_CONTRACT = "0xdd11AA371492B94AB8CDEdf076F84ECCa72820e1";

    private static final long MAX_TX_RESULT_QUERY_TIME = 100;

    private static PolygonBBCService polygonBBCService;

    private static AppContract appContract;

    private static boolean setupBBC;

    @BeforeClass
    public static void init() throws Exception {
        if (StrUtil.equals(APP_USER_ETH_PRIVATE_KEY, "YourPrivateKey")) {
            throw new IllegalArgumentException(
                    "You must set the variable `APP_USER_ETH_PRIVATE_KEY` a valid blockchain account private key. "
            );
        }

        polygonBBCService = new PolygonBBCService();
        Method method = AbstractBBCService.class.getDeclaredMethod("setLogger", Logger.class);
        method.setAccessible(true);
        method.invoke(polygonBBCService, log);

        Web3j web3j = Web3j.build(new HttpService(VALID_URL));
        Credentials credentials = Credentials.create(APP_USER_ETH_PRIVATE_KEY);

        RawTransactionManager rawTransactionManager = new RawTransactionManager(
                web3j, credentials, web3j.ethChainId().send().getChainId().longValue());

        appContract = AppContract.deploy(
                web3j,
                rawTransactionManager,
                new StaticGasProvider(BigInteger.valueOf(2300000000L), BigInteger.valueOf(3000000))
        ).send();
    }

    @Test
    public void testStartup() {
        // start up success
        AbstractBBCContext mockValidCtx = mockValidCtx();
        PolygonBBCService polygonBBCService = new PolygonBBCService();
        polygonBBCService.startup(mockValidCtx);
        Assert.assertNull(polygonBBCService.getBbcContext().getAuthMessageContract());
        Assert.assertNull(polygonBBCService.getBbcContext().getSdpContract());

        // start up failed
        AbstractBBCContext mockInvalidCtx = mockInvalidCtx();
        try {
            polygonBBCService.startup(mockInvalidCtx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testStartupWithDeployedContract() {
        // start up a tmp
        AbstractBBCContext mockValidCtx = mockValidCtx();
        PolygonBBCService bbcServiceTmp = new PolygonBBCService();
        bbcServiceTmp.startup(mockValidCtx);

        // set up am and sdp
        bbcServiceTmp.setupAuthMessageContract();
        bbcServiceTmp.setupSDPMessageContract();
        String amAddr = bbcServiceTmp.getContext().getAuthMessageContract().getContractAddress();
        String sdpAddr = bbcServiceTmp.getContext().getSdpContract().getContractAddress();

        // start up success
        AbstractBBCContext ctx = mockValidCtxWithPreDeployedContracts(amAddr, sdpAddr);
        PolygonBBCService polygonBBCService = new PolygonBBCService();
        polygonBBCService.startup(ctx);

        Assert.assertEquals(amAddr, polygonBBCService.getBbcContext().getAuthMessageContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, polygonBBCService.getBbcContext().getAuthMessageContract().getStatus());
        Assert.assertEquals(sdpAddr, polygonBBCService.getBbcContext().getSdpContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, polygonBBCService.getBbcContext().getSdpContract().getStatus());
    }

    @Test
    public void testStartupWithReadyContract() {
        // start up a tmp polygonBBCService to set up contract
        AbstractBBCContext mockValidCtx = mockValidCtx();
        PolygonBBCService polygonBBCServiceTmp = new PolygonBBCService();
        polygonBBCServiceTmp.startup(mockValidCtx);

        // set up am and sdp
        polygonBBCServiceTmp.setupAuthMessageContract();
        polygonBBCServiceTmp.setupSDPMessageContract();
        String amAddr = polygonBBCServiceTmp.getContext().getAuthMessageContract().getContractAddress();
        String sdpAddr = polygonBBCServiceTmp.getContext().getSdpContract().getContractAddress();

        // start up success
        PolygonBBCService polygonBBCService = new PolygonBBCService();
        AbstractBBCContext ctx = mockValidCtxWithPreReadyContracts(amAddr, sdpAddr);
        polygonBBCService.startup(ctx);
        Assert.assertEquals(amAddr, polygonBBCService.getBbcContext().getAuthMessageContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, polygonBBCService.getBbcContext().getAuthMessageContract().getStatus());
        Assert.assertEquals(sdpAddr, polygonBBCService.getBbcContext().getSdpContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, polygonBBCService.getBbcContext().getSdpContract().getStatus());
    }

    @Test
    public void testShutdown() {
        AbstractBBCContext mockValidCtx = mockValidCtx();
        PolygonBBCService polygonBBCService = new PolygonBBCService();
        polygonBBCService.startup(mockValidCtx);
        polygonBBCService.shutdown();
    }

    @Test
    public void testGetContext() {
        AbstractBBCContext mockValidCtx = mockValidCtx();
        PolygonBBCService polygonBBCService = new PolygonBBCService();
        polygonBBCService.startup(mockValidCtx);
        AbstractBBCContext ctx = polygonBBCService.getContext();
        Assert.assertNotNull(ctx);
        Assert.assertNull(ctx.getAuthMessageContract());
    }

    @Test
    public void testSetupAuthMessageContract() {
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        polygonBBCService.startup(mockValidCtx);

        // set up am
        polygonBBCService.setupAuthMessageContract();

        // get context
        AbstractBBCContext ctx = polygonBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
    }

    @Test
    public void testSetupSDPMessageContract() {
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        polygonBBCService.startup(mockValidCtx);

        // set up sdp
        polygonBBCService.setupSDPMessageContract();

        // get context
        AbstractBBCContext ctx = polygonBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());
    }

    @Test
    public void testQuerySDPMessageSeq() {
        relayAmPrepare();

        // query seq
        long seq = polygonBBCService.querySDPMessageSeq(
                "senderDomain",
                DigestUtil.sha256Hex("senderID"),
                "receiverDomain",
                DigestUtil.sha256Hex("receiverID")
        );
        Assert.assertEquals(0L, seq);
    }

    @Test
    public void testSetProtocol() throws Exception {
        PolygonBBCService polygonBBCService = new PolygonBBCService();
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        polygonBBCService.startup(mockValidCtx);

        // set up am
        polygonBBCService.setupAuthMessageContract();

        // set up sdp
        polygonBBCService.setupSDPMessageContract();

        // get context
        AbstractBBCContext ctx = polygonBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        // set protocol to am (sdp type: 0)
        polygonBBCService.setProtocol(
                ctx.getSdpContract().getContractAddress(),
                "0");

        String addr = AuthMsg.load(
                polygonBBCService.getBbcContext().getAuthMessageContract().getContractAddress(),
                polygonBBCService.getWeb3j(),
                polygonBBCService.getCredentials(),
                new DefaultGasProvider()
        ).getProtocol(BigInteger.ZERO).send();
        log.info("protocol: {}", addr);

        // check am status
        ctx = polygonBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getAuthMessageContract().getStatus());
    }

    @Test
    public void testSetAmContractAndLocalDomain() throws Exception {
        PolygonBBCService polygonBBCService = new PolygonBBCService();
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        polygonBBCService.startup(mockValidCtx);

        // set up am
        polygonBBCService.setupAuthMessageContract();

        // set up sdp
        polygonBBCService.setupSDPMessageContract();

        // get context
        AbstractBBCContext ctx = polygonBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        // set am to sdp
        polygonBBCService.setAmContract(ctx.getAuthMessageContract().getContractAddress());

        String amAddr = SDPMsg.load(
                polygonBBCService.getBbcContext().getSdpContract().getContractAddress(),
                polygonBBCService.getWeb3j(),
                polygonBBCService.getCredentials(),
                new DefaultGasProvider()
        ).getAmAddress().send();
        log.info("amAddr: {}", amAddr);

        // check contract status
        ctx = polygonBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        // set the domain
        polygonBBCService.setLocalDomain("receiverDomain");

        byte[] rawDomain = SDPMsg.load(
                polygonBBCService.getBbcContext().getSdpContract().getContractAddress(),
                polygonBBCService.getWeb3j(),
                polygonBBCService.getCredentials(),
                new DefaultGasProvider()
        ).getLocalDomain().send();
        log.info("domain: {}", HexUtil.encodeHexStr(rawDomain));

        // check contract status
        ctx = polygonBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getSdpContract().getStatus());
    }

    @Test
    public void testRelayAuthMessage() throws Exception {
        relayAmPrepare();

        // relay am msg
        CrossChainMessageReceipt receipt = polygonBBCService.relayAuthMessage(getRawMsgFromRelayer());
        Assert.assertTrue(receipt.isSuccessful());

        waitForTxConfirmed(receipt.getTxhash(), polygonBBCService.getWeb3j());

        EthGetTransactionReceipt ethGetTransactionReceipt = polygonBBCService.getWeb3j().ethGetTransactionReceipt(receipt.getTxhash()).send();
        TransactionReceipt transactionReceipt = ethGetTransactionReceipt.getTransactionReceipt().get();
        Assert.assertNotNull(transactionReceipt);
        Assert.assertTrue(transactionReceipt.isStatusOK());
    }

    @Test
    public void testReadCrossChainMessageReceipt() throws IOException, InterruptedException {
        relayAmPrepare();

        // relay am msg
        CrossChainMessageReceipt crossChainMessageReceipt = polygonBBCService.relayAuthMessage(getRawMsgFromRelayer());

        waitForTxConfirmed(crossChainMessageReceipt.getTxhash(), polygonBBCService.getWeb3j());

        // read receipt by txHash
        CrossChainMessageReceipt crossChainMessageReceipt1 = polygonBBCService.readCrossChainMessageReceipt(crossChainMessageReceipt.getTxhash());
        Assert.assertTrue(crossChainMessageReceipt1.isConfirmed());
        Assert.assertEquals(crossChainMessageReceipt.isSuccessful(), crossChainMessageReceipt1.isSuccessful());
    }

    @Test
    public void testReadCrossChainMessagesByHeight_sendUnordered() throws Exception {
        relayAmPrepare();

        // 2. send msg
        String txhash;
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
            EthCall call = polygonBBCService.getWeb3j().ethCall(
                    Transaction.createEthCallTransaction(
                            polygonBBCService.getCredentials().getAddress(),
                            appContract.getContractAddress(),
                            encodedFunc
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();

            // 2.3 async send tx
            EthSendTransaction ethSendTransaction = polygonBBCService.getRawTransactionManager().sendTransaction(
                    BigInteger.valueOf(polygonBBCService.getConfig().getGasPrice()),
                    BigInteger.valueOf(polygonBBCService.getConfig().getGasLimit()),
                    appContract.getContractAddress(),
                    encodedFunc,
                    BigInteger.ZERO
            );
            txhash = ethSendTransaction.getTransactionHash();

            log.info("send unordered msg tx {}", ethSendTransaction.getTransactionHash());
        } catch (Exception e) {
            throw new RuntimeException(
                    "failed to send unordered msg", e
            );
        }

        // 3. query latest height
        long height1 = polygonBBCService.queryLatestHeight();

        waitForTxConfirmed(txhash, polygonBBCService.getWeb3j());

        long height2 = polygonBBCService.queryLatestHeight();

        // 4. read cc msg
        List<CrossChainMessage> messageList = ListUtil.toList();
        for (long i = height1; i <= height2; i++) {
            messageList.addAll(polygonBBCService.readCrossChainMessagesByHeight(i));
        }
        Assert.assertEquals(1, messageList.size());
        Assert.assertEquals(CrossChainMessage.CrossChainMessageType.AUTH_MSG, messageList.get(0).getType());
    }

    @Test
    public void testReadCrossChainMessagesByHeight_sendOrdered() throws Exception {
        relayAmPrepare();

        // 2. send msg
        String txhash;
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
            EthCall call = polygonBBCService.getWeb3j().ethCall(
                    Transaction.createEthCallTransaction(
                            polygonBBCService.getCredentials().getAddress(),
                            appContract.getContractAddress(),
                            encodedFunc
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();

            // 2.3 async send tx
            EthSendTransaction ethSendTransaction = polygonBBCService.getRawTransactionManager().sendTransaction(
                    BigInteger.valueOf(polygonBBCService.getConfig().getGasPrice()),
                    BigInteger.valueOf(polygonBBCService.getConfig().getGasLimit()),
                    appContract.getContractAddress(),
                    encodedFunc,
                    BigInteger.ZERO
            );
            txhash = ethSendTransaction.getTransactionHash();
            log.info("send ordered msg tx {}", ethSendTransaction.getTransactionHash());
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("failed to send ordered msg"), e
            );
        }

        // 3. query latest height
        long height1 = polygonBBCService.queryLatestHeight();

        waitForTxConfirmed(txhash, polygonBBCService.getWeb3j());

        long height2 = polygonBBCService.queryLatestHeight();

        // 4. read cc msg
        List<CrossChainMessage> messageList = ListUtil.toList();
        for (long i = height1; i <= height2; i++) {
            messageList.addAll(polygonBBCService.readCrossChainMessagesByHeight(i));
        }
        Assert.assertEquals(1, messageList.size());
        Assert.assertEquals(CrossChainMessage.CrossChainMessageType.AUTH_MSG, messageList.get(0).getType());
    }

    @SneakyThrows
    private void relayAmPrepare() {
        if (setupBBC) {
            return;
        }
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        polygonBBCService.startup(mockValidCtx);

        // set up am
        polygonBBCService.setupAuthMessageContract();

        // set up sdp
        polygonBBCService.setupSDPMessageContract();

        // set protocol to am (sdp type: 0)
        polygonBBCService.setProtocol(
                mockValidCtx.getSdpContract().getContractAddress(),
                "0");

        // set am to sdp
        polygonBBCService.setAmContract(mockValidCtx.getAuthMessageContract().getContractAddress());

        // set local domain to sdp
        polygonBBCService.setLocalDomain("receiverDomain");

        // check contract ready
        AbstractBBCContext ctxCheck = polygonBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctxCheck.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctxCheck.getSdpContract().getStatus());

        TransactionReceipt receipt = appContract.setProtocol(polygonBBCService.getBbcContext().getSdpContract().getContractAddress()).send();
        if (receipt.isStatusOK()) {
            log.info("set protocol({}) to app contract({})",
                    appContract.getContractAddress(),
                    polygonBBCService.getBbcContext().getSdpContract().getContractAddress());
        } else {
            throw new Exception(String.format("failed to set protocol(%s) to app contract(%s)",
                    appContract.getContractAddress(),
                    polygonBBCService.getBbcContext().getSdpContract().getContractAddress()));
        }

        setupBBC = true;
    }

    private AbstractBBCContext mockValidCtx() {
        EthereumConfig mockConf = new EthereumConfig();
        mockConf.setUrl(VALID_URL);
        mockConf.setPrivateKey(APP_USER_ETH_PRIVATE_KEY);
        mockConf.setGasPrice(2300000000L);
        mockConf.setGasLimit(3000000);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    private AbstractBBCContext mockValidCtxWithPreDeployedContracts(String amAddr, String sdpAddr) {
        EthereumConfig mockConf = new EthereumConfig();
        mockConf.setUrl(VALID_URL);
        mockConf.setPrivateKey(APP_USER_ETH_PRIVATE_KEY);
        mockConf.setAmContractAddressDeployed(amAddr);
        mockConf.setSdpContractAddressDeployed(sdpAddr);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    private AbstractBBCContext mockValidCtxWithPreReadyContracts(String amAddr, String sdpAddr) {
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

    private AbstractBBCContext mockInvalidCtx() {
        EthereumConfig mockConf = new EthereumConfig();
        mockConf.setUrl(INVALID_URL);
        mockConf.setPrivateKey(APP_USER_ETH_PRIVATE_KEY);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    @SneakyThrows
    private void waitForTxConfirmed(String txhash, Web3j web3j) {
        for (int i = 0; i < MAX_TX_RESULT_QUERY_TIME; i++) {
            Optional<TransactionReceipt> receiptOptional = web3j.ethGetTransactionReceipt(txhash).send().getTransactionReceipt();
            if (receiptOptional.isPresent() && receiptOptional.get().getBlockNumber().longValue() > 0L) {
                if (receiptOptional.get().getStatus().equals("0x1")) {
                    log.info("tx {} has been confirmed as success", txhash);
                } else {
                    log.error("tx {} has been confirmed as failed", txhash);
                }
                break;
            }
            Thread.sleep(1_000);
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

