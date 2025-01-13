package com.alipay.antchain.bridge.ptc.committee.supervisor.cli.command;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.List;
import java.util.Map;

import cn.ac.caict.bid.model.BIDDocumentOperation;
import cn.ac.caict.bid.model.BIDpublicKeyOperation;
import cn.bif.common.JsonUtils;
import cn.bif.module.encryption.model.KeyType;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.ECKeyUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.PemUtil;
import cn.hutool.jwt.signers.AlgorithmUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alipay.antchain.bridge.bcdns.factory.BlockChainDomainNameServiceFactory;
import com.alipay.antchain.bridge.bcdns.service.BCDNSTypeEnum;
import com.alipay.antchain.bridge.bcdns.service.IBlockChainDomainNameService;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.alipay.antchain.bridge.commons.bcdns.utils.BIDHelper;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.base.*;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTypeEnum;
import com.alipay.antchain.bridge.commons.core.ptc.PTCVerifyAnchor;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import com.alipay.antchain.bridge.ptc.committee.supervisor.cli.config.BCDNSConfig;
import com.alipay.antchain.bridge.ptc.committee.supervisor.cli.config.CommitteeNodeConfig;
import com.alipay.antchain.bridge.ptc.committee.supervisor.cli.config.SupervisorCLIConfig;
import com.alipay.antchain.bridge.ptc.committee.types.network.CommitteeNetworkInfo;
import com.alipay.antchain.bridge.ptc.committee.types.trustroot.CommitteeVerifyAnchor;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPublicKey;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.standard.*;

@Getter
@ShellCommandGroup(value = "Commands about Supervisor")
@ShellComponent
public class SupervisorManagerCommands {

    @Value("${supervisor.cli.config-file-path}")
    private org.springframework.core.io.Resource configFile;

    @Resource
    private SupervisorCLIConfig supervisorCLIConfig;

    @Resource
    private Map<String, IBlockChainDomainNameService> domainSpaceToBcdnsMap;

    /*** generate PTC account for supervisor node
     * @author yeuchi.jch
     * @version 0.1.0
     */
    @ShellMethod(value = "Generate PEM files for the PTC private and public key")
    public String generatePtcAccount(
            @ShellOption(help = "Committee ID") String committeeId,
            @ShellOption(help = "Key algorithm, default Keccak256WithSecp256k1", defaultValue = "Keccak256WithSecp256k1") String keyAlgo,
            @ShellOption(valueProvider = FileValueProvider.class, help = "Directory path to save the keys", defaultValue = "./") String outDir
    ) {
        try {
            if (StrUtil.isEmpty(committeeId)) {
                throw new IllegalArgumentException("Committee ID is required");
            }

            var keyPair = SignAlgoEnum.getByName(keyAlgo).getSigner().generateKeyPair();

            // dump the private key into pem
            var privatePath = Paths.get(outDir, "private_key.pem").toAbsolutePath();
            writePrivateKey(keyPair.getPrivate(), privatePath);

            // dump the public key into pem
            var publicPath = Paths.get(outDir, "public_key.pem").toAbsolutePath();
            writePublicKey(keyPair.getPublic(), publicPath);

            // update configFile which located in ${supervisor.cli.config-file-path}
            supervisorCLIConfig.setPrivateKey(privatePath.toString());
            supervisorCLIConfig.setPublicKey(publicPath.toString());
            supervisorCLIConfig.setSignAlgo(SignAlgoEnum.getByName(keyAlgo));
            supervisorCLIConfig.setCommitteeId(committeeId);
            Files.write(configFile.getFile().toPath(), JSON.toJSONBytes(supervisorCLIConfig, SerializerFeature.PrettyFormat));

            return StrUtil.format("private key path: {}\npublic key path: {}", privatePath.toAbsolutePath(), publicPath.toAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", e);
        }
    }

    /*** generate PTC Certificate Signing Request
     * @author yeuchi.jch
     * @version 0.1.0
     */
    @ShellMethod(value = "Generate the ptc certificate sign request in Base64 format")
    public String generatePtcCSR(
            @ShellOption(
                    help = "Version of ptc crosschain certificate to apply",
                    defaultValue = CrossChainCertificateFactory.DEFAULT_VERSION
            ) String certVersion,
            @ShellOption(help = "Name of ptc credential subject for crosschain certificate", defaultValue = "myptc") String credSubjectName,
            @ShellOption(
                    valueProvider = EnumValueProvider.class,
                    help = "Type of object identity who owned the ptc crosschain certificate",
                    defaultValue = "X509_PUBLIC_KEY_INFO"
            ) ObjectIdentityType oidType,
            @ShellOption(
                    valueProvider = EnumValueProvider.class,
                    help = "Type of ptc to own the crosschain certificate",
                    defaultValue = "COMMITTEE"
            ) PTCTypeEnum ptcType,
            @ShellOption(
                    valueProvider = FileValueProvider.class,
                    help = "Path to ptc public key who apply the certificate"
            ) String pubkeyFile
    ) {
        try {
            ObjectIdentity oid;
            byte[] rootSubjectInfo = new byte[]{};
            PublicKey publicKey;
            Path pubkeyFilePath = Paths.get(pubkeyFile);
            if (Files.isReadable(pubkeyFilePath)) {
                publicKey = readPublicKeyFromPem(Files.readAllBytes(pubkeyFilePath));
            } else {
                return "please input the path to the correct applicant public key file";
            }

            if (oidType == ObjectIdentityType.X509_PUBLIC_KEY_INFO) {
                oid = new X509PubkeyInfoObjectIdentity(publicKey.getEncoded());
            } else {
                BIDDocumentOperation bidDocumentOperation = getBid(publicKey);
                oid = new BIDInfoObjectIdentity(
                        BIDHelper.encAddress(
                                bidDocumentOperation.getPublicKey()[0].getType(),
                                BIDHelper.getRawPublicKeyFromBIDDocument(bidDocumentOperation)
                        )
                );
                rootSubjectInfo = JsonUtils.toJSONString(bidDocumentOperation).getBytes();
            }

            return StrUtil.format(
                    "your CSR is \n{}",
                    Base64.encode(
                            CrossChainCertificateFactory.createPTCCertificateSigningRequest(
                                    certVersion,
                                    credSubjectName,
                                    ptcType,
                                    oid,
                                    rootSubjectInfo
                            ).encode()
                    )
            );

        } catch (Exception e) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", e);
        }
    }

    /*** start one bcdns client for every domainspace
     * @author yeuchi.jch
     * @version 0.1.0
     */
    @ShellMethod(value = "start a BCDNS client to interact with BlockChainDomainNameService")
    Object startBcdnsClient(
            @ShellOption(
                    help = "Specify which DomainSpace's BCDNS client to start", defaultValue = ""
            ) String domainSpace,
            @ShellOption(
                    valueProvider = EnumValueProvider.class,
                    help = "if the supervisorCLI has not been created domainSpace‘s BCDNS client, you need to specify the BCDNS type to create, e.g EMBEDDED, BIF"
            ) BCDNSTypeEnum bcdnsType,
            @ShellOption(
                    help = "BCDNS Client Config File", valueProvider = FileValueProvider.class
            ) String bcdnsClientConfigPath) {
        try {
            if (domainSpaceToBcdnsMap.get(domainSpace) != null) {
                return StrUtil.format("(domainSpace: {})'s bcdns already started, cannot create it again ", domainSpace);
            } else {
                if (Files.isReadable(Paths.get(bcdnsClientConfigPath))) {
                    byte[] bcdnsClientConf = Files.readAllBytes(Paths.get(bcdnsClientConfigPath));
                    IBlockChainDomainNameService bcdnsClient = BlockChainDomainNameServiceFactory.create(bcdnsType, bcdnsClientConf);
                    domainSpaceToBcdnsMap.put(domainSpace, bcdnsClient);
                    // update supervisorCLI config file
                    Path bcdnsClientConfigPathAbs = Paths.get(bcdnsClientConfigPath).toAbsolutePath();
                    BCDNSConfig bcdnsConfig = new BCDNSConfig(bcdnsType, bcdnsClientConfigPathAbs);
                    supervisorCLIConfig.getBcdnsConfig().put(domainSpace, bcdnsConfig);
                    Files.write(configFile.getFile().toPath(), JSON.toJSONBytes(supervisorCLIConfig, SerializerFeature.PrettyFormat));
                } else {
                    throw new RuntimeException(StrUtil.format("can not read bcdns client config file from: {}", bcdnsClientConfigPath));
                }
            }
            return StrUtil.format("start bcdns client success, {}", configFile.getFile().toPath());
        } catch (Exception e) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", e);
        }
    }

    /*** generate PTCTrustRoot by supervisor node
     * @author yeuchi.jch
     * @version 0.1.0
     */
    @ShellMethod(value = "Generate the PTCTrustRoot in Base64 format")
    Object generatePtcTrustRoot(
            @ShellOption(
                    help = "Specify PTCTrustRoot for domainSpace e.g \".org\", \".com\"", defaultValue = ""
            ) String domainSpace,
            @ShellOption(
                    help = "Directory where stored committeeNodes' Info",
                    defaultValue = "."
            ) String committeeNodesInfoDir,
            @ShellOption(
                    help = "Which nodes are selected as verifyAnchor"
            ) List<String> committeeNodeIds) {
        try {
            // prepare supervisor certificate with BCDNS signature
            byte[] ptcCertBytes = Files.readAllBytes(Paths.get(supervisorCLIConfig.getPtcCertificate()));
            AbstractCrossChainCertificate ptcCertObj = CrossChainCertificateUtil.readCrossChainCertificateFromPem(ptcCertBytes);

            // prepare supervisor node's private key
            byte[] ptcPrivateKey = Files.readAllBytes(Paths.get(supervisorCLIConfig.getPrivateKey()));
            PrivateKey ptcPrivateKeyObj = supervisorCLIConfig.getSignAlgo().getSigner().readPemPrivateKey(ptcPrivateKey);

            // prepare what parameters one verifyAnchor need
            String committeeId = supervisorCLIConfig.getCommitteeId();
            CommitteeVerifyAnchor verifyAnchors = new CommitteeVerifyAnchor(committeeId);
            CommitteeNetworkInfo committeeNetworkInfo = new CommitteeNetworkInfo(committeeId);
            // traverse committeeNodeIds，for every nodeId: open file and write in
            for (String committeeNodeId : committeeNodeIds) {
                Path committeeNodeConfigPath = Paths.get(committeeNodesInfoDir, committeeNodeId + ".json");
                byte[] committeeNodeConfigByte = Files.readAllBytes(committeeNodeConfigPath);
                CommitteeNodeConfig committeeNodeConfig = JSON.parseObject(committeeNodeConfigByte, CommitteeNodeConfig.class);
                // prepare the public keys
                for (String keyId : committeeNodeConfig.getKeys().keySet()) {
                    String committeeNodePublicKey = committeeNodeConfig.getKeys().get(keyId);
                    PublicKey committeeNodePubkey = new X509PubkeyInfoObjectIdentity(
                            PemUtil.readPem(new ByteArrayInputStream(committeeNodePublicKey.getBytes()))
                    ).getPublicKey();
                    // add committee node to verifyAnchors List
                    verifyAnchors.addNode(committeeNodeId, keyId, committeeNodePubkey);
                }
                // prepare the network stuff
                String endpointURL = committeeNodeConfig.getEndPointUrl();
                String committeeNodeTLSCert = committeeNodeConfig.getTlsCert();
                committeeNetworkInfo.addEndpoint(committeeNodeId, endpointURL, committeeNodeTLSCert);
            }
            // build it first
            PTCTrustRoot ptcTrustRoot = PTCTrustRoot.builder()
                    .ptcCrossChainCert(ptcCertObj)
                    .networkInfo(committeeNetworkInfo.encode())
                    .issuerBcdnsDomainSpace(new CrossChainDomain(domainSpace))
                    .sigAlgo(supervisorCLIConfig.getSignAlgo())
                    .verifyAnchorMap(MapUtil.builder(
                            BigInteger.ZERO,
                            new PTCVerifyAnchor(
                                    BigInteger.ZERO,
                                    verifyAnchors.encode()
                            )
                    ).build())
                    .build();
            // sign it with ptc private key which applied PTC certificate
            ptcTrustRoot.sign(ptcPrivateKeyObj);

            return StrUtil.format(
                    "your new generated PTCTrustRoot is \n{}",
                    Base64.encode(
                            ptcTrustRoot.encode()
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", e);
        }
    }


    /*** add PTCTrustRoot to BCDNS Service
     * @author yeuchi.jch
     * @version 0.1.0
     */
    @ShellMethod(value = "upload PTCTrustRoot to specified BCDNS Service")
    Object addPtcTrustRoot(
            @ShellOption(
                    help = "Upload PTCTrustRoot to the BCDNS Service corresponding to the specified domainSpace",
                    defaultValue = ""
            ) String domainSpace,
            @ShellOption(
                    help = "(String)PTCTrustRoot to upload"
            ) String ptcTrustRootBase64) {
        try {
            PTCTrustRoot ptcTrustRoot = PTCTrustRoot.decode(Base64.decode(ptcTrustRootBase64));
            if (domainSpaceToBcdnsMap.get(domainSpace) == null) {
                return StrUtil.format("PTC service has not yet created domainSpace: {}'s BCDNS client", domainSpace);
            } else {
                IBlockChainDomainNameService bcdns = domainSpaceToBcdnsMap.get(domainSpace);
                bcdns.addPTCTrustRoot(ptcTrustRoot);
                return StrUtil.format("add ptcTrustRoot to (domain:{})'s BCDNS Service success", domainSpace);
            }
        } catch (Exception e) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", e);
        }
    }

    @ShellMethod(value = "Convert the crosschain certificate from other format to PEM")
    public String convertCrossChainCertToPem(
            @ShellOption(help = "Base64 format string of crosschain certificate") String base64Input,
            @ShellOption(valueProvider = FileValueProvider.class, help = "Directory path to save the output", defaultValue = "") String outDir
    ) {
        try {
            AbstractCrossChainCertificate crossChainCertificate = CrossChainCertificateFactory.createCrossChainCertificate(Base64.decode(base64Input));
            if (StrUtil.isNotEmpty(outDir)) {
                Path path = Paths.get(outDir, StrUtil.format("output_{}.crt", System.currentTimeMillis()));
                Files.write(path, CrossChainCertificateUtil.formatCrossChainCertificateToPem(crossChainCertificate).getBytes());
                return StrUtil.format("certificate in pem saved here: {}", path.toAbsolutePath().toString());
            }
            return CrossChainCertificateUtil.formatCrossChainCertificateToPem(crossChainCertificate);
        } catch (Exception e) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", e);
        }
    }

    @ShellMethod(value = "Show running bcdns client")
    public String showRunningBcdnsClient() {
        try {
            return JSON.toJSONString(supervisorCLIConfig.getBcdnsConfig(), SerializerFeature.PrettyFormat);
        } catch (Exception e) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", e);
        }
    }

    @SneakyThrows
    private void writePrivateKey(PrivateKey privateKey, Path outputFile) {
        // dump the private key into pem
        StringWriter stringWriter = new StringWriter(256);
        JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
        jcaPEMWriter.writeObject(privateKey);
        jcaPEMWriter.close();
        String privatePem = stringWriter.toString();
        Files.write(outputFile, privatePem.getBytes());
    }

    @SneakyThrows
    private void writePublicKey(PublicKey publicKey, Path outputFile) {
        // dump the public key into pem
        StringWriter stringWriter = new StringWriter(256);
        JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
        jcaPEMWriter.writeObject(publicKey);
        jcaPEMWriter.close();
        String pubkeyPem = stringWriter.toString();
        Files.write(outputFile, pubkeyPem.getBytes());
    }

    @SneakyThrows
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

    @SneakyThrows
    private PublicKey readPublicKeyFromPem(byte[] publicKeyPem) {
        SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(PemUtil.readPem(new ByteArrayInputStream(publicKeyPem)));
        return KeyUtil.generatePublicKey(
                AlgorithmUtil.getAlgorithm(keyInfo.getAlgorithm().getAlgorithm().getId()),
                keyInfo.getEncoded()
        );
    }
}
