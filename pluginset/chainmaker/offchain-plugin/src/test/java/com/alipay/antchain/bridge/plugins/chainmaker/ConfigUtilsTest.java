package com.alipay.antchain.bridge.plugins.chainmaker;

import com.google.gson.Gson;
import org.chainmaker.sdk.config.AuthType;
import org.chainmaker.sdk.config.ChainClientConfig;
import org.chainmaker.sdk.config.NodeConfig;
import org.chainmaker.sdk.config.SdkConfig;
import org.chainmaker.sdk.crypto.ChainMakerCryptoSuiteException;
import org.chainmaker.sdk.utils.CryptoUtils;
import org.chainmaker.sdk.utils.FileUtils;
import org.chainmaker.sdk.utils.UtilsException;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigUtilsTest {

    static String filePath = "chainmaker.json";

    static String SDK_CONFIG = "sdk_config.yml";

    private static final String ADMIN1_TLS_KEY_PATH = "src/test/resources/crypto-config/wx-org1.chainmaker.org/user/admin1/admin1.tls.key";
    private static final String ADMIN1_TLS_CERT_PATH = "src/test/resources/crypto-config/wx-org1.chainmaker.org/user/admin1/admin1.tls.crt";
    private static final String ADMIN2_TLS_KEY_PATH = "src/test/resources/crypto-config/wx-org2.chainmaker.org/user/admin1/admin1.tls.key";
    private static final String ADMIN2_TLS_CERT_PATH = "src/test/resources/crypto-config/wx-org2.chainmaker.org/user/admin1/admin1.tls.crt";
    private static final String ADMIN3_TLS_KEY_PATH = "src/test/resources/crypto-config/wx-org3.chainmaker.org/user/admin1/admin1.tls.key";
    private static final String ADMIN3_TLS_CERT_PATH = "src/test/resources/crypto-config/wx-org3.chainmaker.org/user/admin1/admin1.tls.crt";

    private static final String ADMIN1_KEY_PATH = "src/test/resources/crypto-config/wx-org1.chainmaker.org/user/admin1/admin1.sign.key";
    private static final String ADMIN1_CERT_PATH = "src/test/resources/crypto-config/wx-org1.chainmaker.org/user/admin1/admin1.sign.crt";
    private static final String ADMIN2_KEY_PATH = "src/test/resources/crypto-config/wx-org2.chainmaker.org/user/admin1/admin1.sign.key";
    private static final String ADMIN2_CERT_PATH = "src/test/resources/crypto-config/wx-org2.chainmaker.org/user/admin1/admin1.sign.crt";
    private static final String ADMIN3_KEY_PATH = "src/test/resources/crypto-config/wx-org3.chainmaker.org/user/admin1/admin1.sign.key";
    private static final String ADMIN3_CERT_PATH = "src/test/resources/crypto-config/wx-org3.chainmaker.org/user/admin1/admin1.sign.crt";

    private static final String ORG_ID1 = "wx-org1.chainmaker.org";
    private static final String ORG_ID2 = "wx-org2.chainmaker.org";
    private static final String ORG_ID3 = "wx-org3.chainmaker.org";


    @Test
    public void testGeneratorConfig() throws Exception {
        parseChainConfig();
    }

    public static void parseChainConfig() throws Exception {
        Yaml yaml = new Yaml();
        InputStream in = ConfigUtilsTest.class.getClassLoader().getResourceAsStream(SDK_CONFIG);

        SdkConfig sdkConfig;
        sdkConfig = yaml.loadAs(in, SdkConfig.class);
        assert in != null;
        in.close();

        ChainClientConfig chainClientConfig = sdkConfig.getChainClient();
        dealChainClientConfig(chainClientConfig);

        NodeConfig[] nodes = chainClientConfig.getNodes();
        NodeConfig[] newNodes = new NodeConfig[nodes.length];

        int nodesCount = nodes.length;
        for (int i = 0; i < nodesCount; i++) {
            NodeConfig nodeConfig = nodes[i];
            List<byte[]> tlsCaCertList = new ArrayList<>();

            if (nodeConfig.getTrustRootBytes() == null && nodeConfig.getTrustRootPaths() != null) {
                String[] trustRootPaths = nodeConfig.getTrustRootPaths();
                int trustRootPathsLen = trustRootPaths.length;
                int j = 0;
                while (true) {
                    if (j >= trustRootPathsLen) {
                        byte[][] tlsCaCerts = new byte[tlsCaCertList.size()][];
                        tlsCaCertList.toArray(tlsCaCerts);
                        nodeConfig.setTrustRootBytes(tlsCaCerts);
                        break;
                    }

                    String rootPath = trustRootPaths[j];
                    List<String> filePathList = FileUtils.getFilesByPath(rootPath);

                    for (String filePath : filePathList) {
                        tlsCaCertList.add(FileUtils.getFileBytes(filePath));
                    }
                    ++j;
                }
            }
            newNodes[i] = nodeConfig;
        }
        chainClientConfig.setNodes(newNodes);

        sdkConfig.setChainClient(chainClientConfig);
        Gson gson = new Gson();
        String sdkConfigString = gson.toJson(sdkConfig);
        ChainMakerConfig upperConfig = new ChainMakerConfig();

        upperConfig.setSdkConfig(sdkConfigString);
        upperConfig.setAdminTlsKeyPaths(
                new ArrayList<>(Arrays.asList(
                        FileUtils.getFileBytes(ADMIN1_TLS_KEY_PATH),
                        FileUtils.getFileBytes(ADMIN2_TLS_KEY_PATH),
                        FileUtils.getFileBytes(ADMIN3_TLS_KEY_PATH)))
        );
        upperConfig.setAdminTlsCertPaths(
                new ArrayList<>(Arrays.asList(
                        FileUtils.getFileBytes(ADMIN1_TLS_CERT_PATH),
                        FileUtils.getFileBytes(ADMIN2_TLS_CERT_PATH),
                        FileUtils.getFileBytes(ADMIN3_TLS_CERT_PATH)))
        );
        upperConfig.setAdminKeyPaths(
                new ArrayList<>(Arrays.asList(
                        FileUtils.getFileBytes(ADMIN1_KEY_PATH),
                        FileUtils.getFileBytes(ADMIN2_KEY_PATH),
                        FileUtils.getFileBytes(ADMIN3_KEY_PATH)))
        );
        upperConfig.setAdminCertPaths(
                new ArrayList<>(Arrays.asList(
                        FileUtils.getFileBytes(ADMIN1_CERT_PATH),
                        FileUtils.getFileBytes(ADMIN2_CERT_PATH),
                        FileUtils.getFileBytes(ADMIN3_CERT_PATH)))
        );
        upperConfig.setOrgIds(
                new ArrayList<>(Arrays.asList(
                        ORG_ID1,
                        ORG_ID2,
                        ORG_ID3))
        );

        String allConfigString = upperConfig.toJsonString();

        try (FileWriter writer = new FileWriter(filePath); BufferedWriter bufWriter = new BufferedWriter(writer)) {
            bufWriter.write(allConfigString);
        }

    }

    private static void dealChainClientConfig(ChainClientConfig chainClientConfig) throws UtilsException, ChainMakerCryptoSuiteException {
        String authType = chainClientConfig.getAuthType();
        byte[] userKeyBytes;
        if (!authType.equals(AuthType.PermissionedWithKey.getMsg()) && !authType.equals(AuthType.Public.getMsg())) {
            chainClientConfig.setAuthType(AuthType.PermissionedWithCert.getMsg());
            userKeyBytes = chainClientConfig.getUserKeyBytes();
            if (userKeyBytes == null && chainClientConfig.getUserKeyFilePath() != null) {
                chainClientConfig.setUserKeyBytes(FileUtils.getFileBytes(chainClientConfig.getUserKeyFilePath()));
            }

            byte[] userCrtBytes = chainClientConfig.getUserCrtBytes();
            if (userCrtBytes == null && chainClientConfig.getUserCrtFilePath() != null) {
                chainClientConfig.setUserCrtBytes(FileUtils.getFileBytes(chainClientConfig.getUserCrtFilePath()));
            }

            byte[] userSignKeyBytes = chainClientConfig.getUserSignKeyBytes();
            if (userSignKeyBytes == null && chainClientConfig.getUserSignKeyFilePath() != null) {
                chainClientConfig.setUserSignKeyBytes(FileUtils.getFileBytes(chainClientConfig.getUserSignKeyFilePath()));
            }

            byte[] userSignCrtBytes = chainClientConfig.getUserSignCrtBytes();
            if (userSignCrtBytes == null && chainClientConfig.getUserSignCrtFilePath() != null) {
                chainClientConfig.setUserSignCrtBytes(FileUtils.getFileBytes(chainClientConfig.getUserSignCrtFilePath()));
            }
        } else {
            userKeyBytes = FileUtils.getFileBytes(chainClientConfig.getUserSignKeyFilePath());
            PrivateKey privateKey = CryptoUtils.getPrivateKeyFromBytes(userKeyBytes);

            PublicKey publicKey;
            try {
                publicKey = CryptoUtils.getPublicKeyFromPrivateKey(privateKey);
            } catch (NoSuchProviderException | InvalidKeySpecException | NoSuchAlgorithmException nodesCount) {
                throw new ChainMakerCryptoSuiteException("Get publicKey from privateKey Error: " + nodesCount.getMessage());
            }

            chainClientConfig.setPublicKey(publicKey);
        }
    }
}
