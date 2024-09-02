package org.example.plugintestrunner.config;

public class ChainConfig {

    public static class EthChainConfig extends ChainConfig {
        public static final String dataDir;
        public static final String httpAddr;
        public static final int httpPort;
        public static final String httpApi;
        public static final String privateKeyFile;

        static {
            ChainConfigManager config = ChainConfigManager.getInstance();
            dataDir = config.getProperty("ethereum.data_dir");
            httpAddr = config.getProperty("ethereum.http_addr");
            httpPort = Integer.parseInt(config.getProperty("ethereum.http_port"));
            httpApi = config.getProperty("ethereum.http_api");
            privateKeyFile = config.getProperty("ethereum.private_key_file");
        }

        public static String getHttpUrl() {
            return "http://" + httpAddr + ":" + httpPort;
        }
    }

    // TODO add more chains
}