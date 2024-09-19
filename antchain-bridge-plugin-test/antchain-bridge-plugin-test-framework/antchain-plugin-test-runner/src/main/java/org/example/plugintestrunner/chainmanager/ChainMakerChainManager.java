package org.example.plugintestrunner.chainmanager;

import com.alipay.antchain.bridge.plugins.chainmaker.ChainMakerConfig;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Getter
public class ChainMakerChainManager extends IChainManager{

//    @Getter
//    private ChainClient chainClient;
//    private ChainManager chainManager;
    private String config;


    public ChainMakerChainManager(String sdk_config, String chainmaker_json_file) throws Exception {
        ChainMakerConfigUtil.parseChainConfig(sdk_config, chainmaker_json_file);

        // 使用 StringBuilder 构建 JSON 字符串
        StringBuilder jsonStringBuilder = new StringBuilder();

        // 使用 try-with-resources 来确保流在使用后关闭
        try (InputStream inputStream = ChainMakerConfig.class.getClassLoader().getResourceAsStream(convertToClasspathPath(chainmaker_json_file));
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read chainmaker JSON file", e);
        }

        // 将读取的 JSON 文件内容赋值给 config
        config = jsonStringBuilder.toString();
//        Yaml yaml = new Yaml();
//        InputStream in = Files.newInputStream(Paths.get(config_file));
//        SdkConfig sdkConfig;
//        sdkConfig = yaml.loadAs(in, SdkConfig.class);
//        in.close();
//        for (NodeConfig nodeConfig : sdkConfig.getChainClient().getNodes()) {
//            List<byte[]> tlsCaCertList = new ArrayList<>();
//            if (nodeConfig.getTrustRootPaths() != null) {
//                for (String rootPath : nodeConfig.getTrustRootPaths()) {
//                    List<String> filePathList = FileUtils.getFilesByPath(rootPath);
//                    for (String filePath : filePathList) {
//                        tlsCaCertList.add(FileUtils.getFileBytes(filePath));
//                    }
//                }
//            }
//            byte[][] tlsCaCerts = new byte[tlsCaCertList.size()][];
//            tlsCaCertList.toArray(tlsCaCerts);
//            nodeConfig.setTrustRootBytes(tlsCaCerts);
//        }
//        chainManager = ChainManager.getInstance();
//        chainClient = chainManager.getChainClient(sdkConfig.getChainClient().getChainId());
//
//        if (chainClient == null) {
//            chainClient = chainManager.createChainClient(sdkConfig);
//        }

    }

    private String convertToClasspathPath(String fullPath) {
        // 定义要移除的前缀
        String prefix = "src/main/resources/";

        // 检查路径是否以该前缀开头
        if (fullPath.startsWith(prefix)) {
            // 去掉前缀，返回相对路径
            return fullPath.substring(prefix.length());
        }

        // 如果路径不包含前缀，直接返回原始路径
        return fullPath;
    }

    @Override
    public boolean isConnected() throws ExecutionException, InterruptedException, IOException {
        return true;
//        try {
//            String chainId = chainClient.getChainId();
//            System.out.println(chainId);
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
    }

    @Override
    public void close() {

    }
}
