package org.example.plugintestrunner.chainmanager;

import org.example.plugintestrunner.config.ChainConfig;
import org.example.plugintestrunner.config.ChainProduct;
import org.example.plugintestrunner.exception.ChainManagerException;
import org.example.plugintestrunner.exception.ChainManagerException.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class IChainManagerFactory {
    public static IChainManager createIChainManager(String chainType, String httpUrl, String privateKey, String gasPrice, String gasLimit) throws IOException, ChainManagerException {
        ChainProduct ct = ChainProduct.fromValue(chainType);
        switch (ct){
            case ETH:
            case TESTCHAIN:
                return new EthChainManager(httpUrl, privateKey, gasPrice, gasLimit);
            default:
                throw new ChainManagerConstructionException("Unsupported chain type: " + chainType);
        }
    }

    // 根据 product 创建 IChainManager
    public static IChainManager createIChainManager(String chainProduct) throws IOException, ChainManagerException {
        ChainProduct cp = ChainProduct.fromValue(chainProduct);
        switch (cp) {
            case ETH:
            case TESTCHAIN:
                return new EthChainManager(ChainConfig.EthChainConfig.getHttpUrl(), ChainConfig.EthChainConfig.privateKeyFile);
            // TODO add more chain products
            default:
                throw new ChainNotSupportedException("Unsupported chain product: " + chainProduct);
        }
    }
}
