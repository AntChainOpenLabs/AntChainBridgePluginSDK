/*
 * Alipay.com Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.alipay.antchain.bridge.relayer.bootstrap;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import cn.hutool.cache.Cache;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.PemUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alipay.antchain.bridge.bcdns.service.BCDNSTypeEnum;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.alipay.antchain.bridge.commons.bcdns.DomainNameCredentialSubject;
import com.alipay.antchain.bridge.commons.bcdns.PTCCredentialSubject;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageFactory;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageTrustLevelEnum;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageV2;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.base.*;
import com.alipay.antchain.bridge.commons.core.bta.BlockchainTrustAnchorV1;
import com.alipay.antchain.bridge.commons.core.ptc.*;
import com.alipay.antchain.bridge.commons.core.sdp.ISDPMessage;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageFactory;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageV1;
import com.alipay.antchain.bridge.commons.utils.crypto.HashAlgoEnum;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import com.alipay.antchain.bridge.pluginserver.service.*;
import com.alipay.antchain.bridge.ptc.committee.types.basic.CommitteeEndorseProof;
import com.alipay.antchain.bridge.ptc.committee.types.basic.CommitteeNodeProof;
import com.alipay.antchain.bridge.ptc.committee.types.basic.NodePublicKeyEntry;
import com.alipay.antchain.bridge.ptc.committee.types.tpbta.CommitteeEndorseRoot;
import com.alipay.antchain.bridge.ptc.committee.types.tpbta.NodeEndorseInfo;
import com.alipay.antchain.bridge.ptc.committee.types.tpbta.OptionalEndorsePolicy;
import com.alipay.antchain.bridge.ptc.committee.types.tpbta.VerifyBtaExtension;
import com.alipay.antchain.bridge.relayer.bootstrap.basic.BlockchainModelsTest;
import com.alipay.antchain.bridge.relayer.bootstrap.utils.MyRedisServer;
import com.alipay.antchain.bridge.relayer.commons.constant.*;
import com.alipay.antchain.bridge.relayer.commons.model.*;
import com.google.protobuf.ByteString;
import lombok.SneakyThrows;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisExecProvider;
import redis.embedded.util.Architecture;
import redis.embedded.util.OS;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = AntChainBridgeRelayerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Sql(scripts = {"classpath:data/ddl.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/drop_all.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public abstract class TestBase {

    public static final BlockchainMeta.BlockchainProperties blockchainProperties1
            = BlockchainMeta.BlockchainProperties.decode(BlockchainModelsTest.BLOCKCHAIN_META_EXAMPLE_OBJ.getBytes());

    public static final BlockchainMeta.BlockchainProperties blockchainProperties2
            = BlockchainMeta.BlockchainProperties.decode(BlockchainModelsTest.BLOCKCHAIN_META_EXAMPLE_OBJ.getBytes());

    public static final BlockchainMeta.BlockchainProperties blockchainProperties3
            = BlockchainMeta.BlockchainProperties.decode(BlockchainModelsTest.BLOCKCHAIN_META_EXAMPLE_OBJ.getBytes());

    public static final BlockchainMeta testchain1Meta = new BlockchainMeta("testchain", "testchain_1.id", "", "", blockchainProperties1);

    public static final BlockchainMeta testchain2Meta = new BlockchainMeta("testchain", "testchain_2.id", "", "", blockchainProperties2);

    public static final BlockchainMeta testchain3Meta = new BlockchainMeta("testchain", "testchain_3.id", "", "", blockchainProperties3);

    public static AbstractCrossChainCertificate antchainDotCommCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
            FileUtil.readBytes("cc_certs/antchain.com.crt")
    );

    public static String antChainDotComDomain = "antchain.com";

    public static String antChainDotComProduct = "mychain";

    public static String antChainDotComBlockchainId = antChainDotComDomain + ".id";

    public static AbstractCrossChainCertificate catchainDotCommCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
            FileUtil.readBytes("cc_certs/catchain.com.crt")
    );

    public static String catChainDotComProduct = "ethereum";

    public static String catChainDotComDomain = "catchain.com";

    public static String catChainDotComBlockchainId = catChainDotComDomain + ".id";

    public static AbstractCrossChainCertificate dogchainDotCommCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
            FileUtil.readBytes("cc_certs/dogchain.com.crt")
    );

    public static DomainNameCredentialSubject antchainSubject = DomainNameCredentialSubject.decode(antchainDotCommCert.getCredentialSubject());

    public static DomainNameCredentialSubject catchainSubject = DomainNameCredentialSubject.decode(catchainDotCommCert.getCredentialSubject());

    public static DomainNameCredentialSubject dogchainSubject = DomainNameCredentialSubject.decode(dogchainDotCommCert.getCredentialSubject());

    public static CrossChainIdentity antchainIdentity = new CrossChainIdentity(RandomUtil.randomBytes(32));

    public static CrossChainIdentity catchainIdentity = new CrossChainIdentity(RandomUtil.randomBytes(32));

    public static CrossChainIdentity antchainIdentity2 = new CrossChainIdentity(RandomUtil.randomBytes(32));

    public static CrossChainIdentity catchainIdentity2 = new CrossChainIdentity(RandomUtil.randomBytes(32));

    public static AbstractCrossChainCertificate dotComDomainSpaceCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
            FileUtil.readBytes("cc_certs/x.com.crt")
    );

    public static AbstractCrossChainCertificate dotComDomainSpaceCertWrongIssuer = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
            FileUtil.readBytes("cc_certs/x.com_wrong_issuer.crt")
    );

    public static String dotComDomainSpace = ".com";

    public static AbstractCrossChainCertificate relayerCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
            FileUtil.readBytes("cc_certs/relayer.crt")
    );

    public static AbstractCrossChainCertificate trustRootCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
            FileUtil.readBytes("cc_certs/trust_root.crt")
    );

    public static AbstractCrossChainCertificate relayerCertWrongIssuer = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
            FileUtil.readBytes("cc_certs/relayer_wrong_issuer.crt")
    );

    public static byte[] rawBIDDocument = FileUtil.readBytes("cc_certs/bid_document.json");

    public static String bid = "did:bid:efbThy5sbG7P3mFUp2EWN5oQGX6LUGwg";

    public static PrivateKey privateKey;

    public static MyRedisServer redisServer;

    public static BCDNSServiceDO rootBcdnsServiceDO = new BCDNSServiceDO(
            CrossChainDomain.ROOT_DOMAIN_SPACE,
            trustRootCert.getCredentialSubjectInstance().getApplicant(),
            new DomainSpaceCertWrapper(trustRootCert),
            BCDNSTypeEnum.BIF,
            BCDNSStateEnum.WORKING,
            FileUtil.readBytes("bcdns/root_bcdns.json")
    );

    public static BCDNSServiceDO dotComBcdnsServiceDO = new BCDNSServiceDO(
            dotComDomainSpace,
            dotComDomainSpaceCert.getCredentialSubjectInstance().getApplicant(),
            new DomainSpaceCertWrapper(dotComDomainSpaceCert),
            BCDNSTypeEnum.BIF,
            BCDNSStateEnum.WORKING,
            FileUtil.readBytes("bcdns/root_bcdns.json")
    );

    public static final String PS_ID = "p-QYj86x8Zd";

    public static final String PS_ADDR = "localhost:9090";

    public static String psCert = FileUtil.readString("node_keys/ps/relayer.crt", Charset.defaultCharset());

    public static PluginServerDO pluginServerDO = new PluginServerDO(
            0,
            PS_ID,
            PS_ADDR,
            PluginServerStateEnum.INIT,
            ListUtil.toList(antChainDotComProduct, catChainDotComProduct),
            ListUtil.toList(antChainDotComDomain, catChainDotComDomain),
            new PluginServerDO.PluginServerProperties(psCert),
            new Date(),
            new Date()
    );

    public static AuthMessageV2 authMessageV2 = new AuthMessageV2();

    public static SDPMessageV1 sdpMessageV1 = new SDPMessageV1();

    public static final byte[] RAW_NODE_PTC_PUBLIC_KEY = PemUtil.readPem(
            new ByteArrayInputStream(FileUtil.readBytes("cc_certs/public_key.pem"))
    );

    public static final PrivateKey NODE_PTC_PRIVATE_KEY = SignAlgoEnum.KECCAK256_WITH_SECP256K1.getSigner().readPemPrivateKey(
            FileUtil.readBytes("cc_certs/private_key.pem")
    );

    public static final AbstractCrossChainCertificate NODE_PTC_CERT = CrossChainCertificateUtil.readCrossChainCertificateFromPem(
            FileUtil.readBytes("cc_certs/ptc.crt")
    );

    public static ThirdPartyBlockchainTrustAnchorV1 tpbtaWithChannelLevel;

    public static ThirdPartyBlockchainTrustAnchorV1 tpbtaWithChannelLevel_cat;

    public static ThirdPartyBlockchainTrustAnchorV1 tpbtaWithLaneLevel1;

    public static ThirdPartyBlockchainTrustAnchorV1 tpbtaWithLaneLevel2;

    public static ConsensusState anchorState;

    public static ConsensusState anchorState_cat;

    public static ConsensusState currState;

    public static ConsensusState currState_cat;

    public static ISDPMessage sdpMessage;

    public static SDPMsgWrapper sdpMsgWrapper;

    public static IAuthMessage authMessage;

    public static AuthMsgWrapper authMsgWrapper;

    public static UniformCrosschainPacket ucp;

    public static UniformCrosschainPacketContext ucpContext;

    public static ISDPMessage sdpMessageV3;

    public static SDPMsgWrapper sdpMsgWrapperV3;

    public static IAuthMessage authMessage_sdpV3;

    public static AuthMsgWrapper authMsgWrapper_sdpV3;

    public static UniformCrosschainPacket ucp_sdpV3;

    public static UniformCrosschainPacketContext ucpContext_sdpV3;

    public static ThirdPartyProof thirdPartyProof;

    public static ThirdPartyProof thirdPartyProof_catVcs;

    public static ValidatedConsensusStateV1 anchorVcs;

    public static ValidatedConsensusStateV1 currVcs;

    public static ValidatedConsensusStateV1 currVcs_cat;

    public static ObjectIdentity ptcOid;

    public static final String COMMITTEE_ID = "committee";

    public static final String PTC_SERVICE_ID1 = "ptc1";

    public static final PTCVerifyAnchor ptcVerifyAnchor;

    public static final PTCTrustRoot ptcTrustRoot;

    public static final PtcServiceDO PTC_SERVICE_DO = PtcServiceDO.builder()
            .serviceId(PTC_SERVICE_ID1)
            .type(PTCTypeEnum.COMMITTEE)
            .ptcCert(NODE_PTC_CERT)
            .issuerBcdnsDomainSpace(dotComDomainSpace)
            .ownerId(ptcOid)
            .state(PtcServiceStateEnum.WORKING)
            .clientConfig("{}".getBytes())
            .build();

    public static final CrossChainLane tpbtaLane1 = new CrossChainLane(
            antchainSubject.getDomainName()
    );

    public static final CrossChainLane tpbtaLaneChannelLevel = new CrossChainLane(
            antchainSubject.getDomainName(),
            catchainSubject.getDomainName()
    );

    public static final CrossChainLane tpbtaLaneLaneLevel1 = new CrossChainLane(
            antchainSubject.getDomainName(),
            catchainSubject.getDomainName(),
            antchainIdentity,
            catchainIdentity
    );

    public static final CrossChainLane tpbtaLaneLaneLevel2 = new CrossChainLane(
            antchainSubject.getDomainName(),
            catchainSubject.getDomainName(),
            antchainIdentity2,
            catchainIdentity2
    );

    public static final CrossChainLane tpbtaLaneChannelLevel_cat2mychain = new CrossChainLane(
            catchainSubject.getDomainName(),
            antchainSubject.getDomainName()
    );

    public static BlockchainTrustAnchorV1 antchainBta;

    public static BtaDO antchainBtaDO;

    public static TpBtaDO tpBtaDO_cat2mychain;

    public static ValidatedConsensusStateDO validatedConsensusStateDO_cat;

    static {
        privateKey = getLocalPrivateKey("cc_certs/private_key.pem");

        ptcOid = new X509PubkeyInfoObjectIdentity(RAW_NODE_PTC_PUBLIC_KEY);

        OptionalEndorsePolicy policy = new OptionalEndorsePolicy();
        policy.setThreshold(new OptionalEndorsePolicy.Threshold(OptionalEndorsePolicy.OperatorEnum.GREATER_THAN, -1));
        NodeEndorseInfo nodeEndorseInfo = new NodeEndorseInfo();
        nodeEndorseInfo.setNodeId("node1");
        nodeEndorseInfo.setRequired(true);
        NodePublicKeyEntry nodePubkeyEntry = new NodePublicKeyEntry("default", ((X509PubkeyInfoObjectIdentity) ptcOid).getPublicKey());
        nodeEndorseInfo.setPublicKey(nodePubkeyEntry);

        ptcVerifyAnchor = new PTCVerifyAnchor();
        ptcVerifyAnchor.setAnchor(new byte[]{});
        ptcVerifyAnchor.setVersion(BigInteger.ONE);

        ptcTrustRoot = new PTCTrustRoot();
        ptcTrustRoot.setSig(new byte[]{});
        ptcTrustRoot.setNetworkInfo(new byte[]{});
        ptcTrustRoot.setPtcCrossChainCert(NODE_PTC_CERT);
        ptcTrustRoot.setIssuerBcdnsDomainSpace(new CrossChainDomain(CrossChainDomain.ROOT_DOMAIN_SPACE));
        ptcTrustRoot.setSigAlgo(SignAlgoEnum.KECCAK256_WITH_SECP256K1);
        ptcTrustRoot.setVerifyAnchorMap(MapUtil.builder(BigInteger.ONE, ptcVerifyAnchor).build());

        tpbtaWithChannelLevel = new ThirdPartyBlockchainTrustAnchorV1(
                1,
                BigInteger.ONE,
                (PTCCredentialSubject) NODE_PTC_CERT.getCredentialSubjectInstance(),
                tpbtaLaneChannelLevel,
                1,
                HashAlgoEnum.KECCAK_256,
                new CommitteeEndorseRoot(
                        COMMITTEE_ID,
                        policy,
                        ListUtil.toList(nodeEndorseInfo)
                ).encode(),
                CommitteeEndorseProof.builder()
                        .committeeId(COMMITTEE_ID)
                        .sigs(ListUtil.toList(new CommitteeNodeProof(
                                "node1",
                                SignAlgoEnum.KECCAK256_WITH_SECP256K1,
                                "".getBytes()
                        ))).build().encode()
        );
        tpbtaWithChannelLevel.setEndorseProof(
                CommitteeEndorseProof.builder()
                        .committeeId(COMMITTEE_ID)
                        .sigs(ListUtil.toList(new CommitteeNodeProof(
                                "node1",
                                SignAlgoEnum.KECCAK256_WITH_SECP256K1,
                                SignAlgoEnum.KECCAK256_WITH_SECP256K1.getSigner().sign(privateKey, tpbtaWithChannelLevel.getEncodedToSign())
                        ))).build().encode()
        );

        tpbtaWithChannelLevel_cat = new ThirdPartyBlockchainTrustAnchorV1(
                1,
                BigInteger.ONE,
                (PTCCredentialSubject) NODE_PTC_CERT.getCredentialSubjectInstance(),
                tpbtaLaneChannelLevel_cat2mychain,
                1,
                HashAlgoEnum.KECCAK_256,
                new CommitteeEndorseRoot(
                        COMMITTEE_ID,
                        policy,
                        ListUtil.toList(nodeEndorseInfo)
                ).encode(),
                CommitteeEndorseProof.builder()
                        .committeeId(COMMITTEE_ID)
                        .sigs(ListUtil.toList(new CommitteeNodeProof(
                                "node1",
                                SignAlgoEnum.KECCAK256_WITH_SECP256K1,
                                "".getBytes()
                        ))).build().encode()
        );
        tpbtaWithChannelLevel_cat.setEndorseProof(
                CommitteeEndorseProof.builder()
                        .committeeId(COMMITTEE_ID)
                        .sigs(ListUtil.toList(new CommitteeNodeProof(
                                "node1",
                                SignAlgoEnum.KECCAK256_WITH_SECP256K1,
                                SignAlgoEnum.KECCAK256_WITH_SECP256K1.getSigner().sign(privateKey, tpbtaWithChannelLevel_cat.getEncodedToSign())
                        ))).build().encode()
        );

        tpbtaWithLaneLevel1 = new ThirdPartyBlockchainTrustAnchorV1(
                1,
                BigInteger.ONE,
                (PTCCredentialSubject) NODE_PTC_CERT.getCredentialSubjectInstance(),
                tpbtaLaneLaneLevel1,
                1,
                HashAlgoEnum.KECCAK_256,
                new CommitteeEndorseRoot(
                        COMMITTEE_ID,
                        policy,
                        ListUtil.toList(nodeEndorseInfo)
                ).encode(),
                null
        );
        tpbtaWithLaneLevel1.setEndorseProof(
                CommitteeEndorseProof.builder()
                        .committeeId(COMMITTEE_ID)
                        .sigs(ListUtil.toList(new CommitteeNodeProof(
                                "node1",
                                SignAlgoEnum.KECCAK256_WITH_SECP256K1,
                                SignAlgoEnum.KECCAK256_WITH_SECP256K1.getSigner().sign(privateKey, tpbtaWithLaneLevel1.getEncodedToSign())
                        ))).build().encode()
        );

        tpbtaWithLaneLevel2 = new ThirdPartyBlockchainTrustAnchorV1(
                1,
                BigInteger.ONE,
                (PTCCredentialSubject) NODE_PTC_CERT.getCredentialSubjectInstance(),
                tpbtaLaneLaneLevel2,
                1,
                HashAlgoEnum.KECCAK_256,
                new CommitteeEndorseRoot(
                        COMMITTEE_ID,
                        policy,
                        ListUtil.toList(nodeEndorseInfo)
                ).encode(),
                null
        );
        tpbtaWithLaneLevel2.setEndorseProof(
                CommitteeEndorseProof.builder()
                        .committeeId(COMMITTEE_ID)
                        .sigs(ListUtil.toList(new CommitteeNodeProof(
                                "node1",
                                SignAlgoEnum.KECCAK256_WITH_SECP256K1,
                                SignAlgoEnum.KECCAK256_WITH_SECP256K1.getSigner().sign(privateKey, tpbtaWithLaneLevel2.getEncodedToSign())
                        ))).build().encode()
        );

        antchainBta = new BlockchainTrustAnchorV1();
        antchainBta.setBcOwnerPublicKey(RAW_NODE_PTC_PUBLIC_KEY);
        antchainBta.setDomain(antchainSubject.getDomainName());
        antchainBta.setSubjectIdentity("test".getBytes());
        antchainBta.setBcOwnerSigAlgo(SignAlgoEnum.KECCAK256_WITH_SECP256K1);
        antchainBta.setPtcOid(NODE_PTC_CERT.getCredentialSubjectInstance().getApplicant());
        antchainBta.setSubjectProduct("mychain");
        antchainBta.setExtension(
                new VerifyBtaExtension(
                        CommitteeEndorseRoot.decode(tpbtaWithChannelLevel.getEndorseRoot()),
                        tpbtaLaneChannelLevel
                ).encode()
        );
        antchainBta.setSubjectVersion(0);
        antchainBta.setAmId(RandomUtil.randomBytes(32));

        antchainBta.sign(NODE_PTC_PRIVATE_KEY);

        antchainBtaDO = BtaDO.builder()
                .blockchainProduct(testchain1Meta.getProduct())
                .blockchainId(testchain1Meta.getBlockchainId())
                .bta(antchainBta)
                .build();

        anchorState = new ConsensusState(
                tpbtaLaneChannelLevel.getSenderDomain(),
                BigInteger.valueOf(100L),
                RandomUtil.randomBytes(32),
                RandomUtil.randomBytes(32),
                System.currentTimeMillis(),
                "{}".getBytes(),
                "{}".getBytes(),
                "{}".getBytes()
        );

        anchorState_cat = new ConsensusState(
                tpbtaLaneChannelLevel.getReceiverDomain(),
                BigInteger.valueOf(100L),
                RandomUtil.randomBytes(32),
                RandomUtil.randomBytes(32),
                System.currentTimeMillis(),
                "{}".getBytes(),
                "{}".getBytes(),
                "{}".getBytes()
        );

        currState = new ConsensusState(
                tpbtaLaneChannelLevel.getSenderDomain(),
                BigInteger.valueOf(101L),
                RandomUtil.randomBytes(32),
                anchorState.getParentHash(),
                System.currentTimeMillis(),
                "{}".getBytes(),
                "{}".getBytes(),
                "{}".getBytes()
        );

        currState_cat = new ConsensusState(
                tpbtaLaneChannelLevel.getReceiverDomain(),
                BigInteger.valueOf(101L),
                RandomUtil.randomBytes(32),
                anchorState_cat.getParentHash(),
                System.currentTimeMillis(),
                "{}".getBytes(),
                "{}".getBytes(),
                "{}".getBytes()
        );

        sdpMessage = SDPMessageFactory.createSDPMessage(
                1,
                new byte[32],
                tpbtaLaneLaneLevel1.getReceiverDomain().getDomain(),
                tpbtaLaneLaneLevel1.getReceiverId().getRawID(),
                -1,
                "awesome antchain-bridge".getBytes()
        );

        sdpMessageV3 = SDPMessageFactory.createSDPMessage(
                3,
                new byte[32],
                tpbtaLaneLaneLevel1.getReceiverDomain().getDomain(),
                tpbtaLaneLaneLevel1.getReceiverId().getRawID(),
                -1,
                "awesome antchain-bridge".getBytes()
        );

        authMessage = AuthMessageFactory.createAuthMessage(
                1,
                tpbtaLaneLaneLevel1.getSenderId().getRawID(),
                0,
                sdpMessage.encode()
        );

        authMessage_sdpV3 = AuthMessageFactory.createAuthMessage(
                1,
                tpbtaLaneLaneLevel1.getSenderId().getRawID(),
                0,
                sdpMessageV3.encode()
        );

        ucp = new UniformCrosschainPacket(
                tpbtaLaneLaneLevel1.getSenderDomain(),
                CrossChainMessage.createCrossChainMessage(
                        CrossChainMessage.CrossChainMessageType.AUTH_MSG,
                        BigInteger.valueOf(101L),
                        DateUtil.current(),
                        currState.getHash(),
                        authMessage.encode(),
                        "event".getBytes(),
                        "merkle proof".getBytes(),
                        RandomUtil.randomBytes(32)
                ),
                NODE_PTC_CERT.getCredentialSubjectInstance().getApplicant()
        );

        ucp_sdpV3 = new UniformCrosschainPacket(
                tpbtaLaneLaneLevel1.getSenderDomain(),
                CrossChainMessage.createCrossChainMessage(
                        CrossChainMessage.CrossChainMessageType.AUTH_MSG,
                        BigInteger.valueOf(101L),
                        DateUtil.current(),
                        currState.getHash(),
                        authMessage_sdpV3.encode(),
                        "event".getBytes(),
                        "merkle proof".getBytes(),
                        RandomUtil.randomBytes(32)
                ),
                NODE_PTC_CERT.getCredentialSubjectInstance().getApplicant()
        );

        thirdPartyProof = ThirdPartyProof.create(
                tpbtaWithChannelLevel.getTpbtaVersion(),
                ucp.getSrcMessage().getMessage(),
                ucp.getCrossChainLane()
        );
        CommitteeEndorseProof endorseProof = CommitteeEndorseProof.builder()
                .committeeId(COMMITTEE_ID)
                .sigs(ListUtil.toList(
                        CommitteeNodeProof.builder()
                                .nodeId("node1")
                                .signAlgo(SignAlgoEnum.KECCAK256_WITH_SECP256K1)
                                .signature(SignAlgoEnum.KECCAK256_WITH_SECP256K1.getSigner().sign(
                                        privateKey,
                                        thirdPartyProof.getEncodedToSign()
                                )).build()
                )).build();
        thirdPartyProof.setRawProof(endorseProof.encode());

        ucpContext = new UniformCrosschainPacketContext();
        ucpContext.setUcp(ucp);
        ucpContext.setProduct(testchain1Meta.getProduct());
        ucpContext.setBlockchainId(testchain1Meta.getBlockchainId());
        ucpContext.setRelayerId(RelayerNodeInfo.calculateNodeId(relayerCert));
        ucpContext.setTpbtaVersion(tpbtaWithChannelLevel.getTpbtaVersion());
        ucpContext.setTpbtaLaneKey(tpbtaLaneChannelLevel.getLaneKey());
        ucpContext.setFromNetwork(false);
        ucpContext.setProcessState(UniformCrosschainPacketStateEnum.PENDING);

        ucpContext_sdpV3 = new UniformCrosschainPacketContext();
        ucpContext_sdpV3.setUcp(ucp_sdpV3);
        ucpContext_sdpV3.setProduct(testchain1Meta.getProduct());
        ucpContext_sdpV3.setBlockchainId(testchain1Meta.getBlockchainId());
        ucpContext_sdpV3.setRelayerId(RelayerNodeInfo.calculateNodeId(relayerCert));
        ucpContext_sdpV3.setTpbtaVersion(tpbtaWithChannelLevel.getTpbtaVersion());
        ucpContext_sdpV3.setTpbtaLaneKey(tpbtaLaneChannelLevel.getLaneKey());
        ucpContext_sdpV3.setFromNetwork(false);
        ucpContext_sdpV3.setProcessState(UniformCrosschainPacketStateEnum.PENDING);

        authMsgWrapper = AuthMsgWrapper.buildFrom(
                ucpContext.getProduct(),
                ucpContext.getBlockchainId(),
                ucpContext.getSrcDomain(),
                ucpContext.getUcpId(),
                authMessage
        );

        authMsgWrapper_sdpV3 = AuthMsgWrapper.buildFrom(
                ucpContext_sdpV3.getProduct(),
                ucpContext_sdpV3.getBlockchainId(),
                ucpContext_sdpV3.getSrcDomain(),
                ucpContext_sdpV3.getUcpId(),
                authMessage_sdpV3
        );

        sdpMsgWrapper = SDPMsgWrapper.buildFrom(authMsgWrapper);
        sdpMsgWrapper.setReceiverBlockchainProduct(testchain2Meta.getProduct());
        sdpMsgWrapper.setReceiverBlockchainId(testchain2Meta.getBlockchainId());
        sdpMsgWrapper.setProcessState(SDPMsgProcessStateEnum.PENDING);
        sdpMsgWrapper.setReceiverAMClientContract(testchain2Meta.getProperties().getAmClientContractAddress());

        sdpMsgWrapperV3 = SDPMsgWrapper.buildFrom(authMsgWrapper_sdpV3);
        sdpMsgWrapperV3.setReceiverBlockchainProduct(testchain2Meta.getProduct());
        sdpMsgWrapperV3.setReceiverBlockchainId(testchain2Meta.getBlockchainId());
        sdpMsgWrapperV3.setProcessState(SDPMsgProcessStateEnum.PENDING);
        sdpMsgWrapperV3.setReceiverAMClientContract(testchain2Meta.getProperties().getAmClientContractAddress());

        anchorVcs = BeanUtil.copyProperties(anchorState, ValidatedConsensusStateV1.class);
        anchorVcs.setPtcOid(NODE_PTC_CERT.getCredentialSubjectInstance().getApplicant());
        anchorVcs.setTpbtaVersion(tpbtaWithChannelLevel.getTpbtaVersion());
        anchorVcs.setPtcType(PTCTypeEnum.COMMITTEE);

        CommitteeNodeProof nodeProof = CommitteeNodeProof.builder()
                .nodeId("node1")
                .signAlgo(SignAlgoEnum.KECCAK256_WITH_SECP256K1)
                .signature(SignAlgoEnum.KECCAK256_WITH_SECP256K1.getSigner().sign(NODE_PTC_PRIVATE_KEY, anchorVcs.getEncodedToSign()))
                .build();
        CommitteeEndorseProof proof = new CommitteeEndorseProof();
        proof.setCommitteeId(COMMITTEE_ID);
        proof.setSigs(ListUtil.toList(nodeProof));
        anchorVcs.setPtcProof(proof.encode());

        currVcs = BeanUtil.copyProperties(currState, ValidatedConsensusStateV1.class);
        currVcs.setPtcOid(NODE_PTC_CERT.getCredentialSubjectInstance().getApplicant());
        currVcs.setTpbtaVersion(tpbtaWithChannelLevel.getTpbtaVersion());
        currVcs.setPtcType(PTCTypeEnum.COMMITTEE);

        nodeProof = CommitteeNodeProof.builder()
                .nodeId("node1")
                .signAlgo(SignAlgoEnum.KECCAK256_WITH_SECP256K1)
                .signature(SignAlgoEnum.KECCAK256_WITH_SECP256K1.getSigner().sign(NODE_PTC_PRIVATE_KEY, currVcs.getEncodedToSign()))
                .build();
        proof = new CommitteeEndorseProof();
        proof.setCommitteeId(COMMITTEE_ID);
        proof.setSigs(ListUtil.toList(nodeProof));
        currVcs.setPtcProof(proof.encode());

        currVcs_cat = BeanUtil.copyProperties(currState_cat, ValidatedConsensusStateV1.class);
        currVcs_cat.setPtcOid(NODE_PTC_CERT.getCredentialSubjectInstance().getApplicant());
        currVcs_cat.setTpbtaVersion(tpbtaWithChannelLevel_cat.getTpbtaVersion());
        currVcs_cat.setPtcType(PTCTypeEnum.COMMITTEE);

        nodeProof = CommitteeNodeProof.builder()
                .nodeId("node1")
                .signAlgo(SignAlgoEnum.KECCAK256_WITH_SECP256K1)
                .signature(SignAlgoEnum.KECCAK256_WITH_SECP256K1.getSigner().sign(NODE_PTC_PRIVATE_KEY, currVcs.getEncodedToSign()))
                .build();
        proof = new CommitteeEndorseProof();
        proof.setCommitteeId(COMMITTEE_ID);
        proof.setSigs(ListUtil.toList(nodeProof));
        currVcs_cat.setPtcProof(proof.encode());


        tpBtaDO_cat2mychain = TpBtaDO.builder()
                .blockchainProduct(catChainDotComProduct)
                .blockchainId(catChainDotComBlockchainId)
                .ptcServiceId(PTC_SERVICE_ID1)
                .tpbta(tpbtaWithChannelLevel_cat)
                .build();

        validatedConsensusStateDO_cat = ValidatedConsensusStateDO.builder()
                .tpbtaLane(tpBtaDO_cat2mychain.getCrossChainLane())
                .ptcServiceId(tpBtaDO_cat2mychain.getPtcServiceId())
                .blockchainProduct(tpBtaDO_cat2mychain.getBlockchainProduct())
                .blockchainId(tpBtaDO_cat2mychain.getBlockchainId())
                .validatedConsensusState(currVcs_cat)
                .build();

        thirdPartyProof_catVcs = ThirdPartyProof.create(
                tpbtaWithChannelLevel_cat.getTpbtaVersion(),
                currVcs_cat.encode(),
                tpbtaLaneChannelLevel_cat2mychain
        );
        CommitteeEndorseProof endorseProof_cartVbs = CommitteeEndorseProof.builder()
                .committeeId(COMMITTEE_ID)
                .sigs(ListUtil.toList(
                        CommitteeNodeProof.builder()
                                .nodeId("node1")
                                .signAlgo(SignAlgoEnum.KECCAK256_WITH_SECP256K1)
                                .signature(SignAlgoEnum.KECCAK256_WITH_SECP256K1.getSigner().sign(
                                        privateKey,
                                        thirdPartyProof_catVcs.getEncodedToSign()
                                )).build()
                )).build();
        thirdPartyProof_catVcs.setRawProof(endorseProof_cartVbs.encode());
    }

    @MockBean
    public Cache<String, RelayerNodeInfo> relayerNodeInfoCache;

    @MockBean
    public Cache<String, BlockchainMeta> blockchainMetaCache;

    @MockBean
    public Cache<String, DomainCertWrapper> domainCertWrapperCache;

    @MockBean
    public Cache<String, RelayerNetwork.DomainRouterItem> relayerNetworkItemCache;

    public AtomicLong currHeight = new AtomicLong(100L);

    public void initBaseBBCMock(
            CrossChainServiceGrpc.CrossChainServiceBlockingStub crossChainServiceBlockingStub,
            MockedStatic<CrossChainServiceGrpc> mockedStaticCrossChainServiceGrpc
    ) {
        mockedStaticCrossChainServiceGrpc.when(
                () -> CrossChainServiceGrpc.newBlockingStub(Mockito.any())
        ).thenReturn(crossChainServiceBlockingStub);

        Mockito.when(crossChainServiceBlockingStub.bbcCall(Mockito.argThat(
                argument -> {
                    if (ObjectUtil.isNull(argument)) {
                        return false;
                    }
                    return argument.hasGetContextReq();
                }
        ))).thenReturn(Response.newBuilder().setCode(0).setBbcResp(
                        CallBBCResponse.newBuilder().setGetContextResp(
                                GetContextResponse.newBuilder().setRawContext(
                                        ByteString.copyFrom(blockchainProperties1.getBbcContext().encodeToBytes())
                                ).build()
                        ).build()
                ).build()
        );
        Mockito.when(crossChainServiceBlockingStub.bbcCall(Mockito.argThat(
                argument -> {
                    if (ObjectUtil.isNull(argument)) {
                        return false;
                    }
                    return argument.hasQueryLatestHeightReq();
                }
        ))).thenReturn(Response.newBuilder().setCode(0).setBbcResp(
                        CallBBCResponse.newBuilder().setQueryLatestHeightResponse(
                                QueryLatestHeightResponse.newBuilder()
                                        .setHeight(currHeight.incrementAndGet()).build()
                        ).build()
                ).build(),
                Response.newBuilder().setCode(0).setBbcResp(
                        CallBBCResponse.newBuilder().setQueryLatestHeightResponse(
                                QueryLatestHeightResponse.newBuilder()
                                        .setHeight(currHeight.incrementAndGet()).build()
                        ).build()
                ).build(),
                Response.newBuilder().setCode(0).setBbcResp(
                        CallBBCResponse.newBuilder().setQueryLatestHeightResponse(
                                QueryLatestHeightResponse.newBuilder()
                                        .setHeight(currHeight.incrementAndGet()).build()
                        ).build()
                ).build()
        );
        Mockito.when(crossChainServiceBlockingStub.bbcCall(
                Mockito.argThat(
                        argument -> {
                            if (ObjectUtil.isNull(argument)) {
                                return false;
                            }
                            return argument.hasStartUpReq() || argument.hasSetupAuthMessageContractReq()
                                    || argument.hasSetupSDPMessageContractReq() || argument.hasSetProtocolReq()
                                    || argument.hasSetAmContractReq() || argument.hasSetLocalDomainReq()
                                    || argument.hasSetupPTCContractReq() || argument.hasSetPtcContractReq();
                        }
                )
        )).thenReturn(Response.newBuilder().setCode(0).build());
        Mockito.when(crossChainServiceBlockingStub.heartbeat(Mockito.any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setHeartbeatResp(
                                HeartbeatResponse.newBuilder()
                                        .addAllProducts(ListUtil.toList(antChainDotComProduct))
                                        .addAllDomains(ListUtil.toList(antChainDotComDomain))
                                        .build()
                        ).build()
        );

        Mockito.when(crossChainServiceBlockingStub.ifProductSupport(Mockito.any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setIfProductSupportResp(
                                IfProductSupportResponse.newBuilder()
                                        .putResults(antChainDotComProduct, true)
                                        .putResults(catChainDotComProduct, true)
                                        .build()
                        ).build()
        );
    }

    @BeforeClass
    public static void beforeTest() throws Exception {

        sdpMessageV1.setSequence(-1);
        sdpMessageV1.setSdpPayload("test"::getBytes);
        sdpMessageV1.setTargetDomain(catchainSubject.getDomainName());
        sdpMessageV1.setTargetIdentity(catchainIdentity);

        authMessageV2.setTrustLevel(AuthMessageTrustLevelEnum.NEGATIVE_TRUST);
        authMessageV2.setIdentity(antchainIdentity);
        authMessageV2.setUpperProtocol(UpperProtocolTypeBeyondAMEnum.SDP.getCode());
        authMessageV2.setPayload(sdpMessageV1.encode());

        // if the embedded redis can't start correctly,
        // try to use local redis server binary to start it.
        redisServer = new MyRedisServer(
                RedisExecProvider.defaultProvider()
                        .override(OS.MAC_OS_X, Architecture.x86_64, "src/test/resources/bins/redis-server")
                        .override(OS.MAC_OS_X, Architecture.x86, "src/test/resources/bins/redis-server"),
                6379
        );
        redisServer.start();
    }

    @AfterClass
    public static void after() throws Exception {
        redisServer.stop();
        Path dumpFile = Paths.get("src/test/resources/bins/dump.rdb");
        if (Files.exists(dumpFile)) {
            Files.delete(dumpFile);
            System.out.println("try to delete redis dump file");
        }
    }

    @SneakyThrows
    public static PrivateKey getLocalPrivateKey(String path) {
        try {
            return PemUtil.readPemPrivateKey(new ByteArrayInputStream(FileUtil.readBytes(path)));
        } catch (Exception e) {
            byte[] rawPemOb = PemUtil.readPem(new ByteArrayInputStream(FileUtil.readBytes(path)));
            KeyFactory keyFactory = KeyFactory.getInstance(
                    PrivateKeyInfo.getInstance(rawPemOb).getPrivateKeyAlgorithm().getAlgorithm().getId()
            );
            return keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(
                            rawPemOb
                    )
            );
        }
    }
}
