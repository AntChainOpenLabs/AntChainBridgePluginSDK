package org.example.plugintestrunner.chainmanager;

import lombok.Getter;
import org.chainmaker.pb.discovery.Discovery;
import org.chainmaker.sdk.ChainClient;
import org.chainmaker.sdk.ChainClientException;
import org.chainmaker.sdk.ChainManager;
import org.chainmaker.sdk.RpcServiceClientException;
import org.chainmaker.sdk.config.NodeConfig;
import org.chainmaker.sdk.config.SdkConfig;
import org.chainmaker.sdk.crypto.ChainMakerCryptoSuiteException;
import org.chainmaker.sdk.utils.FileUtils;
import org.chainmaker.sdk.utils.UtilsException;
import org.yaml.snakeyaml.Yaml;
import retrofit2.http.GET;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ChainMakerChainManager extends IChainManager{

    @Getter
    private ChainClient chainClient;
    private ChainManager chainManager;


    public ChainMakerChainManager(String config_file) throws IOException, UtilsException, ChainClientException, RpcServiceClientException, ChainMakerCryptoSuiteException {
        Yaml yaml = new Yaml();
        InputStream in = Files.newInputStream(Paths.get(config_file));
        SdkConfig sdkConfig;
        sdkConfig = yaml.loadAs(in, SdkConfig.class);
        in.close();
        for (NodeConfig nodeConfig : sdkConfig.getChainClient().getNodes()) {
            List<byte[]> tlsCaCertList = new ArrayList<>();
            if (nodeConfig.getTrustRootPaths() != null) {
                for (String rootPath : nodeConfig.getTrustRootPaths()) {
                    List<String> filePathList = FileUtils.getFilesByPath(rootPath);
                    for (String filePath : filePathList) {
                        tlsCaCertList.add(FileUtils.getFileBytes(filePath));
                    }
                }
            }
            byte[][] tlsCaCerts = new byte[tlsCaCertList.size()][];
            tlsCaCertList.toArray(tlsCaCerts);
            nodeConfig.setTrustRootBytes(tlsCaCerts);
        }
        chainManager = ChainManager.getInstance();
        chainClient = chainManager.getChainClient(sdkConfig.getChainClient().getChainId());

        if (chainClient == null) {
            chainClient = chainManager.createChainClient(sdkConfig);
        }
    }

    @Override
    public boolean isConnected() throws ExecutionException, InterruptedException, IOException {
        try {
            String chainId = chainClient.getChainId();
            System.out.println(chainId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void close() {

    }
}
