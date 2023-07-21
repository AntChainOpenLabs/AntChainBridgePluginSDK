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

package com.alipay.antchain.bridge.plugins.eos;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
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
import com.alipay.antchain.bridge.plugins.eos.types.EosTxInfo;
import com.alipay.antchain.bridge.plugins.eos.utils.Utils;
import lombok.Getter;
import lombok.Setter;
import okhttp3.RequestBody;
import one.block.eosiojava.error.session.TransactionPrepareError;
import one.block.eosiojava.error.session.TransactionSignAndBroadCastError;
import one.block.eosiojava.models.rpcProvider.Action;
import one.block.eosiojava.models.rpcProvider.Authorization;
import one.block.eosiojava.models.rpcProvider.TransactionConfig;
import one.block.eosiojava.models.rpcProvider.response.SendTransactionResponse;
import one.block.eosiojava.session.TransactionProcessor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Replace EOS url, private key, username and other stuff with your environment configuration
 */
public class EosBBCServiceTest {

    private static final String VALID_URL = "http://127.0.0.1:8888";

    private static final String INVALID_URL = "127.0.0.1:9999";

    // !!! replace to your test key
    private static final String EOS_DEFAULT_PRIVATE_KEY = "5JzGkEmpSQWb92DAS...hhY5aCWN4VZQc6uAxrQLAr";

    private static final String EOS_DEFAULT_USER_NAME = "relayer1";

    private static final String EOS_SDP_CONTRACT_NAME = "sdp";

    private static final String EOS_AM_CONTRACT_NAME = "am";

    private static final String EOS_RECEIVER_CONTRACT_NAME = "app";

    private static final long WAIT_TIME = 3000;

    private static EosBBCService eosBBCService;

    @Before
    public void init() {
        eosBBCService = new EosBBCService();
    }

    /**
     * EOS的Startup必须携带已部署合约信息
     */
    @Test
    public void testStartup() {

        // start up context success with deployed contract
        AbstractBBCContext mockValidCtx = mockValidCtx();
        eosBBCService.startup(mockValidCtx);

        Assert.assertNotNull(eosBBCService.getBbcContext().getAuthMessageContract());
        Assert.assertNotNull(eosBBCService.getBbcContext().getSdpContract());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, eosBBCService.getBbcContext().getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, eosBBCService.getBbcContext().getSdpContract().getStatus());

        // start up context success with ready contract
        AbstractBBCContext mockValidCtxWithPreReadyContracts = mockValidCtxWithPreReadyContracts();
        eosBBCService.startup(mockValidCtxWithPreReadyContracts);
        Assert.assertNotNull(eosBBCService.getBbcContext().getAuthMessageContract());
        Assert.assertNotNull(eosBBCService.getBbcContext().getSdpContract());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, eosBBCService.getBbcContext().getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, eosBBCService.getBbcContext().getSdpContract().getStatus());

        // start up failed without deployed contract
        AbstractBBCContext mockInvalidCtxWithoutDeployedContracts = mockInvalidCtxWithoutDeployedContracts();
        try {
            eosBBCService.startup(mockInvalidCtxWithoutDeployedContracts);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // start up failed with wrong url
        AbstractBBCContext mockInvalidCtxWithWrongUrl = mockInvalidCtxWithWrongUrl();
        try {
            eosBBCService.startup(mockInvalidCtxWithWrongUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testShutdown() {
        AbstractBBCContext mockValidCtx = mockValidCtx();
        eosBBCService.startup(mockValidCtx);
        eosBBCService.shutdown();
    }

    @Test
    public void testGetContext() {
        AbstractBBCContext mockValidCtx = mockValidCtx();
        eosBBCService.startup(mockValidCtx);

        AbstractBBCContext ctx = eosBBCService.getContext();
        Assert.assertNotNull(ctx);
        Assert.assertNotNull(ctx.getAuthMessageContract());
        Assert.assertNotNull(ctx.getSdpContract());
        Assert.assertEquals(EOS_AM_CONTRACT_NAME, ctx.getAuthMessageContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
    }

    @Test
    public void testSetupAuthMessageContract() {
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        eosBBCService.startup(mockValidCtx);

        // set up am
        eosBBCService.setupAuthMessageContract();

        // get context
        AbstractBBCContext ctx = eosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
    }

    @Test
    public void testSetupSDPMessageContract() {
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        eosBBCService.startup(mockValidCtx);

        // set up sdp
        eosBBCService.setupSDPMessageContract();

        // get context
        AbstractBBCContext ctx = eosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());
    }

    @Test
    public void testReadCrossChainMessageReceipt() throws IOException, InterruptedException {
        relayAmPrepare();

        CrossChainMessageReceipt crossChainMessageReceipt = eosBBCService.relayAuthMessage(
                getRawMsgFromRelayer(DigestUtil.sha256Hex(UUID.randomUUID().toString()), 0)
        );
        Assert.assertNotNull(crossChainMessageReceipt);
        Assert.assertTrue(crossChainMessageReceipt.isSuccessful());

        System.out.println("sleep 3s for tx to be packaged...");
        Thread.sleep(WAIT_TIME);

        // read receipt by txHash
        crossChainMessageReceipt = eosBBCService.readCrossChainMessageReceipt(crossChainMessageReceipt.getTxhash());
        Assert.assertTrue(crossChainMessageReceipt.isSuccessful());
        Assert.assertTrue(crossChainMessageReceipt.isConfirmed());
    }

    @Test
    public void testReadCrossChainMessageReceipt_sendUnordered() throws Exception {
        relayAmPrepare();

        CrossChainMessageReceipt crossChainMessageReceipt = eosBBCService.relayAuthMessage(
                getRawMsgFromRelayer(DigestUtil.sha256Hex(UUID.randomUUID().toString()), -1)
        );
        Assert.assertNotNull(crossChainMessageReceipt);
        Assert.assertTrue(crossChainMessageReceipt.isSuccessful());

        System.out.println("sleep 3s for tx to be packaged...");
        Thread.sleep(WAIT_TIME);

        // read receipt by txHash
        crossChainMessageReceipt = eosBBCService.readCrossChainMessageReceipt(crossChainMessageReceipt.getTxhash());
        Assert.assertTrue(crossChainMessageReceipt.isSuccessful());
        Assert.assertTrue(crossChainMessageReceipt.isConfirmed());
    }

    @Test
    public void testQueryLatestHeight() {
        relayAmPrepare();
        Assert.assertNotEquals(0, eosBBCService.queryLatestHeight().longValue());
    }

    @Test
    public void testQuerySDPMessageSeq() {
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        eosBBCService.startup(mockValidCtx);

        // set up sdp
        eosBBCService.setupSDPMessageContract();

        // set the domain
        eosBBCService.setLocalDomain("receiverDomain");

        // query seq
        long seq = eosBBCService.querySDPMessageSeq(
                "senderDomain",
                DigestUtil.sha256Hex(UUID.randomUUID().toString()),
                "receiverDomain",
                DigestUtil.sha256Hex("receiverID")
        );
        Assert.assertEquals(0L, seq);
    }

    @Test
    public void testRelayAuthMessage() throws Exception {
        relayAmPrepare();

        // relay am msg
        CrossChainMessageReceipt receipt = eosBBCService.relayAuthMessage(
                getRawMsgFromRelayer(DigestUtil.sha256Hex("senderID"), -1)
        );
        Assert.assertTrue(receipt.isSuccessful());

        System.out.println("sleep 3s for tx to be packaged...");
        Thread.sleep(WAIT_TIME);

        Assert.assertEquals(
                "executed",
                JSON.parseObject(
                                eosBBCService.getRpcProvider().getTransaction(
                                        RequestBody.create(
                                                String.format("{\"id\":\"%s\"}", receipt.getTxhash()),
                                                okhttp3.MediaType.parse("application/json; charset=utf-8")
                                        )
                                )
                        ).getJSONObject("trx")
                        .getJSONObject("receipt")
                        .getString("status")
        );
    }

    private SendTransactionResponse bbcInvokeContractsOnRpc(String[][] invokeParams) {
        List<Action> actionList = new ArrayList<>();

        for (String[] infos : invokeParams) {
            if (infos.length != 3) {
                throw new RuntimeException(String.format(
                        "the parameters length should be 3 but %s", infos.length));
            }
            Action action = new Action(
                    // 合约账户名
                    infos[0].trim(),
                    // action名
                    infos[1].trim(),
                    // 调用者权限
                    Collections.singletonList(new Authorization(EOS_DEFAULT_USER_NAME, "active")),
                    // 合约参数
                    infos[2].trim()
            );
            actionList.add(action);
        }

        try {
            TransactionProcessor processor = eosBBCService.getSession().getTransactionProcessor();

            // 2.3 Now the TransactionConfig can be altered, if desired
            TransactionConfig transactionConfig = processor.getTransactionConfig();

            // Use blocksBehind (default 3) the current head block to calculate TAPOS
            transactionConfig.setUseLastIrreversible(false);

            // Set the expiration time of transactions 600(default 300) seconds later than the timestamp
            // of the block used to calculate TAPOS
            transactionConfig.setExpiresSeconds(600);

            // Update the TransactionProcessor with the config changes
            processor.setTransactionConfig(transactionConfig);

            processor.prepare(actionList);

            return processor.signAndBroadcast();
        } catch (TransactionPrepareError e) {
            throw new RuntimeException("failed to prepare invoke contract action", e);
        } catch (TransactionSignAndBroadCastError e) {
            throw new RuntimeException("failed to sign and broadcast invoke contract action", e);
        }
    }

    @Test
    public void testReadCrossChainMessagesByHeight_sendOrdered() throws Exception {
        relayAmPrepare();

        // 1. set sdp addr
        SendTransactionResponse sendTransactionResponse = bbcInvokeContractsOnRpc(
                new String[][]{
                        {
                                EOS_RECEIVER_CONTRACT_NAME,
                                "sendmsg",
                                String.format(
                                        "{\"invoker\": \"%s\", \"receiver_domain\": \"%s\", \"receiver_id\": \"%s\", \"msg\": \"%s\"}",
                                        EOS_DEFAULT_USER_NAME,
                                        "senderDomain",
                                        DigestUtil.sha256Hex(UUID.randomUUID().toString()),
                                        "awesome antchain bridge"
                                )
                        },
                }
        );

        System.out.println("sleep 3s for tx to be packaged...");
        Thread.sleep(WAIT_TIME);

        EosTxInfo txInfo = JSON.parseObject(
                eosBBCService.getRpcProvider().getTransaction(
                        RequestBody.create(
                                String.format("{\"id\":\"%s\"}", sendTransactionResponse.getTransactionId()),
                                okhttp3.MediaType.parse("application/json; charset=utf-8")
                        )
                ), EosTxInfo.class
        );

        List<CrossChainMessage> messages = eosBBCService.readCrossChainMessagesByHeight(txInfo.getBlockNum());

        Assert.assertNotNull(messages);
        Assert.assertTrue(ObjectUtil.isNotEmpty(messages.size()));
    }

    private void relayAmPrepare() {
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        eosBBCService.startup(mockValidCtx);

        // set up am
        eosBBCService.setupAuthMessageContract();

        // set up sdp
        eosBBCService.setupSDPMessageContract();

        // set protocol to am (sdp type: 0)
        eosBBCService.setProtocol(
                mockValidCtx.getSdpContract().getContractAddress(),
                "0"
        );

        // set am to sdp
        eosBBCService.setAmContract(mockValidCtx.getAuthMessageContract().getContractAddress());

        // set local domain to sdp
        eosBBCService.setLocalDomain("receiverDomain");

        // check contract ready
        AbstractBBCContext ctxCheck = eosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctxCheck.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctxCheck.getSdpContract().getStatus());
    }

    private AbstractBBCContext mockValidCtx() {
        EosConfig mockConf = new EosConfig();
        mockConf.setUrl(VALID_URL);
        mockConf.setUserPriKey(EOS_DEFAULT_PRIVATE_KEY);
        mockConf.setAmContractAddressDeployed(EOS_AM_CONTRACT_NAME);
        mockConf.setSdpContractAddressDeployed(EOS_SDP_CONTRACT_NAME);
        mockConf.setUserName(EOS_DEFAULT_USER_NAME);
        mockConf.setWaitUtilTxIrreversible(true);

        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());

        return mockCtx;
    }

    private AbstractBBCContext mockInvalidCtxWithWrongUrl() {
        EosConfig mockConf = new EosConfig();
        mockConf.setUrl(INVALID_URL);
        mockConf.setUserPriKey(EOS_DEFAULT_PRIVATE_KEY);
        mockConf.setUserName(EOS_DEFAULT_USER_NAME);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    private AbstractBBCContext mockInvalidCtxWithoutDeployedContracts() {
        EosConfig mockConf = new EosConfig();
        mockConf.setUrl(VALID_URL);
        mockConf.setUserPriKey(EOS_DEFAULT_PRIVATE_KEY);
        mockConf.setUserName(EOS_DEFAULT_USER_NAME);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    private AbstractBBCContext mockValidCtxWithPreReadyContracts() {
        EosConfig mockConf = new EosConfig();
        mockConf.setUrl(VALID_URL);
        mockConf.setUserPriKey(EOS_DEFAULT_PRIVATE_KEY);
        mockConf.setAmContractAddressDeployed(EOS_AM_CONTRACT_NAME);
        mockConf.setSdpContractAddressDeployed(EOS_SDP_CONTRACT_NAME);
        mockConf.setUserName(EOS_DEFAULT_USER_NAME);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());

        AuthMessageContract authMessageContract = new AuthMessageContract();
        authMessageContract.setContractAddress(EOS_AM_CONTRACT_NAME);
        authMessageContract.setStatus(ContractStatusEnum.CONTRACT_READY);
        mockCtx.setAuthMessageContract(authMessageContract);

        SDPContract sdpContract = new SDPContract();
        sdpContract.setContractAddress(EOS_SDP_CONTRACT_NAME);
        sdpContract.setStatus(ContractStatusEnum.CONTRACT_READY);
        mockCtx.setSdpContract(sdpContract);

        return mockCtx;
    }

    private byte[] getRawMsgFromRelayer(String senderHex, int seq) throws IOException {
        byte[] raw = Utils.convertEosBase32NameToNum(EOS_RECEIVER_CONTRACT_NAME).toByteArray();
        byte[] rawReceiverNum = new byte[8];
        System.arraycopy(
                raw,
                0,
                rawReceiverNum,
                0,
                raw.length
        );

        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(
                1,
                "receiverDomain",
                HexUtil.decodeHex(
                        String.format("000000000000000000000000000000000000000000000000%s", HexUtil.encodeHexStr(rawReceiverNum))
                ),
                seq,
                "awesome antchain-bridge".getBytes()
        );

        IAuthMessage am = AuthMessageFactory.createAuthMessage(
                1,
                HexUtil.decodeHex(senderHex),
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
