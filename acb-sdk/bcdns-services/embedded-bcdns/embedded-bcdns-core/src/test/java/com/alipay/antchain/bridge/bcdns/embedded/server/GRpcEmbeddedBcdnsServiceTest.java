package com.alipay.antchain.bridge.bcdns.embedded.server;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;

import cn.ac.caict.bid.model.BIDDocumentOperation;
import cn.ac.caict.bid.model.BIDpublicKeyOperation;
import cn.bif.common.JsonUtils;
import cn.bif.module.encryption.model.KeyType;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.ECKeyUtil;
import cn.hutool.crypto.PemUtil;
import com.alipay.antchain.bridge.bcdns.embedded.grpc.*;
import com.alipay.antchain.bridge.bcdns.embedded.types.enums.ApplicationStateEnum;
import com.alipay.antchain.bridge.bcdns.embedded.types.models.CertApplicationResult;
import com.alipay.antchain.bridge.bcdns.types.base.DomainRouter;
import com.alipay.antchain.bridge.bcdns.types.base.Relayer;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateTypeEnum;
import com.alipay.antchain.bridge.commons.bcdns.PTCCredentialSubject;
import com.alipay.antchain.bridge.commons.bcdns.utils.BIDHelper;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.bcdns.utils.ObjectIdentityUtil;
import com.alipay.antchain.bridge.commons.core.base.*;
import com.alipay.antchain.bridge.commons.core.ptc.*;
import com.alipay.antchain.bridge.commons.utils.crypto.HashAlgoEnum;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import com.alipay.antchain.bridge.ptc.committee.types.basic.CommitteeEndorseProof;
import com.alipay.antchain.bridge.ptc.committee.types.basic.CommitteeNodeProof;
import com.alipay.antchain.bridge.ptc.committee.types.basic.NodePublicKeyEntry;
import com.alipay.antchain.bridge.ptc.committee.types.network.CommitteeNetworkInfo;
import com.alipay.antchain.bridge.ptc.committee.types.tpbta.CommitteeEndorseRoot;
import com.alipay.antchain.bridge.ptc.committee.types.tpbta.NodeEndorseInfo;
import com.alipay.antchain.bridge.ptc.committee.types.tpbta.OptionalEndorsePolicy;
import com.alipay.antchain.bridge.ptc.committee.types.trustroot.CommitteeVerifyAnchor;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPublicKey;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GRpcEmbeddedBcdnsServiceTest {

    public static final String BCDNS_CERT = "-----BEGIN BCDNS TRUST ROOT CERTIFICATE-----\n" +
            "AADWAQAAAAABAAAAMQEABAAAAHRlc3QCAAEAAAAAAwBrAAAAAABlAAAAAAABAAAA\n" +
            "AAEAWAAAADBWMBAGByqGSM49AgEGBSuBBAAKA0IABFC80gzzOJoUZBqQolYqx2U1\n" +
            "n87mfz3zuvv0X1YBqWcbOBFEGYcOUp2FiMCvfSsQzzcbWBuzhIlgwO/hCmVFgSME\n" +
            "AAgAAACU2f9mAAAAAAUACAAAABQN4WgAAAAABgCGAAAAAACAAAAAAAADAAAAYmlm\n" +
            "AQBrAAAAAABlAAAAAAABAAAAAAEAWAAAADBWMBAGByqGSM49AgEGBSuBBAAKA0IA\n" +
            "BFC80gzzOJoUZBqQolYqx2U1n87mfz3zuvv0X1YBqWcbOBFEGYcOUp2FiMCvfSsQ\n" +
            "zzcbWBuzhIlgwO/hCmVFgSMCAAAAAAAHAJ8AAAAAAJkAAAAAAAoAAABLRUNDQUst\n" +
            "MjU2AQAgAAAA1/SncCIPlAQGRJ4Zp2WPBmrk5poje12brhJatwWR5BwCABYAAABL\n" +
            "ZWNjYWsyNTZXaXRoU2VjcDI1NmsxAwBBAAAAR23ngOzN3b8gaJY9ikvNtdqzwF6K\n" +
            "zAkr89qnHDJQei9iXVds+7Padq41StiQShIiB9yWtx8/3Qu878R9zmJbZAA=\n" +
            "-----END BCDNS TRUST ROOT CERTIFICATE-----\n";

    public static final String PTC_CERT = "-----BEGIN PROOF TRANSFORMATION COMPONENT CERTIFICATE-----\n" +
            "AAD4AQAAAAABAAAAMQEADAAAAGFudGNoYWluLXB0YwIAAQAAAAIDAGsAAAAAAGUA\n" +
            "AAAAAAEAAAAAAQBYAAAAMFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEULzSDPM4mhRk\n" +
            "GpCiVirHZTWfzuZ/PfO6+/RfVgGpZxs4EUQZhw5SnYWIwK99KxDPNxtYG7OEiWDA\n" +
            "7+EKZUWBIwQACAAAAJTZ/2YAAAAABQAIAAAAFA3haAAAAAAGAKAAAAAAAJoAAAAA\n" +
            "AAMAAAAxLjABAA0AAABjb21taXR0ZWUtcHRjAgABAAAAAQMAawAAAAAAZQAAAAAA\n" +
            "AQAAAAABAFgAAAAwVjAQBgcqhkjOPQIBBgUrgQQACgNCAARQvNIM8ziaFGQakKJW\n" +
            "KsdlNZ/O5n8987r79F9WAalnGzgRRBmHDlKdhYjAr30rEM83G1gbs4SJYMDv4Qpl\n" +
            "RYEjBAAAAAAABwCfAAAAAACZAAAAAAAKAAAAS0VDQ0FLLTI1NgEAIAAAAM87/iLc\n" +
            "e6uD6qD6prxj4z75IoGzydOhd68+3Y8dODHxAgAWAAAAS2VjY2FrMjU2V2l0aFNl\n" +
            "Y3AyNTZrMQMAQQAAAMK+DN7gXmDRv8nfXwWZe3XCZQQu5mO86LNZxXcp7BgMPfJj\n" +
            "y1wKW5yD51nhMEW2K1AfwEG6n8RWk5Z2jFDE8GMA\n" +
            "-----END PROOF TRANSFORMATION COMPONENT CERTIFICATE-----\n";

    public static final AbstractCrossChainCertificate NODE_PTC_CERT = CrossChainCertificateUtil.readCrossChainCertificateFromPem(PTC_CERT.getBytes());

    public static final String NODE_SERVER_TLS_CERT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDSjCCAjICCQC7UpnZnD+2AjANBgkqhkiG9w0BAQsFADBmMQswCQYDVQQGEwJD\n" +
            "TjEOMAwGA1UECAwFbXlrZXkxDjAMBgNVBAcMBW15a2V5MQ4wDAYDVQQKDAVteWtl\n" +
            "eTEOMAwGA1UECwwFbXlrZXkxFzAVBgNVBAMMDkNPTU1JVFRFRS1OT0RFMCAXDTI0\n" +
            "MDgyNzExMzgxMloYDzIxMjQwODAzMTEzODEyWjBmMQswCQYDVQQGEwJDTjEOMAwG\n" +
            "A1UECAwFbXlrZXkxDjAMBgNVBAcMBW15a2V5MQ4wDAYDVQQKDAVteWtleTEOMAwG\n" +
            "A1UECwwFbXlrZXkxFzAVBgNVBAMMDkNPTU1JVFRFRS1OT0RFMIIBIjANBgkqhkiG\n" +
            "9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyBDWvnDIBfCg0Bpb1Zzyu1/TI9W/eVO9S/n9\n" +
            "TyfVoWHOd4vLuHs3EmP6QtPWBtS/wSx579Fm+qhg+Px5TdrPIPceK3CgFlWqesz1\n" +
            "+qrPCfuHJ0yRpQb1yQ77l23JPTb4ze7zmeG2mCBImpCl2TFnUgNDrK8VsqALZ5fE\n" +
            "O3eFkwn8zpKaqEmRMOY8PJJh76/PNYYu5XobS6Sie08gb7Vr5BKCC1aAvM6yeapC\n" +
            "ZRZ1QoAgbgOz5wQgOVmni5zn9UWeSYe2xeI6geqKLVc/1Jr5zA3nLItWF+KveUix\n" +
            "V3oKO43nfO3e+fPli9jJkFM0HZD5FhMxQIpPjuF7aecUZG0GwwIDAQABMA0GCSqG\n" +
            "SIb3DQEBCwUAA4IBAQCXF83k4Tdlfcc456agxHUDW4kCgsQVZ0mcfJLVL0+UBMLj\n" +
            "VFz50Su2Vtq8Pe/N4T2BHB1zZgQzs+T7oRtEb65H2uTxO8AFtang9OgpFJYxEkgH\n" +
            "9oRgAeD32xIwob5cj2znf3Ct03FKKkrpaXvGuPlSYV/qfy1Gittb3UFi2y7+LDk2\n" +
            "q6Qd7jpMYK6HdtVwj0FgyxRIzzGQT6/d9soyI8a8bW/VKsJ/+DEANIZFSw9Y80Ye\n" +
            "EVQJHi2LfmEXaxvD9m1vO50Dmspc5dy4J6QKY6HgzXcdxZbjetwaJ9ilG7LgLbh8\n" +
            "XOL/r9jTK9//UMFPq27xZ5PMrsGR+PjV/9NasLac\n" +
            "-----END CERTIFICATE-----\n";

    public static final byte[] RAW_NODE_PTC_PUBLIC_KEY = PemUtil.readPem(new ByteArrayInputStream(
                    ("-----BEGIN PUBLIC KEY-----\n" +
                            "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEULzSDPM4mhRkGpCiVirHZTWfzuZ/PfO6\n" +
                            "+/RfVgGpZxs4EUQZhw5SnYWIwK99KxDPNxtYG7OEiWDA7+EKZUWBIw==\n" +
                            "-----END PUBLIC KEY-----\n").getBytes()
            )
    );

    public static final PrivateKey NODE_PTC_PRIVATE_KEY = SignAlgoEnum.KECCAK256_WITH_SECP256K1.getSigner().readPemPrivateKey(
            ("-----BEGIN EC PRIVATE KEY-----\n" +
                    "MHQCAQEEINtcJsfWygsBn4u8sscy/04yPSpafFwCW4yVg1Vrb8looAcGBSuBBAAK\n" +
                    "oUQDQgAEULzSDPM4mhRkGpCiVirHZTWfzuZ/PfO6+/RfVgGpZxs4EUQZhw5SnYWI\n" +
                    "wK99KxDPNxtYG7OEiWDA7+EKZUWBIw==\n" +
                    "-----END EC PRIVATE KEY-----\n").getBytes()
    );

    private static final ThirdPartyBlockchainTrustAnchorV1 tpbta;

    private static final PTCTrustRoot ptcTrustRoot;

    private static final CrossChainLane crossChainLane;

    private static final ObjectIdentity oid;

    private static final String COMMITTEE_ID = "committee";

    static {
        oid = new X509PubkeyInfoObjectIdentity(RAW_NODE_PTC_PUBLIC_KEY);

        OptionalEndorsePolicy policy = new OptionalEndorsePolicy();
        policy.setThreshold(new OptionalEndorsePolicy.Threshold(OptionalEndorsePolicy.OperatorEnum.GREATER_OR_EQUALS, 0));
        NodeEndorseInfo nodeEndorseInfo = new NodeEndorseInfo();
        nodeEndorseInfo.setNodeId("node1");
        nodeEndorseInfo.setRequired(true);
        NodePublicKeyEntry nodePubkeyEntry = new NodePublicKeyEntry("default", ((X509PubkeyInfoObjectIdentity) oid).getPublicKey());
        nodeEndorseInfo.setPublicKey(nodePubkeyEntry);
        crossChainLane = new CrossChainLane(new CrossChainDomain("test"), new CrossChainDomain("test"), CrossChainIdentity.fromHexStr("0000000000000000000000000000000000000000000000000000000000000001"), CrossChainIdentity.fromHexStr("0000000000000000000000000000000000000000000000000000000000000001"));
        tpbta = new ThirdPartyBlockchainTrustAnchorV1(
                1,
                BigInteger.ONE,
                (PTCCredentialSubject) NODE_PTC_CERT.getCredentialSubjectInstance(),
                crossChainLane,
                1,
                HashAlgoEnum.KECCAK_256,
                new CommitteeEndorseRoot(
                        COMMITTEE_ID,
                        policy,
                        ListUtil.toList(nodeEndorseInfo)
                ).encode(),
                null
        );
        tpbta.setEndorseProof(
                CommitteeEndorseProof.builder()
                        .committeeId(COMMITTEE_ID)
                        .sigs(ListUtil.toList(new CommitteeNodeProof(
                                "node1",
                                SignAlgoEnum.KECCAK256_WITH_SECP256K1,
                                SignAlgoEnum.KECCAK256_WITH_SECP256K1.getSigner()
                                        .sign(NODE_PTC_PRIVATE_KEY, tpbta.getEncodedToSign())
                        ))).build().encode()
        );

        CommitteeVerifyAnchor verifyAnchor = new CommitteeVerifyAnchor("committee");
        verifyAnchor.addNode("node1", "default", ((X509PubkeyInfoObjectIdentity) oid).getPublicKey());

        // prepare the network stuff
        CommitteeNetworkInfo committeeNetworkInfo = new CommitteeNetworkInfo("committee");
        committeeNetworkInfo.addEndpoint("node1", "grpcs://0.0.0.0:8080", NODE_SERVER_TLS_CERT);

        // build it first
        ptcTrustRoot = PTCTrustRoot.builder()
                .ptcCrossChainCert(NODE_PTC_CERT)
                .networkInfo(committeeNetworkInfo.encode())
                .issuerBcdnsDomainSpace(new CrossChainDomain(""))
                .sigAlgo(SignAlgoEnum.KECCAK256_WITH_SECP256K1)
                .verifyAnchorMap(MapUtil.builder(
                        BigInteger.ONE,
                        new PTCVerifyAnchor(
                                BigInteger.ONE,
                                verifyAnchor.encode()
                        )
                ).build())
                .build();

        // sign it with ptc private key which applied PTC certificate
        ptcTrustRoot.sign(NODE_PTC_PRIVATE_KEY);
    }

    @Mock
    private IBcdnsState bcdnsState;

    private PrivateKey bcdnsRootKey;
    private AbstractCrossChainCertificate bcdnsRootCert;
    private PublicKey publicKey;
    @InjectMocks
    private GRpcEmbeddedBcdnsService service;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        bcdnsRootKey = NODE_PTC_PRIVATE_KEY;
        publicKey = ObjectIdentityUtil.getPublicKeyFromSubject(oid, null);

        // Create a mock certificate, if needed
        bcdnsRootCert = CrossChainCertificateUtil.readCrossChainCertificateFromPem(BCDNS_CERT.getBytes());

        service = new GRpcEmbeddedBcdnsService(
                bcdnsState,
                HashAlgoEnum.KECCAK_256,  // Replace with actual enum value
                SignAlgoEnum.KECCAK256_WITH_SECP256K1,     // Replace with actual enum value
                bcdnsRootKey,
                bcdnsRootCert
        );
    }

    private BIDDocumentOperation getBid(PublicKey publicKey) {
        byte[] rawPublicKey;
        if (StrUtil.equalsIgnoreCase(publicKey.getAlgorithm(), "Ed25519")) {
            if (publicKey instanceof BCEdDSAPublicKey) {
                rawPublicKey = ((BCEdDSAPublicKey) publicKey).getPointEncoding();
            } else {
                throw new RuntimeException("your Ed25519 public key class not support: " + publicKey.getClass().getName());
            }
        } else if (StrUtil.equalsAnyIgnoreCase(publicKey.getAlgorithm(), "SM2", "EC")) {
            if (publicKey instanceof ECPublicKey) {
                rawPublicKey = ECKeyUtil.toPublicParams(publicKey).getQ().getEncoded(false);
            } else {
                throw new RuntimeException("your SM2/EC public key class not support: " + publicKey.getClass().getName());
            }
        } else {
            throw new RuntimeException(publicKey.getAlgorithm() + " not support");
        }

        byte[] rawPublicKeyWithSignals = new byte[rawPublicKey.length + 3];
        System.arraycopy(rawPublicKey, 0, rawPublicKeyWithSignals, 3, rawPublicKey.length);
        rawPublicKeyWithSignals[0] = -80;
        rawPublicKeyWithSignals[1] = StrUtil.equalsIgnoreCase(publicKey.getAlgorithm(), "Ed25519") ? KeyType.ED25519_VALUE : KeyType.SM2_VALUE;
        rawPublicKeyWithSignals[2] = 102;

        BIDpublicKeyOperation[] biDpublicKeyOperation = new BIDpublicKeyOperation[1];
        biDpublicKeyOperation[0] = new BIDpublicKeyOperation();
        biDpublicKeyOperation[0].setPublicKeyHex(HexUtil.encodeHexStr(rawPublicKeyWithSignals));
        biDpublicKeyOperation[0].setType(StrUtil.equalsIgnoreCase(publicKey.getAlgorithm(), "Ed25519") ? KeyType.ED25519 : KeyType.SM2);
        BIDDocumentOperation bidDocumentOperation = new BIDDocumentOperation();
        bidDocumentOperation.setPublicKey(biDpublicKeyOperation);

        return bidDocumentOperation;
    }

    @Test
    public void testQueryBCDNSTrustRootCertificate() {
        StreamObserver<Response> responseObserver = mock(StreamObserver.class);

        service.queryBCDNSTrustRootCertificate(Empty.getDefaultInstance(), responseObserver);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(responseObserver).onNext(captor.capture());
        Response response = captor.getValue();
        assertNotNull(response.getQueryBCDNSTrustRootCertificateResp());
        verify(responseObserver).onCompleted();
    }

    @Test
    public void testApplyRelayerCertificate() {
        ObjectIdentity oid;
        byte[] rootSubjectInfo = new byte[]{};
        PublicKey pk = publicKey;

        BIDDocumentOperation bidDocumentOperation = getBid(pk);
        oid = new BIDInfoObjectIdentity(
                BIDHelper.encAddress(
                        bidDocumentOperation.getPublicKey()[0].getType(),
                        BIDHelper.getRawPublicKeyFromBIDDocument(bidDocumentOperation)
                )
        );
        rootSubjectInfo = JsonUtils.toJSONString(bidDocumentOperation).getBytes();
        ApplyRelayerCertificateReq request = ApplyRelayerCertificateReq.newBuilder()
                .setCertSigningRequest(ByteString.copyFrom(CrossChainCertificateFactory.createRelayerCertificateSigningRequest(
                        "RELAYER_CERTIFICATE",
                        "myrelayer",
                        oid,
                        rootSubjectInfo
                ).encode()))
                .build();

        doNothing().when(bcdnsState).saveCrossChainCert(any(AbstractCrossChainCertificate.class));
        doNothing().when(bcdnsState).saveApplication(any(AbstractCrossChainCertificate.class), anyString(), any(ApplicationStateEnum.class));

        StreamObserver<Response> responseObserver = mock(StreamObserver.class);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

        service.applyRelayerCertificate(request, responseObserver);

        verify(responseObserver, times(1)).onNext(responseCaptor.capture());
        verify(responseObserver, times(1)).onCompleted();

        assertEquals(0, responseCaptor.getValue().getCode());
        assertNotNull(responseCaptor.getValue().getApplyRelayerCertificateResp().getApplyReceipt());
    }

    @Test
    public void testQueryRelayerCertificateApplicationResult() {
        String receipt = "mockRelayerReceipt"; // 示例 receipt
        QueryRelayerCertApplicationResultReq request = QueryRelayerCertApplicationResultReq.newBuilder()
                .setApplyReceipt(receipt)
                .build();

        CertApplicationResult mockApplicationResult = mock(CertApplicationResult.class);
        when(bcdnsState.queryApplication(anyString())).thenReturn(mockApplicationResult);

        StreamObserver<Response> responseObserver = mock(StreamObserver.class);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

        service.queryRelayerCertificateApplicationResult(request, responseObserver);

        verify(responseObserver, times(1)).onNext(responseCaptor.capture());
        verify(responseObserver, times(1)).onCompleted();
        assertEquals(0, responseCaptor.getValue().getCode());
        assertNotNull(responseCaptor.getValue().getApplicationResult());
    }

    @Test
    public void testApplyPTCCertificate() {
        ObjectIdentity oid;
        byte[] rootSubjectInfo = new byte[]{};
        PublicKey pk = publicKey;

        BIDDocumentOperation bidDocumentOperation = getBid(pk);
        oid = new BIDInfoObjectIdentity(
                BIDHelper.encAddress(
                        bidDocumentOperation.getPublicKey()[0].getType(),
                        BIDHelper.getRawPublicKeyFromBIDDocument(bidDocumentOperation)
                )
        );
        rootSubjectInfo = JsonUtils.toJSONString(bidDocumentOperation).getBytes();
        ApplyPTCCertificateReq request = ApplyPTCCertificateReq.newBuilder()
                .setCertSigningRequest(ByteString.copyFrom(CrossChainCertificateFactory.createPTCCertificateSigningRequest(
                        "PTC_CERTIFICATE",
                        "myptc",
                        PTCTypeEnum.COMMITTEE,
                        oid,
                        rootSubjectInfo
                ).encode()))
                .build();

        doNothing().when(bcdnsState).saveCrossChainCert(any(AbstractCrossChainCertificate.class));
        doNothing().when(bcdnsState).saveApplication(any(AbstractCrossChainCertificate.class), anyString(), any(ApplicationStateEnum.class));

        StreamObserver<Response> responseObserver = mock(StreamObserver.class);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

        service.applyPTCCertificate(request, responseObserver);

        verify(responseObserver, times(1)).onNext(responseCaptor.capture());
        verify(responseObserver, times(1)).onCompleted();

        assertEquals(0, responseCaptor.getValue().getCode());
        assertNotNull(responseCaptor.getValue().getApplyPTCCertificateResp().getApplyReceipt());
    }


    @Test
    public void testQueryPTCCertificateApplicationResult() {
        String receipt = "mockPTCReceipt";
        QueryPTCCertApplicationResultReq request = QueryPTCCertApplicationResultReq.newBuilder()
                .setApplyReceipt(receipt)
                .build();

        CertApplicationResult mockApplicationResult = mock(CertApplicationResult.class);
        when(bcdnsState.queryApplication(anyString())).thenReturn(mockApplicationResult);

        StreamObserver<Response> responseObserver = mock(StreamObserver.class);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

        service.queryPTCCertificateApplicationResult(request, responseObserver);

        verify(responseObserver, times(1)).onNext(responseCaptor.capture());
        verify(responseObserver, times(1)).onCompleted();
        assertEquals(0, responseCaptor.getValue().getCode());
        assertNotNull(responseCaptor.getValue().getApplicationResult());
    }

    @Test
    public void testApplyDomainNameCertificate() {
        ObjectIdentity oid;
        byte[] rootSubjectInfo = new byte[]{};
        PublicKey pk = publicKey;

        BIDDocumentOperation bidDocumentOperation = getBid(pk);
        oid = new BIDInfoObjectIdentity(
                BIDHelper.encAddress(
                        bidDocumentOperation.getPublicKey()[0].getType(),
                        BIDHelper.getRawPublicKeyFromBIDDocument(bidDocumentOperation)
                )
        );
        rootSubjectInfo = JsonUtils.toJSONString(bidDocumentOperation).getBytes();
        ApplyDomainNameCertificateReq request = ApplyDomainNameCertificateReq.newBuilder()
                .setCertSigningRequest(ByteString.copyFrom(CrossChainCertificateFactory.createDomainNameCertificateSigningRequest(
                        "DomainName_CERTIFICATE",
                        new CrossChainDomain(CrossChainDomain.ROOT_DOMAIN_SPACE),
                        new CrossChainDomain(CrossChainDomain.ROOT_DOMAIN_SPACE),
                        oid,
                        rootSubjectInfo
                ).encode()))
                .build();

        doNothing().when(bcdnsState).saveCrossChainCert(any(AbstractCrossChainCertificate.class));
        doNothing().when(bcdnsState).saveApplication(any(AbstractCrossChainCertificate.class), anyString(), any(ApplicationStateEnum.class));

        StreamObserver<Response> responseObserver = mock(StreamObserver.class);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

        service.applyDomainNameCertificate(request, responseObserver);

        verify(responseObserver, times(1)).onNext(responseCaptor.capture());
        verify(responseObserver, times(1)).onCompleted();

        assertEquals(0, responseCaptor.getValue().getCode());
        assertNotNull(responseCaptor.getValue().getApplyDomainNameCertificateResp().getApplyReceipt());
    }

    @Test
    public void testQueryDomainNameCertificateApplicationResult() {
        String receipt = "mockDomainNameReceipt";
        QueryDomainNameCertApplicationResultReq request = QueryDomainNameCertApplicationResultReq.newBuilder()
                .setApplyReceipt(receipt)
                .build();

        CertApplicationResult mockApplicationResult = mock(CertApplicationResult.class);
        when(bcdnsState.queryApplication(anyString())).thenReturn(mockApplicationResult);

        StreamObserver<Response> responseObserver = mock(StreamObserver.class);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

        service.queryDomainNameCertificateApplicationResult(request, responseObserver);

        verify(responseObserver, times(1)).onNext(responseCaptor.capture());
        verify(responseObserver, times(1)).onCompleted();
        assertEquals(0, responseCaptor.getValue().getCode());
        assertNotNull(responseCaptor.getValue().getApplicationResult());
    }

    @Test
    public void testQueryRelayerCertificate() {
        String certId = "mockRelayerCertId";
        QueryRelayerCertificateReq request = QueryRelayerCertificateReq.newBuilder()
                .setRelayerCertId(certId)
                .build();

        AbstractCrossChainCertificate mockCertificate = mock(AbstractCrossChainCertificate.class);
        when(mockCertificate.encode()).thenReturn(new byte[]{1, 2, 3, 4});
        when(bcdnsState.queryCrossChainCert(anyString(), any(CrossChainCertificateTypeEnum.class))).thenReturn(mockCertificate);

        StreamObserver<Response> responseObserver = mock(StreamObserver.class);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

        service.queryRelayerCertificate(request, responseObserver);

        verify(responseObserver, times(1)).onNext(responseCaptor.capture());
        verify(responseObserver, times(1)).onCompleted();
        assertEquals(0, responseCaptor.getValue().getCode());
        assertTrue(responseCaptor.getValue().getQueryRelayerCertificateResp().getExist());
    }

    @Test
    public void testQueryPTCCertificate() {
        String certId = "mockPTCCertId";
        QueryPTCCertificateReq request = QueryPTCCertificateReq.newBuilder()
                .setPtcCertId(certId)
                .build();

        AbstractCrossChainCertificate mockCertificate = mock(AbstractCrossChainCertificate.class);
        when(mockCertificate.encode()).thenReturn(new byte[]{1, 2, 3, 4});
        when(bcdnsState.queryCrossChainCert(anyString(), any(CrossChainCertificateTypeEnum.class))).thenReturn(mockCertificate);

        StreamObserver<Response> responseObserver = mock(StreamObserver.class);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

        service.queryPTCCertificate(request, responseObserver);

        verify(responseObserver, times(1)).onNext(responseCaptor.capture());
        verify(responseObserver, times(1)).onCompleted();
        assertEquals(0, responseCaptor.getValue().getCode());
        assertTrue(responseCaptor.getValue().getQueryPTCCertificateResp().getExist());
    }

    @Test
    public void testQueryDomainNameCertificate() {
        String domain = "mockDomainName";
        QueryDomainNameCertificateReq request = QueryDomainNameCertificateReq.newBuilder()
                .setDomain(domain)
                .build();

        AbstractCrossChainCertificate mockCertificate = mock(AbstractCrossChainCertificate.class);
        when(mockCertificate.encode()).thenReturn(new byte[]{1, 2, 3, 4});
        when(bcdnsState.queryDomainCert(anyString())).thenReturn(mockCertificate);

        StreamObserver<Response> responseObserver = mock(StreamObserver.class);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

        service.queryDomainNameCertificate(request, responseObserver);

        verify(responseObserver, times(1)).onNext(responseCaptor.capture());
        verify(responseObserver, times(1)).onCompleted();
        assertEquals(0, responseCaptor.getValue().getCode());
        assertTrue(responseCaptor.getValue().getQueryDomainNameCertificateResp().getExist());
    }

    @Test
    public void testRegisterDomainRouter() {
        String domain = "mockDomainName";
        AbstractCrossChainCertificate relayerCert = mock(AbstractCrossChainCertificate.class);
        DomainRouter domainRouter = new DomainRouter(
                new CrossChainDomain(domain),
                new Relayer(
                        "relayerCertId",
                        relayerCert,
                        null
                ));
        RegisterDomainRouterReq request = RegisterDomainRouterReq.newBuilder()
                .setDomainRouter(ByteString.copyFrom(domainRouter.encode()))
                .build();

        StreamObserver<Response> responseObserver = mock(StreamObserver.class);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

        doNothing().when(bcdnsState).registerDomainRouter(any(DomainRouter.class));

        service.registerDomainRouter(request, responseObserver);

        verify(responseObserver, times(1)).onNext(responseCaptor.capture());
        verify(responseObserver, times(1)).onCompleted();

        Response response = responseCaptor.getValue();
        assertEquals(0, response.getCode());
    }

    @Test
    public void testRegisterThirdPartyBlockchainTrustAnchor() {
        byte[] tpbtaBytes = tpbta.encode();
        RegisterThirdPartyBlockchainTrustAnchorReq request = RegisterThirdPartyBlockchainTrustAnchorReq.newBuilder()
                .setPtcId(ByteString.copyFrom(oid.encode()))
                .setDomain(tpbta.getCrossChainLane().getSenderDomain().getDomain())
                .setTpbta(ByteString.copyFrom(tpbtaBytes))
                .build();

        StreamObserver<Response> responseObserver = mock(StreamObserver.class);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

        doNothing().when(bcdnsState).registerTPBTA(any(byte[].class));
        when(bcdnsState.queryMatchedTpBta(any())).thenReturn(null);
        when(bcdnsState.queryPTCTrustRoot(any())).thenReturn(ptcTrustRoot);

        service.registerThirdPartyBlockchainTrustAnchor(request, responseObserver);

        verify(responseObserver, times(1)).onNext(responseCaptor.capture());
        verify(responseObserver, times(1)).onCompleted();

        Response response = responseCaptor.getValue();
        assertEquals(0, response.getCode());
    }

    @Test
    public void testQueryDomainRouter() {
        String domain = "mockDomain";
        QueryDomainRouterReq request = QueryDomainRouterReq.newBuilder()
                .setDestDomain(domain)
                .build();

        DomainRouter mockDomainRouter = new DomainRouter();
        mockDomainRouter.setDestDomain(new CrossChainDomain(domain));
        byte[] domainRouterBytes = mockDomainRouter.encode();

        when(bcdnsState.queryDomainRouter(anyString())).thenReturn(mockDomainRouter);

        StreamObserver<Response> responseObserver = mock(StreamObserver.class);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

        service.queryDomainRouter(request, responseObserver);

        verify(responseObserver, times(1)).onNext(responseCaptor.capture());
        verify(responseObserver, times(1)).onCompleted();

        Response response = responseCaptor.getValue();
        assertNotNull(response.getQueryDomainRouterResp());
        assertArrayEquals(domainRouterBytes, response.getQueryDomainRouterResp().getDomainRouter().toByteArray());
        assertEquals(0, response.getCode());
    }

    @Test
    public void testQueryThirdPartyBlockchainTrustAnchor() {
        CrossChainLane tpbtaLane = new CrossChainLane(new CrossChainDomain("mockDomain"));
        QueryThirdPartyBlockchainTrustAnchorReq request = QueryThirdPartyBlockchainTrustAnchorReq.newBuilder()
                .setTpbtaLaneKey(tpbtaLane.getLaneKey())
                .setTpbtaVersion(0)
                .build();

        byte[] tpbtaBytes = tpbta.encode();

        when(bcdnsState.queryExactTpBta(notNull(), anyLong())).thenReturn(tpbta);

        StreamObserver<Response> responseObserver = mock(StreamObserver.class);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

        service.queryThirdPartyBlockchainTrustAnchor(request, responseObserver);

        verify(responseObserver, times(1)).onNext(responseCaptor.capture());
        verify(responseObserver, times(1)).onCompleted();

        Response response = responseCaptor.getValue();
        assertNotNull(response.getQueryThirdPartyBlockchainTrustAnchorResp());
        assertArrayEquals(tpbtaBytes, response.getQueryThirdPartyBlockchainTrustAnchorResp().getTpbta().toByteArray());
        assertEquals(0, response.getCode());
    }

    @Test
    public void testAddPTCTrustRoot() {
        AddPTCTrustRootReq request = AddPTCTrustRootReq.newBuilder()
                .setPtcTrustRoot(ByteString.copyFrom(ptcTrustRoot.encode()))
                .build();

        StreamObserver<Response> responseObserver = mock(StreamObserver.class);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

        doNothing().when(bcdnsState).addPTCTrustRoot(any());

        service.addPTCTrustRoot(request, responseObserver);

        verify(responseObserver, times(1)).onNext(responseCaptor.capture());
        verify(responseObserver, times(1)).onCompleted();

        Response response = responseCaptor.getValue();
        assertEquals(0, response.getCode());
    }

    @Test
    public void testQueryPTCTrustRoot() {
        QueryPtcTrustRootReq request = QueryPtcTrustRootReq.newBuilder()
                .setPtcOid(ByteString.copyFrom(oid.encode()))
                .build();

        StreamObserver<Response> responseObserver = mock(StreamObserver.class);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);

        when(bcdnsState.queryPTCTrustRoot(ArgumentMatchers.argThat(argument -> argument.equals(oid)))).thenReturn(ptcTrustRoot);

        service.queryPtcTrustRoot(request, responseObserver);

        verify(responseObserver, times(1)).onNext(responseCaptor.capture());
        verify(responseObserver, times(1)).onCompleted();

        Response response = responseCaptor.getValue();
        assertTrue(response.hasQueryPtcTrustRootResp());
        assertArrayEquals(ptcTrustRoot.encode(), response.getQueryPtcTrustRootResp().getPtcTrustRoot().toByteArray());
        assertEquals(0, response.getCode());
    }
}