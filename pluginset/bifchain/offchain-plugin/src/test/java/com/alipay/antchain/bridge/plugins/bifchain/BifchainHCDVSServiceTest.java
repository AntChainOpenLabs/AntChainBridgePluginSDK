package com.alipay.antchain.bridge.plugins.bifchain;

import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
import com.alipay.antchain.bridge.commons.core.base.ConsensusState;
import com.alipay.antchain.bridge.commons.core.bta.BlockchainTrustAnchorV1;
import com.alipay.antchain.bridge.commons.core.bta.IBlockchainTrustAnchor;
import com.alipay.antchain.bridge.plugins.spi.ptc.core.VerifyResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;

public class BifchainHCDVSServiceTest {

    private static BifchainHCDVSService bifchainHCDVSService;

    @Before
    public void init() throws Exception {
        bifchainHCDVSService = new BifchainHCDVSService();
    }

    @Test
    public void testVerifyAnchorConsensusState(){
        // start up success

        BlockchainTrustAnchorV1 blockchainTrustAnchorV1 = new BlockchainTrustAnchorV1();
        String[] validitors = {"did:bid:efuDacbAeXdwBENVkPCYNQQVE7KvYVkP",
                "did:bid:efperBrbfFJrpRUebqDSmXjoLKoBiuGT",
                "did:bid:ef16mauW9ukBbLqYpbZ8b7bavXTPeGMC",
                "did:bid:efk8iXFgo533n7waZAym6WVeZNPKKyX5"};

        BigInteger bigInteger = new BigInteger("4189941");

        String joinedString = String.join(",", validitors);
        blockchainTrustAnchorV1.setSubjectIdentity(joinedString.getBytes());
        blockchainTrustAnchorV1.setInitHeight(bigInteger);

        byte[] initHash = {-10, 104, -19, 89, 27, 64, 37, -82, 40, 77, 27, -28, 11, -41, -101, -6, 64, 113, -107, -107, 36, -112, 72, -123, 20, 48, 98, 55, -2, 13, -33, -34};
        blockchainTrustAnchorV1.setInitBlockHash(initHash);
        BifchainBBCService bifchainBBCService = new BifchainBBCService();

        BifchainConfig mockConf = new BifchainConfig();
        mockConf.setUrl("http://test.bifcore.bitfactory.cn");
        mockConf.setPrivateKey("priSPKgxGjV3kCJbSDCYAjY7iETF7UtJcba8XpMKZNsxTQBRkU");
        mockConf.setDomainName(".com");
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());

        bifchainBBCService.startup(mockCtx);
        ConsensusState consensusState = bifchainBBCService.readConsensusState(bigInteger);

        VerifyResult verifyResult = bifchainHCDVSService.verifyAnchorConsensusState(blockchainTrustAnchorV1, consensusState);
        Assert.assertTrue(verifyResult.isSuccess());
    }

    @Test
    public void testVerifyConsensusState(){
        // start up success
        BifchainBBCService bifchainBBCService = new BifchainBBCService();

        BifchainConfig mockConf = new BifchainConfig();
        mockConf.setUrl("http://test.bifcore.bitfactory.cn");
        mockConf.setPrivateKey("priSPKgxGjV3kCJbSDCYAjY7iETF7UtJcba8XpMKZNsxTQBRkU");
        mockConf.setDomainName(".com");
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());

        bifchainBBCService.startup(mockCtx);

        BigInteger bigInteger1 = new BigInteger("4189941");
        ConsensusState consensusState1 = bifchainBBCService.readConsensusState(bigInteger1);

        BigInteger bigInteger2 = new BigInteger("4189942");
        ConsensusState consensusState2 = bifchainBBCService.readConsensusState(bigInteger2);

        VerifyResult verifyResult = bifchainHCDVSService.verifyConsensusState(consensusState2, consensusState1);
        Assert.assertTrue(verifyResult.isSuccess());

    }

}
