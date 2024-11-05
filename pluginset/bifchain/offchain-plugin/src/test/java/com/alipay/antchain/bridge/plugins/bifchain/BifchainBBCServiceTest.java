package com.alipay.antchain.bridge.plugins.bifchain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import cn.ac.caict.bid.model.BIDDocumentOperation;
import cn.ac.caict.bid.model.BIDpublicKeyOperation;
import cn.bif.api.BIFSDK;
import cn.bif.common.JsonUtils;
import cn.bif.common.ToBaseUnit;
import cn.bif.model.crypto.KeyPairEntity;
import cn.bif.model.request.BIFContractCreateRequest;
import cn.bif.model.request.BIFContractGetAddressRequest;
import cn.bif.model.request.BIFContractInvokeRequest;
import cn.bif.model.request.BIFTransactionGetInfoRequest;
import cn.bif.model.response.BIFContractCreateResponse;
import cn.bif.model.response.BIFContractGetAddressResponse;
import cn.bif.model.response.BIFContractInvokeResponse;
import cn.bif.model.response.BIFTransactionGetInfoResponse;
import cn.bif.module.encryption.key.PrivateKeyManager;
import cn.bif.module.encryption.model.KeyType;
import cn.bif.utils.base.Base58;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.digest.SM3;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.AuthMessageContract;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.bbc.syscontract.SDPContract;
import com.alipay.antchain.bridge.commons.bcdns.*;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageFactory;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.commons.core.base.*;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTypeEnum;
import com.alipay.antchain.bridge.commons.core.ptc.PTCVerifyAnchor;
import com.alipay.antchain.bridge.commons.core.sdp.ISDPMessage;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageFactory;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import com.alipay.antchain.bridge.commons.utils.crypto.HashAlgoEnum;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import lombok.Getter;
import lombok.Setter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BifchainBBCServiceTest {

    //private static final String VALID_URL = "http://172.17.6.84:10086";
    private static final String VALID_URL = "http://test.bifcore.bitfactory.cn";

    private static final String PRIVATE_KEY = "priSPKgxGjV3kCJbSDCYAjY7iETF7UtJcba8XpMKZNsxTQBRkU";

    private static final String ADDDRESS = "did:bid:efexmw5GLPUU92ECpZMxpBPyCeZJhCDW";

    private static final String ISSUER = "priSPKgXX97ti8p7FebfEWPxLUx1UNAJAK93i8zCTFW1Pa6fnm";

    private static final String SUPER = "priSPKeThUrwmBvigbe153GWQXRDuMUWwpM8fRBs4eQ82sQSgQ";

    private static final String PTC = "priSPKncqxV7SR5bJgTWxBpLDAotDbBsrGNAVky34VKzLXHppi";

    private static final String PTCID = "did:bid:efiPHJD4V1PxjNiWUfNUTNhfoqFjTggu";

    private static BifchainBBCService bifchainBBCService;

    @Before
    public void init() throws Exception {
        bifchainBBCService = new BifchainBBCService();
    }

    @Test
    public void testStartup(){
        // start up success
        AbstractBBCContext mockValidCtx = mockValidCtxWithPreDeployedContracts(ADDDRESS,ADDDRESS);
        bifchainBBCService.startup(mockValidCtx);
        Assert.assertEquals(ADDDRESS, bifchainBBCService.getBbcContext().getAuthMessageContract().getContractAddress());
        Assert.assertEquals(ADDDRESS, bifchainBBCService.getBbcContext().getSdpContract().getContractAddress());
    }

    @Test
    public void testStartupWithDeployedContract(){
        // start up a tmp
        AbstractBBCContext mockValidCtx = mockValidCtx();
        BifchainBBCService bifchainBBCServiceTmp = new BifchainBBCService();
        bifchainBBCServiceTmp.startup(mockValidCtx);

        // set up am and sdp
        bifchainBBCServiceTmp.setupAuthMessageContract();
        bifchainBBCServiceTmp.setupSDPMessageContract();
        String amAddr = bifchainBBCServiceTmp.getContext().getAuthMessageContract().getContractAddress();
        String sdpAddr = bifchainBBCServiceTmp.getContext().getSdpContract().getContractAddress();

        // start up success
        AbstractBBCContext ctx = mockValidCtxWithPreDeployedContracts(amAddr, sdpAddr);
        bifchainBBCService.startup(ctx);
        Assert.assertEquals(amAddr, bifchainBBCService.getBbcContext().getAuthMessageContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, bifchainBBCService.getBbcContext().getAuthMessageContract().getStatus());
        Assert.assertEquals(sdpAddr, bifchainBBCService.getBbcContext().getSdpContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, bifchainBBCService.getBbcContext().getSdpContract().getStatus());
    }

    @Test
    public void testStartupWithReadyContract(){
        // start up a tmp ethereumBBCService to set up contract
        AbstractBBCContext mockValidCtx = mockValidCtx();
        BifchainBBCService bifchainBBCServiceTmp = new BifchainBBCService();
        bifchainBBCServiceTmp.startup(mockValidCtx);

        // set up am and sdp
        bifchainBBCServiceTmp.setupAuthMessageContract();
        bifchainBBCServiceTmp.setupSDPMessageContract();
        String amAddr = bifchainBBCServiceTmp.getContext().getAuthMessageContract().getContractAddress();
        String sdpAddr = bifchainBBCServiceTmp.getContext().getSdpContract().getContractAddress();

        // start up success
        AbstractBBCContext ctx = mockValidCtxWithPreReadyContracts(amAddr, sdpAddr);
        bifchainBBCService.startup(ctx);
        Assert.assertEquals(amAddr, bifchainBBCService.getBbcContext().getAuthMessageContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, bifchainBBCService.getBbcContext().getAuthMessageContract().getStatus());
        Assert.assertEquals(sdpAddr, bifchainBBCService.getBbcContext().getSdpContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, bifchainBBCService.getBbcContext().getSdpContract().getStatus());
    }

    @Test
    public void testShutdown(){
        AbstractBBCContext mockValidCtx = mockValidCtx();
        bifchainBBCService.startup(mockValidCtx);
        bifchainBBCService.shutdown();
    }

    @Test
    public void testGetContext(){
        AbstractBBCContext mockValidCtx = mockValidCtx();
        bifchainBBCService.startup(mockValidCtx);
        AbstractBBCContext ctx = bifchainBBCService.getContext();
        Assert.assertNotNull(ctx);
    }

    @Test
    public void testSetupAuthMessageContract(){
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        bifchainBBCService.startup(mockValidCtx);

        // set up am
        bifchainBBCService.setupAuthMessageContract();

        // get context
        AbstractBBCContext ctx = bifchainBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
    }

    @Test
    public void testSetupSDPMessageContract(){
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        bifchainBBCService.startup(mockValidCtx);

        // set up sdp
        bifchainBBCService.setupSDPMessageContract();

        // get context
        AbstractBBCContext ctx = bifchainBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());
    }

    @Test
    public void testSetupPtcHubContract(){
        // start up
        AbstractBBCContext mockValidCtx = mockValidPTCCtx();
        bifchainBBCService.startup(mockValidCtx);

        // set up ptc
        bifchainBBCService.setupPTCContract();

        // get context
        AbstractBBCContext ctx = bifchainBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getPtcContract().getStatus());
    }

    @Test
    public void testQuerySDPMessageSeq(){
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        bifchainBBCService.startup(mockValidCtx);

        // set up sdp
        bifchainBBCService.setupSDPMessageContract();

        // set the domain
        bifchainBBCService.setLocalDomain("receiverDomain");

        // query seq
        long seq = bifchainBBCService.querySDPMessageSeq(
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
        bifchainBBCService.startup(mockValidCtx);

        // set up am
        bifchainBBCService.setupAuthMessageContract();

        // set up sdp
        bifchainBBCService.setupSDPMessageContract();

        // get context
        AbstractBBCContext ctx = bifchainBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        // set protocol to am (sdp type: 0)
        bifchainBBCService.setProtocol(
                ctx.getSdpContract().getContractAddress(),
                "0");

        // check am status
        ctx = bifchainBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getAuthMessageContract().getStatus());
    }

    @Test
    public void testSetAmContractAndLocalDomain() throws Exception {
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        bifchainBBCService.startup(mockValidCtx);

        // set up am
        bifchainBBCService.setupAuthMessageContract();

        // set up sdp
        bifchainBBCService.setupSDPMessageContract();

        // get context
        AbstractBBCContext ctx = bifchainBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        // set am to sdp
        bifchainBBCService.setAmContract(ctx.getAuthMessageContract().getContractAddress());

        // check contract status
        ctx = bifchainBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getSdpContract().getStatus());

        // set the domain
        bifchainBBCService.setLocalDomain("receiverDomain");

        // check contract status
        ctx = bifchainBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getSdpContract().getStatus());
    }

    @Test
    public void testRelayAuthMessage() throws Exception {
        relayAmPrepare();

        String appContract = deployAppContract();

        try {
            String senderAddress = ADDDRESS;
            String contractAddress = appContract;
            String senderPrivateKey = PRIVATE_KEY;
            Long amount = 0L;
            String invokeInput = StrUtil.format("{\"function\":\"setProtocol(address)\",\"args\":\"{}\"}", bifchainBBCService.getBbcContext().getSdpContract().getContractAddress());
            BIFContractInvokeRequest request = new BIFContractInvokeRequest();
            request.setSenderAddress(senderAddress);
            request.setPrivateKey(senderPrivateKey);
            request.setContractAddress(contractAddress);
            request.setBIFAmount(amount);
            request.setRemarks("contract invoke");
            request.setInput(invokeInput);
            request.setFeeLimit(500000000L);
            request.setGasPrice(1L);

            BIFSDK sdk = BIFSDK.getInstance(VALID_URL);
            BIFContractInvokeResponse response = sdk.getBIFContractService().contractInvoke(request);
            if (response.getErrorCode() == 0) {
                if (!queryTxResult(response.getResult().getHash())) {
                    throw new RuntimeException("failed to set protocol to app, transaction executing failed");
                }
            } else {
                throw new RuntimeException("failed to set protocol to app, transaction sending failed");
            }
        } catch (Exception e) {
            throw new RuntimeException("failed to set protocol to app", e);
        }


        // relay am msg
        CrossChainMessageReceipt receipt = bifchainBBCService.relayAuthMessage(getRawMsgFromRelayer(appContract));
        Assert.assertTrue(receipt.isSuccessful());

        BIFSDK sdk = BIFSDK.getInstance(VALID_URL);
        BIFTransactionGetInfoRequest request = new BIFTransactionGetInfoRequest();
        request.setHash(receipt.getTxhash());
        BIFTransactionGetInfoResponse response = sdk.getBIFTransactionService().getTransactionInfo(request);
        Assert.assertTrue(response.getResult().getTransactions()[0].getErrorCode() == 0);
    }

    @Test
    public void testReadCrossChainMessageReceipt() throws IOException, InterruptedException {
        relayAmPrepare();

        String appContract = deployAppContract();

        try {
            String senderAddress = ADDDRESS;
            String contractAddress = appContract;
            String senderPrivateKey = PRIVATE_KEY;
            Long amount = 0L;
            String invokeInput = StrUtil.format("{\"function\":\"setProtocol(address)\",\"args\":\"{}\"}", bifchainBBCService.getBbcContext().getSdpContract().getContractAddress());
            BIFContractInvokeRequest request = new BIFContractInvokeRequest();
            request.setSenderAddress(senderAddress);
            request.setPrivateKey(senderPrivateKey);
            request.setContractAddress(contractAddress);
            request.setBIFAmount(amount);
            request.setRemarks("contract invoke");
            request.setInput(invokeInput);
            request.setFeeLimit(500000000L);
            request.setGasPrice(1L);

            BIFSDK sdk = BIFSDK.getInstance(VALID_URL);
            BIFContractInvokeResponse response = sdk.getBIFContractService().contractInvoke(request);
            if (response.getErrorCode() == 0) {
                if (!queryTxResult(response.getResult().getHash())) {
                    throw new RuntimeException("failed to set protocol to app, transaction executing failed");
                }
            } else {
                throw new RuntimeException("failed to set protocol to app, transaction sending failed");
            }
        } catch (Exception e) {
            throw new RuntimeException("failed to set protocol to app", e);
        }
        // relay am msg
        CrossChainMessageReceipt crossChainMessageReceipt = bifchainBBCService.relayAuthMessage(getRawMsgFromRelayer(appContract));

        // read receipt by txHash
        CrossChainMessageReceipt crossChainMessageReceipt1 = bifchainBBCService.readCrossChainMessageReceipt(crossChainMessageReceipt.getTxhash());
        Assert.assertTrue(crossChainMessageReceipt1.isConfirmed());
        Assert.assertEquals(crossChainMessageReceipt.isSuccessful(), crossChainMessageReceipt1.isSuccessful());
    }

    @Test
    public void testUpdatePTCTrustRoot() throws Exception {
        // start up
        AbstractBBCContext mockValidCtx = mockValidPTCCtx();
        bifchainBBCService.startup(mockValidCtx);

        // set up ptc
        bifchainBBCService.setupPTCContract();

        // get context
        AbstractBBCContext ctx = bifchainBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getPtcContract().getStatus());

        byte[] PTC_CERT = createPTCCert();

        PTCTrustRoot ptcTrustRoot = PTCTrustRoot.builder()
                .ptcCrossChainCert(CrossChainCertificateFactory.createCrossChainCertificate(PTC_CERT))
                .networkInfo("{}".getBytes())
                .issuerBcdnsDomainSpace(new CrossChainDomain(".com"))
                .sigAlgo(SignAlgoEnum.ED25519)
                .verifyAnchorMap(MapUtil.builder(
                        BigInteger.ZERO,
                        new PTCVerifyAnchor(
                                BigInteger.ZERO,
                                "{}".getBytes()
                        )
                ).build())
                .build();

        // sign it with ptc private key which applied PTC certificate
        PrivateKeyManager privateKeyManager = new PrivateKeyManager(PTC);
        byte[] sign = privateKeyManager.sign(ptcTrustRoot.getEncodedToSign());
        ptcTrustRoot.setSig(sign);

        bifchainBBCService.updatePTCTrustRoot(ptcTrustRoot);

        PrivateKeyManager ptcPrivateKeyManager = new PrivateKeyManager(PTC);
        PTCTrustRoot ptcTrustRoot1 = bifchainBBCService.getPTCTrustRoot(new ObjectIdentity(ObjectIdentityType.BID, privateKeyManager.getEncAddress().getBytes()));

        Assert.assertEquals(ptcTrustRoot.getPtcCrossChainCert().getId(), ptcTrustRoot1.getPtcCrossChainCert().getId());

        PTCVerifyAnchor ptcVerifyAnchor = bifchainBBCService.getPTCVerifyAnchor(new ObjectIdentity(ObjectIdentityType.BID, privateKeyManager.getEncAddress().getBytes()), BigInteger.ZERO);

        Assert.assertEquals(ptcVerifyAnchor.getVersion(), BigInteger.ZERO);
    }

    @Test
    public void testReadConsensusState() throws Exception {

        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        bifchainBBCService.startup(mockValidCtx);

        BigInteger bigInteger = new BigInteger("4189941");
        ConsensusState consensusState = bifchainBBCService.readConsensusState(bigInteger);
        Assert.assertEquals(consensusState.getHeight(), bigInteger);


    }

    private byte[] createBcdnsRootCert() {
        PrivateKeyManager issuerPrivateKeyManager = new PrivateKeyManager(ISSUER);
        PrivateKeyManager superPrivateKeyManager = new PrivateKeyManager(SUPER);
        BIDpublicKeyOperation[] biDpublicKeyOperation = new BIDpublicKeyOperation[1];
        biDpublicKeyOperation[0] = new BIDpublicKeyOperation();
        biDpublicKeyOperation[0].setType(issuerPrivateKeyManager.getKeyType());
        biDpublicKeyOperation[0].setPublicKeyHex(issuerPrivateKeyManager.getEncPublicKey());
        BIDDocumentOperation bidDocumentOperation = new BIDDocumentOperation();
        bidDocumentOperation.setPublicKey(biDpublicKeyOperation);

        AbstractCrossChainCertificate certificate = CrossChainCertificateFactory.createCrossChainCertificate(
                CrossChainCertificateV1.MY_VERSION,
                KeyPairEntity.getBidAndKeyPair().getEncAddress(),
                new ObjectIdentity(ObjectIdentityType.BID, superPrivateKeyManager.getEncAddress().getBytes()),
                DateUtil.currentSeconds(),
                DateUtil.offsetDay(new Date(), 365).getTime() / 1000,
                new BCDNSTrustRootCredentialSubject(
                        "root",
                        new ObjectIdentity(ObjectIdentityType.BID, issuerPrivateKeyManager.getEncAddress().getBytes()),
                        JsonUtils.toJSONString(bidDocumentOperation).getBytes()
                )
        );

        byte[] msg = certificate.getEncodedToSign();
        byte[] sign = superPrivateKeyManager.sign(msg);
        String signAlg = "";
        KeyType keyType = superPrivateKeyManager.getKeyType();
        if (keyType.equals(KeyType.SM2)) {
            signAlg = "SM2";
        } else if (keyType.equals(KeyType.ED25519)){
            signAlg = "Ed25519";
        }
        certificate.setProof(
                new AbstractCrossChainCertificate.IssueProof(
                        HashAlgoEnum.SM3,
                        SM3.create().digest(certificate.getEncodedToSign()),
                        SignAlgoEnum.getByName(signAlg),
                        sign
                )
        );

        return certificate.encode();
    }

    private byte[] createPTCCert() {
        PrivateKeyManager issuerPrivateKeyManager = new PrivateKeyManager(ISSUER);
        PrivateKeyManager ptcPrivateKeyManager = new PrivateKeyManager(PTC);

        BIDpublicKeyOperation[] biDpublicKeyOperation = new BIDpublicKeyOperation[1];
        biDpublicKeyOperation[0] = new BIDpublicKeyOperation();
        biDpublicKeyOperation[0].setType(ptcPrivateKeyManager.getKeyType());
        biDpublicKeyOperation[0].setPublicKeyHex(ptcPrivateKeyManager.getEncPublicKey());
        BIDDocumentOperation bidDocumentOperation = new BIDDocumentOperation();
        bidDocumentOperation.setPublicKey(biDpublicKeyOperation);


        AbstractCrossChainCertificate certificate = CrossChainCertificateFactory.createCrossChainCertificate(
                CrossChainCertificateV1.MY_VERSION,
                PTCID,
                new ObjectIdentity(ObjectIdentityType.BID, issuerPrivateKeyManager.getEncAddress().getBytes()),
                DateUtil.currentSeconds(),
                DateUtil.offsetDay(new Date(), 365).getTime() / 1000,
                new PTCCredentialSubject(
                        "1.0",
                        "test",
                        PTCTypeEnum.COMMITTEE,
                        new ObjectIdentity(ObjectIdentityType.BID, ptcPrivateKeyManager.getEncAddress().getBytes()),
                        JsonUtils.toJSONString(bidDocumentOperation).getBytes()
                )
        );

        byte[] msg = certificate.getEncodedToSign();
        byte[] sign = issuerPrivateKeyManager.sign(msg);
        String signAlg = "";
        KeyType keyType = issuerPrivateKeyManager.getKeyType();
        if (keyType.equals(KeyType.SM2)) {
            signAlg = "SM2";
        } else if (keyType.equals(KeyType.ED25519)){
            signAlg = "Ed25519";
        }
        certificate.setProof(
                new AbstractCrossChainCertificate.IssueProof(
                        HashAlgoEnum.SM3,
                        SM3.create().digest(certificate.getEncodedToSign()),
                        SignAlgoEnum.getByName(signAlg),
                        sign
                )
        );

        return certificate.encode();
    }

    @Test
    public void testReadCrossChainMessagesByHeight_sendUnordered() throws Exception {
        relayAmPrepare();

        //1.deploy app contract
        String appContract = deployAppContract();

        //2.app contract set protocol
        BIFSDK sdk = BIFSDK.getInstance(VALID_URL);
        try {
            String senderAddress = ADDDRESS;
            String contractAddress = appContract;
            String senderPrivateKey = PRIVATE_KEY;
            Long amount = 0L;
            String invokeInput = StrUtil.format("{\"function\":\"setProtocol(address)\",\"args\":\"{}\"}", bifchainBBCService.getBbcContext().getSdpContract().getContractAddress());
            BIFContractInvokeRequest request = new BIFContractInvokeRequest();
            request.setSenderAddress(senderAddress);
            request.setPrivateKey(senderPrivateKey);
            request.setContractAddress(contractAddress);
            request.setBIFAmount(amount);
            request.setRemarks("contract invoke");
            request.setInput(invokeInput);
            request.setFeeLimit(500000000L);
            request.setGasPrice(1L);

            BIFContractInvokeResponse response = sdk.getBIFContractService().contractInvoke(request);
            if (response.getErrorCode() == 0) {
                if (!queryTxResult(response.getResult().getHash())) {
                    throw new RuntimeException("failed to set protocol, transaction executing failed");
                }
            } else {
                throw new RuntimeException("failed to set protocol, transaction sending failed");
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to set protocol (address: %s) to APP %s",
                            bifchainBBCService.getBbcContext().getSdpContract().getContractAddress(), appContract), e);
        }

        long height1 = bifchainBBCService.queryLatestHeight();

        //3.send msg
        try {
            String senderAddress = ADDDRESS;
            String contractAddress = appContract;
            String senderPrivateKey = PRIVATE_KEY;
            Long amount = 0L;
            String invokeInput = StrUtil.format("{\"function\":\"sendUnorderedMessage(string,bytes32,bytes)\",\"args\":\"'{}','{}','{}'\"}", "remoteDomain", "0x51bb64f74fdbb370bc5ee89d1bf4e3a3f1e61e2fadbc102844f9b9cd5652da20", HexUtil.encodeHexStr("UnorderedCrossChainMessage".getBytes()));
            BIFContractInvokeRequest request = new BIFContractInvokeRequest();
            request.setSenderAddress(senderAddress);
            request.setPrivateKey(senderPrivateKey);
            request.setContractAddress(contractAddress);
            request.setBIFAmount(amount);
            request.setRemarks("contract invoke");
            request.setInput(invokeInput);
            request.setFeeLimit(500000000L);
            request.setGasPrice(1L);

            BIFContractInvokeResponse response = sdk.getBIFContractService().contractInvoke(request);
            if (response.getErrorCode() == 0) {
                if (!queryTxResult(response.getResult().getHash())) {
                    throw new RuntimeException("failed to send unordered message, transaction executing failed");
                }
            } else {
                throw new RuntimeException("failed to send unordered message, transaction sending failed");
            }
        } catch (Exception e) {
            throw new RuntimeException("failed to send unordered message", e);
        }

        // 3. query latest height
        long height2 = bifchainBBCService.queryLatestHeight();

        // 4. read cc msg
        List<CrossChainMessage> messageList = ListUtil.toList();
        for(long i = height1; i <= height2; i++){
            messageList.addAll(bifchainBBCService.readCrossChainMessagesByHeight(i));
        }
        Assert.assertEquals(1, messageList.size());
        Assert.assertEquals(CrossChainMessage.CrossChainMessageType.AUTH_MSG, messageList.get(0).getType());
    }

    @Test
    public void testReadCrossChainMessagesByHeight_sendOrdered() throws Exception {
        relayAmPrepare();

        //1.deploy app contract
        String appContract = deployAppContract();

        //2.app contract set protocol
        BIFSDK sdk = BIFSDK.getInstance(VALID_URL);
        try {
            String senderAddress = ADDDRESS;
            String contractAddress = appContract;
            String senderPrivateKey = PRIVATE_KEY;
            Long amount = 0L;
            String invokeInput = StrUtil.format("{\"function\":\"setProtocol(address)\",\"args\":\"{}\"}", bifchainBBCService.getBbcContext().getSdpContract().getContractAddress());
            BIFContractInvokeRequest request = new BIFContractInvokeRequest();
            request.setSenderAddress(senderAddress);
            request.setPrivateKey(senderPrivateKey);
            request.setContractAddress(contractAddress);
            request.setBIFAmount(amount);
            request.setRemarks("contract invoke");
            request.setInput(invokeInput);
            request.setFeeLimit(500000000L);
            request.setGasPrice(1L);

            BIFContractInvokeResponse response = sdk.getBIFContractService().contractInvoke(request);
            if (response.getErrorCode() == 0) {
                if (!queryTxResult(response.getResult().getHash())) {
                    throw new RuntimeException("failed to set protocol, transaction executing failed");
                }
            } else {
                throw new RuntimeException("failed to set protocol, transaction sending failed");
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to set protocol (address: %s) to APP %s",
                            bifchainBBCService.getBbcContext().getSdpContract().getContractAddress(), appContract), e);
        }

        long height1 = bifchainBBCService.queryLatestHeight();

        //3.send msg
        try {
            String senderAddress = ADDDRESS;
            String contractAddress = appContract;
            String senderPrivateKey = PRIVATE_KEY;
            Long amount = 0L;
            String invokeInput = StrUtil.format("{\"function\":\"sendMessage(string,bytes32,bytes)\",\"args\":\"'{}','{}','{}'\"}", "remoteDomain", "0x51bb64f74fdbb370bc5ee89d1bf4e3a3f1e61e2fadbc102844f9b9cd5652da20", HexUtil.encodeHexStr("UnorderedCrossChainMessage".getBytes()));
            BIFContractInvokeRequest request = new BIFContractInvokeRequest();
            request.setSenderAddress(senderAddress);
            request.setPrivateKey(senderPrivateKey);
            request.setContractAddress(contractAddress);
            request.setBIFAmount(amount);
            request.setRemarks("contract invoke");
            request.setInput(invokeInput);
            request.setFeeLimit(500000000L);
            request.setGasPrice(1L);

            BIFContractInvokeResponse response = sdk.getBIFContractService().contractInvoke(request);
            if (response.getErrorCode() == 0) {
                if (!queryTxResult(response.getResult().getHash())) {
                    throw new RuntimeException("failed to send unordered message, transaction executing failed");
                }
            } else {
                throw new RuntimeException("failed to send unordered message, transaction sending failed");
            }
        } catch (Exception e) {
            throw new RuntimeException("failed to send unordered message", e);
        }

        // 3. query latest height
        long height2 = bifchainBBCService.queryLatestHeight();

        // 4. read cc msg
        List<CrossChainMessage> messageList = ListUtil.toList();
        for(long i = height1; i <= height2; i++){
            messageList.addAll(bifchainBBCService.readCrossChainMessagesByHeight(i));
        }
        Assert.assertEquals(1, messageList.size());
        Assert.assertEquals(CrossChainMessage.CrossChainMessageType.AUTH_MSG, messageList.get(0).getType());
    }

    private String deployAppContract() {
        BIFSDK sdk = BIFSDK.getInstance(VALID_URL);
        String txHash;
        try {
            String senderAddress = ADDDRESS;
            String senderPrivateKey = PRIVATE_KEY;
            String payload = "608060405234801561000f575f80fd5b505f61001f6100d060201b60201c565b9050805f806101000a81548177ffffffffffffffffffffffffffffffffffffffffffffffff021916908377ffffffffffffffffffffffffffffffffffffffffffffffff1602179055508077ffffffffffffffffffffffffffffffffffffffffffffffff165f77ffffffffffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a3506100d7565b5f33905090565b6115f8806100e45f395ff3fe608060405234801561000f575f80fd5b50600436106100a7575f3560e01c80639670efcb1161006f5780639670efcb1461013d578063c09b261b1461016d578063c1cecc5a14610189578063f2fde38b146101a5578063f76f703b146101c1578063ff098be7146101dd576100a7565b80630a9d793d146100ab578063387868ae146100c75780633fecfe3f146100e5578063715018a6146101155780638da5cb5b1461011f575b5f80fd5b6100c560048036038101906100c09190610c79565b6101f9565b005b6100cf6102c8565b6040516100dc9190610cb3565b60405180910390f35b6100ff60048036038101906100fa9190610d32565b6102f1565b60405161010c9190610dfa565b60405180910390f35b61011d6103a2565b005b6101276104f4565b6040516101349190610cb3565b60405180910390f35b61015760048036038101906101529190610d32565b61051f565b6040516101649190610dfa565b60405180910390f35b61018760048036038101906101829190610fe4565b6105d0565b005b6101a3600480360381019061019e9190610fe4565b6106ef565b005b6101bf60048036038101906101ba9190610c79565b610805565b005b6101db60048036038101906101d69190610fe4565b6109ce565b005b6101f760048036038101906101f29190610fe4565b610ae5565b005b610201610c03565b77ffffffffffffffffffffffffffffffffffffffffffffffff166102236104f4565b77ffffffffffffffffffffffffffffffffffffffffffffffff161461027d576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610274906110c6565b60405180910390fd5b8060035f6101000a81548177ffffffffffffffffffffffffffffffffffffffffffffffff021916908377ffffffffffffffffffffffffffffffffffffffffffffffff16021790555050565b60035f9054906101000a900477ffffffffffffffffffffffffffffffffffffffffffffffff1681565b6002602052815f5260405f20818154811061030a575f80fd5b905f5260205f20015f9150915050805461032390611111565b80601f016020809104026020016040519081016040528092919081815260200182805461034f90611111565b801561039a5780601f106103715761010080835404028352916020019161039a565b820191905f5260205f20905b81548152906001019060200180831161037d57829003601f168201915b505050505081565b6103aa610c03565b77ffffffffffffffffffffffffffffffffffffffffffffffff166103cc6104f4565b77ffffffffffffffffffffffffffffffffffffffffffffffff1614610426576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161041d906110c6565b60405180910390fd5b5f77ffffffffffffffffffffffffffffffffffffffffffffffff165f8054906101000a900477ffffffffffffffffffffffffffffffffffffffffffffffff1677ffffffffffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a35f805f6101000a81548177ffffffffffffffffffffffffffffffffffffffffffffffff021916908377ffffffffffffffffffffffffffffffffffffffffffffffff160217905550565b5f805f9054906101000a900477ffffffffffffffffffffffffffffffffffffffffffffffff16905090565b6001602052815f5260405f208181548110610538575f80fd5b905f5260205f20015f9150915050805461055190611111565b80601f016020809104026020016040519081016040528092919081815260200182805461057d90611111565b80156105c85780601f1061059f576101008083540402835291602001916105c8565b820191905f5260205f20905b8154815290600101906020018083116105ab57829003601f168201915b505050505081565b60035f9054906101000a900477ffffffffffffffffffffffffffffffffffffffffffffffff1677ffffffffffffffffffffffffffffffffffffffffffffffff163377ffffffffffffffffffffffffffffffffffffffffffffffff161461066b576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016106629061118b565b60405180910390fd5b60015f8381526020019081526020015f2081908060018154018082558091505060019003905f5260205f20015f9091909190915090816106ab9190611346565b507f7b59e766dd02d6ce4e574f0ab75dfc4b180c90deb50dd9dbdbc65768abcf80f183838360016040516106e29493929190611480565b60405180910390a1505050565b60035f9054906101000a900477ffffffffffffffffffffffffffffffffffffffffffffffff1677ffffffffffffffffffffffffffffffffffffffffffffffff1663c1cecc5a8484846040518463ffffffff1660e01b8152600401610755939291906114d1565b5f604051808303815f87803b15801561076c575f80fd5b505af115801561077e573d5f803e3d5ffd5b5050505060025f8381526020019081526020015f2081908060018154018082558091505060019003905f5260205f20015f9091909190915090816107c29190611346565b507fbbe83d8459c305cf51f2425b93e693dcfaaf60f22766486b0983c905b98ead408383835f6040516107f89493929190611480565b60405180910390a1505050565b61080d610c03565b77ffffffffffffffffffffffffffffffffffffffffffffffff1661082f6104f4565b77ffffffffffffffffffffffffffffffffffffffffffffffff1614610889576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610880906110c6565b60405180910390fd5b5f77ffffffffffffffffffffffffffffffffffffffffffffffff168177ffffffffffffffffffffffffffffffffffffffffffffffff16036108ff576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016108f690611584565b60405180910390fd5b8077ffffffffffffffffffffffffffffffffffffffffffffffff165f8054906101000a900477ffffffffffffffffffffffffffffffffffffffffffffffff1677ffffffffffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a3805f806101000a81548177ffffffffffffffffffffffffffffffffffffffffffffffff021916908377ffffffffffffffffffffffffffffffffffffffffffffffff16021790555050565b60035f9054906101000a900477ffffffffffffffffffffffffffffffffffffffffffffffff1677ffffffffffffffffffffffffffffffffffffffffffffffff1663f76f703b8484846040518463ffffffff1660e01b8152600401610a34939291906114d1565b5f604051808303815f87803b158015610a4b575f80fd5b505af1158015610a5d573d5f803e3d5ffd5b5050505060025f8381526020019081526020015f2081908060018154018082558091505060019003905f5260205f20015f909190919091509081610aa19190611346565b507fbbe83d8459c305cf51f2425b93e693dcfaaf60f22766486b0983c905b98ead408383836001604051610ad89493929190611480565b60405180910390a1505050565b60035f9054906101000a900477ffffffffffffffffffffffffffffffffffffffffffffffff1677ffffffffffffffffffffffffffffffffffffffffffffffff163377ffffffffffffffffffffffffffffffffffffffffffffffff1614610b80576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610b779061118b565b60405180910390fd5b60015f8381526020019081526020015f2081908060018154018082558091505060019003905f5260205f20015f909190919091509081610bc09190611346565b507f7b59e766dd02d6ce4e574f0ab75dfc4b180c90deb50dd9dbdbc65768abcf80f18383835f604051610bf69493929190611480565b60405180910390a1505050565b5f33905090565b5f604051905090565b5f80fd5b5f80fd5b5f77ffffffffffffffffffffffffffffffffffffffffffffffff82169050919050565b5f610c4882610c1b565b9050919050565b610c5881610c3e565b8114610c62575f80fd5b50565b5f81359050610c7381610c4f565b92915050565b5f60208284031215610c8e57610c8d610c13565b5b5f610c9b84828501610c65565b91505092915050565b610cad81610c3e565b82525050565b5f602082019050610cc65f830184610ca4565b92915050565b5f819050919050565b610cde81610ccc565b8114610ce8575f80fd5b50565b5f81359050610cf981610cd5565b92915050565b5f819050919050565b610d1181610cff565b8114610d1b575f80fd5b50565b5f81359050610d2c81610d08565b92915050565b5f8060408385031215610d4857610d47610c13565b5b5f610d5585828601610ceb565b9250506020610d6685828601610d1e565b9150509250929050565b5f81519050919050565b5f82825260208201905092915050565b5f5b83811015610da7578082015181840152602081019050610d8c565b5f8484015250505050565b5f601f19601f8301169050919050565b5f610dcc82610d70565b610dd68185610d7a565b9350610de6818560208601610d8a565b610def81610db2565b840191505092915050565b5f6020820190508181035f830152610e128184610dc2565b905092915050565b5f80fd5b5f80fd5b7f4e487b71000000000000000000000000000000000000000000000000000000005f52604160045260245ffd5b610e5882610db2565b810181811067ffffffffffffffff82111715610e7757610e76610e22565b5b80604052505050565b5f610e89610c0a565b9050610e958282610e4f565b919050565b5f67ffffffffffffffff821115610eb457610eb3610e22565b5b610ebd82610db2565b9050602081019050919050565b828183375f83830152505050565b5f610eea610ee584610e9a565b610e80565b905082815260208101848484011115610f0657610f05610e1e565b5b610f11848285610eca565b509392505050565b5f82601f830112610f2d57610f2c610e1a565b5b8135610f3d848260208601610ed8565b91505092915050565b5f67ffffffffffffffff821115610f6057610f5f610e22565b5b610f6982610db2565b9050602081019050919050565b5f610f88610f8384610f46565b610e80565b905082815260208101848484011115610fa457610fa3610e1e565b5b610faf848285610eca565b509392505050565b5f82601f830112610fcb57610fca610e1a565b5b8135610fdb848260208601610f76565b91505092915050565b5f805f60608486031215610ffb57610ffa610c13565b5b5f84013567ffffffffffffffff81111561101857611017610c17565b5b61102486828701610f19565b935050602061103586828701610ceb565b925050604084013567ffffffffffffffff81111561105657611055610c17565b5b61106286828701610fb7565b9150509250925092565b5f82825260208201905092915050565b7f4f776e61626c653a2063616c6c6572206973206e6f7420746865206f776e65725f82015250565b5f6110b060208361106c565b91506110bb8261107c565b602082019050919050565b5f6020820190508181035f8301526110dd816110a4565b9050919050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52602260045260245ffd5b5f600282049050600182168061112857607f821691505b60208210810361113b5761113a6110e4565b5b50919050565b7f494e56414c49445f5045524d495353494f4e00000000000000000000000000005f82015250565b5f61117560128361106c565b915061118082611141565b602082019050919050565b5f6020820190508181035f8301526111a281611169565b9050919050565b5f819050815f5260205f209050919050565b5f6020601f8301049050919050565b5f82821b905092915050565b5f600883026112057fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff826111ca565b61120f86836111ca565b95508019841693508086168417925050509392505050565b5f819050919050565b5f61124a61124561124084610cff565b611227565b610cff565b9050919050565b5f819050919050565b61126383611230565b61127761126f82611251565b8484546111d6565b825550505050565b5f90565b61128b61127f565b61129681848461125a565b505050565b5b818110156112b9576112ae5f82611283565b60018101905061129c565b5050565b601f8211156112fe576112cf816111a9565b6112d8846111bb565b810160208510156112e7578190505b6112fb6112f3856111bb565b83018261129b565b50505b505050565b5f82821c905092915050565b5f61131e5f1984600802611303565b1980831691505092915050565b5f611336838361130f565b9150826002028217905092915050565b61134f82610d70565b67ffffffffffffffff81111561136857611367610e22565b5b6113728254611111565b61137d8282856112bd565b5f60209050601f8311600181146113ae575f841561139c578287015190505b6113a6858261132b565b86555061140d565b601f1984166113bc866111a9565b5f5b828110156113e3578489015182556001820191506020850194506020810190506113be565b8683101561140057848901516113fc601f89168261130f565b8355505b6001600288020188555050505b505050505050565b5f81519050919050565b5f61142982611415565b611433818561106c565b9350611443818560208601610d8a565b61144c81610db2565b840191505092915050565b61146081610ccc565b82525050565b5f8115159050919050565b61147a81611466565b82525050565b5f6080820190508181035f830152611498818761141f565b90506114a76020830186611457565b81810360408301526114b98185610dc2565b90506114c86060830184611471565b95945050505050565b5f6060820190508181035f8301526114e9818661141f565b90506114f86020830185611457565b818103604083015261150a8184610dc2565b9050949350505050565b7f4f776e61626c653a206e6577206f776e657220697320746865207a65726f20615f8201527f6464726573730000000000000000000000000000000000000000000000000000602082015250565b5f61156e60268361106c565b915061157982611514565b604082019050919050565b5f6020820190508181035f83015261159b81611562565b905091905056fea26469706673582212207aa38a26a42055253138c1fe204d48d8271e6384a6e09490e9a26d3c3442f34464736f6c637822302e382e32312d63692e323032342e332e312b636f6d6d69742e31383065353661320053";
            Long initBalance = ToBaseUnit.ToUGas("0");

            BIFContractCreateRequest request = new BIFContractCreateRequest();
            request.setSenderAddress(senderAddress);
            request.setPrivateKey(senderPrivateKey);
            request.setInitBalance(initBalance);
            request.setPayload(payload);
            request.setRemarks("create contract");
            request.setType(1);
            request.setFeeLimit(500000000L);
            request.setGasPrice(1L);

            BIFContractCreateResponse response = sdk.getBIFContractService().contractCreate(request);
            if (response.getErrorCode() == 0) {
                txHash = response.getResult().getHash();
                boolean result = queryTxResult(txHash);
                if (!result) {
                    throw new RuntimeException("transaction executing failed");
                }
            } else {
                throw new RuntimeException("failed to deploy APP contract");
            }
        } catch (Exception e) {
            throw new RuntimeException("failed to deploy APP contract", e);
        }

        String address;
        try {
            BIFContractGetAddressRequest request = new BIFContractGetAddressRequest();
            request.setHash(txHash);

            BIFContractGetAddressResponse response = sdk.getBIFContractService().getContractAddress(request);
            if (response.getErrorCode() == 0) {
                address = response.getResult().getContractAddressInfos().get(0).getContractAddress();
            } else {
                throw new RuntimeException("failed to get APP contract address");
            }
        } catch (Exception e) {
            throw new RuntimeException("failed to get APP contract address", e);
        }

        return address;
    }

    private Boolean queryTxResult(String txHash) {
        BIFTransactionGetInfoRequest bifTransactionGetInfoRequest =new BIFTransactionGetInfoRequest();
        bifTransactionGetInfoRequest.setHash(txHash);
        BIFTransactionGetInfoResponse bifTransactionGetInfoResponse;
        BIFSDK sdk = BIFSDK.getInstance(VALID_URL);
        while (true) {
            try {
                Thread.sleep(1000L);
                bifTransactionGetInfoResponse = sdk.getBIFTransactionService().getTransactionInfo(bifTransactionGetInfoRequest);
                if (ObjectUtil.isNotNull(bifTransactionGetInfoResponse.getResult()) && bifTransactionGetInfoResponse.getResult().getTransactions().length > 0) {
                    break;
                }
            } catch (Throwable e) {
                throw new RuntimeException("failed to query tx", e);
            }
        }
        return bifTransactionGetInfoResponse.getResult().getTransactions()[0].getErrorCode() == 0;
    }

    private void relayAmPrepare(){
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        bifchainBBCService.startup(mockValidCtx);

        // set up am
        bifchainBBCService.setupAuthMessageContract();

        // set up sdp
        bifchainBBCService.setupSDPMessageContract();

        // set protocol to am (sdp type: 0)
        bifchainBBCService.setProtocol(
                mockValidCtx.getSdpContract().getContractAddress(),
                "0");

        // set am to sdp
        bifchainBBCService.setAmContract(mockValidCtx.getAuthMessageContract().getContractAddress());

        // set local domain to sdp
        bifchainBBCService.setLocalDomain("receiverDomain");

        // check contract ready
        AbstractBBCContext ctxCheck = bifchainBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctxCheck.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctxCheck.getSdpContract().getStatus());
    }

    private AbstractBBCContext mockValidPTCCtx(){
        BifchainConfig mockConf = new BifchainConfig();
        mockConf.setUrl(VALID_URL);
        mockConf.setPrivateKey(PRIVATE_KEY);
        mockConf.setAddress(ADDDRESS);
        mockConf.setPtcContractInitInput(HexUtil.encodeHexStr(createBcdnsRootCert()));
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    private AbstractBBCContext mockValidCtx(){
        BifchainConfig mockConf = new BifchainConfig();
        mockConf.setUrl(VALID_URL);
        mockConf.setPrivateKey(PRIVATE_KEY);
        mockConf.setAddress(ADDDRESS);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    private AbstractBBCContext mockValidCtxWithPreDeployedContracts(String amAddr, String sdpAddr){
        BifchainConfig mockConf = new BifchainConfig();
        mockConf.setUrl(VALID_URL);
        mockConf.setPrivateKey(PRIVATE_KEY);
        mockConf.setAddress(ADDDRESS);
        mockConf.setAmContractAddressDeployed(amAddr);
        mockConf.setSdpContractAddressDeployed(sdpAddr);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    private AbstractBBCContext mockValidCtxWithPreReadyContracts(String amAddr, String sdpAddr){
        BifchainConfig mockConf = new BifchainConfig();
        mockConf.setUrl(VALID_URL);
        mockConf.setPrivateKey(PRIVATE_KEY);
        mockConf.setAddress(ADDDRESS);
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

    private byte[] getRawMsgFromRelayer(String appContract) throws IOException {
        String base58String = appContract.substring(10);
        byte[] decodedBytes = Base58.decode(base58String);
        String addr = HexUtil.encodeHexStr(decodedBytes);

        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(
                1,
                "".getBytes(),
                "receiverDomain",
                HexUtil.decodeHex(
                        String.format("00000000000000006566%s", addr)
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
