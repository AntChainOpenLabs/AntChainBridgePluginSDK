package com.alipay.antchain.bridge.plugins.fiscobcos2;

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
import com.alipay.antchain.bridge.plugins.fiscobcos2.abi.AuthMsg;
import com.alipay.antchain.bridge.plugins.fiscobcos2.abi.ReceiverContract;
import com.alipay.antchain.bridge.plugins.fiscobcos2.abi.SDPMsg;
import com.alipay.antchain.bridge.plugins.fiscobcos2.abi.SenderContract;
import lombok.Getter;
import lombok.Setter;
import org.fisco.bcos.sdk.model.TransactionReceipt;
import org.fisco.bcos.sdk.model.callback.TransactionCallback;
import org.fisco.bcos.sdk.transaction.manager.AssembleTransactionProcessor;
import org.fisco.bcos.sdk.transaction.manager.TransactionProcessorFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class FISCOBCOSBBCServiceTest {
    public static final String abiFile = FISCOBCOSBBCService.class.getClassLoader().getResource("sdk/abi").getPath();

    public static final String abiFileSM = FISCOBCOSBBCService.class.getClassLoader().getResource("sdk/abi/sm").getPath();

    public static final String binFile = FISCOBCOSBBCService.class.getClassLoader().getResource("sdk/bin").getPath();

    public static final String binFileSM = FISCOBCOSBBCService.class.getClassLoader().getResource("sdk/bin/sm").getPath();

    public static final String HYPERCHAIN_CONFIG_WITHOUT_ACCOUNT_FILENAME = "fiscobcos_default.json";

    private static final String CA_CERT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIBxTCCAWqgAwIBAgIJAJZCdxpG7UmuMAoGCCqBHM9VAYN1MDcxEDAOBgNVBAMM\n" +
            "B2dtY2hhaW4xEzARBgNVBAoMCmZpc2NvLWJjb3MxDjAMBgNVBAsMBWNoYWluMCAX\n" +
            "DTI0MTEyMjA2MTkxMVoYDzIxMjQxMDI5MDYxOTExWjA3MRAwDgYDVQQDDAdnbWNo\n" +
            "YWluMRMwEQYDVQQKDApmaXNjby1iY29zMQ4wDAYDVQQLDAVjaGFpbjBZMBMGByqG\n" +
            "SM49AgEGCCqBHM9VAYItA0IABKS5SHxe3nKjCxtK4b7JHnTXb/GzmqGaY1Rsx17k\n" +
            "6wVFYGs+K5f/rnAp/CQQGYp1Lp2lQcoxAzkvnkKZEAkYFOKjXTBbMB0GA1UdDgQW\n" +
            "BBR6G939H1r385UHkMYp+3gWtWEPIDAfBgNVHSMEGDAWgBR6G939H1r385UHkMYp\n" +
            "+3gWtWEPIDAMBgNVHRMEBTADAQH/MAsGA1UdDwQEAwIBBjAKBggqgRzPVQGDdQNJ\n" +
            "ADBGAiEA+XVlBznyDVMG2CeZPOiJh78PmVf6m5/X7xiTAqefyiMCIQD2BUNzO+jf\n" +
            "b0HblG3KnlfNwlWRxnD8Z/1xWmwkkpAH1w==\n" +
            "-----END CERTIFICATE-----";

    private static final String SSL_CERT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIBfzCCASSgAwIBAgIJALevFjFvnwudMAoGCCqBHM9VAYN1MDoxEjAQBgNVBAMM\n" +
            "CWFnZW5jeS1nbTETMBEGA1UECgwKZmlzY28tYmNvczEPMA0GA1UECwwGYWdlbmN5\n" +
            "MCAXDTI0MTEyMjA2MTkxMVoYDzIxMjQxMDI5MDYxOTExWjAxMQwwCgYDVQQDDANz\n" +
            "ZGsxEzARBgNVBAoMCmZpc2NvLWJjb3MxDDAKBgNVBAsMA3NkazBZMBMGByqGSM49\n" +
            "AgEGCCqBHM9VAYItA0IABCTEnyJDg/RF+p4W3mhve1almkgsRg0OB2E52KogklqH\n" +
            "SFXj8q1sDOwMU+8cjEiNa9TnFiFzcAapvHMcjsvHFKejGjAYMAkGA1UdEwQCMAAw\n" +
            "CwYDVR0PBAQDAgbAMAoGCCqBHM9VAYN1A0kAMEYCIQDYB/nC/Pq+8bleG4u//W2a\n" +
            "E5O1+J3aC4s/2W7F569RJQIhAJpyCC8pRclBcjp/exbMuprPCnaZdhD2mwg2WJ/s\n" +
            "SNQB\n" +
            "-----END CERTIFICATE-----\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIByDCCAW2gAwIBAgIJALmwJajJSZXwMAoGCCqBHM9VAYN1MDcxEDAOBgNVBAMM\n" +
            "B2dtY2hhaW4xEzARBgNVBAoMCmZpc2NvLWJjb3MxDjAMBgNVBAsMBWNoYWluMCAX\n" +
            "DTI0MTEyMjA2MTkxMVoYDzIxMjQxMDI5MDYxOTExWjA6MRIwEAYDVQQDDAlhZ2Vu\n" +
            "Y3ktZ20xEzARBgNVBAoMCmZpc2NvLWJjb3MxDzANBgNVBAsMBmFnZW5jeTBZMBMG\n" +
            "ByqGSM49AgEGCCqBHM9VAYItA0IABObdgOKZumyqi8aS1B5LPIpswhZB92XCQ1DZ\n" +
            "Jz3uJjztU0/c9g8vSP5h+rzaQYd4JQS/Lq5qwDZeuIxHAoDwuVmjXTBbMB0GA1Ud\n" +
            "DgQWBBQsZQcLBaN0AHa6VTj+452LiKkvSDAfBgNVHSMEGDAWgBR6G939H1r385UH\n" +
            "kMYp+3gWtWEPIDAMBgNVHRMEBTADAQH/MAsGA1UdDwQEAwIBBjAKBggqgRzPVQGD\n" +
            "dQNJADBGAiEAhto03sGvep24z9XRKV3oI4AIEmOo14r+8KwS1ZkpVAcCIQCYpKqM\n" +
            "DwvLBsfVxJbGE26HVpm8UvZLq+WyFl+r273Vng==\n" +
            "-----END CERTIFICATE-----\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIBxTCCAWqgAwIBAgIJAJZCdxpG7UmuMAoGCCqBHM9VAYN1MDcxEDAOBgNVBAMM\n" +
            "B2dtY2hhaW4xEzARBgNVBAoMCmZpc2NvLWJjb3MxDjAMBgNVBAsMBWNoYWluMCAX\n" +
            "DTI0MTEyMjA2MTkxMVoYDzIxMjQxMDI5MDYxOTExWjA3MRAwDgYDVQQDDAdnbWNo\n" +
            "YWluMRMwEQYDVQQKDApmaXNjby1iY29zMQ4wDAYDVQQLDAVjaGFpbjBZMBMGByqG\n" +
            "SM49AgEGCCqBHM9VAYItA0IABKS5SHxe3nKjCxtK4b7JHnTXb/GzmqGaY1Rsx17k\n" +
            "6wVFYGs+K5f/rnAp/CQQGYp1Lp2lQcoxAzkvnkKZEAkYFOKjXTBbMB0GA1UdDgQW\n" +
            "BBR6G939H1r385UHkMYp+3gWtWEPIDAfBgNVHSMEGDAWgBR6G939H1r385UHkMYp\n" +
            "+3gWtWEPIDAMBgNVHRMEBTADAQH/MAsGA1UdDwQEAwIBBjAKBggqgRzPVQGDdQNJ\n" +
            "ADBGAiEA+XVlBznyDVMG2CeZPOiJh78PmVf6m5/X7xiTAqefyiMCIQD2BUNzO+jf\n" +
            "b0HblG3KnlfNwlWRxnD8Z/1xWmwkkpAH1w==\n" +
            "-----END CERTIFICATE-----";

    private static final String SSL_KEY = "-----BEGIN PRIVATE KEY-----\n" +
            "MIGHAgEAMBMGByqGSM49AgEGCCqBHM9VAYItBG0wawIBAQQgRStKl9jTIVnghpLi\n" +
            "dALXRXjsFJW1IMUqf/1vKZqoCmGhRANCAAQkxJ8iQ4P0RfqeFt5ob3tWpZpILEYN\n" +
            "DgdhOdiqIJJah0hV4/KtbAzsDFPvHIxIjWvU5xYhc3AGqbxzHI7LxxSn\n" +
            "-----END PRIVATE KEY-----";

    private static final String VALID_GROUPID = "1";

    private static final String INVALID_GROUPID = "0";

    private static final long WAIT_TIME = 5000;

    private static FISCOBCOSBBCService fiscobcosBBCService;

    private static SenderContract senderContract;

    private static ReceiverContract receiverContract;

    private static final String RECEIVER_APP_CONTRACT = "BdcbjgcC01";

    @Before
    public void init() throws Exception {
        fiscobcosBBCService = new FISCOBCOSBBCService();
    }


    private AbstractBBCContext fiscobcoNoContractCtx(String chainConfigFileName) throws IOException {
        String jsonStr = FISCOBCOSConfig.readFileJson(chainConfigFileName);
        FISCOBCOSConfig mockConf = FISCOBCOSConfig.fromJsonString(jsonStr);

        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    @Test
    public void testStartup() {
        // start up success
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);
        Assert.assertEquals(null, fiscobcosBBCService.getBbcContext().getAuthMessageContract());
        Assert.assertEquals(null, fiscobcosBBCService.getBbcContext().getSdpContract());
        // start up failed
        AbstractBBCContext mockInvalidCtx = mockInvalidCtx();
        try {
            fiscobcosBBCService.startup(mockInvalidCtx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @Test
    public void testStartupWithDeployedContract() {
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

//    @Test
    public void testStartupWithReadyContract() {
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
    public void testShutdown() {
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);
        fiscobcosBBCService.shutdown();
    }

    @Test
    public void testGetContext() {
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);
        AbstractBBCContext ctx = fiscobcosBBCService.getContext();
        Assert.assertNotNull(ctx);
        System.out.println(ctx);
        Assert.assertEquals(null, ctx.getAuthMessageContract());

        fiscobcosBBCService.shutdown();
    }

    @Test
    public void testQueryLatestHeight() {
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);
        Long height = fiscobcosBBCService.queryLatestHeight();
        Assert.assertNotNull(height);
        System.out.println("lasted height: " + height);

        fiscobcosBBCService.shutdown();
    }

    @Test
    public void testSetupAuthMessageContract() {
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);

        // set up am
        fiscobcosBBCService.setupAuthMessageContract();

        // get context
        AbstractBBCContext ctx = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());

        System.out.println("am contract addr: " + ctx.getAuthMessageContract().getContractAddress());

        fiscobcosBBCService.shutdown();
    }

    @Test
    public void testSetupSDPMessageContract() {
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);

        // set up sdp
        fiscobcosBBCService.setupSDPMessageContract();

        // get context
        AbstractBBCContext ctx = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        System.out.println("sdp contract addr: " + ctx.getSdpContract().getContractAddress());

        fiscobcosBBCService.shutdown();
    }

    @Test
    public void testQuerySDPMessageSeq() {
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

        fiscobcosBBCService.shutdown();
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
        System.out.println("am contract addr: " + ctx.getAuthMessageContract().getContractAddress());
        System.out.println("sdp contract addr: " + ctx.getSdpContract().getContractAddress());

        // set protocol to am (sdp type: 0)
        fiscobcosBBCService.setProtocol(
                ctx.getSdpContract().getContractAddress(),
                "0");

        String addr = AuthMsg.load(
                fiscobcosBBCService.getBbcContext().getAuthMessageContract().getContractAddress(),
                fiscobcosBBCService.getClient(),
                fiscobcosBBCService.getKeyPair()
        ).getProtocol(BigInteger.ZERO);
        System.out.printf("get protocol from am contract: %s\n", addr);

        // check am status
        ctx = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getAuthMessageContract().getStatus());

        fiscobcosBBCService.shutdown();
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
        System.out.printf("get domain from sdp contract: %s\n", HexUtil.encodeHexStr(rawDomain));

        // check contract status
        ctx = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getSdpContract().getStatus());

        fiscobcosBBCService.shutdown();
    }

    @Test
    public void testRelayAuthMessage() throws Exception {
        relayAmPrepare();

        // relay am msg
        CrossChainMessageReceipt receipt = fiscobcosBBCService.relayAuthMessage(getRawMsgFromRelayer());
        System.out.println(String.format("sleep %ds for tx to be packaged...", WAIT_TIME / 1000));
        Thread.sleep(WAIT_TIME);

        System.out.println("isSuccessful: " + receipt.isSuccessful());
        System.out.println("isConfirmed: " + receipt.isConfirmed());
        System.out.println("txHash: " + receipt.getTxhash());
        System.out.println("errMsg: " + receipt.getErrorMsg());
        Thread.sleep(WAIT_TIME);

        TransactionReceipt transactionReceipt = fiscobcosBBCService.getClient().getTransactionReceipt(receipt.getTxhash()).getTransactionReceipt().get();
        Assert.assertNotNull(transactionReceipt);

        System.out.println("get tx receipt, status: " + transactionReceipt.getStatus());
        System.out.println("get tx receipt, msg: " + transactionReceipt.getMessage());
        System.out.println("get tx receipt, block_number: " + transactionReceipt.getBlockNumber());
        System.out.println("get tx receipt, block_hash: " + transactionReceipt.getBlockHash());

        fiscobcosBBCService.shutdown();
    }

    @Test
    public void testReadCrossChainMessageReceipt() throws IOException, InterruptedException {
        relayAmPrepare();

        // relay am msg
        CrossChainMessageReceipt crossChainMessageReceipt = fiscobcosBBCService.relayAuthMessage(getRawMsgFromRelayer());

        System.out.println(String.format("sleep %ds for tx to be packaged...", WAIT_TIME / 1000));
        Thread.sleep(WAIT_TIME);

        // read receipt by txHash
        CrossChainMessageReceipt crossChainMessageReceipt1 = fiscobcosBBCService.readCrossChainMessageReceipt(crossChainMessageReceipt.getTxhash());
        Assert.assertTrue(crossChainMessageReceipt1.isConfirmed());

        fiscobcosBBCService.shutdown();
    }

    @Test
    public void testReadCrossChainMessagesByHeight_sendUnordered() throws Exception {
        relayAmPrepare();

        // 0. deploy sender contrract
        try {
            senderContract = SenderContract.deploy(fiscobcosBBCService.getClient(), fiscobcosBBCService.getKeyPair());
        } catch (Exception e) {
            throw new RuntimeException("failed to deploy sender contract", e);
        }
        System.out.println("sender contract:" + senderContract.getContractAddress());

        // 1. set sdp addr
        TransactionReceipt receipt = senderContract.setSdpMSGAddress(
                fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress());
        if (receipt.isStatusOK()) {
            System.out.printf("set protocol(%s) to sender_app contract(%s) \n",
                    senderContract.getContractAddress(),
                    fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress());
        } else {
            throw new Exception(String.format("failed to set protocol(%s) to sender_app contract(%s)",
                    senderContract.getContractAddress(),
                    fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress()));
        }

        // 2. send msg
        try {
            // 2.1 create inputParameters
            List<Object> inputParameters = new ArrayList<>();
            // 注意此处需要传入byte[]类型，fisco-java-sdk无法识别bytes32
            inputParameters.add(DigestUtil.sha256(RECEIVER_APP_CONTRACT));
            // 注意此处需要传入String类型，Utf8String
            inputParameters.add("remoteDomain");
            // 注意此处需要传入byte[]类型，fisco-java-sdk无法识别bytes
            inputParameters.add("UnorderedCrossChainMessage".getBytes());

            // 2.2 async send tx
            AssembleTransactionProcessor transactionProcessor = TransactionProcessorFactory.createAssembleTransactionProcessor(
                    fiscobcosBBCService.getClient(),
                    fiscobcosBBCService.getKeyPair(),
                    this.abiFileSM,
                    this.binFileSM
            );
            transactionProcessor.sendTransactionAndGetReceiptByContractLoaderAsync(
                    "SenderContract", // contract name
                    senderContract.getContractAddress(),  // contract address
                    SenderContract.FUNC_SENDUNORDERED, // function name
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

        System.out.printf("sleep %ds for tx to be packaged...%n", WAIT_TIME / 1000);
        Thread.sleep(WAIT_TIME);

        long height2 = fiscobcosBBCService.queryLatestHeight();

        // 4. read cc msg
        List<CrossChainMessage> messageList = ListUtil.toList();
        for (long i = height1; i <= height2; i++) {
            messageList.addAll(fiscobcosBBCService.readCrossChainMessagesByHeight(i));
        }
        Assert.assertEquals(1, messageList.size());
        Assert.assertEquals(CrossChainMessage.CrossChainMessageType.AUTH_MSG, messageList.get(0).getType());

        System.out.println(HexUtil.encodeHexStr(messageList.get(0).getMessage()));
        IAuthMessage authMsg = AuthMessageFactory.createAuthMessage(messageList.get(0).getMessage());
        System.out.println("am msg version: " + authMsg.getVersion());
        System.out.println("am msg send addr: " + authMsg.getIdentity());

        ISDPMessage sdpMsg = SDPMessageFactory.createSDPMessage(authMsg.getPayload());
        System.out.println("sdp msg version: " + sdpMsg.getVersion());
        System.out.println("sdp msg target addr: " + sdpMsg.getTargetIdentity());
        System.out.println("sdp msg seq: " + sdpMsg.getSequence());
        System.out.println("sdp msg info: " + new String(sdpMsg.getPayload()));
        Assert.assertEquals("UnorderedCrossChainMessage", new String(sdpMsg.getPayload()));

        fiscobcosBBCService.shutdown();
    }

    @Test
    public void testReadCrossChainMessagesByHeight_sendOrdered() throws Exception {
        relayAmPrepare();

        // 0. deploy sender contrract
        try {
            senderContract = SenderContract.deploy(fiscobcosBBCService.getClient(), fiscobcosBBCService.getKeyPair());
        } catch (Exception e) {
            throw new RuntimeException("failed to deploy sender contract", e);
        }
        System.out.println("sender contract:" + senderContract.getContractAddress());


        // 1. set sdp addr
        TransactionReceipt receipt = senderContract.setSdpMSGAddress(fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress());
        if (receipt.isStatusOK()) {
            System.out.printf("set protocol(%s) to sender_app contract(%s) \n",
                    senderContract.getContractAddress(),
                    fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress());
        } else {
            throw new Exception(String.format("failed to set protocol(%s) to sender_app contract(%s)",
                    senderContract.getContractAddress(),
                    fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress()));
        }

        // 2. send msg
        try {
            // 2.1 create inputParameters
            List<Object> inputParameters = new ArrayList<>();
            // 注意此处需要传入byte[]类型，fisco-java-sdk无法识别bytes32
            inputParameters.add(DigestUtil.sha256(RECEIVER_APP_CONTRACT));
            // 注意此处需要传入String类型，Utf8String
            inputParameters.add("remoteDomain");
            // 注意此处需要传入byte[]类型，fisco-java-sdk无法识别bytes
            inputParameters.add("CrossChainMessage".getBytes());

            // 2.2 async send tx
            AssembleTransactionProcessor transactionProcessor = TransactionProcessorFactory.createAssembleTransactionProcessor(
                    fiscobcosBBCService.getClient(),
                    fiscobcosBBCService.getKeyPair(),
                    this.abiFileSM,
                    this.binFileSM
            );
            transactionProcessor.sendTransactionAndGetReceiptByContractLoaderAsync(
                    "SenderContract", // contract name
                    senderContract.getContractAddress(),  // contract address
                    SenderContract.FUNC_SEND, // function name
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

        System.out.printf("sleep %ds for tx to be packaged...%n", WAIT_TIME / 1000);
        Thread.sleep(WAIT_TIME);

        long height2 = fiscobcosBBCService.queryLatestHeight();

        // 4. read cc msg
        List<CrossChainMessage> messageList = ListUtil.toList();
        for (long i = height1; i <= height2; i++) {
            messageList.addAll(fiscobcosBBCService.readCrossChainMessagesByHeight(i));
        }
        Assert.assertEquals(1, messageList.size());
        Assert.assertEquals(CrossChainMessage.CrossChainMessageType.AUTH_MSG, messageList.get(0).getType());


        System.out.println(HexUtil.encodeHexStr(messageList.get(0).getMessage()));
        IAuthMessage authMsg = AuthMessageFactory.createAuthMessage(messageList.get(0).getMessage());
        System.out.println("am msg version: " + authMsg.getVersion());
        System.out.println("am msg send addr: " + authMsg.getIdentity());

        ISDPMessage sdpMsg = SDPMessageFactory.createSDPMessage(authMsg.getPayload());
        System.out.println("sdp msg version: " + sdpMsg.getVersion());
        System.out.println("sdp msg target addr: " + sdpMsg.getTargetIdentity());
        System.out.println("sdp msg seq: " + sdpMsg.getSequence());
        System.out.println("sdp msg info: " + new String(sdpMsg.getPayload()));
        Assert.assertEquals("CrossChainMessage", new String(sdpMsg.getPayload()));

        fiscobcosBBCService.shutdown();
    }

    private void relayAmPrepare() {
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
        System.out.println("sdp address:" + mockValidCtx.getSdpContract().getContractAddress());
        System.out.println("am address:" + mockValidCtx.getAuthMessageContract().getContractAddress());

        // set am to sdp
        fiscobcosBBCService.setAmContract(mockValidCtx.getAuthMessageContract().getContractAddress());

        // set local domain to sdp
        fiscobcosBBCService.setLocalDomain("receiverDomain");

        // check contract ready
        AbstractBBCContext ctxCheck = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctxCheck.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctxCheck.getSdpContract().getStatus());
    }


    private AbstractBBCContext mockValidCtx() {
        FISCOBCOSConfig mockConf = new FISCOBCOSConfig();
        mockConf.setCaCert(CA_CERT);
        mockConf.setSslCert(SSL_CERT);
        mockConf.setSslKey(SSL_KEY);
        mockConf.setGroupID(VALID_GROUPID);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    private AbstractBBCContext mockInvalidCtx() {
        FISCOBCOSConfig mockConf = new FISCOBCOSConfig();
        mockConf.setCaCert(CA_CERT);
        mockConf.setSslCert(SSL_CERT);
        mockConf.setSslKey(SSL_KEY);
        mockConf.setGroupID(INVALID_GROUPID);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    private AbstractBBCContext mockValidCtxWithPreDeployedContracts(String amAddr, String sdpAddr) {
        FISCOBCOSConfig mockConf = new FISCOBCOSConfig();
        mockConf.setCaCert(CA_CERT);
        mockConf.setSslCert(SSL_CERT);
        mockConf.setSslKey(SSL_KEY);
        mockConf.setGroupID(VALID_GROUPID);
        mockConf.setAmContractAddressDeployed(amAddr);
        mockConf.setSdpContractAddressDeployed(sdpAddr);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    private AbstractBBCContext mockValidCtxWithPreReadyContracts(String amAddr, String sdpAddr) {
        FISCOBCOSConfig mockConf = new FISCOBCOSConfig();
        mockConf.setCaCert(CA_CERT);
        mockConf.setSslCert(SSL_CERT);
        mockConf.setSslKey(SSL_KEY);
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
