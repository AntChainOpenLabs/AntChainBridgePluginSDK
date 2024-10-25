package com.alipay.antchain.bridge.plugins.bifchain;

import cn.bif.api.BIFSDK;
import cn.bif.common.JsonUtils;
import cn.bif.model.request.BIFTransactionGetInfoRequest;
import cn.bif.model.response.BIFTransactionGetInfoResponse;
import cn.bif.module.encryption.key.PublicKeyManager;
import cn.bif.utils.generator.response.Log;
import com.alipay.antchain.bridge.commons.core.base.ConsensusState;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.bta.IBlockchainTrustAnchor;
import com.alipay.antchain.bridge.plugins.lib.HeteroChainDataVerifierService;
import com.alipay.antchain.bridge.plugins.spi.ptc.AbstractHCDVSService;
import com.alipay.antchain.bridge.plugins.spi.ptc.core.VerifyResult;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@HeteroChainDataVerifierService(products = "bifchain", pluginId = "plugin-simple-bifchain")
public class BifchainHCDVSService extends AbstractHCDVSService {

    private static final Logger log = LoggerFactory.getLogger(BifchainHCDVSService.class);

    private AbstractHCDVSService hcdvsContext;

    @Override
    public VerifyResult verifyAnchorConsensusState(IBlockchainTrustAnchor bta, ConsensusState anchorState) {
        VerifyResult.VerifyResultBuilder verifyResultBuilder = VerifyResult.builder();
        try {
            //1. get bta validator
            byte[] subjectIdentity = bta.getSubjectIdentity();
            String validatorStr = new String(subjectIdentity, StandardCharsets.UTF_8);
            String[] validators = validatorStr.split(",");

            //2. get consensus validator
            byte[] consensusNodeInfoByte = anchorState.getConsensusNodeInfo();
            String decodedString = new String(consensusNodeInfoByte, StandardCharsets.UTF_8);
            String[] consensusNodeInfo = decodedString.split(",");

            if (validators.length != consensusNodeInfo.length) {
                verifyResultBuilder.success(false);
                verifyResultBuilder.errorMsg("validator number is not equal");
                return verifyResultBuilder.build();
            }

            //3. check equal
            Arrays.sort(validators);
            Arrays.sort(consensusNodeInfo);
            if (!Arrays.equals(validators, consensusNodeInfo)) {
                verifyResultBuilder.success(false);
                verifyResultBuilder.errorMsg("validator array is not equal");
                return verifyResultBuilder.build();
            }

            //4.check endorsement
            byte[] endorsementByte = anchorState.getEndorsements();
            Consensus.PbftProof pbftProof = Consensus.PbftProof.parseFrom(endorsementByte);
            for (int i = 0; i < pbftProof.getCommitsCount(); i++) {
                String publicKey = pbftProof.getCommits(i).getSignature().getPublicKey();
                byte[] signData = pbftProof.getCommits(i).getSignature().getSignData().toByteArray();
                byte[] data = pbftProof.getCommits(i).getPbft().toByteArray();

                PublicKeyManager publicKeyManager = new PublicKeyManager(publicKey);
                if (!publicKeyManager.verify(data, signData)) {
                    verifyResultBuilder.success(false);
                    verifyResultBuilder.errorMsg("failed to verify sign");
                    return verifyResultBuilder.build();
                }

                String address = publicKeyManager.getEncAddress();
                if (Arrays.stream(validators).noneMatch(str -> str.equals(address))) {
                    verifyResultBuilder.success(false);
                    String error_msg = address + "is not found";
                    verifyResultBuilder.errorMsg(error_msg);
                    return verifyResultBuilder.build();
                }
            }

            if (!Arrays.equals(anchorState.getHash(), bta.getInitBlockHash())
                    || !anchorState.getHeight().equals(bta.getInitHeight())) {
                verifyResultBuilder.success(false);
                verifyResultBuilder.errorMsg("hash or height in not equal");
                return verifyResultBuilder.build();
            }

            return verifyResultBuilder.success(true).build();
        } catch (Exception e) {
            throw new RuntimeException("failed to deploy ptc contract", e);
        }
    }

    @Override
    public VerifyResult verifyConsensusState(ConsensusState stateToVerify, ConsensusState parentState) {
        VerifyResult.VerifyResultBuilder verifyResultBuilder = VerifyResult.builder();
        try {
            byte[] consensusNodeInfoByte = parentState.getConsensusNodeInfo();
            String decodedString = new String(consensusNodeInfoByte, StandardCharsets.UTF_8);
            String[] consensusNodeInfo = decodedString.split(",");

            byte[] endorsementByte = stateToVerify.getEndorsements();
            Consensus.PbftProof pbftProof = Consensus.PbftProof.parseFrom(endorsementByte);
            for (int i = 0; i < pbftProof.getCommitsCount(); i++) {
                String publicKey = pbftProof.getCommits(i).getSignature().getPublicKey();
                byte[] signData = pbftProof.getCommits(i).getSignature().getSignData().toByteArray();
                byte[] data = pbftProof.getCommits(i).getPbft().toByteArray();

                PublicKeyManager publicKeyManager = new PublicKeyManager(publicKey);
                if (!publicKeyManager.verify(data, signData)) {
                    verifyResultBuilder.success(false);
                    verifyResultBuilder.errorMsg("failed to verify sign");
                    return verifyResultBuilder.build();
                }

                String address = publicKeyManager.getEncAddress();
                if (Arrays.stream(consensusNodeInfo).noneMatch(str -> str.equals(address))) {
                    verifyResultBuilder.success(false);
                    String error_msg = address + "is not found";
                    verifyResultBuilder.errorMsg(error_msg);
                    return verifyResultBuilder.build();
                }

                long seq = pbftProof.getCommits(i).getPbft().getCommit().getSequence();
                if (stateToVerify.getHeight().longValue() != (seq+1)) {
                    verifyResultBuilder.success(false);
                    String error_msg = address + "failed to check block seq";
                    verifyResultBuilder.errorMsg(error_msg);
                    return verifyResultBuilder.build();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("failed to verify consensus state", e);
        }
        return verifyResultBuilder.success(true).build();
    }

    @Override
    public VerifyResult verifyCrossChainMessage(CrossChainMessage message, ConsensusState currState) {
        VerifyResult.VerifyResultBuilder verifyResultBuilder = VerifyResult.builder();
        try {
            JsonObject jsonObject = JsonParser.parseString(new String(message.getProvableData().getLedgerData())).getAsJsonObject();
            String url = jsonObject.get("url").getAsString();
            BIFSDK sdk = BIFSDK.getInstance(url);
            String txHash = jsonObject.get("txHash").getAsString();

            BIFTransactionGetInfoRequest bifTransactionGetInfoRequest = new BIFTransactionGetInfoRequest();
            bifTransactionGetInfoRequest.setHash(txHash);
            BIFTransactionGetInfoResponse bifTransactionGetInfoResponse = sdk.getBIFTransactionService().getTransactionInfo(bifTransactionGetInfoRequest);
            if (bifTransactionGetInfoResponse.getErrorCode() != 0) {
                verifyResultBuilder.success(false);
                verifyResultBuilder.errorMsg("cross chain message is not existed");
                return verifyResultBuilder.build();
            }

            if (bifTransactionGetInfoResponse.getResult().getTransactions()[0].getLedgerSeq() != currState.getHeight().longValue() ||
                    bifTransactionGetInfoResponse.getResult().getTransactions()[0].getLedgerSeq() != message.getProvableData().getHeight()) {
                verifyResultBuilder.success(false);
                verifyResultBuilder.errorMsg("cross chain message height is not equal");
                return verifyResultBuilder.build();
            }

            if (bifTransactionGetInfoResponse.getResult().getTransactions()[0].getConfirmTime() != currState.getStateTimestamp() ||
                    bifTransactionGetInfoResponse.getResult().getTransactions()[0].getConfirmTime() != message.getProvableData().getTimestamp()) {
                verifyResultBuilder.success(false);
                verifyResultBuilder.errorMsg("cross chain message timestamp is not equal");
                return verifyResultBuilder.build();
            }

            if (currState.getHash() != message.getProvableData().getBlockHash()) {
                verifyResultBuilder.success(false);
                verifyResultBuilder.errorMsg("cross chain message block hash is not equal");
                return verifyResultBuilder.build();
            }
            return verifyResultBuilder.success(true).build();
        } catch (Exception e) {
            throw new RuntimeException("failed to verify consensus state", e);
        }
    }

    @Override
    public byte[] parseMessageFromLedgerData(byte[] ledgerData) {
        try {
            JsonObject jsonObject = JsonParser.parseString(new String(ledgerData)).getAsJsonObject();
            String url = jsonObject.get("url").getAsString();
            BIFSDK sdk = BIFSDK.getInstance(url);
            String txHash = jsonObject.get("txHash").getAsString();

            BIFTransactionGetInfoRequest bifTransactionGetInfoRequest = new BIFTransactionGetInfoRequest();
            bifTransactionGetInfoRequest.setHash(txHash);
            BIFTransactionGetInfoResponse bifTransactionGetInfoResponse = sdk.getBIFTransactionService().getTransactionInfo(bifTransactionGetInfoRequest);
            if (bifTransactionGetInfoResponse.getErrorCode() == 0) {
                String topic = bifTransactionGetInfoResponse.getResult().getTransactions()[0].getTransaction().getOperations()[0].getLog().getTopic();
                if (!topic.equals("79b7516b1b7a6a39fb4b7b22e8667cd3744e5c27425292f8a9f49d1042c0c651")) {
                    throw new RuntimeException("topic is wrong");
                }
                String json = JsonUtils.toJSONString(bifTransactionGetInfoResponse.getResult().getTransactions()[0].getTransaction().getOperations()[0].getLog());
                AuthMsg authMsg = new AuthMsg();
                Log log = authMsg.jsonToLog(json);
                return AuthMsg.getSendAuthMessageEventFromLog(log).result.pkg;
            } else {
                throw new RuntimeException("failed to get transaction info");
            }
        } catch (Exception e) {
            throw new RuntimeException("failed to parse message from ledger data", e);
        }
    }
}

