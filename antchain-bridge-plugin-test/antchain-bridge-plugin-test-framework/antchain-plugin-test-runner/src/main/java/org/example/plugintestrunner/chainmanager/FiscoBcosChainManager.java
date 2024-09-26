package org.example.plugintestrunner.chainmanager;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
public class FiscoBcosChainManager extends IChainManager{

//    private final BcosSDK sdk;
//    private final Client client;
    private String ca_cert;
    private String ssl_cert;
    private String ssl_key;
    private String group_id;
    private String config;
    

    public FiscoBcosChainManager(String confDir) throws IOException {
        this.ca_cert = readFile(Paths.get(confDir, "ca.crt"));
        this.ssl_cert = readFile(Paths.get(confDir, "sdk.crt"));
        this.ssl_key = readFile(Paths.get(confDir, "sdk.key"));
        this.group_id = "group0";
        this.config = String.format("{\"caCert\":\"%s\",\"sslCert\":\"%s\",\"sslKey\":\"%s\",\"groupID\":\"%s\"}",
                ca_cert, ssl_cert, ssl_key, group_id);

//        输出
//        System.out.println("ca_cert: " + ca_cert);
//        System.out.println("ssl_cert: " + ssl_cert);
//        System.out.println("ssl_key: " + ssl_key);
//        System.out.println("group_id: " + group_id);
    }

    private String readFile(Path path) throws IOException {
        String ret = "";
        ret = new String(Files.readAllBytes(path));
        return ret;
    }

//    public FiscoBcosChainManager(String confFile) {
//        Path path = Paths.get(confFile).toAbsolutePath();
//        sdk =  BcosSDK.build(path.toString());
//
//        // 构造用于插件测试的配置信息
//        CryptoMaterialConfig cryptoMaterialConfig = sdk.getConfig().getCryptoMaterialConfig();
//        ca_cert = cryptoMaterialConfig.getCaCert();
//        ssl_cert = cryptoMaterialConfig.getSdkCert();
//        ssl_key = cryptoMaterialConfig.getSdkPrivateKey();
//        group_id = sdk.getConfig().getNetworkConfig().getDefaultGroup();
//
//        // 输出一下
//        System.out.println("ca_cert: " + ca_cert);
//        System.out.println("ssl_cert: " + ssl_cert);
//        System.out.println("ssl_key: " + ssl_key);
//        System.out.println("group_id: " + group_id);
//
//
//        client = sdk.getClient(group_id);
//        config = String.format("{\"caCert\":\"%s\",\"sslCert\":\"%s\",\"sslKey\":\"%s\",\"defaultGroup\":\"%s\"}",
//                ca_cert, ssl_cert, ssl_key, group_id);
//
//    }


    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void close() {

    }
}
