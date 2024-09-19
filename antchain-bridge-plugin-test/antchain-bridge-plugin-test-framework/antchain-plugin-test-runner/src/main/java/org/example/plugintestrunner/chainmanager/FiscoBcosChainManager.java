package org.example.plugintestrunner.chainmanager;

import cn.hyperchain.sdk.account.Algo;
import com.alipay.antchain.bridge.plugins.hyperchain.HyperchainBBCService;
import lombok.Getter;
import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.response.BlockNumber;
import org.fisco.bcos.sdk.v3.config.ConfigOption;
import org.fisco.bcos.sdk.v3.config.model.CryptoMaterialConfig;

import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
public class FiscoBcosChainManager extends IChainManager{

    private final BcosSDK sdk;
    private final Client client;
    private String ca_cert;
    private String ssl_cert;
    private String ssl_key;
    private String group_id;
    private String config;

    private HyperchainBBCService hyperchainBBCService;


    public FiscoBcosChainManager(String confFile) {
        Path path = Paths.get(confFile).toAbsolutePath();
        sdk =  BcosSDK.build(path.toString());

        // 构造用于插件测试的配置信息
        CryptoMaterialConfig cryptoMaterialConfig = sdk.getConfig().getCryptoMaterialConfig();
        ca_cert = cryptoMaterialConfig.getCaCert();
        ssl_cert = cryptoMaterialConfig.getSdkCert();
        ssl_key = cryptoMaterialConfig.getSdkPrivateKey();
        group_id = sdk.getConfig().getNetworkConfig().getDefaultGroup();

        // 输出一下
        System.out.println("ca_cert: " + ca_cert);
        System.out.println("ssl_cert: " + ssl_cert);
        System.out.println("ssl_key: " + ssl_key);
        System.out.println("group_id: " + group_id);


        client = sdk.getClient(group_id);
        config = String.format("{\"caCert\":\"%s\",\"sslCert\":\"%s\",\"sslKey\":\"%s\",\"defaultGroup\":\"%s\"}",
                ca_cert, ssl_cert, ssl_key, group_id);
    }


    @Override
    public boolean isConnected() {
        try {
            client.getBlockNumber();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void close() {

    }
}
