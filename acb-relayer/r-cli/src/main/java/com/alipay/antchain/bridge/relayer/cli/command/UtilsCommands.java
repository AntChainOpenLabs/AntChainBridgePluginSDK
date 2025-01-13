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

package com.alipay.antchain.bridge.relayer.cli.command;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.util.Date;

import cn.ac.caict.bid.model.BIDDocumentOperation;
import cn.ac.caict.bid.model.BIDpublicKeyOperation;
import cn.bif.common.JsonUtils;
import cn.bif.module.encryption.key.PrivateKeyManager;
import cn.bif.module.encryption.key.PublicKeyManager;
import cn.bif.module.encryption.model.KeyMember;
import cn.bif.module.encryption.model.KeyType;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.ECKeyUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.PemUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alipay.antchain.bridge.bcdns.impl.bif.conf.BifBCNDSConfig;
import com.alipay.antchain.bridge.bcdns.impl.bif.conf.BifCertificationServiceConfig;
import com.alipay.antchain.bridge.bcdns.impl.bif.conf.BifChainConfig;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.BCDNSTrustRootCredentialSubject;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.alipay.antchain.bridge.commons.bcdns.utils.BIDHelper;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.base.BIDInfoObjectIdentity;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentityType;
import com.alipay.antchain.bridge.commons.core.base.X509PubkeyInfoObjectIdentity;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTypeEnum;
import com.alipay.antchain.bridge.commons.utils.crypto.HashAlgoEnum;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.springframework.shell.standard.*;
import sun.security.x509.AlgorithmId;

@Getter
@ShellCommandGroup(value = "Utils Commands")
@ShellComponent
public class UtilsCommands {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String EMBEDDED_BCDNS_PUBKEY_DEFAULT_FILENAME = "embedded-bcdns-root-pubkey.key";

    private static final String EMBEDDED_BCDNS_PRIVATE_KEY_DEFAULT_FILENAME = "embedded-bcdns-root-private-key.key";

    private static final String EMBEDDED_BCDNS_ROOT_CERT = "embedded-bcdns-root.crt";

    private static final String EMBEDDED_BCDNS_ROOT_BID_DOCUMENT = "embedded-bcdns-root-bid-document.json";

    @ShellMethod(value = "Generate PEM files for the relayer private and public key")
    public String generateRelayerAccount(
            @ShellOption(help = "Key algorithm, default SECP256K1", defaultValue = "SECP256K1") String keyAlgo,
            @ShellOption(valueProvider = FileValueProvider.class, help = "Directory path to save the keys", defaultValue = "") String outDir
    ) {
        try {
            SignAlgoEnum signAlgo = SignAlgoEnum.getSignAlgoByKeySuffix(keyAlgo);
            KeyPair keyPair = signAlgo.getSigner().generateKeyPair();

            // dump the private key into pem
            Path privatePath = Paths.get(outDir, "private_key.pem");
            writePrivateKey(keyPair.getPrivate(), privatePath);

            // dump the public key into pem
            Path publicPath = Paths.get(outDir, "public_key.pem");
            writePublicKey(keyPair.getPublic(), publicPath);

            return StrUtil.format("private key path: {}\npublic key path: {}", privatePath.toAbsolutePath(), publicPath.toAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", e);
        }
    }

    @ShellMethod(value = "Generate the relayer certificate sign request in Base64 format")
    public String generateRelayerCSR(
            @ShellOption(
                    help = "Version of relayer crosschain certificate to apply",
                    defaultValue = CrossChainCertificateFactory.DEFAULT_VERSION
            ) String certVersion,
            @ShellOption(help = "Name of relayer credential subject for crosschain certificate", defaultValue = "myrelayer") String credSubjectName,
            @ShellOption(
                    valueProvider = EnumValueProvider.class,
                    help = "Type of object identity who owned the relayer crosschain certificate",
                    defaultValue = "X509_PUBLIC_KEY_INFO"
            ) ObjectIdentityType oidType,
            @ShellOption(
                    valueProvider = FileValueProvider.class,
                    help = "Path to relayer public key who apply the certificate"
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
                            CrossChainCertificateFactory.createRelayerCertificateSigningRequest(
                                    certVersion,
                                    credSubjectName,
                                    oid,
                                    rootSubjectInfo
                            ).encode()
                    )
            );

        } catch (Exception e) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", e);
        }
    }

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
                    defaultValue = "BLOCKCHAIN"
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

    @ShellMethod(value = "Generate the BID document file containing the raw public key")
    public String generateBidDocument(
            @ShellOption(valueProvider = FileValueProvider.class, help = "The path to public key in PEM") String publicKeyPath,
            @ShellOption(valueProvider = FileValueProvider.class, help = "Directory path to save the output", defaultValue = "") String outDir
    ) {
        try {
            PublicKey publicKey = readPublicKeyFromPem(Files.readAllBytes(Paths.get(publicKeyPath)));
            BIDDocumentOperation bidDocumentOperation = getBid(publicKey);
            String rawBidDoc = JsonUtils.toJSONString(bidDocumentOperation);
            Path path = Paths.get(outDir, "bid_document.json");
            Files.write(path, rawBidDoc.getBytes());

            return "file is : " + path.toAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", e);
        }
    }

    @ShellMethod(value = "Generate the config json file for BIF BCDNS client")
    public String generateBifBcdnsConf(
            @ShellOption(valueProvider = FileValueProvider.class, help = "authorized private key to apply the relayer and ptc certificates from BIF BCDNS, default using relayer key", defaultValue = "") String authPrivateKeyFile,
            @ShellOption(valueProvider = FileValueProvider.class, help = "authorized public key to apply the relayer and ptc certificates from BIF BCDNS, default using relayer key", defaultValue = "") String authPublicKeyFile,
            @ShellOption(help = "Authorized key sig algorithm, default Ed25519", defaultValue = "Ed25519") String authSigAlgo,
            @ShellOption(valueProvider = FileValueProvider.class, help = "relayer private key") String relayerPrivateKeyFile,
            @ShellOption(valueProvider = FileValueProvider.class, help = "relayer cross-chain certificate") String relayerCrossChainCertFile,
            @ShellOption(help = "Relayer key sig algorithm, default Ed25519", defaultValue = "Ed25519") String relayerSigAlgo,
            @ShellOption(help = "Certificate server url of BIF BCDNS, e.g. http://localhost:8112") String certServerUrl,
            @ShellOption(help = "The RPC url for BIF blockchain node, e.g. `http://test.bifcore.bitfactory.cn` for testnet") String bifChainRpcUrl,
            @ShellOption(help = "The RPC port for BIF blockchain node if needed", defaultValue = "-1") Integer bifChainRpcPort,
            @ShellOption(help = "Domain govern contract address on BIF chain") String bifDomainGovernContract,
            @ShellOption(help = "PTC govern contract address on BIF chain") String bifPtcGovernContract,
            @ShellOption(help = "Relayer govern contract address on BIF chain") String bifRelayerGovernContract,
            @ShellOption(valueProvider = FileValueProvider.class, help = "Directory path to save the output", defaultValue = "") String outDir
    ) {
        try {
            if (!StrUtil.equalsIgnoreCase(relayerSigAlgo, "Ed25519")) {
                return "relayer sig algo only support Ed25519 for now";
            }

            AbstractCrossChainCertificate relayerCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
                    Files.readAllBytes(Paths.get(relayerCrossChainCertFile))
            );
            String authPublicKey;
            if (ObjectUtil.isEmpty(authPrivateKeyFile)) {
                authPrivateKeyFile = relayerPrivateKeyFile;
                authPublicKey = getPemPublicKey(CrossChainCertificateUtil.getPublicKeyFromCrossChainCertificate(relayerCert));
                authSigAlgo = relayerSigAlgo;
            } else {
                authPublicKey = new String(Files.readAllBytes(Paths.get(authPublicKeyFile)));
            }

            BifCertificationServiceConfig bifCertificationServiceConfig = new BifCertificationServiceConfig();
            bifCertificationServiceConfig.setAuthorizedKeyPem(new String(Files.readAllBytes(Paths.get(authPrivateKeyFile))));
            bifCertificationServiceConfig.setAuthorizedPublicKeyPem(authPublicKey);
            bifCertificationServiceConfig.setAuthorizedSigAlgo(authSigAlgo);
            bifCertificationServiceConfig.setClientPrivateKeyPem(new String(Files.readAllBytes(Paths.get(relayerPrivateKeyFile))));
            bifCertificationServiceConfig.setSigAlgo(relayerSigAlgo);
            bifCertificationServiceConfig.setClientCrossChainCertPem(CrossChainCertificateUtil.formatCrossChainCertificateToPem(relayerCert));
            bifCertificationServiceConfig.setUrl(certServerUrl);

            BifChainConfig bifChainConfig = new BifChainConfig();
            bifChainConfig.setBifChainRpcUrl(bifChainRpcUrl);
            if (bifChainRpcPort > 0) {
                bifChainConfig.setBifChainRpcPort(bifChainRpcPort);
            }
            bifChainConfig.setBifPrivateKey(convertToBIFPrivateKey(bifCertificationServiceConfig.getClientPrivateKeyPem()));
            bifChainConfig.setBifAddress(convertToBIFAddress(
                    CrossChainCertificateUtil.getRawPublicKeyFromCrossChainCertificate(relayerCert)
            ));
            bifChainConfig.setDomainGovernContract(bifDomainGovernContract);
            bifChainConfig.setPtcGovernContract(bifPtcGovernContract);
            bifChainConfig.setRelayerGovernContract(bifRelayerGovernContract);
            bifChainConfig.setCertificatesGovernContract("");

            BifBCNDSConfig config = new BifBCNDSConfig();
            config.setChainConfig(bifChainConfig);
            config.setCertificationServiceConfig(bifCertificationServiceConfig);

            Path path = Paths.get(outDir, "bif_bcdns_conf.json");
            Files.write(path, JSON.toJSONString(config, SerializerFeature.PrettyFormat).getBytes());

            return "file is : " + path.toAbsolutePath();
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

    @ShellMethod(value = "Generate self-signed BCDNS root crosschain certificate in PEM")
    public String generateBcdnsRootCert(
            @ShellOption(
                    help = "Version of crosschain certificate to generate",
                    defaultValue = CrossChainCertificateFactory.DEFAULT_VERSION
            ) String certVersion,
            @ShellOption(help = "ID for crosschain certificate", defaultValue = "mybcdns") String certId,
            @ShellOption(help = "Name of credential subject for crosschain certificate", defaultValue = "mybcdns") String credSubjectName,
            @ShellOption(
                    valueProvider = EnumValueProvider.class,
                    help = "Hash algorithm of issue proof for bcdns root crosschain certificate to generate",
                    defaultValue = "KECCAK_256"
            ) HashAlgoEnum hashAlgo,
            @ShellOption(
                    valueProvider = EnumValueProvider.class,
                    help = "Signature algorithm of crosschain certificate to generate",
                    defaultValue = "KECCAK256_WITH_SECP256K1"
            ) SignAlgoEnum signAlgo,
            @ShellOption(
                    valueProvider = EnumValueProvider.class,
                    help = "Type of object identity who owned the crosschain certificate",
                    defaultValue = "X509_PUBLIC_KEY_INFO"
            ) ObjectIdentityType oidType,
            @ShellOption(
                    valueProvider = FileValueProvider.class,
                    help = "Path to root public key for embedded BCDNS, default generate new public key with filename \"embedded-bcdns-root-pubkey.key\"",
                    defaultValue = ""
            ) String pubkeyFile,
            @ShellOption(
                    valueProvider = FileValueProvider.class,
                    help = "Path to root private key for embedded BCDNS, default generate new private key with filename \"embedded-bcdns-root-private-key.key\"",
                    defaultValue = ""
            ) String privateKeyFile,
            @ShellOption(
                    valueProvider = FileValueProvider.class,
                    help = "Directory path to save the files default current directory. Certificate would save as \"embedded-bcdns-root.crt\"",
                    defaultValue = ""
            ) String outDir
    ) {
        try {
            if (oidType == ObjectIdentityType.BID && SignAlgoEnum.ED25519 != signAlgo) {
                return "BID object identity only support Ed25519 for now";
            }

            ObjectIdentity oid;
            byte[] rootSubjectInfo = new byte[]{};
            PrivateKey privateKey;
            PublicKey publicKey;
            if (StrUtil.isAllNotEmpty(pubkeyFile, privateKeyFile)) {
                privateKey = signAlgo.getSigner().readPemPrivateKey(Files.readAllBytes(Paths.get(privateKeyFile)));
                publicKey = readPublicKeyFromPem(Files.readAllBytes(Paths.get(pubkeyFile)));
            } else {
                KeyPair keyPair = signAlgo.getSigner().generateKeyPair();
                privateKey = keyPair.getPrivate();
                publicKey = keyPair.getPublic();
                writePrivateKey(privateKey, Paths.get(outDir, EMBEDDED_BCDNS_PRIVATE_KEY_DEFAULT_FILENAME));
                writePublicKey(publicKey, Paths.get(outDir, EMBEDDED_BCDNS_PUBKEY_DEFAULT_FILENAME));
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
                Files.write(Paths.get(outDir, EMBEDDED_BCDNS_ROOT_BID_DOCUMENT), rootSubjectInfo);
            }

            AbstractCrossChainCertificate certificate = CrossChainCertificateFactory.createCrossChainCertificate(
                    certVersion,
                    certId,
                    oid,
                    DateUtil.currentSeconds(),
                    DateUtil.offsetDay(new Date(), 365).getTime() / 1000,
                    new BCDNSTrustRootCredentialSubject(
                            credSubjectName, oid, rootSubjectInfo
                    )
            );

            certificate.setProof(
                    new AbstractCrossChainCertificate.IssueProof(
                            hashAlgo,
                            hashAlgo.hash(certificate.getEncodedToSign()),
                            signAlgo,
                            signAlgo.getSigner().sign(privateKey, certificate.getEncodedToSign())
                    )
            );
            String rootCertPem = CrossChainCertificateUtil.formatCrossChainCertificateToPem(certificate);
            Files.write(Paths.get(outDir, EMBEDDED_BCDNS_ROOT_CERT), rootCertPem.getBytes());
            return StrUtil.format(
                    "your bcdns root cert is:\n{}{}{}",
                    rootCertPem,
                    StrUtil.format(
                            "your bcdns root cert file is {}{}",
                            Paths.get(outDir, EMBEDDED_BCDNS_ROOT_CERT).toAbsolutePath().toString(),
                            StrUtil.isAllEmpty(pubkeyFile, privateKeyFile) ? StrUtil.format(
                                    "\nyour bcdns root private key file is {}\n" +
                                            "your bcdns root public key file is {}",
                                    Paths.get(outDir, EMBEDDED_BCDNS_PRIVATE_KEY_DEFAULT_FILENAME).toAbsolutePath().toString(),
                                    Paths.get(outDir, EMBEDDED_BCDNS_PUBKEY_DEFAULT_FILENAME).toAbsolutePath().toString()
                            ) : ""
                    ), oidType == ObjectIdentityType.BID ?
                            StrUtil.format("\nyour bid document is {}", Paths.get(outDir, EMBEDDED_BCDNS_ROOT_BID_DOCUMENT).toAbsolutePath().toString()) : ""
            );
        } catch (Exception e) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", e);
        }
    }

    private String convertToBIFAddress(byte[] rawPublicKey) {
        PublicKeyManager publicKeyManager = new PublicKeyManager();
        publicKeyManager.setRawPublicKey(rawPublicKey);
        publicKeyManager.setKeyType(KeyType.ED25519);
        return publicKeyManager.getEncAddress();
    }

    private String convertToBIFPrivateKey(String privateKeyPem) {
        byte[] rawOctetStr = PrivateKeyInfo.getInstance(
                PemUtil.readPem(new ByteArrayInputStream(privateKeyPem.getBytes()))
        ).getPrivateKey().getOctets();
        KeyMember keyMember = new KeyMember();
        keyMember.setRawSKey(ArrayUtil.sub(rawOctetStr, 2, rawOctetStr.length));
        keyMember.setKeyType(KeyType.ED25519);
        return PrivateKeyManager.getEncPrivateKey(keyMember.getRawSKey(), keyMember.getKeyType());
    }

    @SneakyThrows
    private String getPemPublicKey(PublicKey publicKey) {
        StringWriter stringWriter = new StringWriter(256);
        JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
        jcaPEMWriter.writeObject(publicKey);
        jcaPEMWriter.close();
        return stringWriter.toString();
    }

    @SneakyThrows
    private PublicKey readPublicKeyFromPem(byte[] publicKeyPem) {
        SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(PemUtil.readPem(new ByteArrayInputStream(publicKeyPem)));
        return KeyUtil.generatePublicKey(
                AlgorithmId.get(keyInfo.getAlgorithm().getAlgorithm().getId()).getName(),
                keyInfo.getEncoded()
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
}
